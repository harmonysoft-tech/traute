package tech.harmonysoft.oss.traute.javac.log;

import com.sun.tools.javac.util.Log;
import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.traute.common.util.TrauteConstants;

import javax.tools.JavaCompiler;

/**
 * Custom wrapper around standard {@link JavaCompiler javac} {@link Log logger}.
 */
public class CompilerOutputLogger extends AbstractLogger {

    private static final String NOTICE_PREFIX = String.format("[%s plugin]: ", TrauteConstants.PLUGIN_NAME);

    @NotNull private final Log log;

    public CompilerOutputLogger(@NotNull Log log) {
        this.log = log;
    }

    @Override
    @NotNull
    public Log getKey() {
        return log;
    }

    @Override
    public void info(@NotNull String message) {
        log.printRawLines(Log.WriterKind.NOTICE, NOTICE_PREFIX + message);
    }

    @Override
    public void warn(@NotNull String message) {
        log.rawWarning(-1, message);
    }
}
