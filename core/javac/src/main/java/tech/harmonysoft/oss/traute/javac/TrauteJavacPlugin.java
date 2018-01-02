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
import org.jetbrains.annotations.Nullable;
import tech.harmonysoft.oss.traute.common.instrumentation.InstrumentationType;
import tech.harmonysoft.oss.traute.common.settings.TrautePluginSettings;
import tech.harmonysoft.oss.traute.common.settings.TrautePluginSettingsBuilder;
import tech.harmonysoft.oss.traute.common.stats.StatsCollector;
import tech.harmonysoft.oss.traute.common.util.TrauteConstants;
import tech.harmonysoft.oss.traute.javac.common.CompilationUnitProcessingContext;
import tech.harmonysoft.oss.traute.javac.common.InstrumentationApplianceFinder;
import tech.harmonysoft.oss.traute.javac.common.PackageInfoManager;
import tech.harmonysoft.oss.traute.javac.instrumentation.Instrumentator;
import tech.harmonysoft.oss.traute.javac.instrumentation.method.MethodReturnInstrumentator;
import tech.harmonysoft.oss.traute.javac.instrumentation.method.ReturnToInstrumentInfo;
import tech.harmonysoft.oss.traute.javac.instrumentation.parameter.ParameterInstrumentator;
import tech.harmonysoft.oss.traute.javac.instrumentation.parameter.ParameterToInstrumentInfo;
import tech.harmonysoft.oss.traute.javac.log.AbstractLogger;
import tech.harmonysoft.oss.traute.javac.log.CompilerOutputLogger;
import tech.harmonysoft.oss.traute.javac.log.FileLogger;
import tech.harmonysoft.oss.traute.javac.log.TrautePluginLogger;
import tech.harmonysoft.oss.traute.javac.text.ExceptionTextGeneratorManager;

import javax.tools.JavaFileObject;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.reflect.Modifier.*;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static tech.harmonysoft.oss.traute.common.settings.TrautePluginSettingsBuilder.settingsBuilder;
import static tech.harmonysoft.oss.traute.common.util.TrauteConstants.OPTION_LOG_VERBOSE;
import static tech.harmonysoft.oss.traute.common.util.TrauteConstants.OPTION_PREFIX_ANNOTATIONS_NOT_NULL_BY_DEFAULT;
import static tech.harmonysoft.oss.traute.common.util.TrauteConstants.SEPARATOR;
import static tech.harmonysoft.oss.traute.javac.log.AbstractLogger.getProblemMessageSuffix;

/**
 * <p>A {@code javac} plugin which inserts {@code null}-checks for target method arguments and returns from method.</p>
 * <p><b><i>Method argument check example</i></b></p>
 * <p>Consider the sources below:</p>
 * <pre>
 * public void service(&#064;NotNull Data data) {
 *     // Method instructions
 * }
 * </pre>
 * <p>
 *     When this code is compiled with the current plugin enabled, resulting binary looks like if it's compiled
 *     from a source below:
 * </p>
 * <pre>
 * public void serve(&#064;NotNull Data data) {
 *     if (data == null) {
 *         throw new NullPointerException("Argument 's' of type Data is declared as &#064;NotNull but got null for it");
 *     }
 *     // Method instructions
 * }
 * </pre>
 * <i>Note: exact message text is slightly different in a way that it provides more details about the problem.</i>
 * <p><b><i>Method return type example</i></b></p>
 * <p>Consider the source below:</p>
 * <pre>
 * &#064;NotNull
 * public Data fetch() {
 *     return dao.fetch();
 * }
 * </pre>
 * <p>
 *     When it's compiled with the current plugin enabled, resulting binary looks like if it's compiled
 *     from a source below:
 * </p>
 * <pre>
 * &#064;NotNull
 * public Data fetch() {
 *     Data tmpVar1 = dao.fetch();
 *     if (tmpVar1 == null) {
 *         throw new NullPointerException("Detected an attempt to return null from a method marked by &#064;NotNull");
 *     }
 *     return tmpVar1;
 * }
 * </pre>
 * <i>Note: exact message text is slightly different in a way that it provides more details about the problem.</i>
 */
public class TrauteJavacPlugin implements Plugin {

    private final AtomicReference<WeakReference<AbstractLogger>> loggerRef             = new AtomicReference<>();
    private final AtomicReference<TrautePluginSettings>          pluginSettingsRef     = new AtomicReference<>();

    private final Instrumentator<ParameterToInstrumentInfo> parameterInstrumentator = new ParameterInstrumentator();
    private final Instrumentator<ReturnToInstrumentInfo>    methodInstrumentator    = new MethodReturnInstrumentator();
    private final Set<String>                               pluginOptionKeys        = new HashSet<>();
    private final PackageInfoManager                        packageInfoManager      = new PackageInfoManager();

    public TrauteJavacPlugin() {
        pluginOptionKeys.addAll(collectPluginOptionKeys());
    }

    @Override
    public String getName() {
        return TrauteConstants.PLUGIN_NAME;
    }

    @Override
    public void init(JavacTask task, String... args) {
        if (!(task instanceof BasicJavacTask)) {
            throw new RuntimeException(AbstractLogger.getProblemMessage(String.format(
                    "get an instance of type %s in init() method but got %s (%s)",
                    BasicJavacTask.class.getName(), task.getClass().getName(), task
            )));
        }
        Context context = ((BasicJavacTask) task).getContext();
        TrautePluginSettings settings = getPluginSettings(context);
        pluginSettingsRef.set(settings);
        task.addTaskListener(new TaskListener() {
            @Override
            public void started(TaskEvent event) {
                if (event.getKind() != TaskEvent.Kind.ENTER || isContextClosed()) {
                    // The idea is to add our checks just after the parser builds an AST. Further on the code
                    // will also be analyzed for errors and included into resulting binary.
                    // We don't apply the instrumentations after TaskEvent.Kind.PARSE event because there is
                    // a possible case that there are package-level annotations like
                    // javax.annotation.ParametersAreNonnullByDefault in package-info.java. So, we need to build
                    // AST for all input files first and instrument only after that.
                    return;
                }

                Log log = Log.instance(context);
                if (log == null) {
                    throw new RuntimeException(AbstractLogger.getProblemMessage(
                            "get a javac logger from the current javac context but got <null>"
                    ));
                }
                TrautePluginLogger logger = getPluginLogger(settings.getLogFile().orElse(null), log);
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
                                                                 statsCollector,
                                                                 new ExceptionTextGeneratorManager(logger),
                                                                 packageInfoManager),
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

            @Override
            public void finished(TaskEvent event) {
                if (event.getKind() != TaskEvent.Kind.PARSE || isContextClosed()) {
                    return;
                }
                Log log = Log.instance(context);
                if (log == null) {
                    throw new RuntimeException(AbstractLogger.getProblemMessage(
                            "get a javac logger from the current javac context but got <null>"
                    ));
                }
                TrautePluginLogger logger = getPluginLogger(settings.getLogFile().orElse(null), log);
                CompilationUnitTree compilationUnit = event.getCompilationUnit();
                if (compilationUnit == null) {
                    logger.reportDetails("get a prepared compilation unit object but got <null>");
                    return;
                }
                packageInfoManager.onCompilationUnit(compilationUnit);
            }

            /**
             * We encountered a situation when target context is closed (internal state is {@code null}) but plugin's
             * listener is called. That was the case for processing sources generated by an annotation processor.
             * Unfortunately, there is no public API to check that, hence, we use this trick
             * (see {@link Context#checkState(java.util.Map)}).
             *
             * @return      {@code true} if current context is closed; {@code false} otherwise
             */
            private boolean isContextClosed() {
                try {
                    Log.instance(context);
                    return false;
                } catch (Exception e) {
                    return true;
                }
            }
        });
    }

    @NotNull
    private TrautePluginLogger getPluginLogger(@Nullable File logFile, @Nullable Log log) {
        WeakReference<AbstractLogger> ref = loggerRef.get();
        AbstractLogger logger = null;
        if (logFile != null) {
            logger = new FileLogger(logFile);
        } else if (log != null) {
            logger = new CompilerOutputLogger(log);
        }
        if (ref != null) {
            AbstractLogger cached = ref.get();
            if (cached != null && logger != null && cached.getKey() == logger.getKey()) {
                return cached;
            }
        }
        if (logger == null) {
            throw new IllegalStateException(
                    "Can't create a logger instance - neither log file nor javac logger are specified"
            );
        }
        loggerRef.set(new WeakReference<>(logger));
        return logger;
    }

    @NotNull
    private TrautePluginSettings getPluginSettings(@NotNull Context context) {
        Log log = Log.instance(context);
        TrautePluginLogger logger = null;
        if (log != null) {
            logger = getPluginLogger(null, log);
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

        for (Map.Entry<String, String> entry : options.entrySet()) {
            if (entry.getKey().contains("traute") && !pluginOptionKeys.contains(entry.getKey())) {
                String error = String.format(
                        "Found an unknown setting '%s' with value '%s'. Probably a typo? Known settings: %s",
                        entry.getKey(), entry.getValue(), pluginOptionKeys
                );
                if (log == null) {
                    throw new RuntimeException(error);
                } else {
                    log.printRawLines(Log.WriterKind.ERROR, error);
                }
            }
        }

        String logFilePath = options.get(TrauteConstants.OPTION_LOG_FILE);
        if (logFilePath != null) {
            File file = new File(logFilePath);
            logger = new FileLogger(file);
            builder.withLogFile(file);
        }

        applyVerboseMode(logger, builder, options);
        applyNotNullAnnotations(logger, builder, options);
        applyNullableAnnotations(logger, builder, options);
        applyInstrumentations(logger, builder, options);
        applyExceptionsToThrow(logger, builder, options);
        applyExceptionTextPatterns(logger, builder, options);
        applyNotNullByDefaultAnnotations(logger, builder, options);

        return builder.build();
    }

    private void applyInstrumentations(@Nullable TrautePluginLogger logger,
                                       @NotNull TrautePluginSettingsBuilder builder,
                                       @NotNull Map<String, String> options)
    {
        String instrumentationsString = options.get(TrauteConstants.OPTION_INSTRUMENTATIONS_TO_USE);
        if (instrumentationsString == null) {
            return;
        }
        instrumentationsString = instrumentationsString.trim();
        String[] instrumentationNamesArray = instrumentationsString.split(SEPARATOR);
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

    private void applyNotNullAnnotations(@Nullable TrautePluginLogger logger,
                                         @NotNull TrautePluginSettingsBuilder builder,
                                         @NotNull Map<String, String> options)
    {
        String notNullAnnotationsString = options.get(TrauteConstants.OPTION_ANNOTATIONS_NOT_NULL);
        if (notNullAnnotationsString == null) {
            return;
        }
        notNullAnnotationsString = notNullAnnotationsString.trim();
        String[] notNullAnnotations = notNullAnnotationsString.split(SEPARATOR);
        if (notNullAnnotations.length > 0) {
            builder.withNotNullAnnotations(notNullAnnotations);
            if (logger != null) {
                logger.info("using the following NotNull annotations: " + Arrays.toString(notNullAnnotations));
            }
        }
    }

    private void applyNullableAnnotations(@Nullable TrautePluginLogger logger,
                                          @NotNull TrautePluginSettingsBuilder builder,
                                          @NotNull Map<String, String> options)
    {
        String annotationsString = options.get(TrauteConstants.OPTION_ANNOTATIONS_NULLABLE);
        if (annotationsString == null) {
            return;
        }
        annotationsString = annotationsString.trim();
        String[] annotations = annotationsString.split(SEPARATOR);
        if (annotations.length > 0) {
            builder.withNullableAnnotations(annotations);
            if (logger != null) {
                logger.info("using the following Nullable annotations: " + Arrays.toString(annotations));
            }
        }
    }

    private void applyExceptionsToThrow(@Nullable TrautePluginLogger logger,
                                        @NotNull TrautePluginSettingsBuilder builder,
                                        @NotNull Map<String, String> options)
    {
        for (Map.Entry<String, String> entry : options.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith(TrauteConstants.OPTION_PREFIX_EXCEPTION_TO_THROW)) {
                continue;
            }
            String instrumentationString = key.substring(TrauteConstants.OPTION_PREFIX_EXCEPTION_TO_THROW.length());
            InstrumentationType type = InstrumentationType.byShortName(instrumentationString);
            if (type != null) {
                builder.withExceptionToThrow(type, entry.getValue());
                if (logger != null) {
                    logger.info(String.format("using %s in '%s' checks", entry.getValue(), instrumentationString));
                }
            }
        }
    }

    private void applyExceptionTextPatterns(@Nullable TrautePluginLogger logger,
                                            @NotNull TrautePluginSettingsBuilder builder,
                                            @NotNull Map<String, String> options)
    {
        for (Map.Entry<String, String> entry : options.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith(TrauteConstants.OPTION_PREFIX_EXCEPTION_TEXT)) {
                continue;
            }
            String instrumentationString = key.substring(TrauteConstants.OPTION_PREFIX_EXCEPTION_TEXT.length());
            InstrumentationType type = InstrumentationType.byShortName(instrumentationString);
            if (type != null) {
                builder.withExceptionTextPattern(type, entry.getValue());
                if (logger != null) {
                    logger.info(String.format("using custom exception text generator with pattern '%s' "
                                              + "in '%s' checks", entry.getValue(), instrumentationString));
                }
            }
        }
    }

    private void applyNotNullByDefaultAnnotations(@Nullable TrautePluginLogger logger,
                                                  @NotNull TrautePluginSettingsBuilder builder,
                                                  @NotNull Map<String, String> options)
    {
        for (Map.Entry<String, String> entry : options.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith(OPTION_PREFIX_ANNOTATIONS_NOT_NULL_BY_DEFAULT)) {
                continue;
            }
            String instrumentationString = key.substring(OPTION_PREFIX_ANNOTATIONS_NOT_NULL_BY_DEFAULT.length());
            InstrumentationType type = InstrumentationType.byShortName(instrumentationString);
            if (type == null) {
                continue;
            }
            String[] annotationsArray = entry.getValue().split(SEPARATOR);
            List<String> annotations = Arrays.stream(annotationsArray)
                                             .map(String::trim).filter(a -> !a.isEmpty())
                                             .collect(toList());
            if (!annotations.isEmpty()) {
                builder.withNotNullByDefaultAnnotations(type, annotations);
                if (logger != null) {
                    logger.info(String.format("using the following NotNullByDefault annotations in '%s' checks: %s",
                                              type, annotations));
                }
            }
        }
    }

    private void applyVerboseMode(@Nullable TrautePluginLogger logger,
                                  @NotNull TrautePluginSettingsBuilder builder,
                                  @NotNull Map<String, String> options)
    {
        boolean verbose = "true".equalsIgnoreCase(options.get(OPTION_LOG_VERBOSE));
        if (verbose && logger != null) {
            logger.info("'verbose mode' is on");
        }
        builder.withVerboseMode(verbose);
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
        if (totalInstrumentationsNumber <= 0) {
            return;
        }
        StringBuilder details = new StringBuilder();
        for (InstrumentationType type : InstrumentationType.values()) {
            Long count = stats.get(type);
            if (count != null) {
                details.append(type).append(": ").append(count).append(", ");
            }
        }
        details.setLength(details.length() - 2);

        String fileName = file.toUri().getSchemeSpecificPart();
        while (fileName.startsWith("//")) {
            fileName = fileName.substring(1);
        }
        logger.info(String.format(
                "added %d instrumentation%s to the class %s - %s",
                totalInstrumentationsNumber, totalInstrumentationsNumber > 1 ? "s" : "", fileName, details)
        );
    }

    @NotNull
    private static Set<String> collectPluginOptionKeys() {
        Set<String> result = new HashSet<>();
        for (Field field : TrauteConstants.class.getFields()) {
            int modifiers = field.getModifiers();
            if ((modifiers & PUBLIC) == 0 || (modifiers & STATIC) == 0 || (modifiers & FINAL) == 0
                || field.getType() != String.class)
            {
                continue;
            }
            try {
                String value = field.get(null).toString();
                if (!value.contains("traute")) {
                    continue;
                }
                if (field.getName().contains("PREFIX")) {
                    for (InstrumentationType type : InstrumentationType.values()) {
                        result.add(value + type.getShortName());
                    }
                } else {
                    result.add(value);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(String.format(
                        "Unexpected exception on attempt to collect plugin option keys for %s.%s",
                        TrauteConstants.class.getName(), field.getName()
                ), e);
            }
        }
        return result;
    }
}
