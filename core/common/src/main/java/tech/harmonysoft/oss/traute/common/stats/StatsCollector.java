package tech.harmonysoft.oss.traute.common.stats;

import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.traute.common.instrumentation.InstrumentationType;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class StatsCollector {

    private final ConcurrentMap<InstrumentationType, Long> stats = new ConcurrentHashMap<>();

    public void increment(@NotNull InstrumentationType type) {
        add(type, 1);
    }

    public void add(@NotNull InstrumentationType type, long count) {
        stats.compute(type, (key, value) -> value == null ? count : value + count);
    }

    @NotNull
    public ConcurrentMap<InstrumentationType, Long> getStats() {
        return stats;
    }

    @Override
    public String toString() {
        return stats.toString();
    }
}
