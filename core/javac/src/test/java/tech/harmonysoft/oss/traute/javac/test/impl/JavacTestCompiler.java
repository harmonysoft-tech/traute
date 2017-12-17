package tech.harmonysoft.oss.traute.javac.test.impl;

import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.traute.common.instrumentation.InstrumentationType;
import tech.harmonysoft.oss.traute.common.settings.TrautePluginSettings;
import tech.harmonysoft.oss.traute.common.util.TrauteConstants;
import tech.harmonysoft.oss.traute.test.api.engine.TestCompiler;
import tech.harmonysoft.oss.traute.test.api.model.ClassFile;
import tech.harmonysoft.oss.traute.test.api.model.CompilationResult;
import tech.harmonysoft.oss.traute.test.api.model.TestSource;
import tech.harmonysoft.oss.traute.test.impl.model.ClassFileImpl;
import tech.harmonysoft.oss.traute.test.impl.model.CompilationResultImpl;
import tech.harmonysoft.oss.traute.test.util.TestUtil;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.StringWriter;
import java.util.*;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static tech.harmonysoft.oss.traute.common.instrumentation.InstrumentationType.METHOD_PARAMETER;
import static tech.harmonysoft.oss.traute.common.instrumentation.InstrumentationType.METHOD_RETURN;
import static tech.harmonysoft.oss.traute.common.settings.TrautePluginSettingsBuilder.*;

public class JavacTestCompiler implements TestCompiler {

    public static final JavacTestCompiler INSTANCE = new JavacTestCompiler();

    @Override
    @NotNull
    public CompilationResult compile(@NotNull Collection<TestSource> testSources,
                                     @NotNull TrautePluginSettings settings)
    {
        StringWriter output = new StringWriter();

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        SimpleFileManager fileManager = new SimpleFileManager(compiler.getStandardFileManager(
                null,
                null,
                null
        ));
        List<SimpleSourceFile> compilationUnits = testSources.stream().map(SimpleSourceFile::new).collect(toList());
        List<String> arguments = new ArrayList<>();
        arguments.addAll(asList("-classpath", System.getProperty("java.class.path")));
        arguments.addAll(getAdditionalCompilerArgs(settings));
        JavaCompiler.CompilationTask task = compiler.getTask(output,
                                                             fileManager,
                                                             null,
                                                             arguments,
                                                             null,
                                                             compilationUnits);
        Boolean successfulCompilation = task.call();
        output.flush();
        if (!successfulCompilation) {
            return new CompilationResultImpl(() -> {
                throw new IllegalStateException(String.format(
                        "Failed to compile test class sources below:%n%n%s%nCompiler output: %s",
                        TestUtil.getSources(testSources), output));
                }, output.toString(), testSources);
        }

        Supplier<Collection<ClassFile>> compiledClassesSupplier = () -> {
            List<SimpleClassFile> compiledJavacClasses = fileManager.getCompiled();
            return compiledJavacClasses.stream().map(c -> {
                String className = c.getUri()
                                    .getSchemeSpecificPart()
                                    .replaceAll("/", "");
                return new ClassFileImpl(className, c.getCompiledBinaries());
            }).collect(toList());
        };

        return new CompilationResultImpl(compiledClassesSupplier, output.toString(), testSources);
    }

    @NotNull
    protected List<String> getAdditionalCompilerArgs(@NotNull TrautePluginSettings settings) {
        List<String> result = new ArrayList<>();
        result.add("-Xplugin:" + TrauteConstants.PLUGIN_NAME);

        Set<String> notNullAnnotations = settings.getNotNullAnnotations();
        if (!notNullAnnotations.equals(DEFAULT_NOT_NULL_ANNOTATIONS)) {
            String optionValue = notNullAnnotations.stream().collect(joining(TrauteConstants.SEPARATOR));
            result.add(String.format("-A%s=%s", TrauteConstants.OPTION_ANNOTATIONS_NOT_NULL, optionValue));
        }

        Set<String> nullableAnnotations = settings.getNullableAnnotations();
        if (!nullableAnnotations.equals(DEFAULT_NULLABLE_ANNOTATIONS)) {
            String optionValue = nullableAnnotations.stream().collect(joining(TrauteConstants.SEPARATOR));
            result.add(String.format("-A%s=%s", TrauteConstants.OPTION_ANNOTATIONS_NULLABLE, optionValue));
        }

        Set<InstrumentationType> instrumentationTypes = settings.getInstrumentationsToApply();
        if (!instrumentationTypes.equals(DEFAULT_INSTRUMENTATIONS_TO_APPLY)) {
            String optionValue = instrumentationTypes.stream()
                                                     .map(InstrumentationType::getShortName)
                                                     .collect(joining(TrauteConstants.SEPARATOR));
            result.add(String.format("-A%s=%s", TrauteConstants.OPTION_INSTRUMENTATIONS_TO_USE, optionValue));
        }

        settings.getLogFile().ifPresent(file -> result.add(
                String.format("-A%s=%s", TrauteConstants.OPTION_LOG_FILE, file.getAbsolutePath())
        ));

        for (InstrumentationType instrumentationType : instrumentationTypes) {
            String exceptionToThrow = settings.getExceptionToThrow(instrumentationType);
            if (!TrautePluginSettings.DEFAULT_EXCEPTION_TO_THROW.equals(exceptionToThrow)) {
                result.add(String.format("-A%s%s=%s",
                                         TrauteConstants.OPTION_PREFIX_EXCEPTION_TO_THROW,
                                         instrumentationType.getShortName(),
                                         exceptionToThrow));
            }
        }

        for (Map.Entry<InstrumentationType, String> entry : settings.getExceptionTextPatterns().entrySet()) {
            result.add(String.format("-A%s%s=%s",
                                     TrauteConstants.OPTION_PREFIX_EXCEPTION_TEXT,
                                     entry.getKey().getShortName(),
                                     entry.getValue()));
        }

        Map<InstrumentationType, Set<String>> notNullByDefaultAnnotations = settings.getNotNullByDefaultAnnotations();
        Set<String> parametersNotNullByDefaultAnnotations = notNullByDefaultAnnotations.get(METHOD_PARAMETER);
        if (!parametersNotNullByDefaultAnnotations.isEmpty()
            && !DEFAULT_PARAMETERS_NOT_NULL_BY_DEFAULT_ANNOTATIONS.equals(parametersNotNullByDefaultAnnotations))
        {
            String optionValue = parametersNotNullByDefaultAnnotations.stream().collect(joining(TrauteConstants.SEPARATOR));
            result.add(String.format("-A%s%s=%s",
                                     TrauteConstants.OPTION_PREFIX_ANNOTATIONS_NOT_NULL_BY_DEFAULT,
                                     METHOD_PARAMETER.getShortName(),
                                     optionValue));
        }
        Set<String> returnNotNullByDefaultAnnotations = notNullByDefaultAnnotations.get(METHOD_RETURN);
        if (!returnNotNullByDefaultAnnotations.isEmpty()
            && !DEFAULT_RETURN_NOT_NULL_BY_DEFAULT_ANNOTATIONS.equals(returnNotNullByDefaultAnnotations))
        {
            String optionValue = returnNotNullByDefaultAnnotations.stream().collect(joining(TrauteConstants.SEPARATOR));
            result.add(String.format("-A%s%s=%s",
                                     TrauteConstants.OPTION_PREFIX_ANNOTATIONS_NOT_NULL_BY_DEFAULT,
                                     METHOD_RETURN.getShortName(),
                                     optionValue));
        }

        boolean verboseLog = settings.isVerboseMode();
        if (verboseLog != DEFAULT_VERBOSE_MODE) {
            result.add(String.format("-A%s=true", TrauteConstants.OPTION_LOG_VERBOSE));
        }
        return result;
    }

    @Override
    public void release(@NotNull CompilationResult result) {
        // We don't need to do any cleanup as we compile into memory.
    }
}
