package tech.harmonysoft.oss.traute.javac.common;

import com.sun.tools.javac.util.Log;
import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.traute.javac.TrauteJavacPlugin;

import javax.tools.JavaCompiler;

/**
 * Custom wrapper around standard {@link JavaCompiler javac} {@link Log logger}.
 */
public class TrautePluginLogger {

    private static final String NOTICE_PREFIX = String.format("[%s plugin]: ", TrauteJavacPlugin.NAME);

    @NotNull private final Log log;

    /**
     * <p>
     *     There is a possible case that javac implementation is changed not in a backward compatible way in
     *     a new release. This plugin might stop working then (e.g. new approach should be used for fetching
     *     {@code AST} builder).
     * </p>
     * <p>
     *     We don't want to generate numerous errors for the compilation then but report at most once.
     * </p>
     * <p>
     *     This flag allows to check if a problem has already been reported.
     * </p>
     */
    private boolean problemReported;

    public TrautePluginLogger(@NotNull Log log) {
        this.log = log;
    }

    @NotNull
    public Log getLog() {
        return log;
    }

    public void info(@NotNull String message) {
        log.printRawLines(Log.WriterKind.NOTICE, NOTICE_PREFIX + message);
    }

    /**
     * Delegates to the {@link #report(String)} but adds more generic info to the given problem details
     *
     * @param problemDetails details of the problem to report
     */
    public void reportDetails(@NotNull String problemDetails) {
        report(getProblemMessage(problemDetails));
    }

    /**
     * Shows given problem message to the end-user if necessary.
     *
     * @param message   a problem message to show
     */
    public void report(@NotNull String message) {
        // Do not report a problem more than once
        if (!problemReported) {
            log.rawWarning(-1, message);
            problemReported = true;
        }
    }

    /**
     * Prepares a problem message to show end-user in case the plugin can't do the job during compilation.
     *
     * @param details   exact problem details, will be appended to the general problem description suffix
     * @return          a problem message to use
     */
    @NotNull
    public static String getProblemMessage(@NotNull String details) {
        return String.format(
                "NotNull-instrumentation failed, it might be that javac implementation has significantly changed "
                + "- '%s' javac plugin expected to %s. %s", TrauteJavacPlugin.NAME, details, getProblemMessageSuffix()
        );
    }

    @NotNull
    public static String getProblemMessageSuffix() {
        return "Please contact the plugin's author via traute.java@gmail.com";
    }
}
