package tech.harmonysoft.oss.traute.javac;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Names;
import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.traute.javac.common.CompilationUnitProcessingContext;
import tech.harmonysoft.oss.traute.javac.common.InstrumentationApplianceFinder;
import tech.harmonysoft.oss.traute.javac.common.ProblemReporter;
import tech.harmonysoft.oss.traute.javac.method.MethodReturnInstrumentator;
import tech.harmonysoft.oss.traute.javac.method.ReturnToInstrumentInfo;
import tech.harmonysoft.oss.traute.javac.parameter.ParameterInstrumentator;
import tech.harmonysoft.oss.traute.javac.parameter.ParameterToInstrumentInfo;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Arrays.asList;

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
     *     Compiler's option name to use for specifying custom {@code @NotNull} annotations to use
     *     ({@value #ANNOTATIONS_SEPARATOR}-separated).
     * </p>
     * <p>
     *     This is not mandatory setting as default annotations are used otherwise. Only given annotations are
     *     checked if this argument is specified.
     * </p>
     * <p>
     *     Example: consider a situation when given parameter's value is
     *     {@code 'org.eclipse.jdt.annotation.NonNull:android.support.annotation.NonNull'} (eclipse and android
     *     annotations). That means that a method which parameter is marked by, say
     *     {@code org.jetbrains.annotations.NotNull} won't trigger {@code null}-check generation by the plugin.
     * </p>
     */
    public static final String OPTION_ANNOTATIONS_NOT_NULL = "traute.annotations.not.null";

    private static final String ANNOTATIONS_SEPARATOR = ":";

    private static final Set<String> DEFAULT_ANNOTATIONS = new HashSet<>(asList(
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
    ));

    private final AtomicReference<ProblemReporter>          problemReporterRef      = new AtomicReference<>();
    private final Instrumentator<ParameterToInstrumentInfo> parameterInstrumentator = new ParameterInstrumentator();
    private final Instrumentator<ReturnToInstrumentInfo>    methodInstrumentator    = new MethodReturnInstrumentator();

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void init(JavacTask task, String... args) {
        if (!(task instanceof BasicJavacTask)) {
            throw new RuntimeException(ProblemReporter.getProblemMessage(String.format(
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
            public void finished(TaskEvent event) {
                if (event.getKind() != TaskEvent.Kind.PARSE) {
                    // The idea is to add our checks just after the parser builds an AST. Further on checks code
                    // will also be analyzed for errors and included into resulting binary.
                    return;
                }

                Log log = Log.instance(context);
                if (log == null) {
                    throw new RuntimeException(ProblemReporter.getProblemMessage(
                            "get a javac logger from the current javac context but got <null>"
                    ));
                }
                ProblemReporter problemReporter = getProblemReporter(log);

                CompilationUnitTree compilationUnit = event.getCompilationUnit();
                if (compilationUnit == null) {
                    problemReporter.reportDetails("get a prepared compilation unit object but got <null>");
                    return;
                }
                TreeMaker treeMaker = TreeMaker.instance(context);
                if (treeMaker == null) {
                    problemReporter.reportDetails("get an AST factory from the current javac context but got <null>");
                    return;
                }
                Names names = Names.instance(context);
                if (names == null) {
                    problemReporter.reportDetails("get a name table from the current javac context but got <null>");
                    return;
                }
                Set<String> notNullAnnotationsToUse = DEFAULT_ANNOTATIONS;
                JavacProcessingEnvironment environment = JavacProcessingEnvironment.instance(context);
                if (environment == null) {
                    problemReporter.report(String.format(
                            "Can't check if custom @NotNull annotation are specified (through a -A%s javac option) "
                            + "- can't get a %s instance from the current javac context. %s",
                            OPTION_ANNOTATIONS_NOT_NULL, JavacProcessingEnvironment.class.getName(),
                            ProblemReporter.getProblemMessageSuffix()));
                } else {
                    Map<String, String> options = environment.getOptions();
                    if (options != null) {
                        String customAnnotationsString = options.get(OPTION_ANNOTATIONS_NOT_NULL);
                        if (customAnnotationsString != null) {
                            customAnnotationsString = customAnnotationsString.trim();
                            String[] customAnnotations = customAnnotationsString.split(ANNOTATIONS_SEPARATOR);
                            if (customAnnotations.length > 0) {
                                notNullAnnotationsToUse = new HashSet<>(asList(customAnnotations));
                            }
                        }
                    }
                }
                try {
                    compilationUnit.accept(new InstrumentationApplianceFinder(
                            new CompilationUnitProcessingContext(notNullAnnotationsToUse,
                                                                 treeMaker,
                                                                 names,
                                                                 problemReporter),
                            parameterInstrumentator,
                            methodInstrumentator),null);
                } catch (Throwable e) {
                    StringWriter writer = new StringWriter();
                    e.printStackTrace(new PrintWriter(writer));
                    log.rawError(-1, String.format(
                            "Unexpected exception occurred on attempt to perform NotNull instrumentation for %s:%n%s",
                            event.getSourceFile(), writer
                    ));
                }
            }
        });
    }

    @NotNull
    private ProblemReporter getProblemReporter(@NotNull Log log) {
        ProblemReporter reporter = problemReporterRef.get();
        if (reporter != null && reporter.getLog() == log) {
            return reporter;
        }
        ProblemReporter result = new ProblemReporter(log);
        problemReporterRef.set(result);
        return result;
    }
}
