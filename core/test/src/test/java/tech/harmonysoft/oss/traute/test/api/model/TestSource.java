package tech.harmonysoft.oss.traute.test.api.model;

import org.jetbrains.annotations.NotNull;

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
}
