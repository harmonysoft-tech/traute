package tech.harmonysoft.oss.traute.test.api.model;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface RunResult extends Result<CompilationResult> {

    @NotNull
    Optional<Throwable> getException();
}
