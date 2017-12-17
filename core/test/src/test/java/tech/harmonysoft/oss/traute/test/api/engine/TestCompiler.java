package tech.harmonysoft.oss.traute.test.api.engine;

import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.traute.common.settings.TrautePluginSettings;
import tech.harmonysoft.oss.traute.test.api.expectation.Expectation;
import tech.harmonysoft.oss.traute.test.api.model.CompilationResult;
import tech.harmonysoft.oss.traute.test.api.model.TestSource;

import java.util.Collection;

/**
 * Defines an interface for a service which knows how to produce a {@link CompilationResult} from a {@link TestSource}.
 */
public interface TestCompiler {

    /**
     * Produces a {@link CompilationResult} from the given {@link TestSource}
     *
     * @param testSources    test sources to compile
     * @param settings       plugin settings to use
     * @return               compilation result
     */
    @NotNull
    CompilationResult compile(@NotNull Collection<TestSource> testSources, @NotNull TrautePluginSettings settings);

    /**
     * Behaves like {@link #compile(Collection, TrautePluginSettings)} but additionally checks
     * resulting {@link CompilationResult} against the given {@link Expectation}
     *
     * @param testSources    test sources to compile
     * @param settings       plugin settings to use
     * @param expectation    a compilation result's expectation
     * @return               compilation result for the given test sources
     */
    @NotNull
    default CompilationResult compile(@NotNull Collection<TestSource> testSources,
                                      @NotNull TrautePluginSettings settings,
                                      @NotNull Expectation<CompilationResult> expectation)
    {
        CompilationResult result = compile(testSources, settings);
        expectation.match(result);
        return result;
    }

    /**
     * @param result    notifies that given compilation result is not needed anymore and all resources associated
     *                  with it can be released
     */
    void release(@NotNull CompilationResult result);
}
