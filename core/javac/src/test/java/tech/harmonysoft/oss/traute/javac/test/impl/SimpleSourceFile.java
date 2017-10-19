package tech.harmonysoft.oss.traute.javac.test.impl;

import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.traute.test.api.model.TestSource;

import javax.tools.JavaCompiler;
import javax.tools.SimpleJavaFileObject;
import java.net.URI;

/**
 * <p>Stands for a source file with the predefined content.</p>
 * <p>Taken from {@link JavaCompiler} javadoc.</p>
 */
public class SimpleSourceFile extends SimpleJavaFileObject {

    @NotNull private final String content;

    public SimpleSourceFile(@NotNull TestSource testSource) {
        super(URI.create(String.format("file://%s%s",
                                       testSource.getQualifiedClassName().replaceAll("\\.", "/"),
                                       Kind.SOURCE.extension)),
              Kind.SOURCE);
        content = testSource.getSourceText();
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return content;
    }
}