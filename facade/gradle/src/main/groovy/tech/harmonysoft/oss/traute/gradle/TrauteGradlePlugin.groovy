package tech.harmonysoft.oss.traute.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.PluginInstantiationException
import org.gradle.api.tasks.compile.JavaCompile
import tech.harmonysoft.oss.traute.common.instrumentation.InstrumentationType

import static tech.harmonysoft.oss.traute.common.util.TrauteConstants.*

class TrautePluginExtension {
    // One of the options below must be specified
    def javacPluginVersion
    def javacPluginSpec

    // Optional
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

        project.afterEvaluate {
            addTrauteDependency(project, extension)

            project.tasks.withType(JavaCompile) {
                applyOptions(options.compilerArgs, extension)
            }
        }
    }

    private static void addTrauteDependency(Project project, TrautePluginExtension extension) {
        if (extension.javacPluginSpec) {
            project.dependencies.add('compileOnly', extension.javacPluginSpec)
            project.dependencies.add('testCompileOnly', extension.javacPluginSpec)
            return
        }
        if (!extension.javacPluginVersion) {
            throw new PluginInstantiationException(
                    "Error on ${PLUGIN_NAME} plugin initialization - mandatory 'javacPluginVersion' option "
                            + "is undefined"
            )
        }
        project.dependencies.add('compileOnly', "tech.harmonysoft:traute-javac:$extension.javacPluginVersion")
        project.dependencies.add('testCompileOnly', "tech.harmonysoft:traute-javac:$extension.javacPluginVersion")
    }

    private static void applyOptions(compilerArgs, extension) {
        compilerArgs << "-Xplugin:${PLUGIN_NAME}"
        mayBeApplyNotNullAnnotations(compilerArgs, extension)
        mayBeApplyNullableAnnotations(compilerArgs, extension)
        mayBeApplyNotNullByDefaultAnnotations(compilerArgs, extension)
        mayBeApplyLoggingSettings(compilerArgs, extension)
        mayBeApplyLogFile(compilerArgs, extension)
        mayBeApplyInstrumentations(compilerArgs, extension)
        mayBeApplyExceptionsToThrow(compilerArgs, extension)
        mayBeApplyExceptionTexts(compilerArgs, extension)
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
}
