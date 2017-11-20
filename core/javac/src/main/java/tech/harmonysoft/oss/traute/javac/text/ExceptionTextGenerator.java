package tech.harmonysoft.oss.traute.javac.text;

import org.jetbrains.annotations.NotNull;

public interface ExceptionTextGenerator<T> {

    @NotNull
    String generate(@NotNull T context);
}
