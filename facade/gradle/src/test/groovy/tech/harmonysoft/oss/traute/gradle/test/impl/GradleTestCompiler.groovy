package tech.harmonysoft.oss.traute.gradle.test.impl

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.jetbrains.annotations.NotNull
import org.junit.Test
import tech.harmonysoft.oss.traute.common.instrumentation.InstrumentationType
import tech.harmonysoft.oss.traute.common.settings.TrautePluginSettings
import tech.harmonysoft.oss.traute.common.settings.TrautePluginSettingsBuilder
import tech.harmonysoft.oss.traute.gradle.TrauteGradlePlugin
import tech.harmonysoft.oss.traute.javac.TrauteJavacPlugin
import tech.harmonysoft.oss.traute.test.fixture.NN
import tech.harmonysoft.oss.traute.test.impl.engine.AbstractExternalSystemTestCompiler

import static tech.harmonysoft.oss.traute.common.instrumentation.InstrumentationType.METHOD_PARAMETER
import static tech.harmonysoft.oss.traute.common.instrumentation.InstrumentationType.METHOD_RETURN
import static tech.harmonysoft.oss.traute.common.settings.TrautePluginSettingsBuilder.DEFAULT_NOT_NULL_ANNOTATIONS
import static tech.harmonysoft.oss.traute.common.settings.TrautePluginSettingsBuilder.DEFAULT_NULLABLE_ANNOTATIONS
import static tech.harmonysoft.oss.traute.common.settings.TrautePluginSettingsBuilder.DEFAULT_PARAMETERS_NOT_NULL_BY_DEFAULT_ANNOTATIONS
import static tech.harmonysoft.oss.traute.common.settings.TrautePluginSettingsBuilder.DEFAULT_RETURN_NOT_NULL_BY_DEFAULT_ANNOTATIONS
import static tech.harmonysoft.oss.traute.gradle.TrauteGradlePlugin.findRootInClassPath

class GradleTestCompiler extends AbstractExternalSystemTestCompiler {

    static final def INSTANCE = new GradleTestCompiler()

    private static final def MARKER_NOT_NULL_ANNOTATION = '<NOT_NULL_ANNOTATIONS>'
    private static final def MARKER_NULLABLE_ANNOTATION = '<NULLABLE_ANNOTATIONS>'
    private static final def MARKER_NOT_NULL_BY_DEFAULT_ANNOTATION = '<MARKER_NOT_NULL_BY_DEFAULT_ANNOTATION>'
    private static final def MARKER_LOGGING = '<LOGGING>'
    private static final def MARKER_INSTRUMENTATIONS = '<INSTRUMENTATIONS>'
    private static final def MARKER_LOG_FILE = '<LOG_FILE>'
    private static final def MARKER_EXCEPTIONS_TO_THROW = '<EXCEPTIONS_TO_THROW>'
    private static final def MARKER_EXCEPTION_TEXTS = '<EXCEPTION_TEXTS>'
    private static final def BUILD_GRADLE_CONTENT =
            """buildscript {
              |    dependencies {
              |        classpath ${getTrauteJavacDependencySpec()}
              |    }
              |}
              |
              |apply plugin: 'java'
              |apply plugin: 'tech.harmonysoft.oss.traute'
              |
              |sourceCompatibility = 1.8
              |
              |repositories {
              |    mavenCentral()
              |    maven { url 'https://maven.google.com' }
              |}
              |
              |traute {
              |    $MARKER_NOT_NULL_ANNOTATION
              |    $MARKER_NULLABLE_ANNOTATION
              |    $MARKER_NOT_NULL_BY_DEFAULT_ANNOTATION
              |    $MARKER_LOGGING
              |    $MARKER_INSTRUMENTATIONS
              |    $MARKER_LOG_FILE
              |    $MARKER_EXCEPTIONS_TO_THROW
              |    $MARKER_EXCEPTION_TEXTS
              |}
              |
              |dependencies {
              |    compile 'org.jetbrains:annotations:15.0'
              |    compile 'com.google.code.findbugs:jsr305:3.0.2'
              |    compile 'javax:javaee-api:8.0'
              |    compile 'findbugs:annotations:1.0.0'
              |    compile 'com.android.support:support-core-utils:26.1.0'
              |    compile 'org.eclipse.jdt:org.eclipse.jdt.annotation:2.1.0'
              |    compile 'org.eclipse.jdt:org.eclipse.jdt.annotation:2.1.0'
              |    compile 'org.springframework:spring-core:5.0.1.RELEASE'
              |    compile 'org.checkerframework:checker:2.3.0'
              |    compile ${getCommonDependency()}
              |}""".stripMargin()

    @NotNull
    @Override
    protected File createExternalSystemConfig(@NotNull File projectRootDir,
                                              @NotNull TrautePluginSettings settings)
            throws IOException
    {
        def file = new File(projectRootDir, 'build.gradle')
        def content = BUILD_GRADLE_CONTENT

        content = content.replace(
                MARKER_NOT_NULL_ANNOTATION,
                (settings.notNullAnnotations
                        && settings.notNullAnnotations.size() < DEFAULT_NOT_NULL_ANNOTATIONS.size())
                        ? "notNullAnnotations = [${settings.notNullAnnotations.collect{"'$it'"}.join(', ')}]"
                        : ''
        )

        content = content.replace(
                MARKER_NULLABLE_ANNOTATION,
                (settings.nullableAnnotations
                        && settings.nullableAnnotations.size() < DEFAULT_NULLABLE_ANNOTATIONS.size())
                        ? "nullableAnnotations = [${settings.nullableAnnotations.collect{"'$it'"}.join(', ')}]"
                        : ''
        )

        def notNullByDefaultAnnotations = [:]
        if (settings.notNullByDefaultAnnotations) {
            def parameterNotNullByDefaultAnnotations = settings.notNullByDefaultAnnotations.get(METHOD_PARAMETER)
            if (parameterNotNullByDefaultAnnotations
                    && DEFAULT_PARAMETERS_NOT_NULL_BY_DEFAULT_ANNOTATIONS != parameterNotNullByDefaultAnnotations)
            {
                notNullByDefaultAnnotations.put(METHOD_PARAMETER.shortName, parameterNotNullByDefaultAnnotations)
            }
            def returnNotNullByDefaultAnnotations = settings.notNullByDefaultAnnotations.get(METHOD_RETURN)
            if (returnNotNullByDefaultAnnotations
                    && DEFAULT_RETURN_NOT_NULL_BY_DEFAULT_ANNOTATIONS != returnNotNullByDefaultAnnotations)
            {
                notNullByDefaultAnnotations.put(METHOD_RETURN.shortName, returnNotNullByDefaultAnnotations)
            }
        }
        def notNullByDefaultAnnotationsString = ''
        if (notNullByDefaultAnnotations) {
            notNullByDefaultAnnotationsString = 'notNullByDefaultAnnotations = ['
            notNullByDefaultAnnotations.each { key, value ->
                notNullByDefaultAnnotationsString += "\n'$key' : [${value.collect {"'$it'"}.join(', ')}]"
            }
            notNullByDefaultAnnotationsString =
                    notNullByDefaultAnnotationsString.replace("\n", "\n        ")
            notNullByDefaultAnnotationsString += '\n    ]'
        }
        content = content.replace(MARKER_NOT_NULL_BY_DEFAULT_ANNOTATION, notNullByDefaultAnnotationsString)

        content = content.replace(
                MARKER_LOGGING,
                settings.verboseMode ? 'verbose = true' : ''
        )

        content = content.replace(
                MARKER_INSTRUMENTATIONS,
                (settings.instrumentationsToApply
                        && settings.instrumentationsToApply.size() < InstrumentationType.values().length)
                        ? "instrumentations = [${settings.instrumentationsToApply.collect{"'${it.shortName}'"}.join(', ')}]"
                        : ''
        )

        content = content.replace(
                MARKER_LOG_FILE,
                settings.logFile.present ? "logFile = '${settings.logFile.get()}'" : ''
        )

        content = content.replace(
                MARKER_EXCEPTIONS_TO_THROW,
                settings.exceptionsToThrow
                        ? "exceptionsToThrow = ${settings.exceptionsToThrow.collect { "'${it.key.shortName}' : '${it.value}'" }}"
                        : ''
        )

        content = content.replace(
                MARKER_EXCEPTION_TEXTS,
                settings.exceptionTextPatterns
                        ? "exceptionTexts = ${settings.exceptionTextPatterns.collect { "'${it.key.shortName}' : '${it.value}'" }}"
                        : ''
        )

        file.text = content
        return file
    }

    @NotNull
    @Override
    protected String getRelativeSrcPath() {
        return 'src/main/java'
    }

    @NotNull
    @Override
    protected String compile(@NotNull File projectRootDir) throws Exception {
        def pluginClasspathResource = getClass().classLoader.getResource("plugin-classpath.txt")
        if (pluginClasspathResource == null) {
            throw new IllegalStateException("Did not find plugin classpath resource, run `testClasses` build task.")
        }

        def pluginClasspath = pluginClasspathResource.readLines().collect { new File(it) }

        try {
            def buildResult = GradleRunner.create()
                    .withProjectDir(projectRootDir)
                    .withPluginClasspath(pluginClasspath)
                    .withArguments('compileJava')
                    .withDebug(true)
                    .build()
            return buildResult.output
        } catch (UnexpectedBuildFailure e) {
            projectRootDir.deleteDir()
            return e.buildResult.output
        }
    }

    @NotNull
    @Override
    protected String getRelativeBinariesPath() {
        return 'build/classes/java/main'
    }

    /**
     * We want to setup our test gradle project in a way to use javac traute plugin from the local project.
     * So, it's necessary to locate plugin classpath root(s) (roots in case of the IDE runs where there
     * are different roots for binaries and resources by default) and specify them as a
     * <a href="https://docs.gradle.org/current/userguide/working_with_files.html#sec:file_collections">FileCollection</a>
     *
     * @return  dependency spec to the {@code 'javac-plugin'} classpath root(s)
     */
    @NotNull
    static String getTrauteJavacDependencySpec() {
        def roots = [].toSet()
        roots << findRootInClassPath('META-INF/gradle-plugins/tech.harmonysoft.oss.traute.properties')
        roots << findRootInClassPath(TrauteGradlePlugin)
        roots << findRootInClassPath(TrauteJavacPlugin)
        roots << findRootInClassPath(TrautePluginSettingsBuilder)
        roots << findRootInClassPath('META-INF/services/com.sun.source.util.Plugin')

        return "files(${roots.collect {"'$it'"}.join(',')})"
    }

    /**
     * Some of the test work with the custom {@code NotNull} annotation ({@link NN), that's why we explicitly add
     * a dependency to the 'common' classpath root in the test project
     *
     * @return dependency spec for the {@code 'common'} classpath root
     */
    @Test
    private static String getCommonDependency() {
        return "files('${findRootInClassPath(NN)}')"
    }
}
