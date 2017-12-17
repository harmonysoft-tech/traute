package tech.harmonysoft.oss.traute.maven.test.impl;

import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.traute.common.settings.TrautePluginSettings;
import tech.harmonysoft.oss.traute.common.util.TrauteConstants;
import tech.harmonysoft.oss.traute.test.impl.engine.AbstractExternalSystemTestCompiler;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import static java.util.stream.Collectors.joining;

public class MavenTestCompiler extends AbstractExternalSystemTestCompiler {

    public static final MavenTestCompiler INSTANCE = new MavenTestCompiler();

    private static final String MAVEN_COMPILER_PLUGIN_GROUP = "org.apache.maven.plugins";
    private static final String MAVEN_COMPILER_PLUGIN_ARTIFACT = "maven-compiler-plugin";
    private static final String MAVEN_COMPILER_PLUGIN_VERSION = "3.7.0";

    private static final String MARKER_DEPENDENCIES  = "<DEPENDENCIES>";
    private static final String MARKER_COMPILER_ARGS = "<COMPILER_ARGS>";

    private static final String POM_XML_CONTENT =
            "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
            "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "  xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
            "  <modelVersion>4.0.0</modelVersion>\n" +
            "\n" +
            "  <groupId>tech.harmonysoft.oss.traute</groupId>\n" +
            "  <artifactId>maven-facade-test</artifactId>\n" +
            "  <version>1.0-SNAPSHOT</version>\n" +
            "  <packaging>jar</packaging>\n" +
            "\n" +
            "  <repositories>\n" +
            "    <repository>\n" +
            "      <id>android</id>\n" +
            "      <name>Android Repo</name>\n" +
            "      <url>https://maven.google.com</url>\n" +
            "    </repository>\n" +
            "  </repositories>\n" +
            "\n" +
            "  <dependencies>\n" +
            MARKER_DEPENDENCIES +
            "  </dependencies>\n" +
            "\n" +
            "  <build>\n" +
            "    <plugins>\n" +
            "      <plugin>\n" +
            "        <groupId>" + MAVEN_COMPILER_PLUGIN_GROUP + "</groupId>\n" +
            "        <artifactId>" + MAVEN_COMPILER_PLUGIN_ARTIFACT + "</artifactId>\n" +
            "        <version>" + MAVEN_COMPILER_PLUGIN_VERSION + "</version>\n" +
            "        <configuration>\n" +
            "          <source>1.8</source>\n" +
            "          <target>1.8</target>\n" +
            "          <compilerArgs>\n" +
            "            <arg>-Xplugin:" + TrauteConstants.PLUGIN_NAME + "</arg>\n" +
            "            " + MARKER_COMPILER_ARGS + "\n" +
            "          </compilerArgs>\n" +
            "        </configuration>\n" +
            "      </plugin>\n" +
            "    </plugins>\n" +
            "  </build>\n" +
            "</project>";

    @Override
    @NotNull
    protected File createExternalSystemConfig(@NotNull File projectRootDir, @NotNull TrautePluginSettings settings)
            throws IOException
    {
        File pom = new File(projectRootDir, "pom.xml");
        fillPomContent(pom, settings);
        return pom;
    }

    @Override
    @NotNull
    protected String getRelativeSrcPath() {
        return "src/main/java";
    }

    @Override
    @NotNull
    protected String compile(@NotNull File projectRootDir) throws Exception {
        File stdOut = new File(projectRootDir, "stdout");
        File stdErr = new File(projectRootDir, "stderr");
        ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", "mvn compile")
                .directory(projectRootDir)
                .redirectOutput(stdOut)
                .redirectError(stdErr);
        Process start = processBuilder.start();
        start.waitFor();
        return new String(read(stdOut)) + "\n" + new String(read(stdErr));
    }

    @Override
    @NotNull
    protected String getRelativeBinariesPath() {
        return "target/classes";
    }

    private static void fillPomContent(@NotNull File pom, @NotNull TrautePluginSettings settings) throws IOException {
        String content = POM_XML_CONTENT;

        content = content.replace(MARKER_DEPENDENCIES,
                                  "    " + System.getProperty("trauteTestDependencies")
                                                 .replace("\n", "\n    ") + "\n");

        Collection<String> arguments = getCompilerArgs(settings);

        content = content.replace(MARKER_COMPILER_ARGS,
                                  arguments.stream()
                                           .map(arg -> String.format("<arg>%s</arg>", arg))
                                           .collect(joining("\n            ")));

        write(pom, content);
    }
}
