package tech.harmonysoft.oss.traute.javac.settings;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.harmonysoft.oss.traute.javac.common.InstrumentationType;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;

@SuppressWarnings("UnusedReturnValue")
public class TrautePluginSettingsBuilder {

    private static final Set<String> DEFAULT_NOT_NULL_ANNOTATIONS = new HashSet<>(asList(
            // Used by IntelliJ by default - https://www.jetbrains.com/help/idea/nullable-and-notnull-annotations.html
            "org.jetbrains.annotations.NotNull",

            // JSR-305 - status=dormant - https://jcp.org/en/jsr/detail?id=305
            "javax.annotation.Nonnull",

            // JavaEE - https://docs.oracle.com/javaee/7/api/javax/validation/constraints/NotNull.html
            "javax.validation.constraints.NotNull",

            // FindBugs - http://findbugs.sourceforge.net/api/edu/umd/cs/findbugs/annotations/NonNull.html
            "edu.umd.cs.findbugs.annotations.NonNull",

            // Android - https://developer.android.com/reference/android/support/annotation/NonNull.html
            "android.support.annotation.NonNull",

            // Eclipse - http://help.eclipse.org/oxygen/index.jsp?topic=%2Forg.eclipse.jdt.doc.user%2Ftasks%2Ftask-using_null_annotations.htm
            "org.eclipse.jdt.annotation.NonNull"
    ));

    private static final Set<InstrumentationType> DEFAULT_INSTRUMENTATIONS_TO_APPLY =
            EnumSet.allOf(InstrumentationType.class);

    private static final boolean DEFAULT_VERBOSE_MODE = false;

    private final Set<String>              notNullAnnotations      = new HashSet<>();
    private final Set<InstrumentationType> instrumentationsToApply = EnumSet.noneOf(InstrumentationType.class);

    @Nullable private Boolean verbose;

    @NotNull
    public static TrautePluginSettingsBuilder settingsBuilder() {
        return new TrautePluginSettingsBuilder();
    }

    @NotNull
    public TrautePluginSettingsBuilder withNotNullAnnotations(@NotNull String... notNullAnnotations) {
        this.notNullAnnotations.addAll(Arrays.asList(notNullAnnotations));
        return this;
    }

    @NotNull
    public TrautePluginSettingsBuilder withInstrumentationToApply(@NotNull InstrumentationType type) {
        instrumentationsToApply.add(type);
        return this;
    }

    @NotNull
    public TrautePluginSettingsBuilder withVerbose(boolean verbose) {
        this.verbose = verbose;
        return this;
    }

    @NotNull
    public TrautePluginSettings build() {
        Set<String> notNullAnnotations = new HashSet<>(this.notNullAnnotations);
        if (notNullAnnotations.isEmpty()) {
            notNullAnnotations.addAll(DEFAULT_NOT_NULL_ANNOTATIONS);
        }

        Set<InstrumentationType> instrumentationsToApply = EnumSet.copyOf(this.instrumentationsToApply);
        if (instrumentationsToApply.isEmpty()) {
            instrumentationsToApply.addAll(DEFAULT_INSTRUMENTATIONS_TO_APPLY);
        }

        Boolean verbose = this.verbose;
        if (verbose == null) {
            verbose = DEFAULT_VERBOSE_MODE;
        }
        return new TrautePluginSettings(notNullAnnotations, instrumentationsToApply, verbose);
    }
}
