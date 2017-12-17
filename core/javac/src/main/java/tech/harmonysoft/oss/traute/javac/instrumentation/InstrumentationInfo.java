package tech.harmonysoft.oss.traute.javac.instrumentation;

import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.traute.javac.common.CompilationUnitProcessingContext;
import tech.harmonysoft.oss.traute.common.instrumentation.InstrumentationType;

/**
 * Holds information necessary to perform instrumentation.
 */
public interface InstrumentationInfo {

    /**
     * @return  instrumentation type of the data stored in the current object
     */
    @NotNull
    InstrumentationType getType();

    /**
     * @return current compilation unit processing context
     */
    @NotNull
    CompilationUnitProcessingContext getContext();

    /**
     * @return {@code NotNull} annotation for which an instrumentation should happen (if any)
     */
    String getNotNullAnnotation();

    /**
     * @return {@code NotNullByDefault} annotation description which implies the instrumentation (if any)
     */
    String getNotNullByDefaultAnnotationDescription();
}
