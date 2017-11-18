package tech.harmonysoft.oss.traute.javac.log;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FileLogger extends AbstractLogger {

    @NotNull private final File file;

    public FileLogger(@NotNull File file) {
        ensureFileExists(file);
        this.file = file;
    }

    private static void ensureFileExists(@NotNull File file) {
        File dir = file.getParentFile();
        if (dir.isDirectory()) {
            return;
        }
        boolean created = dir.mkdirs();
        if (!created) {
            throw new IllegalStateException(String.format("Can't create log file '%s'", file.getAbsolutePath()));
        }
    }

    @Override
    @NotNull
    public Object getKey() {
        return file.getAbsolutePath().intern();
    }

    @Override
    protected void warn(@NotNull String message) {
        println(message, "WARN");
    }

    @Override
    public void info(@NotNull String message) {
        println(message, "INFO");
    }

    private void println(@NotNull String message, @NotNull String logLevel) {
        try {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                writer.write('[');
                writer.write(logLevel);
                writer.write("] ");
                writer.write(message);
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
