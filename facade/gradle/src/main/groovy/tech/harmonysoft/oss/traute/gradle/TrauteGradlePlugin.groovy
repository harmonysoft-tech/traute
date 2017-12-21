package tech.harmonysoft.oss.traute.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.PluginInstantiationException
import org.gradle.api.tasks.compile.CompileOptions
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.annotations.NotNull
import tech.harmonysoft.oss.traute.common.instrumentation.InstrumentationType
import tech.harmonysoft.oss.traute.common.settings.TrautePluginSettingsBuilder
import tech.harmonysoft.oss.traute.javac.log.TrautePluginLogger

import static tech.harmonysoft.oss.traute.common.util.TrauteConstants.*

class TrautePluginExtension {
    def notNullAnnotations
    def nullableAnnotations
    def notNullByDefaultAnnotations
    def instrumentations
    def exceptionsToThrow
    def exceptionTexts
    def logFile
    boolean verbose
}

class TrauteGradlePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        def extension = project.extensions.create('traute', TrautePluginExtension)
        def javacPluginFiles = getJavacPluginFiles(project)

        project.afterEvaluate {
            project.tasks.withType(JavaCompile) {
                applyOptions(project, javacPluginFiles, it, extension)
            }
        }
    }

    @NotNull
    private static FileCollection getJavacPluginFiles(@NotNull Project project) {
        def roots = [].toSet()
        roots << findRootInClassPath(TrautePluginLogger)
        roots << findRootInClassPath(TrautePluginSettingsBuilder)
        roots << findRootInClassPath('META-INF/services/com.sun.source.util.Plugin')
        return project.files(roots.collect { new File(it) })
    }

    private static void applyOptions(Project project, FileCollection javacPluginFiles, JavaCompile task, extension) {
        if (task.getClass().name.contains('ndroid')) {
            // This is an Android project
            project.dependencies.add('annotationProcessor', javacPluginFiles)
        } else {
            if (task.options.annotationProcessorPath) {
                task.options.annotationProcessorPath << javacPluginFiles
            } else {
                task.options.annotationProcessorPath = javacPluginFiles
            }
        }

        task.options.compilerArgs << "-Xplugin:${PLUGIN_NAME}"
        mayBeApplyNotNullAnnotations(task.options.compilerArgs, extension)
        mayBeApplyNullableAnnotations(task.options.compilerArgs, extension)
        mayBeApplyNotNullByDefaultAnnotations(task.options.compilerArgs, extension)
        mayBeApplyLoggingSettings(task.options.compilerArgs, extension)
        mayBeApplyLogFile(task.options.compilerArgs, extension)
        mayBeApplyInstrumentations(task.options.compilerArgs, extension)
        mayBeApplyExceptionsToThrow(task.options.compilerArgs, extension)
        mayBeApplyExceptionTexts(task.options.compilerArgs, extension)
    }

    private static void mayBeApplyNotNullAnnotations(compilerArgs, extension) {
        def notNullAnnotations = getListFromProperty(extension, 'notNullAnnotations')
        if (notNullAnnotations) {
            compilerArgs << "-A${OPTION_ANNOTATIONS_NOT_NULL}=${notNullAnnotations.join(SEPARATOR)}"
        }
    }

    private static void mayBeApplyNullableAnnotations(compilerArgs, extension) {
        def notNullAnnotations = getListFromProperty(extension, 'nullableAnnotations')
        if (notNullAnnotations) {
            compilerArgs << "-A${OPTION_ANNOTATIONS_NULLABLE}=${notNullAnnotations.join(SEPARATOR)}"
        }
    }

    private static void mayBeApplyNotNullByDefaultAnnotations(compilerArgs, extension) {
        if (!extension.notNullByDefaultAnnotations) {
            return
        }
        if (!(extension.notNullByDefaultAnnotations instanceof Map)) {
            throw new PluginInstantiationException(
                    "Error on ${PLUGIN_NAME} plugin initialization - expected to find a Map "
                            + "of strings at the 'notNullByDefaultAnnotations' property but found "
                            + "a ${extension.notNullByDefaultAnnotations.class.name} "
            )
        }
        extension.notNullByDefaultAnnotations.each { k, v ->
            def annotations = getListFromValue(v, "'notNullByDefaultAnnotations' value for key '$k'")
            compilerArgs << "-A${OPTION_PREFIX_ANNOTATIONS_NOT_NULL_BY_DEFAULT}${k}=${annotations.join(SEPARATOR)}"
        }
    }

    private static void mayBeApplyLogFile(compilerArgs, extension) {
        if (extension.logFile) {
            compilerArgs << "-A${OPTION_LOG_FILE}=${extension.logFile}"
        }
    }

    private static void mayBeApplyLoggingSettings(compilerArgs, extension) {
        if (extension.verbose) {
            compilerArgs << "-A${OPTION_LOG_VERBOSE}=true"
        }
    }

    private static void mayBeApplyInstrumentations(compilerArgs, extension) {
        def instrumentations = getListFromProperty(extension, 'instrumentations')
        if (!instrumentations) {
            return
        }

        // Validate parameters
        instrumentations.forEach { shortName ->
            if (!InstrumentationType.byShortName(shortName)) {
                throw new PluginInstantiationException(
                        "Error on ${PLUGIN_NAME} plugin initialization - unsupported instrumentation type is "
                                + "provided in the 'instrumentations' option - '$shortName'. "
                                + "Supported names: ${InstrumentationType.values().collect { it.shortName}}"
                )
            }
        }

        compilerArgs << "-A${OPTION_INSTRUMENTATIONS_TO_USE}=${instrumentations.join(SEPARATOR)}"
    }

    private static void mayBeApplyExceptionsToThrow(compilerArgs, extension) {
        if (!extension.exceptionsToThrow) {
            return
        }
        if (!(extension.exceptionsToThrow instanceof Map)) {
            throw new PluginInstantiationException(
                    "Error on ${PLUGIN_NAME} plugin initialization - expected to find a Map "
                            + "of strings at the 'exceptionsToThrow' property but found "
                            + "a ${extension.exceptionsToThrow.class.name} "
            )
        }
        extension.exceptionsToThrow.each { k, v ->
            compilerArgs << "-A${OPTION_PREFIX_EXCEPTION_TO_THROW}${k}=${v}"
        }
    }

    private static void mayBeApplyExceptionTexts(compilerArgs, extension) {
        if (!extension.exceptionTexts) {
            return
        }
        if (!(extension.exceptionTexts instanceof Map)) {
            throw new PluginInstantiationException(
                    "Error on ${PLUGIN_NAME} plugin initialization - expected to find a Map "
                            + "of strings at the 'exceptionTexts' property but found "
                            + "a ${extension.exceptionTexts.class.name} "
            )
        }
        extension.exceptionTexts.each { k, v ->
            compilerArgs << "-A${OPTION_PREFIX_EXCEPTION_TEXT}${k}=${v}"
        }
    }

    private static List<String> getListFromProperty(extension, propertyName) {
        return getListFromValue(extension[propertyName], "'$propertyName' property")
    }

    private static List<String> getListFromValue(value, description) {
        if (!value) {
            return []
        } else if (value instanceof CharSequence) {
            return [value as String]
        } else if (value instanceof List) {
            value.forEach {
                if (!(it instanceof CharSequence)) {
                    throw new PluginInstantiationException(
                            "Error on ${PLUGIN_NAME} plugin initialization - expected to find a list "
                                    + "of strings at the $description but found a ${it.class.name} "
                                    + "instance inside the list - '$it'"
                    )
                }
            }
            return value
        } else {
            throw new PluginInstantiationException(
                    "Error on ${PLUGIN_NAME} plugin initialization - expected to find a string or a "
                            + "list of string at the $description but got a ${value.class.name} "
                            + "instance - '$value'"
            )
        }
    }

    @NotNull
    static String findRootInClassPath(@NotNull Class<?> anchor) {
        return findRootInClassPath(anchor.name.replace('.', '/') + '.class')
    }

    @NotNull
    static String findRootInClassPath(@NotNull String anchor) {
        def url = TrauteGradlePlugin.class.classLoader.getResource(anchor)
        if (!url) {
            throw new IllegalStateException(
                    "Can't setup gradle test compiler - failed to find resource '$anchor' in classpath"
            )
        }

        def path = url.file
        if (!path) {
            throw new IllegalStateException(
                    "Can't setup gradle test compiler - failed to map classpath resource '$url' to a file"
            )
        }

        // When a resource is located inside a jar, an url looks like file://<my-path>/<my-jar>.jar!/<anchor>.
        // We want to reference a jar then
        def result = path.substring(0, path.indexOf(anchor))
        if (result.endsWith('/')) {
            result = result[0..-2]
        }
        if (result.endsWith('!')) {
            result = result[0..-2]
        }
        if (result.startsWith('file:')) {
            result = result.substring('file:'.length())
        }
        return result
    }
}
