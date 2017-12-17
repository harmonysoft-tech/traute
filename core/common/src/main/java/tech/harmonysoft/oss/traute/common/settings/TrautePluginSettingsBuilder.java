package tech.harmonysoft.oss.traute.common.settings;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.harmonysoft.oss.traute.common.instrumentation.InstrumentationType;

import java.io.File;
import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;

@SuppressWarnings("UnusedReturnValue")
public class TrautePluginSettingsBuilder {

    public static final Set<String> DEFAULT_NOT_NULL_ANNOTATIONS = new HashSet<>(asList(
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
            "org.eclipse.jdt.annotation.NonNull",

            // Lombok - https://projectlombok.org/api/lombok/NonNull.html
            "lombok.NonNull",

            // Spring - https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/lang/NonNull.html
            "org.springframework.lang.NonNull"
    ));

    /**
     * It's possible to specify that method parameters should be treated as {@code not-null} by default.
     * Current collection holds default annotations to use.
     */
    public static final Set<String> DEFAULT_PARAMETERS_NOT_NULL_BY_DEFAULT_ANNOTATIONS = new HashSet<>(asList(
            // Eclipse - https://help.eclipse.org/mars/index.jsp?topic=%2Forg.eclipse.jdt.doc.isv%2Freference%2Fapi%2Forg%2Feclipse%2Fjdt%2Fannotation%2FNonNullByDefault.html
            "org.eclipse.jdt.annotation.NonNullByDefault",

            // JSR-305 - https://static.javadoc.io/com.google.code.findbugs/jsr305/3.0.1/javax/annotation/ParametersAreNonnullByDefault.html
            "javax.annotation.ParametersAreNonnullByDefault",

            // Spring - https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/lang/NonNullApi.html
            "org.springframework.lang.NonNullApi"
    ));

    public static final Set<String> DEFAULT_RETURN_NOT_NULL_BY_DEFAULT_ANNOTATIONS = new HashSet<>(
            // Spring - https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/lang/NonNullApi.html
            singleton("org.springframework.lang.NonNullApi")
    );

    /**
     * This annotations work in conjunction with {@link #DEFAULT_PARAMETERS_NOT_NULL_BY_DEFAULT_ANNOTATIONS}
     * annotations, e.g. particular package/class/method might be marked by, say,
     * {@code javax.annotation.ParametersAreNonnullByDefault} and we want to specify that particular method parameter
     * might be {@code null}.
     */
    public static final Set<String> DEFAULT_NULLABLE_ANNOTATIONS = new HashSet<>(asList(
            // IntelliJ - https://www.jetbrains.com/help/idea/nullable-and-notnull-annotations.html#nullable
            "org.jetbrains.annotations.Nullable",

            // JSR-305 - status=dormant - https://jcp.org/en/jsr/detail?id=305
            "javax.annotation.Nullable",

            // JavaEE - https://docs.oracle.com/javaee/7/api/javax/validation/constraints/Null.html
            "javax.validation.constraints.Null",

            // FindBugs - http://findbugs.sourceforge.net/api/edu/umd/cs/findbugs/annotations/Nullable.html
            "edu.umd.cs.findbugs.annotations.Nullable",

            // Android - https://developer.android.com/reference/android/support/annotation/Nullable.html
            "android.support.annotation.Nullable",

            // Eclipse - http://help.eclipse.org/oxygen/index.jsp?topic=%2Forg.eclipse.jdt.doc.user%2Ftasks%2Ftask-using_null_annotations.htm
            "org.eclipse.jdt.annotation.Nullable",

            // Spring - https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/lang/Nullable.html
            "org.springframework.lang.Nullable"
    ));

    public static final Set<InstrumentationType> DEFAULT_INSTRUMENTATIONS_TO_APPLY =
            EnumSet.allOf(InstrumentationType.class);

    public static final boolean DEFAULT_VERBOSE_MODE = false;

    private final Set<String>              notNullAnnotations      = new HashSet<>();
    private final Set<String>              nullableAnnotations     = new HashSet<>();
    private final Set<InstrumentationType> instrumentationsToApply = EnumSet.noneOf(InstrumentationType.class);

    private final Map<InstrumentationType, String>      exceptionsToThrow           = new HashMap<>();
    private final Map<InstrumentationType, String>      exceptionTextPatterns       = new HashMap<>();
    private final Map<InstrumentationType, Set<String>> notNullByDefaultAnnotations = new HashMap<>();

    @Nullable private File    logFile;
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
    public TrautePluginSettingsBuilder withNullableAnnotations(@NotNull String... nullableAnnotations) {
        this.nullableAnnotations.addAll(Arrays.asList(nullableAnnotations));
        return this;
    }

    @NotNull
    public TrautePluginSettingsBuilder withInstrumentationToApply(@NotNull InstrumentationType type) {
        instrumentationsToApply.add(type);
        return this;
    }

    @NotNull
    public TrautePluginSettingsBuilder withExceptionToThrow(@NotNull InstrumentationType type,
                                                            @NotNull String exceptionToThrow)
    {
        exceptionsToThrow.put(type, exceptionToThrow);
        return this;
    }

    @NotNull
    public TrautePluginSettingsBuilder withExceptionTextPattern(@NotNull InstrumentationType type,
                                                                @NotNull String pattern)
    {
        exceptionTextPatterns.put(type, pattern);
        return this;
    }

    @NotNull
    public TrautePluginSettingsBuilder withNotNullByDefaultAnnotations(@NotNull InstrumentationType type,
                                                                       @NotNull Collection<String> annotations)
    {
        notNullByDefaultAnnotations.put(type, new HashSet<>(annotations));
        return this;
    }

    @NotNull
    public TrautePluginSettingsBuilder withLogFile(@NotNull File file) {
        logFile = file;
        return this;
    }

    @NotNull
    public TrautePluginSettingsBuilder withVerboseMode(boolean verbose) {
        this.verbose = verbose;
        return this;
    }

    @NotNull
    public TrautePluginSettings build() {
        Set<String> notNullAnnotations = new HashSet<>(this.notNullAnnotations);
        if (notNullAnnotations.isEmpty()) {
            notNullAnnotations.addAll(DEFAULT_NOT_NULL_ANNOTATIONS);
        }

        Set<String> nullableAnnotations = new HashSet<>(this.nullableAnnotations);
        if (nullableAnnotations.isEmpty()) {
            nullableAnnotations.addAll(DEFAULT_NULLABLE_ANNOTATIONS);
        }

        Map<InstrumentationType, Set<String>> notNullByDefaultAnnotations
                = new HashMap<>(this.notNullByDefaultAnnotations);
        if (!notNullByDefaultAnnotations.containsKey(InstrumentationType.METHOD_PARAMETER)) {
            notNullByDefaultAnnotations.put(InstrumentationType.METHOD_PARAMETER,
                                            DEFAULT_PARAMETERS_NOT_NULL_BY_DEFAULT_ANNOTATIONS);
        }
        if (!notNullByDefaultAnnotations.containsKey(InstrumentationType.METHOD_RETURN)) {
            notNullByDefaultAnnotations.put(InstrumentationType.METHOD_RETURN,
                                            DEFAULT_RETURN_NOT_NULL_BY_DEFAULT_ANNOTATIONS);
        }

        Set<InstrumentationType> instrumentationsToApply = EnumSet.copyOf(this.instrumentationsToApply);
        if (instrumentationsToApply.isEmpty()) {
            instrumentationsToApply.addAll(DEFAULT_INSTRUMENTATIONS_TO_APPLY);
        }

        Boolean verbose = this.verbose;
        if (verbose == null) {
            verbose = DEFAULT_VERBOSE_MODE;
        }
        return new TrautePluginSettings(notNullAnnotations,
                                        nullableAnnotations,
                                        instrumentationsToApply,
                                        exceptionsToThrow,
                                        exceptionTextPatterns,
                                        notNullByDefaultAnnotations,
                                        logFile,
                                        verbose);
    }
}
