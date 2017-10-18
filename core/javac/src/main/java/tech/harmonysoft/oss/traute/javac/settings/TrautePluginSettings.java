package tech.harmonysoft.oss.traute.javac.settings;

import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.traute.javac.common.InstrumentationType;

import java.util.HashSet;
import java.util.Set;

public class TrautePluginSettings {

    private final Set<String>              notNullAnnotations      = new HashSet<>();
    private final Set<InstrumentationType> instrumentationsToApply = new HashSet<>();

    private final boolean verboseLog;

    public TrautePluginSettings(@NotNull Set<String> notNullAnnotations,
                                @NotNull Set<InstrumentationType> instrumentationsToApply,
                                boolean verboseLog)
    {
        this.notNullAnnotations.addAll(notNullAnnotations);
        this.instrumentationsToApply.addAll(instrumentationsToApply);
        this.verboseLog = verboseLog;
    }

    @NotNull
    public Set<String> getNotNullAnnotations() {
        return notNullAnnotations;
    }

    public boolean isEnabled(@NotNull InstrumentationType type) {
        return instrumentationsToApply.contains(type);
    }

    public boolean isVerboseLog() {
        return verboseLog;
    }
}
