package tech.harmonysoft.oss.traute.ant.test.impl;

import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.traute.test.api.model.TestSource;
import tech.harmonysoft.oss.traute.test.impl.engine.AbstractExternalSystemTestCompiler;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

public class AntTestCompiler extends AbstractExternalSystemTestCompiler {

    public static final AntTestCompiler INSTANCE = new AntTestCompiler();

    private static final String MARKER_DEPENDENCIES = "<DEPENDENCIES>";
    private static final String MARKER_COMPILER_ARGS = "<COMPILER_ARGS>";

    private static final String BUILD_XML_CONTENT =
            "<?xml version=\"1.0\"?>\n" +
            "<project name=\"traute-ant-sample\" default=\"compile\" xmlns:ivy=\"antlib:org.apache.ivy.ant\">\n" +
            "\n" +
            "  <property name=\"build.dir\" value=\"build\"/>\n" +
            "  <property name=\"src.dir\" value=\"src\"/>\n" +
            "\n" +
            "  <target name=\"compile\">\n" +
            "    <path id=\"traute.classpath\">\n" +
            MARKER_DEPENDENCIES + "\n" +
            "    </path>\n" +
            "    <mkdir dir=\"${build.dir}\"/>\n" +
            "    <javac srcdir=\"${src.dir}\" destdir=\"${build.dir}\" classpathref=\"traute.classpath\" debug=\"true\">\n" +
            "      <compilerarg value=\"-Xplugin:Traute\"/>\n" +
            MARKER_COMPILER_ARGS + "\n" +
            "    </javac>\n" +
            "  </target>\n" +
            "</project>";

    @Override
    protected @NotNull File createExternalSystemConfig(@NotNull File projectRootDir, @NotNull TestSource testSource)
            throws IOException
    {
        File buildXml = new File(projectRootDir, "build.xml");
        String content = BUILD_XML_CONTENT;

        String dependencies = Arrays.stream(System.getProperty("trauteTestDependencies").split(":"))
                                    .map(p -> String.format("      <pathelement location=\"%s\" />", p))
                                    .collect(Collectors.joining("\n"));
        content = content.replace(MARKER_DEPENDENCIES, dependencies);

        String compilerArguments = getCompilerArgs(testSource.getSettings())
                .stream()
                .map(a -> String.format("        <compilerarg value=\"%s\" />", a))
                .collect(Collectors.joining("\n"));
        content = content.replace(MARKER_COMPILER_ARGS, compilerArguments);
        write(buildXml, content);
        return buildXml;
    }

    @Override
    protected @NotNull String getRelativeSrcPath() {
        return "src";
    }

    @Override
    protected @NotNull String compile(@NotNull File projectRootDir) throws Exception {
        File stdOut = new File(projectRootDir, "stdout");
        File stdErr = new File(projectRootDir, "stderr");
        ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", "ant")
                .directory(projectRootDir)
                .redirectOutput(stdOut)
                .redirectError(stdErr);
        Process start = processBuilder.start();
        start.waitFor();
        return new String(read(stdOut)) + "\n" + new String(read(stdErr));
    }

    @Override
    protected @NotNull String getRelativeBinariesPath() {
        return "build";
    }
}
