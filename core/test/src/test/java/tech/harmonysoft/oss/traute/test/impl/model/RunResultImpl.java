package tech.harmonysoft.oss.traute.test.impl.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.harmonysoft.oss.traute.test.api.model.CompilationResult;
import tech.harmonysoft.oss.traute.test.api.model.RunResult;

import java.util.Optional;

public class RunResultImpl implements RunResult {

    @NotNull private final  CompilationResult input;
    @Nullable private final Throwable         exception;

    public RunResultImpl(@NotNull CompilationResult input, @Nullable Throwable exception) {
        this.input = input;
        this.exception = exception;
    }

    @Override
    public @NotNull Optional<Throwable> getException() {
        return Optional.ofNullable(exception);
    }

    @NotNull
    @Override
    public CompilationResult getInput() {
        return input;
    }
}
