package tech.harmonysoft.oss.traute.javac;

import org.jetbrains.annotations.NotNull;

/**
 * Defines contract for a service which knows how to perform target instrumentation.
 *
 * @param <T>   target instrumentation info type
 */
public interface Instrumentator<T extends InstrumentationInfo> {

    /**
     * Performs instrumentation for the given data.
     *
     * @param instrumentationInfo   instrumentation info
     */
    void instrument(@NotNull T instrumentationInfo);
}
