package tech.harmonysoft.oss.traute.test.suite;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import tech.harmonysoft.oss.traute.common.settings.TrautePluginSettingsBuilder;
import tech.harmonysoft.oss.traute.test.api.engine.TestCompiler;
import tech.harmonysoft.oss.traute.test.api.engine.TestRunner;
import tech.harmonysoft.oss.traute.test.api.model.CompilationResult;
import tech.harmonysoft.oss.traute.test.impl.expectation.CompilationResultExpectationBuilder;
import tech.harmonysoft.oss.traute.test.impl.expectation.RunResultExpectationBuilder;
import tech.harmonysoft.oss.traute.test.impl.model.TestSourceImpl;
import tech.harmonysoft.oss.traute.test.util.TestUtil;

@SuppressWarnings("NullableProblems")
public abstract class AbstractTrauteTest {

    public static final String ACTIVATION_PROPERTY = AbstractTrauteTest.class.getName();

    @NotNull protected TestCompiler                        compiler;
    @NotNull protected TestRunner                          runner;
    @NotNull protected TrautePluginSettingsBuilder         settingsBuilder;
    @NotNull protected CompilationResultExpectationBuilder expectCompilationResult;
    @NotNull protected RunResultExpectationBuilder         expectRunResult;

    @BeforeEach
    public void commonSetUp() {
        settingsBuilder = TrautePluginSettingsBuilder.settingsBuilder();
        expectCompilationResult = CompilationResultExpectationBuilder.expectCompilationResult();
        expectRunResult = RunResultExpectationBuilder.expectRunResult();

        Assumptions.assumeTrue(Boolean.getBoolean(ACTIVATION_PROPERTY));
    }

    protected void doTest(@NotNull String testSource) {
        doTest(TestUtil.QUALIFIED_CLASS_NAME, testSource);
    }

    protected void doTest(@NotNull String qualifiedClassName, @NotNull String testSource) {
        CompilationResult compilationResult = doCompile(qualifiedClassName, testSource);
        try {
            runner.run(compilationResult, expectRunResult.build());
        } finally {
            compiler.release(compilationResult);
        }
    }

    @NotNull
    protected void doCompile(@NotNull String testSource) {
        doCompile(TestUtil.QUALIFIED_CLASS_NAME, testSource);
    }

    @NotNull
    protected CompilationResult doCompile(@NotNull String qualifiedClassName, @NotNull String testSource) {
        return compiler.compile(new TestSourceImpl(testSource,
                                                   qualifiedClassName,
                                                   settingsBuilder.build()),
                                                                   expectCompilationResult.build());
    }
}
