package tech.harmonysoft.oss.traute.test.util;

import org.jetbrains.annotations.NotNull;

import javax.tools.JavaCompiler;
import javax.tools.SimpleJavaFileObject;
import java.net.URI;

import static tech.harmonysoft.oss.traute.test.util.TestConstants.*;

/**
 * <p>Stands for a source file with the predefined content.</p>
 * <p>Taken from {@link JavaCompiler} javadoc.</p>
 */
public class SimpleSourceFile extends SimpleJavaFileObject {

    @NotNull private final String content;

    public SimpleSourceFile(@NotNull String content) {
        super(URI.create(String.format("file://%s/%s%s", PACKAGE, CLASS_NAME, Kind.SOURCE.extension)), Kind.SOURCE);
        this.content = content;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return content;
    }
}
