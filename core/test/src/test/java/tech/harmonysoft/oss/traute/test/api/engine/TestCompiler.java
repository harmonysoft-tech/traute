package tech.harmonysoft.oss.traute.test.api.engine;

import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.traute.test.api.expectation.Expectation;
import tech.harmonysoft.oss.traute.test.api.model.CompilationResult;
import tech.harmonysoft.oss.traute.test.api.model.TestSource;

/**
 * Defines an interface for a service which knows how to produce a {@link CompilationResult} from a {@link TestSource}.
 */
public interface TestCompiler {

    /**
     * Produces a {@link CompilationResult} from the given {@link TestSource}
     *
     * @param testSource    test to compile
     * @return              compilation result
     */
    @NotNull
    CompilationResult compile(@NotNull TestSource testSource);

    /**
     * Behaves like {@link #compile(TestSource)} but additionally checks resulting {@link CompilationResult}
     * against the given {@link Expectation}
     *
     * @param testSource    a test source to compile
     * @param expectation   a compilation result's expectation
     * @return              compilation result for the given test source
     */
    @NotNull
    default CompilationResult compile(@NotNull TestSource testSource,
                                      @NotNull Expectation<CompilationResult> expectation)
    {
        CompilationResult result = compile(testSource);
        expectation.match(result);
        return result;
    }
}
