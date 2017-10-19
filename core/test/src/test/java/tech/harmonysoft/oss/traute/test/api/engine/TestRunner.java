package tech.harmonysoft.oss.traute.test.api.engine;

import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.traute.test.api.expectation.Expectation;
import tech.harmonysoft.oss.traute.test.api.model.CompilationResult;
import tech.harmonysoft.oss.traute.test.api.model.RunResult;

public interface TestRunner {

    @NotNull
    RunResult run(@NotNull CompilationResult compilationResult);

    default void run(@NotNull CompilationResult compilationResult, @NotNull Expectation<RunResult> expectation) {
        expectation.match(run(compilationResult));
    }
}
