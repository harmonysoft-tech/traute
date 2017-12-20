package tech.harmonysoft.oss.traute.test.suite;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import tech.harmonysoft.oss.traute.common.settings.TrautePluginSettingsBuilder;
import tech.harmonysoft.oss.traute.test.api.engine.TestCompiler;
import tech.harmonysoft.oss.traute.test.api.engine.TestRunner;
import tech.harmonysoft.oss.traute.test.api.model.CompilationResult;
import tech.harmonysoft.oss.traute.test.api.model.TestSource;
import tech.harmonysoft.oss.traute.test.impl.expectation.CompilationResultExpectationBuilder;
import tech.harmonysoft.oss.traute.test.impl.expectation.RunResultExpectationBuilder;
import tech.harmonysoft.oss.traute.test.impl.model.TestSourceImpl;
import tech.harmonysoft.oss.traute.test.util.TestDurationPrinter;
import tech.harmonysoft.oss.traute.test.util.TestUtil;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;

@SuppressWarnings("NullableProblems")
@ExtendWith(TestDurationPrinter.class)
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

    protected void doTest(@NotNull TestSource ... testSources) {
        CompilationResult compilationResult = compiler.compile(asList(testSources),
                                                               settingsBuilder.build(),
                                                               expectCompilationResult.build());
        try {
            runner.run(compilationResult, expectRunResult.build());
        } finally {
            compiler.release(compilationResult);
        }
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
        return compiler.compile(singleton(new TestSourceImpl(testSource, qualifiedClassName)),
                                settingsBuilder.build(),
                                expectCompilationResult.build());
    }
}
