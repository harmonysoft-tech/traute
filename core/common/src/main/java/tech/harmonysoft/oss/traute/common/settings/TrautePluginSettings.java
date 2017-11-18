package tech.harmonysoft.oss.traute.common.settings;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.harmonysoft.oss.traute.common.instrumentation.InstrumentationType;

import java.io.File;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class TrautePluginSettings {

    private final Set<String>              notNullAnnotations      = new HashSet<>();
    private final Set<InstrumentationType> instrumentationsToApply = new HashSet<>();

    @Nullable private final File logFile;

    private final boolean verboseMode;

    public TrautePluginSettings(@NotNull Set<String> notNullAnnotations,
                                @NotNull Set<InstrumentationType> instrumentationsToApply,
                                @Nullable File logFile,
                                boolean verboseMode)
    {
        this.logFile = logFile;
        this.notNullAnnotations.addAll(notNullAnnotations);
        this.instrumentationsToApply.addAll(instrumentationsToApply);
        this.verboseMode = verboseMode;
    }

    @NotNull
    public Set<String> getNotNullAnnotations() {
        return notNullAnnotations;
    }

    public boolean isEnabled(@NotNull InstrumentationType type) {
        return instrumentationsToApply.contains(type);
    }

    @NotNull
    public Set<InstrumentationType> getInstrumentationsToApply() {
        return instrumentationsToApply;
    }

    @NotNull
    public Optional<File> getLogFile() {
        return Optional.ofNullable(logFile);
    }

    public boolean isVerboseMode() {
        return verboseMode;
    }
}
