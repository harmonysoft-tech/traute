package tech.harmonysoft.oss.traute.javac.common;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;

public class TrautePluginSettings {

    private static final Set<String> DEFAULT_ANNOTATIONS = new HashSet<>(asList(
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


    private final Set<String> notNullAnnotations = new HashSet<>();

    private final boolean verboseLog;

    public TrautePluginSettings() {
        this(false);
    }

    public TrautePluginSettings(boolean verboseLog) {
        this(DEFAULT_ANNOTATIONS, verboseLog);
    }

    public TrautePluginSettings(@NotNull Set<String> notNullAnnotations, boolean verboseLog) {
        this.notNullAnnotations.addAll(notNullAnnotations);
        this.verboseLog = verboseLog;
    }

    @NotNull
    public Set<String> getNotNullAnnotations() {
        return notNullAnnotations;
    }

    public boolean isVerboseLog() {
        return verboseLog;
    }
}
