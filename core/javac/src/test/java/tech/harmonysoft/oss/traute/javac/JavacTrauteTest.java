package tech.harmonysoft.oss.traute.javac;

import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.traute.AbstractTrauteTest;
import tech.harmonysoft.oss.traute.util.TestConstants;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static java.util.Arrays.asList;
import static org.junit.Assert.fail;
import static tech.harmonysoft.oss.traute.util.TestConstants.CLASS_NAME;

public class JavacTrauteTest extends AbstractTrauteTest {

    private static final String PLUGIN_JAR_NAME = "traute.jar";

    @Override
    protected @NotNull List<String> getAdditionalCompilerArgs() {
        // TODO den deliver target annotations
        List<String> result = new ArrayList<>();
        result.add("-Xplugin:" + TrautePlugin.NAME);
        result.addAll(asList("-processorpath", getPluginPath()));
        return result;
    }

    @NotNull
    private static String getPluginPath() {
        File compiledPluginClass = getCompiledPluginClass();

        File jar;
        try {
            jar = Files.createTempFile(PLUGIN_JAR_NAME, "").toFile();
        } catch (IOException e) {
            throw new RuntimeException("Can't create a jar file with plugin content");
        }

        try {
            JarOutputStream out = new JarOutputStream(new FileOutputStream(jar));
            prepareJar(out, new BufferedInputStream(new FileInputStream(compiledPluginClass)));
            out.close();
        } catch (IOException e) {
            throw new RuntimeException("Can't write plugin content to the temp jar file at " + jar.getAbsolutePath(),
                                       e);
        }

        return jar.getAbsolutePath();
    }

    @NotNull
    private static File getCompiledPluginClass() {
        String pluginPathSuffix = TrautePlugin.class.getName().replace('.', '/') + ".class";
        String classpath = System.getProperty("java.class.path");
        for (String rootPath : classpath.split(System.getProperty("path.separator"))) {
            File root = new File(rootPath);
            File candidate = new File(root, pluginPathSuffix);
            if (candidate.isFile()) {
                return candidate;
            }
        }
        fail(String.format("Can't prepare a %s javac plugin for tests - target binary (%s) is not found "
                           + "in the classpath (%s)", TrautePlugin.NAME, pluginPathSuffix, classpath));
        throw new RuntimeException("this exception must never be thrown");
    }

    private static void prepareJar(@NotNull JarOutputStream out, @NotNull InputStream compiledPluginContent)
            throws IOException
    {
        // Write binary.
        StringBuilder path = new StringBuilder("/");
        for (String entry : TrautePlugin.class.getPackage().getName().split("\\.")) {
            path.append(entry).append("/");
            out.putNextEntry(new JarEntry(path.toString()));
            out.closeEntry();
        }
        JarEntry pluginBinary = new JarEntry(String.format("%s/%s.class", path, CLASS_NAME));
        out.putNextEntry(pluginBinary);
        byte[] buffer = new byte[1024];
        int read;
        while ((read = compiledPluginContent.read(buffer)) >= 0) {
            out.write(buffer, 0, read);
        }
        out.closeEntry();

        // Write service discovery config
        out.putNextEntry(new JarEntry("/META-INF/"));
        out.closeEntry();
        out.putNextEntry(new JarEntry("/META-INF/services/"));
        out.closeEntry();
        JarEntry serviceDiscovery = new JarEntry("/META-INF/services/com.sun.source.util.Plugin");
        out.putNextEntry(serviceDiscovery);
        out.write(TrautePlugin.class.getName().getBytes());
        out.closeEntry();
    }
}
