package tech.harmonysoft.oss.traute.javac.log;

import org.jetbrains.annotations.NotNull;

/**
 * Defines plugin logger's {@code API}.
 */
public interface TrautePluginLogger {

    void info(@NotNull String message);

    /**
     * Delegates to the {@link #report(String)} but adds more generic info to the given problem details
     *
     * @param problemDetails details of the problem to report
     */
    void reportDetails(@NotNull String problemDetails);

    /**
     * Asks to report given problem message (the message is not modified and reported as-is).
     *
     * @param message   a message to report
     */
    void report(@NotNull String message);
}
