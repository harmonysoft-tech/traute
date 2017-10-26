package tech.harmonysoft.oss.traute.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.PluginInstantiationException
import org.gradle.api.tasks.compile.JavaCompile

import static tech.harmonysoft.oss.traute.common.util.TrauteConstants.*

class TrautePluginExtension {
    // One of the options below must be specified
    def javacPluginVersion
    def javacPluginSpec

    // Optional
    def notNullAnnotations
    def instrumentations
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
        project.dependencies.add('compileOnly', "tech.harmonysoft:traute-javac-plugin:$extension.javacPluginVersion")
        project.dependencies.add('testCompileOnly', "tech.harmonysoft:traute-javac-plugin:$extension.javacPluginVersion")
    }

    private static void applyOptions(compilerArgs, extension) {
        compilerArgs << "-Xplugin:${PLUGIN_NAME}"
        meyBeApplyNotNullAnnotations(compilerArgs, extension)
    }

    private static void meyBeApplyNotNullAnnotations(compilerArgs, extension) {
        def notNullAnnotations = getList(extension, 'notNullAnnotations')
        if (notNullAnnotations) {
            compilerArgs << "-A${OPTION_ANNOTATIONS_NOT_NULL}=${notNullAnnotations.join(SEPARATOR)}"
        }
    }

    private static List<String> getList(extension, propertyName) {
        def value = extension[propertyName]
        if (!value) {
            return []
        } else if (value instanceof CharSequence) {
            return [value as String]
        } else if (value instanceof List) {
            value.forEach {
                if (!(it instanceof CharSequence)) {
                    throw new PluginInstantiationException(
                            "Error on ${PLUGIN_NAME} plugin initialization - expected to find a list "
                                    + "of strings at the '$propertyName' property but found a ${it.class.name} "
                                    + "instance inside the list - '$it'"
                    )
                }
            }
            return value
        } else {
            throw new PluginInstantiationException(
                    "Error on ${PLUGIN_NAME} plugin initialization - expected to find a string or a "
                            + "list of string at the '$propertyName' property but got a ${value.class.name} "
                            + "instance - '$value'"
            )
        }
    }
}
