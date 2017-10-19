package tech.harmonysoft.oss.traute.test.impl.model;

import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.traute.common.settings.TrautePluginSettings;
import tech.harmonysoft.oss.traute.test.api.model.TestSource;

public class TestSourceImpl implements TestSource {

    @NotNull private final String               sourceText;
    @NotNull private final String               className;
    @NotNull private final TrautePluginSettings settings;

    public TestSourceImpl(@NotNull String sourceText,
                          @NotNull String className,
                          @NotNull TrautePluginSettings settings)
    {
        this.sourceText = sourceText;
        this.className = className;
        this.settings = settings;
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
    @NotNull
    public TrautePluginSettings getSettings() {
        return settings;
    }

    @Override
    public String toString() {
        return className + " source";
    }
}
