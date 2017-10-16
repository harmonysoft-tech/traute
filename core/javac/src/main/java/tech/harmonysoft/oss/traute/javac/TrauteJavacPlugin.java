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

import java.util.*;

import static com.sun.tools.javac.util.List.nil;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;

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

    private static final Set<String> DEFAULT_ANNOTATIONS = unmodifiableSet(new HashSet<>(asList(
            // Used by IntelliJ by default - https://www.jetbrains.com/help/idea/nullable-and-notnull-annotations.html
            "org.jetbrains.annotations.NotNull",

            // JSR-305 - status=dormant - https://jcp.org/en/jsr/detail?id=305
            "javax.annotation.Nonnull",

            // JavaEE - https://docs.oracle.com/javaee/7/api/javax/validation/constraints/NotNull.html
            "javax.validation.constraints.NotNull",

            // FindBugs - http://findbugs.sourceforge.net/api/edu/umd/cs/findbugs/annotations/NonNull.html
            "edu.umd.cs.findbugs.annotations.NonNull",

            // Android - https://developer.android.com/reference/android/support/annotation/NonNull.html
            "android.support.annotation.NonNull",

            // Eclipse - http://help.eclipse.org/oxygen/index.jsp?topic=%2Forg.eclipse.jdt.doc.user%2Ftasks%2Ftask-using_null_annotations.htm
            "org.eclipse.jdt.annotation.NonNull"
    )));

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

                Log log = Log.instance(context);
                if (log == null) {
                    throw new RuntimeException(getProblemMessage(
                            "get a javac logger from the current javac context but got <null>"
                    ));
                }

                CompilationUnitTree compilationUnit = e.getCompilationUnit();
                if (compilationUnit == null) {
                    report(log, getProblemMessage("get a prepared compilation unit object but got <null>"));
                    return;
                }
                TreeMaker treeMaker = TreeMaker.instance(context);
                if (treeMaker == null) {
                    report(log, getProblemMessage(
                            "get an AST factory from the current javac context but got <null>"
                    ));
                    return;
                }
                Names names = Names.instance(context);
                if (names == null) {
                    report(log, getProblemMessage(
                            "get a name table from the current javac context but got <null>"
                    ));
                    return;
                }
                process(new PluginContext(DEFAULT_ANNOTATIONS, compilationUnit, treeMaker, names, log));
            }
        });
    }

    private void process(@NotNull PluginContext context) {
        context.ast.accept(new TreeScanner<Void, Void>() {
            @Override
            public Void visitImport(ImportTree node, Void v) {
                if (!node.isStatic()) {
                    context.addImport(node.getQualifiedIdentifier().toString());
                }
                return v;
            }

            @Override
            public Void visitMethod(MethodTree method, Void v) {
                BlockTree bodyBlock = method.getBody();
                if (!(bodyBlock instanceof JCTree.JCBlock)) {
                    report(context.log, getProblemMessage(String.format(
                            "get a %s instance in the method AST but got %s",
                            JCTree.JCBlock.class.getName(), bodyBlock.getClass().getName()
                    )));
                    return v;
                }
                JCTree.JCBlock jcBlock = (JCTree.JCBlock) bodyBlock;
                SortedSet<ParameterToInstrumentInfo> variablesToCheck = new TreeSet<>();
                int argumentIndex = 0;
                for (VariableTree variable : method.getParameters()) {
                    if (variable == null) {
                        continue;
                    }
                    Optional<String> annotation = findNotNullAnnotation(variable, context.imports, context.annotations);
                    if (annotation.isPresent()) {
                        variablesToCheck.add(new ParameterToInstrumentInfo(annotation.get(),
                                                                           variable,
                                                                           argumentIndex));
                    }
                    argumentIndex++;
                }
                int argumentsNumber = method.getParameters().size();
                for (ParameterToInstrumentInfo info : variablesToCheck) {
                    addCheck(info, jcBlock, context.astFactory, context.symbolsTable, argumentsNumber);
                }
                return v;
            }
        }, null);
    }

    /**
     * Checks if given method parameter {@code AST} element is marked by any configured {@code @NotNull} annotation.
     *
     * @param variable  method parameter {@code AST} element to check
     * @param imports   current source's imports
     * @return          target annotation's name in case the one is found
     */
    @NotNull
    private static Optional<String> findNotNullAnnotation(@NotNull VariableTree variable,
                                                          @NotNull Set<String> imports,
                                                          @NotNull Set<String> supportedAnnotations)
    {
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
            String parameterAnnotation = annotation.getAnnotationType().toString();
            if (supportedAnnotations.contains(parameterAnnotation)) {
                // Qualified annotation, like 'void test(@javax.annotation.Nonnul String s) {}'
                return Optional.of(parameterAnnotation);
            }
            for (String anImport : imports) {
                // Support an import like 'import org.jetbrains.annotations.*;'
                if (anImport.endsWith(".*")) {
                    String candidate = anImport.substring(0, anImport.length() - 1) + parameterAnnotation;
                    if (supportedAnnotations.contains(candidate)) {
                        return Optional.of(candidate);
                    }
                    continue;
                }
                if (!supportedAnnotations.contains(anImport)) {
                    continue;
                }
                if (anImport.endsWith(parameterAnnotation)) {
                    return Optional.of(anImport);
                }
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
     * @param log       {@code javac} logger
     * @param message   a problem message to show
     */
    private void report(@NotNull Log log, @NotNull String message) {
        // Do not report a problem more than once
        if (!problemReported) {
            log.rawWarning(-1, message);
            problemReported = true;
        }
    }

    private static class PluginContext {

        public final Set<String> annotations = new HashSet<>();
        public final Set<String> imports     = new HashSet<>();

        @NotNull public final CompilationUnitTree ast;
        @NotNull public final TreeMaker           astFactory;
        @NotNull public final Names               symbolsTable;
        @NotNull public final Log                 log;

        public PluginContext(@NotNull Collection<String> annotations,
                             @NotNull CompilationUnitTree ast,
                             @NotNull TreeMaker astFactory,
                             @NotNull Names symbolsTable,
                             @NotNull Log log)
        {
            this.log = log;
            this.annotations.addAll(annotations);
            this.ast = ast;
            this.astFactory = astFactory;
            this.symbolsTable = symbolsTable;
        }

        public void addImport(@NotNull String importText) {
            imports.add(importText);
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
