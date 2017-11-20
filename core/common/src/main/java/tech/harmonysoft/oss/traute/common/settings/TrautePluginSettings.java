package tech.harmonysoft.oss.traute.common.settings;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.harmonysoft.oss.traute.common.instrumentation.InstrumentationType;

import java.io.File;
import java.util.*;

public class TrautePluginSettings {

    public static final String DEFAULT_EXCEPTION_TO_THROW = NullPointerException.class.getSimpleName();

    private final Set<String>                      notNullAnnotations      = new HashSet<>();
    private final Set<InstrumentationType>         instrumentationsToApply = new HashSet<>();
    private final Map<InstrumentationType, String> exceptionsToThrow       = new HashMap<>();
    private final Map<InstrumentationType, String> exceptionTextPatterns   = new HashMap<>();

    @Nullable private final File logFile;

    private final boolean verboseMode;

    public TrautePluginSettings(@NotNull Set<String> notNullAnnotations,
                                @NotNull Set<InstrumentationType> instrumentationsToApply,
                                @NotNull Map<InstrumentationType, String> exceptionsToThrow,
                                @NotNull Map<InstrumentationType, String> exceptionTextPatterns,
                                @Nullable File logFile,
                                boolean verboseMode)
    {
        this.logFile = logFile;
        this.notNullAnnotations.addAll(notNullAnnotations);
        this.instrumentationsToApply.addAll(instrumentationsToApply);
        this.exceptionsToThrow.putAll(exceptionsToThrow);
        this.exceptionTextPatterns.putAll(exceptionTextPatterns);
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
    public String getExceptionToThrow(@NotNull InstrumentationType type) {
        return exceptionsToThrow.getOrDefault(type, DEFAULT_EXCEPTION_TO_THROW);
    }

    @NotNull
    public Map<InstrumentationType, String> getExceptionsToThrow() {
        return exceptionsToThrow;
    }

    @Nullable
    public String getExceptionTextPattern(@NotNull InstrumentationType type) {
        return exceptionTextPatterns.get(type);
    }

    @NotNull
    public Map<InstrumentationType, String> getExceptionTextPatterns() {
        return exceptionTextPatterns;
    }

    @NotNull
    public Optional<File> getLogFile() {
        return Optional.ofNullable(logFile);
    }

    public boolean isVerboseMode() {
        return verboseMode;
    }
}
