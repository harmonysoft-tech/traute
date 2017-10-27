package tech.harmonysoft.oss.traute.javac.instrumentation;

import org.jetbrains.annotations.NotNull;

/**
 * A utility {@link Instrumentator} base class which provides logic common for all implementations
 *
 * @param <T>   target instrumentation info
 */
public abstract class AbstractInstrumentator<T extends InstrumentationInfo> implements Instrumentator<T> {

    @Override
    public void instrument(@NotNull T instrumentationInfo) {
        boolean instrumented = mayBeInstrument(instrumentationInfo);
        if (instrumented) {
            instrumentationInfo.getContext().getStatsCollector().increment(instrumentationInfo.getType());
        }
    }

    protected abstract boolean mayBeInstrument(@NotNull T instrumentationInfo);
}
