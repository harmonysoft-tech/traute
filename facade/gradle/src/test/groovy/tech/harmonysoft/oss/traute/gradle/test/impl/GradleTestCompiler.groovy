package tech.harmonysoft.oss.traute.gradle.test.impl

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.jetbrains.annotations.NotNull
import org.junit.Test
import tech.harmonysoft.oss.traute.common.settings.TrautePluginSettingsBuilder
import tech.harmonysoft.oss.traute.javac.TrauteJavacPlugin
import tech.harmonysoft.oss.traute.test.api.model.TestSource
import tech.harmonysoft.oss.traute.test.fixture.NN
import tech.harmonysoft.oss.traute.test.impl.engine.AbstractExternalSystemTestCompiler

class GradleTestCompiler extends AbstractExternalSystemTestCompiler {

    static final def INSTANCE = new GradleTestCompiler()

    private static final def MARKER_NOT_NULL_ANNOTATION = '<NOT_NULL_ANNOTATIONS>'
    private static final def MARKER_LOGGING = '<LOGGING>'
    private static final def MARKER_INSTRUMENTATIONS = '<INSTRUMENTATIONS>'
    private static final def BUILD_GRADLE_CONTENT =
            """plugins {
              |    id 'java'
              |    id 'tech.harmonysoft.oss.traute'
              |}
              |
              |sourceCompatibility = 1.8
              |
              |repositories {
              |    mavenCentral()
              |    maven { url 'https://maven.google.com' }
              |}
              |
              |traute {
              |    javacPluginSpec = ${getTrauteJavacDependencySpec()}
              |    $MARKER_NOT_NULL_ANNOTATION
              |    $MARKER_LOGGING
              |    $MARKER_INSTRUMENTATIONS
              |}
              |
              |dependencies {
              |    compile 'org.jetbrains:annotations:15.0'
              |    compile 'com.google.code.findbugs:jsr305:3.0.2'
              |    compile 'javax:javaee-api:8.0'
              |    compile 'findbugs:annotations:1.0.0'
              |    compile 'com.android.support:support-core-utils:26.1.0'
              |    compile 'org.eclipse.jdt:org.eclipse.jdt.annotation:2.1.0'
              |    compile ${getCommonDependency()}
              |}""".stripMargin()

    @NotNull
    @Override
    protected File createExternalSystemConfig(@NotNull File projectRootDir,
                                              @NotNull TestSource testSource)
            throws IOException
    {
        def file = new File(projectRootDir, 'build.gradle')
        def content = BUILD_GRADLE_CONTENT
        def settings = testSource.settings

        content = content.replace(
                MARKER_NOT_NULL_ANNOTATION,
                settings.notNullAnnotations
                        ? "notNullAnnotations = [${settings.notNullAnnotations.collect{"'$it'"}.join(', ')}]"
                        : ''
        )

        content = content.replace(
                MARKER_LOGGING,
                settings.verboseMode ? 'verbose = true' : ''
        )

        content = content.replace(
                MARKER_INSTRUMENTATIONS,
                settings.instrumentationsToApply
                        ? "instrumentations = [${settings.instrumentationsToApply.collect{"'${it.shortName}'"}.join(', ')}]"
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
    private static String getTrauteJavacDependencySpec() {
        def roots = [].toSet()
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

    @NotNull
    private static String findRootInClassPath(@NotNull Class<?> anchor) {
        return findRootInClassPath(anchor.name.replace('.', '/') + '.class')
    }

    @NotNull
    private static String findRootInClassPath(@NotNull String anchor) {
        def url = GradleTestCompiler.classLoader.getResource(anchor)
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
        return result
    }
}
