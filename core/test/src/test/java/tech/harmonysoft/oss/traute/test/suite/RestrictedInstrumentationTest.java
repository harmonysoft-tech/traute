package tech.harmonysoft.oss.traute.test.suite;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import tech.harmonysoft.oss.traute.test.util.TestUtil;

import static tech.harmonysoft.oss.traute.common.instrumentation.InstrumentationType.METHOD_PARAMETER;
import static tech.harmonysoft.oss.traute.common.instrumentation.InstrumentationType.METHOD_RETURN;
import static tech.harmonysoft.oss.traute.test.util.TestConstants.METHOD_NAME;
import static tech.harmonysoft.oss.traute.test.util.TestUtil.prepareParameterTestSource;

public abstract class RestrictedInstrumentationTest extends AbstractTrauteTest {

    @Test
    public void restrictedInstrumentation_parameterOnly_noCheckForReturn() {
        settingsBuilder.withInstrumentationToApply(METHOD_PARAMETER);
        String testSource = TestUtil.prepareReturnTestSource("return count();");
        // Expecting null-check for return type not to be generated, hence, no exception will be thrown
        doTest(testSource);
    }

    @Test
    public void parameterOnly_active() {
        settingsBuilder.withInstrumentationToApply(METHOD_PARAMETER);
        String testSource = prepareParameterTestSource(NotNull.class.getName(),
                                                       String.format("void %s(@NotNull String param) {}", METHOD_NAME),
                                                       "null");
        TestUtil.expectNpeFromParameterCheck(testSource, "param", expectRunResult);
        doTest(testSource);
    }

    @Test
    public void restrictedInstrumentation_returnOnly_noCheckForParameter() {
        settingsBuilder.withInstrumentationToApply(METHOD_RETURN);
        String testSource = prepareParameterTestSource(NotNull.class.getName(),
                                                       "void test(@NotNull String s) {}",
                                                       "null");
        // Expecting null-check for parameter not to be generated, hence, no exception will be thrown
        doTest(testSource);
    }
}
