package tech.harmonysoft.oss.traute.maven.test.impl;

import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.traute.common.settings.TrautePluginSettings;
import tech.harmonysoft.oss.traute.test.api.engine.TestCompiler;
import tech.harmonysoft.oss.traute.test.api.model.ClassFile;
import tech.harmonysoft.oss.traute.test.api.model.CompilationResult;
import tech.harmonysoft.oss.traute.test.api.model.TestSource;
import tech.harmonysoft.oss.traute.test.impl.model.ClassFileImpl;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class MavenTestCompiler implements TestCompiler {

    public static final MavenTestCompiler INSTANCE = new MavenTestCompiler();

    private static final String MAVEN_COMPILER_PLUGIN_GROUP = "org.apache.maven.plugins";
    private static final String MAVEN_COMPILER_PLUGIN_ARTIFACT = "maven-compiler-plugin";
    private static final String MAVEN_COMPILER_PLUGIN_VERSION = "3.7.0";

    private static final String MARKER_TRAUTE_PLUGIN_GROUP      = "<TRAUTE_PLUGIN_GROUP>";
    private static final String MARKER_TRAUTE_PLUGIN_ARTIFACT   = "<TRAUTE_PLUGIN_ARTIFACT>";
    private static final String MARKER_TRAUTE_PLUGIN_VERSION    = "<TRAUTE_PLUGIN_VERSION>";
    private static final String MARKER_TRAUTE_TEST_DEPENDENCIES = "<TRAUTE_TEST_DEPENDENCIES>";

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
            "  <dependencies>\n" +
            MARKER_TRAUTE_TEST_DEPENDENCIES +
            "  </dependencies>\n" +
            "\n" +
            "  <build>\n" +
            "    <plugins>\n" +
            "      <plugin>\n" +
            "        <groupId>" + MAVEN_COMPILER_PLUGIN_GROUP + "</groupId>\n" +
            "        <artifactId>" + MAVEN_COMPILER_PLUGIN_ARTIFACT + "</artifactId>\n" +
            "        <version>" + MAVEN_COMPILER_PLUGIN_VERSION + "</version>\n" +
            "      </plugin>\n" +
            "      <plugin>\n" +
            "        <groupId>" + MARKER_TRAUTE_PLUGIN_GROUP + "</groupId>\n" +
            "        <artifactId>" + MARKER_TRAUTE_PLUGIN_ARTIFACT + "</artifactId>\n" +
            "        <version>" + MARKER_TRAUTE_PLUGIN_VERSION + "</version>\n" +
            "        <executions>\n" +
            "          <execution>\n" +
            "            <goals>\n" +
            "              <goal>traute</goal>\n" +
            "            </goals>\n" +
            "          </execution>\n" +
            "        </executions>\n" +
            "      </plugin>\n" +
            "    </plugins>\n" +
            "  </build>\n" +
            "</project>";

    @Override
    @NotNull
    public CompilationResult compile(@NotNull TestSource testSource) {
        try {
            return doCompile(testSource);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private CompilationResult doCompile(@NotNull TestSource testSource) throws Exception {
        File projectRootDir = createRootDir();
        File pom = new File(projectRootDir, "pom.xml");
        String content = fillPomContent(pom, testSource.getSettings());

        File sourceRoot = createSourceRootDir(projectRootDir);
        File sourceFile = createFile(testSource.getQualifiedClassName(), sourceRoot);
        write(sourceFile, testSource.getSourceText());

        File stdOut = new File(projectRootDir, "stdout");
        File stdErr = new File(projectRootDir, "stderr");
        ProcessBuilder processBuilder = new ProcessBuilder(
                "bash", "-c", String.format("mvn -Dmaven.repo.local=%s compile", System.getProperty("localRepo"))
        ).directory(projectRootDir)
         .redirectOutput(stdOut)
         .redirectError(stdErr);
        Process start = processBuilder.start();
        int exitCode = start.waitFor();

        int asdf = 1;

//        MojoRule mojoRule = new MojoRule();
//        Mojo mojo = mojoRule.lookupMojo(MAVEN_COMPILER_PLUGIN_GROUP,
//                                        MAVEN_COMPILER_PLUGIN_ARTIFACT,
//                                        MAVEN_COMPILER_PLUGIN_VERSION,
//                                        "compiler:compile", null);
//        mojo.execute();
        throw new UnsupportedOperationException();
    }

    @NotNull
    private static File createRootDir() throws IOException {
        File result = Files.createTempDirectory("maven-traute").toFile();
        if (!result.isDirectory()) {
            throw new IllegalStateException("Can't create a root directory for a test project at "
                                            + result.getAbsolutePath());
        }
        return result;
    }

    @NotNull
    private static String fillPomContent(@NotNull File pom, @NotNull TrautePluginSettings settings)
            throws IOException
    {
        String content = POM_XML_CONTENT;

        content = content.replace(MARKER_TRAUTE_PLUGIN_GROUP, System.getProperty("trauteGroupId"));
        content = content.replace(MARKER_TRAUTE_PLUGIN_ARTIFACT, System.getProperty("trauteArtifactId"));
        content = content.replace(MARKER_TRAUTE_PLUGIN_VERSION, System.getProperty("trauteVersion"));

        String[] dependencies = System.getProperty("trauteTestDependencies").split(":");
        StringBuilder buffer = new StringBuilder();
        int counter = 0;
        for (String dependency : dependencies) {
            buffer.append("\n    <dependency>")
                  .append("\n      ").append("<groupId>traute</groupId>")
                  .append("\n      ").append("<artifactId>traute").append(++counter).append("</artifactId>")
                  .append("\n      ").append("<version>1.0</version>")
                  .append("\n      ").append("<scope>system</scope>")
                  .append("\n      ").append("<systemPath>").append(dependency).append("</systemPath>")
                  .append("\n    </dependency>");
        }
        content = content.replace(MARKER_TRAUTE_TEST_DEPENDENCIES, buffer);

//        content = content.replace(
//                MARKER_NOT_NULL_ANNOTATION,
//                settings.notNullAnnotations
//                ? "notNullAnnotations = [${settings.notNullAnnotations.collect{"'$it'"}.join(', ')}]"
//                        : ''
//        )
//
//        content = content.replace(
//                MARKER_LOGGING,
//                settings.verboseMode ? 'verbose = true' : ''
//        )
//
//        content = content.replace(
//                MARKER_INSTRUMENTATIONS,
//                settings.instrumentationsToApply
//                ? "instrumentations = [${settings.instrumentationsToApply.collect{"'${it.shortName}'"}.join(', ')}]"
//                        : ''

        write(pom, content);
        return content;
    }

    @NotNull
    private static File createSourceRootDir(@NotNull File projectRootDir) {
        File result = new File(projectRootDir, "src/main/java");
        boolean created = result.mkdirs();
        if (!created) {
            throw new IllegalStateException("Can't create a source root directory for a test project at "
                                            + result.getAbsolutePath());
        }
        return result;
    }

    @NotNull
    private static File createFile(@NotNull String qualifiedClassName, @NotNull File sourceRootDir) {
        int i = qualifiedClassName.lastIndexOf('.');
        if (i <= 0) {
            return new File(sourceRootDir, "${qualifiedClassName}.java");
        } else {
            File dir = new File(sourceRootDir, qualifiedClassName.substring(0, i - 1)
                                                                 .replace('.', '/'));
            boolean created = dir.mkdirs();
            if (!created) {
                throw new IllegalStateException(String.format("Can't create a directory for the source class %s at %s",
                                                              qualifiedClassName, dir.getAbsolutePath()));
            }
            return new File(dir, qualifiedClassName.substring(i + 1) + ".java");
        }
    }

    private static void write(@NotNull File file, @NotNull String content) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(content);
        }
    }

    @NotNull
    private static byte[] read(@NotNull File file) throws IOException {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read;
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {
            while ((read = in.read(buffer)) >= 0) {
                bOut.write(buffer, 0, read);
            }
        }
        return bOut.toByteArray();
    }

    @NotNull
    private static Collection<ClassFile> findBinaries(@NotNull File projectRoot) throws IOException {
        File binariesRoot = new File(projectRoot, "build/classes/java/main");
        if (!binariesRoot.isDirectory()) {
            return Collections.emptyList();
        }
        List<ClassFile> result = new ArrayList<>();
        Stack<File> toProcess = new Stack<>();
        while (!toProcess.isEmpty()) {
            File file = toProcess.pop();
            if (file.isDirectory()) {
                File[] children = file.listFiles();
                if (children != null) {
                    for (File child : children) {
                        toProcess.push(child);
                    }
                }
                continue;
            }

            String className = file.getAbsolutePath().substring(binariesRoot.getAbsolutePath().length());
            className = className.substring(0, className.length() - ".class".length());
            if (className.startsWith("/")) {
                className = className.substring(1);
            }
            className = className.replace('/', '.');
            result.add(new ClassFileImpl(className, read(file)));
        }
        return result;
    }

    @Override
    public void release(@NotNull CompilationResult result) {
        // TODO den implement
    }
}
