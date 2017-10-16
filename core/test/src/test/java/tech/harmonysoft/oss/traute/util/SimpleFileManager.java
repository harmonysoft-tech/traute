package tech.harmonysoft.oss.traute.util;

import org.jetbrains.annotations.NotNull;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

// TODO den add doc
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

    @NotNull
    public List<SimpleClassFile> getCompiled() {
        return compiled;
    }
}
