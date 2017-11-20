package tech.harmonysoft.oss.traute.javac.text;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

public interface ExceptionTextGeneratorSpi<T> {

    @NotNull
    Set<String> getSupportedVariables();

    @NotNull
    String getVariableValue(@NotNull String variableName, @NotNull T context);
}
