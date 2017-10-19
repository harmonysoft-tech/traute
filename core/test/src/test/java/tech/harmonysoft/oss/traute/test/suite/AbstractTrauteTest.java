package tech.harmonysoft.oss.traute.test.suite;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import tech.harmonysoft.oss.traute.common.settings.TrautePluginSettingsBuilder;
import tech.harmonysoft.oss.traute.test.api.engine.TestCompiler;
import tech.harmonysoft.oss.traute.test.api.engine.TestRunner;
import tech.harmonysoft.oss.traute.test.impl.expectation.RunResultExpectationBuilder;

@SuppressWarnings("NullableProblems")
public abstract class AbstractTrauteTest {

    @NotNull protected TestCompiler                compiler;
    @NotNull protected TestRunner                  runner;
    @NotNull protected TrautePluginSettingsBuilder settingsBuilder;
    @NotNull protected RunResultExpectationBuilder expectRunResult;

    @BeforeEach
    public void commonSetUp() {
        settingsBuilder = TrautePluginSettingsBuilder.settingsBuilder();
        expectRunResult = RunResultExpectationBuilder.expectRunResult();
    }
}
