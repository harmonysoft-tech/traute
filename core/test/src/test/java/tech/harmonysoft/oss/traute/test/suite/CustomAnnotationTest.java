package tech.harmonysoft.oss.traute.test.suite;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import tech.harmonysoft.oss.traute.test.fixture.NN;

import javax.annotation.Nonnull;

import static tech.harmonysoft.oss.traute.test.util.TestConstants.METHOD_NAME;
import static tech.harmonysoft.oss.traute.test.util.TestUtil.*;

/**
 * Holds tests which check processing for custom annotations defined by end-user.
 */
public abstract class CustomAnnotationTest extends AbstractTrauteTest {

    @Test
    public void reducedNumber_parameter() {
        settingsBuilder.withNotNullAnnotations(Nonnull.class.getName());
        String testSource = prepareParameterTestSource(
                NotNull.class.getName(),
                String.format("public void %s(@NotNull Integer i1, @%s Integer i2) {}",
                              METHOD_NAME, Nonnull.class.getName()),
                "null, null"
        );
        // We expect the plugin to not generate a check for the 'i1' argument since it's annotation (enabled
        // by default) is not listed in the user-defined annotations
        expectNpeFromParameterCheck(testSource, "i2", expectRunResult);
        doTest(testSource);
    }

    @Test
    public void reducedNumber_return_match() {
        settingsBuilder.withNotNullAnnotations(NotNull.class.getName());
        String methodBody = "return count();";
        String testSource = prepareReturnTestSource(NotNull.class, methodBody);
        // We expect the plugin to generate a check because method is marked by a 'custom' annotation
        expectNpeFromReturnCheck(testSource, methodBody, expectRunResult);
        doTest(testSource);
    }

    @Test
    public void reducedNumber_return_noMatch() {
        settingsBuilder.withNotNullAnnotations(Nonnull.class.getName());
        String methodBody = "return count();";
        String testSource = prepareReturnTestSource(NotNull.class, methodBody);
        // We expect do not expect the plugin to generate a check because method is marked by an annotation
        // which is not listed in the 'custom annotations'
        doTest(testSource);
    }

    @Test
    public void moreThanOneCustomAnnotation() {
        settingsBuilder.withNotNullAnnotations(Nonnull.class.getName(), NotNull.class.getName());
        String testSource = prepareParameterTestSource(
                null,
                String.format("public void %s(@%s Integer i1, @%s Integer i2) {}",
                              METHOD_NAME, org.eclipse.jdt.annotation.NonNull.class.getName(), Nonnull.class.getName()),
                "null, null"
        );
        expectNpeFromParameterCheck(testSource, "i2", expectRunResult);
        doTest(testSource);
    }

    @Test
    public void trulyCustomAnnotation() {
        settingsBuilder.withNotNullAnnotations(NN.class.getName());
        String testSource = prepareParameterTestSource(
                NN.class.getName(),
                String.format("public void %s(@%s Integer i1) {}", METHOD_NAME, NN.class.getSimpleName()),
                "null"
        );
        expectNpeFromParameterCheck(testSource, "i1", expectRunResult);
        doTest(testSource);
    }
}
