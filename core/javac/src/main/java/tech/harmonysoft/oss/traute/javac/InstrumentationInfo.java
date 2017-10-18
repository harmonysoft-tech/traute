package tech.harmonysoft.oss.traute.javac;

import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.traute.javac.common.CompilationUnitProcessingContext;

/**
 * Holds information necessary to perform instrumentation.
 */
public interface InstrumentationInfo {

    /**
     * @return current compilation unit processing context
     */
    @NotNull
    CompilationUnitProcessingContext getContext();

    /**
     * @return {@code NotNull} annotation for which an instrumentation should happen
     */
    @NotNull
    String getNotNullAnnotation();
}
