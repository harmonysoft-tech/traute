package tech.harmonysoft.oss.traute.javac;
// TODO den test for java7

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
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.sun.tools.javac.util.List.nil;

public class TrautePlugin implements Plugin {

    public static final String NAME = "Traute";

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
                        // TODO den support NotNull returns
                        BlockTree bodyBlock = method.getBody();
                        if (!(bodyBlock instanceof JCTree.JCBlock)) {
                            report(context, getProblemMessage(String.format(
                                    "get a %s instance in the method AST but got %s",
                                    JCTree.JCBlock.class.getName(), bodyBlock.getClass().getName()
                            )));
                            return o;
                        }
                        JCTree.JCBlock jcBlock = (JCTree.JCBlock) bodyBlock;
                        SortedMap<Integer, VariableTree> variablesToCheck = new TreeMap<>();
                        int argumentIndex = 0;
                        for (VariableTree variable : method.getParameters()) {
                            if (variable != null && hasNotNullAnnotation(variable)) {
                                variablesToCheck.put(argumentIndex, variable);
                            }
                            argumentIndex++;
                        }
                        int argumentsNumber = method.getParameters().size();
                        for (Map.Entry<Integer, VariableTree> entry : variablesToCheck.entrySet()) {
                            addCheck(entry.getValue(), jcBlock, treeMaker, names, entry.getKey(), argumentsNumber);
                        }
                        return o;
                    }
                }, null);
            }
        });
    }

    private static boolean hasNotNullAnnotation(@NotNull VariableTree variable) {
        // TODO den ensure that it's a reference type
        ModifiersTree modifiers = variable.getModifiers();
        if (modifiers == null) {
            return false;
        }
        java.util.List<? extends AnnotationTree> annotations = modifiers.getAnnotations();
        if (annotations == null) {
            return false;
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
            // TODO den resolve annotation name if necessary and check it against a list of supported annotations - JavacProcessingEnvironment
            if (name.contentEquals("NotNull")) {
                return true;
            }
        }
        return false;
    }

    private static void addCheck(@NotNull VariableTree variable,
                                 @NotNull JCTree.JCBlock body,
                                 @NotNull JCTree.Factory factory,
                                 @NotNull Names names,
                                 int argumentIndex,
                                 int argumentsNumber)
    {
        String errorMessage = String.format("Argument '%s' of type %s (#%d out of %d, zero-based) is null",
                                      variable.getName(), variable.getType(), argumentIndex, argumentsNumber);
        JCTree.JCIf statementToAdd = factory.If(
                factory.Parens(
                        factory.Binary(
                                JCTree.Tag.EQ,
                                factory.Ident(
                                        names.fromString(variable.getName().toString())
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

    @NotNull
    private String getProblemMessage(@NotNull String details) {
        return String.format(
                "NotNull-instrumentation failed, it might be that javac implementation has significantly changed "
                + "- '%s' javac plugin expected to %s", getName(), details
        );
    }

    private void report(@NotNull Context context, @NotNull String message) {
        // Do not report a problem more than once
        if (!problemReported) {
            Log.instance(context).rawWarning(-1, message);
            problemReported = true;
        }
    }
}
