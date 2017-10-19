package tech.harmonysoft.oss.traute.test.api.model;

import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.traute.common.settings.TrautePluginSettings;

public interface TestSource {

    /**
     * @return  complete test source
     */
    @NotNull
    String getSourceText();

    /**
     * @return  qualified class name for the given test source
     */
    @NotNull
    String getQualifiedClassName();

    @NotNull
    TrautePluginSettings getSettings();
}
