package tech.harmonysoft.oss.traute.util;

import org.jetbrains.annotations.NotNull;

import javax.tools.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Plugs into the {@link JavaCompiler} infrastructure to be able to capture compiled binaries
 * for the test source and {@link #getCompiled() expose them}.
 */
public class SimpleFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {

    private final List<SimpleClassFile> compiled = new ArrayList<>();

    public SimpleFileManager(StandardJavaFileManager delegate) {
        super(delegate);
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location,
                                               String className,
                                               JavaFileObject.Kind kind,
                                               FileObject sibling)
    {
        SimpleClassFile result = new SimpleClassFile(URI.create("string://" + className));
        compiled.add(result);
        return result;
    }

    /**
     * @return  compiled binaries processed by the current class
     */
    @NotNull
    public List<SimpleClassFile> getCompiled() {
        return compiled;
    }
}
