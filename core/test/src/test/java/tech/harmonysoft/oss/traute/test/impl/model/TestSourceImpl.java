package tech.harmonysoft.oss.traute.test.impl.model;

import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.traute.test.api.model.TestSource;

public class TestSourceImpl implements TestSource {

    @NotNull private final String               sourceText;
    @NotNull private final String               className;

    public TestSourceImpl(@NotNull String sourceText, @NotNull String className) {
        this.sourceText = sourceText;
        this.className = className;
    }

    @Override
    @NotNull
    public String getSourceText() {
        return sourceText;
    }

    @Override
    @NotNull
    public String getQualifiedClassName() {
        return className;
    }

    @Override
    public String toString() {
        return className + " source";
    }
}
