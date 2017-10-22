package tech.harmonysoft.oss.traute.javac.test.impl;

import org.jetbrains.annotations.NotNull;

import javax.tools.SimpleJavaFileObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

/**
 * Compiled test binaries holder.
 */
public class SimpleClassFile extends SimpleJavaFileObject {

    private ByteArrayOutputStream out;

    public SimpleClassFile(URI uri) {
        super(uri, Kind.CLASS);
    }

    @NotNull
    public URI getUri() {
        return uri;
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        return out = new ByteArrayOutputStream();
    }

    @NotNull
    public byte[] getCompiledBinaries() {
        if (out == null) {
            throw new IllegalStateException(String.format("Not compiled binaries are supplied for the %s",
                                                          uri.getPath()));
        }
        return out.toByteArray();
    }
}
