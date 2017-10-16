package tech.harmonysoft.oss.traute.javac;

import com.sun.source.tree.*;
import com.sun.source.util.*;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Names;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Name;
import java.util.*;

import static com.sun.tools.javac.util.List.nil;

/**
 * <p>A {@code javac} plugin which inserts {@code null}-checks for target method arguments and returns from method.</p>
 * <p>
 *     <i>Method argument check example</i>
 *     <p>
 *         Consider the sources below:
 *         <pre>
 *             public void service(&#064;NotNull Data data) {
 *                 // Method instructions
 *             }
 *         </pre>
 *         When this code is compiled with the current plugin enabled, resulting binary looks like if it's compiled
 *         from a source below:
 *         <pre>
 *             public void serve(&#064;NotNull Data data) {
 *                 if (data == null) {
 *                     throw new NullPointerException("Argument 's' of type Data is declared as &#064;NotNull but got null for it");
 *                 }
 *                 // Method instructions
 *             }
 *         </pre>
 *         <i>
 *             Note: exact message text is slightly different in a way that it provides more details about the problem.
 *         </i>
 *     </p>
 * </p>
 * <p>
 *     <i>Method return type example</i>
 *     <p>
 *         Consider the source below:
 *         <pre>
 *             &#064;NotNull
 *             public Data fetch() {
 *                 return dao.fetch();
 *             }
 *         </pre>
 *         When it's compiled with the current plugin enabled, resulting binary looks like if it's compiled
 *         from a source below:
 *         <pre>
 *             &#064;NotNull
 *             public Data fetch() {
 *                 Data tmpVar1 = dao.fetch();
 *                 if (tmpVar1 == null) {
 *                     throw new NullPointerException("Detected an attempt to return null from a method marked by &#064;NotNull");
 *                 }
 *                 return tmpVar1;
 *             }
 *         </pre>
 *         <i>
 *             Note: exact message text is slightly different in a way that it provides more details about the problem.
 *         </i>
 *     </p>
 * </p>
 */
public class TrauteJavacPlugin implements Plugin {

    /**
     * Current plugin's name. It should be used with the {@code -Xplugin} setting, i.e. {@code -Xplugin:Traute}
     * */
    public static final String NAME = "Traute";

    /**
     * <p>
     *     There is a possible case that javac implementation is changed not in a backward compatible way in
     *     a new release. This plugin might stop working then (e.g. new approach should be used for fetching
     *     {@code AST} builder).
     * </p>
     * <p>
     *     We don't want to generate numerous errors for the compilation then but report at most once.
     * </p>
     * <p>
     *     This flag allows to check if a problem has already been reported.
     * </p>
     */
    private boolean problemReported;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void init(JavacTask task, String... args) {
        if (!(task instanceof BasicJavacTask)) {
            throw new RuntimeException(getProblemMessage(String.format(
                    "get an instance of type %s in init() method but got %s (%s)",
                    BasicJavacTask.class.getName(), task.getClass().getName(), task
            )));
        }
        Context context = ((BasicJavacTask) task).getContext();
        task.addTaskListener(new TaskListener() {
            @Override
            public void started(TaskEvent e) {
            }

            @Override
            public void finished(TaskEvent e) {
                if (e.getKind() != TaskEvent.Kind.PARSE) {
                    // The idea is to add our checks just after the parser builds an AST. Further on checks code
                    // will also be analyzed for errors and included into resulting binary.
                    return;
                }

                CompilationUnitTree compilationUnit = e.getCompilationUnit();
                if (compilationUnit == null) {
                    report(context, getProblemMessage("get a prepared compilation unit object but got <null>"));
                    return;
                }
                TreeMaker treeMaker = TreeMaker.instance(context);
                if (treeMaker == null) {
                    report(context, getProblemMessage(
                            "get an AST factory from the current javac context but got <null>"
                    ));
                    return;
                }
                Names names = Names.instance(context);
                if (names == null) {
                    report(context, getProblemMessage(
                            "get a name table from the current javac context but got <null>"
                    ));
                    return;
                }
                compilationUnit.accept(new TreeScanner<Void, Void>() {
                    @Override
                    public Void visitMethod(MethodTree method, Void o) {
                        BlockTree bodyBlock = method.getBody();
                        if (!(bodyBlock instanceof JCTree.JCBlock)) {
                            report(context, getProblemMessage(String.format(
                                    "get a %s instance in the method AST but got %s",
                                    JCTree.JCBlock.class.getName(), bodyBlock.getClass().getName()
                            )));
                            return o;
                        }
                        JCTree.JCBlock jcBlock = (JCTree.JCBlock) bodyBlock;
                        SortedSet<ParameterToInstrumentInfo> variablesToCheck = new TreeSet<>();
                        int argumentIndex = 0;
                        for (VariableTree variable : method.getParameters()) {
                            if (variable == null) {
                                continue;
                            }
                            Optional<String> annotation = findNotNullAnnotation(variable);
                            if (annotation.isPresent()) {
                                variablesToCheck.add(new ParameterToInstrumentInfo(annotation.get(),
                                                                                   variable,
                                                                                   argumentIndex));
                            }
                            argumentIndex++;
                        }
                        int argumentsNumber = method.getParameters().size();
                        for (ParameterToInstrumentInfo info : variablesToCheck) {
                            addCheck(info, jcBlock, treeMaker, names, argumentsNumber);
                        }
                        return o;
                    }
                }, null);
            }
        });
    }

    /**
     * Checks if given method parameter {@code AST} element is marked by any configured {@code @NotNull} annotation.
     *
     * @param variable  method parameter {@code AST} element to check
     * @return          target annotation's name in case the one is found
     */
    @NotNull
    private static Optional<String> findNotNullAnnotation(@NotNull VariableTree variable) {
        ModifiersTree modifiers = variable.getModifiers();
        if (modifiers == null) {
            return Optional.empty();
        }
        java.util.List<? extends AnnotationTree> annotations = modifiers.getAnnotations();
        if (annotations == null) {
            return Optional.empty();
        }
        for (AnnotationTree annotation : annotations) {
            Tree type = annotation.getAnnotationType();
            if (type == null) {
                continue;
            }
            Name name = type.accept(new TreeScanner<Name, Void>() {
                @Override
                public Name visitIdentifier(IdentifierTree node, Void aVoid) {
                    return node.getName();
                }
            }, null);
            if (name.contentEquals("NotNull")) {
                return Optional.of("NotNull");
            }
        }
        return Optional.empty();
    }

    /**
     * Enhances given method {@code AST} element in a way to include a {@code null}-check for the target
     * method parameter.
     *
     * @param parameterInfo         an object which identifies a method parameter marked by a {@code @NotNull} annotation
     * @param body                  {@code AST} element for the method's body
     * @param factory               {@code AST} factory to use
     * @param names                 {@code name table} to use
     * @param argumentsNumber       total number of target method's parameters
     */
    private static void addCheck(@NotNull TrauteJavacPlugin.ParameterToInstrumentInfo parameterInfo,
                                 @NotNull JCTree.JCBlock body,
                                 @NotNull JCTree.Factory factory,
                                 @NotNull Names names,
                                 int argumentsNumber)
    {
        String errorMessage = String.format(
                "Argument '%s' of type %s (#%d out of %d, zero-based) is marked by @%s but got null for it",
                parameterInfo.variable.getName(), parameterInfo.variable.getType(), parameterInfo.methodArgumentIndex,
                argumentsNumber, parameterInfo.annotationName
        );
        JCTree.JCIf statementToAdd = factory.If(
                factory.Parens(
                        factory.Binary(
                                JCTree.Tag.EQ,
                                factory.Ident(
                                        names.fromString(parameterInfo.variable.getName().toString())
                                ),
                                factory.Literal(TypeTag.BOT, null))
                ),
                factory.Block(0, List.of(
                        factory.Throw(
                                factory.NewClass(
                                        null,
                                        nil(),
                                        factory.Ident(
                                                names.fromString("NullPointerException")
                                        ),
                                        List.of(factory.Literal(TypeTag.CLASS, errorMessage)),
                                        null
                                )
                        )
                )),
                null
        );
        body.stats = body.stats.prepend(statementToAdd);
    }

    /**
     * Prepares a problem message to show end-user in case the plugin can't do the job during compilation.
     *
     * @param details   exact problem details, will be appended to the general problem description suffix
     * @return          a problem message to use
     */
    @NotNull
    private String getProblemMessage(@NotNull String details) {
        return String.format(
                "NotNull-instrumentation failed, it might be that javac implementation has significantly changed "
                + "- '%s' javac plugin expected to %s", getName(), details
        );
    }

    /**
     * Shows given problem message to the end-user if necessary.
     *
     * @param context   current javac context
     * @param message   a problem message to show
     */
    private void report(@NotNull Context context, @NotNull String message) {
        // Do not report a problem more than once
        if (!problemReported) {
            Log.instance(context).rawWarning(-1, message);
            problemReported = true;
        }
    }

    /**
     * A utility data class for describing a variable marked by a {@code @NotNull} annotation.
     */
    private static class ParameterToInstrumentInfo implements Comparable<ParameterToInstrumentInfo> {

        /** Target annotation name, e.g. {@code NotNull} or {@code Nonnull}. */
        @NotNull public final String annotationName;
        /** An {@code AST} element for the target variable. */
        @NotNull public final VariableTree variable;
        /** Target method parameter's index in the whole method parameters list (zero-based). */
        public final int methodArgumentIndex;

        public ParameterToInstrumentInfo(@NotNull String annotationName,
                                         @NotNull VariableTree variable,
                                         int methodArgumentIndex)
        {
            this.annotationName = annotationName;
            this.variable = variable;
            this.methodArgumentIndex = methodArgumentIndex;
        }

        @Override
        public int compareTo(@NotNull TrauteJavacPlugin.ParameterToInstrumentInfo that) {
            return that.methodArgumentIndex - methodArgumentIndex;
        }
    }
}
