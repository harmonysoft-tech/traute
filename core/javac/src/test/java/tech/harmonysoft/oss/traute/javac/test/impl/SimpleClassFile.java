package tech.harmonysoft.oss.traute.javac.test.impl;

import org.jetbrains.annotations.NotNull;

import javax.tools.SimpleJavaFileObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Optional;

/**
 * Compiled test binaries holder.
 */
public class SimpleClassFile extends SimpleJavaFileObject {

    private ByteArrayOutputStream out;

    public SimpleClassFile(URI uri) {
        super(uri, Kind.CLASS);
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        return out = new ByteArrayOutputStream();
    }

    @NotNull
    public Optional<byte[]> getCompiledBinaries() {
        return Optional.ofNullable(out == null ? null : out.toByteArray());
    }
}
