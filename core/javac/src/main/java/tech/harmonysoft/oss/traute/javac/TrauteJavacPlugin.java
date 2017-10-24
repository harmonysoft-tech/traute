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
import tech.harmonysoft.oss.traute.common.instrumentation.InstrumentationType;
import tech.harmonysoft.oss.traute.common.settings.TrautePluginSettings;
import tech.harmonysoft.oss.traute.common.settings.TrautePluginSettingsBuilder;
import tech.harmonysoft.oss.traute.common.stats.StatsCollector;
import tech.harmonysoft.oss.traute.common.util.TrauteConstants;
import tech.harmonysoft.oss.traute.javac.common.CompilationUnitProcessingContext;
import tech.harmonysoft.oss.traute.javac.common.InstrumentationApplianceFinder;
import tech.harmonysoft.oss.traute.javac.common.TrautePluginLogger;
import tech.harmonysoft.oss.traute.javac.instrumentation.Instrumentator;
import tech.harmonysoft.oss.traute.javac.instrumentation.method.MethodReturnInstrumentator;
import tech.harmonysoft.oss.traute.javac.instrumentation.method.ReturnToInstrumentInfo;
import tech.harmonysoft.oss.traute.javac.instrumentation.parameter.ParameterInstrumentator;
import tech.harmonysoft.oss.traute.javac.instrumentation.parameter.ParameterToInstrumentInfo;

import javax.tools.JavaFileObject;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.stream.Collectors.joining;
import static tech.harmonysoft.oss.traute.common.settings.TrautePluginSettingsBuilder.settingsBuilder;
import static tech.harmonysoft.oss.traute.javac.common.TrautePluginLogger.getProblemMessageSuffix;

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

    private final AtomicReference<WeakReference<TrautePluginLogger>> loggerRef         = new AtomicReference<>();
    private final AtomicReference<TrautePluginSettings>              pluginSettingsRef = new AtomicReference<>();

    private final Instrumentator<ParameterToInstrumentInfo> parameterInstrumentator = new ParameterInstrumentator();
    private final Instrumentator<ReturnToInstrumentInfo>    methodInstrumentator    = new MethodReturnInstrumentator();

    @Override
    public String getName() {
        return TrauteConstants.PLUGIN_NAME;
    }

    @Override
    public void init(JavacTask task, String... args) {
        if (!(task instanceof BasicJavacTask)) {
            throw new RuntimeException(TrautePluginLogger.getProblemMessage(String.format(
                    "get an instance of type %s in init() method but got %s (%s)",
                    BasicJavacTask.class.getName(), task.getClass().getName(), task
            )));
        }
        Context context = ((BasicJavacTask) task).getContext();
        pluginSettingsRef.set(getPluginSettings(context));
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
                    throw new RuntimeException(TrautePluginLogger.getProblemMessage(
                            "get a javac logger from the current javac context but got <null>"
                    ));
                }
                TrautePluginLogger logger = getPluginLogger(log);

                CompilationUnitTree compilationUnit = event.getCompilationUnit();
                if (compilationUnit == null) {
                    logger.reportDetails("get a prepared compilation unit object but got <null>");
                    return;
                }
                TreeMaker treeMaker = TreeMaker.instance(context);
                if (treeMaker == null) {
                    logger.reportDetails("get an AST factory from the current javac context but got <null>");
                    return;
                }
                Names names = Names.instance(context);
                if (names == null) {
                    logger.reportDetails("get a name table from the current javac context but got <null>");
                    return;
                }
                TrautePluginSettings pluginSettings = pluginSettingsRef.get();
                StatsCollector statsCollector = new StatsCollector();
                try {
                    compilationUnit.accept(new InstrumentationApplianceFinder(
                            new CompilationUnitProcessingContext(pluginSettings,
                                                                 treeMaker,
                                                                 names,
                                                                 logger,
                                                                 statsCollector),
                            parameterInstrumentator,
                            methodInstrumentator),null);
                    if (pluginSettings.isVerboseMode()) {
                        printInstrumentationResults(compilationUnit.getSourceFile(), statsCollector, logger);
                    }
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
    private TrautePluginLogger getPluginLogger(@NotNull Log log) {
        WeakReference<TrautePluginLogger> reporterRef = loggerRef.get();
        if (reporterRef != null) {
            TrautePluginLogger logger = reporterRef.get();
            if (logger != null && logger.getLog() == log) {
                return logger;
            }
        }
        TrautePluginLogger result = new TrautePluginLogger(log);
        loggerRef.set(new WeakReference<>(result));
        return result;
    }

    @NotNull
    private TrautePluginSettings getPluginSettings(@NotNull Context context) {
        Log log = Log.instance(context);
        TrautePluginLogger logger = null;
        if (log != null) {
            logger = getPluginLogger(log);
        }

        TrautePluginSettingsBuilder builder = settingsBuilder();

        JavacProcessingEnvironment environment = JavacProcessingEnvironment.instance(context);
        if (environment == null) {
            if (logger != null) {
                logger.report(String.format(
                        "Can't read plugin settings from the javac command line arguments - expected to find a %s "
                        + "instance in the javac context but it doesn't there. %s",
                        JavacProcessingEnvironment.class.getName(), getProblemMessageSuffix()
                ));
            }
            // Use default settings
            return builder.build();
        }

        Map<String, String> options = environment.getOptions();
        if (options == null) {
            if (logger != null) {
                logger.info("No plugin settings are detected at the javac command line. Using default values");
            }
            // Use default settings
            return builder.build();
        }

        boolean verbose = "true".equalsIgnoreCase(options.get(TrauteConstants.OPTION_LOG_VERBOSE));
        if (verbose && logger != null) {
            logger.info("'verbose mode' is on");
        }
        builder.withVerboseMode(verbose);

        String notNullAnnotationsString = options.get(TrauteConstants.OPTION_ANNOTATIONS_NOT_NULL);
        if (notNullAnnotationsString != null) {
            notNullAnnotationsString = notNullAnnotationsString.trim();
            String[] notNullAnnotations = notNullAnnotationsString.split(TrauteConstants.SEPARATOR);
            if (notNullAnnotations.length > 0) {
                builder.withNotNullAnnotations(notNullAnnotations);
                if (logger != null) {
                    logger.info("using the following NotNull annotations: " + Arrays.toString(notNullAnnotations));
                }
            }
        }

        String instrumentationsString = options.get(TrauteConstants.OPTION_INSTRUMENTATIONS_TO_USE);
        if (instrumentationsString != null) {
            instrumentationsString = instrumentationsString.trim();
            String[] instrumentationNamesArray = instrumentationsString.split(TrauteConstants.SEPARATOR);
            for (String instrumentationShortName : instrumentationNamesArray) {
                InstrumentationType type = InstrumentationType.byShortName(instrumentationShortName.trim());
                if (type == null) {
                    if (logger != null) {
                        String knownTypes = Arrays.stream(InstrumentationType.values())
                                                  .map(InstrumentationType::getShortName)
                                                  .collect(joining(", "));
                        logger.report(String.format(
                                "Unknown instrumentation type is defined through the '%s' option - '%s'. "
                                + "Known types: %s",
                                TrauteConstants.OPTION_INSTRUMENTATIONS_TO_USE, instrumentationShortName, knownTypes
                        ));
                    }
                } else {
                    builder.withInstrumentationToApply(type);
                }
                if (logger != null) {
                    logger.info("using the following instrumentations: " + Arrays.toString(instrumentationNamesArray));
                }
            }
        }

        return builder.build();
    }

    private void printInstrumentationResults(@NotNull JavaFileObject file,
                                             @NotNull StatsCollector statsCollector,
                                             @NotNull TrautePluginLogger logger)
    {

        ConcurrentMap<InstrumentationType, Long> stats = statsCollector.getStats();
        long totalInstrumentationsNumber = stats.entrySet()
                                                .stream()
                                                .mapToLong(Map.Entry::getValue)
                                                .sum();
        StringBuilder details = new StringBuilder();
        if (totalInstrumentationsNumber > 0) {
            details.append(" - ");
            boolean first = true;
            for (InstrumentationType type : InstrumentationType.values()) {
                Long count = stats.get(type);
                if (count != null) {
                    if (first) {
                        first = false;
                    } else {
                        details.append(", ");
                    }
                    details.append(type).append(": ").append(count);
                }
            }
        }

        String fileName = file.toUri().getSchemeSpecificPart();
        while (fileName.startsWith("//")) {
            fileName = fileName.substring(1);
        }
        logger.info(String.format(
                "added %d instrumentation%s to the %s%s",
                totalInstrumentationsNumber, totalInstrumentationsNumber > 1 ? "s" : "", fileName, details)
        );
    }
}
