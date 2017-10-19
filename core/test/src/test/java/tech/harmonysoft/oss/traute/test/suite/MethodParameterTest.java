package tech.harmonysoft.oss.traute.test.suite;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tech.harmonysoft.oss.traute.common.instrumentation.InstrumentationType;
import tech.harmonysoft.oss.traute.test.api.model.CompilationResult;
import tech.harmonysoft.oss.traute.test.impl.model.TestSourceImpl;
import tech.harmonysoft.oss.traute.test.util.TestConstants;

import javax.annotation.Nonnull;

import static org.junit.jupiter.api.Assertions.fail;
import static tech.harmonysoft.oss.traute.common.util.TrauteConstants.PRIMITIVE_TYPES;
import static tech.harmonysoft.oss.traute.test.util.TestConstants.*;

/**
 * Holds tests which check {@link InstrumentationType#METHOD_PARAMETER method parameter} instrumentation.
 */
public abstract class MethodParameterTest extends AbstractTrauteTest {

    /** Test sources template. All concrete tests fulfill it. */
    private static final String PARAMETER_TEST_CLASS_TEMPLATE =
            "package " + PACKAGE + ";\n" +
            "%s\n" +
            "public class " + CLASS_NAME + " {\n" +
            "\n" +
            "%s\n" +
            "\n" +
            "  public static void main(String[] args) {\n" +
            "    new " + CLASS_NAME + "()." + METHOD_NAME + "(%s);\n" +
            "  }\n" +
            "}";
    private static final String QUALIFIED_CLASS_NAME = PACKAGE + "." + CLASS_NAME;

    @Test
    public void default_jetBrains() {
        doTestAnnotationWithDifferentDeclarationTypes(NotNull.class);
    }

    @Test
    public void default_jsr305() {
        doTestAnnotationWithDifferentDeclarationTypes(Nonnull.class);
    }

    @Test
    public void default_javaEE() {
        doTestAnnotationWithDifferentDeclarationTypes(javax.validation.constraints.NotNull.class);
    }

    @Test
    public void default_findBugs() {
        doTestAnnotationWithDifferentDeclarationTypes(NonNull.class);
    }

    @Test
    public void default_android() {
        doTestAnnotationWithDifferentDeclarationTypes(android.support.annotation.NonNull.class);
    }

    @Test
    public void default_eclipse() {
        doTestAnnotationWithDifferentDeclarationTypes(org.eclipse.jdt.annotation.NonNull.class);
    }

    private void doTestAnnotationWithDifferentDeclarationTypes(@NotNull Class<?> notNullAnnotationClass) {
        doTestAnnotation_singleImport(notNullAnnotationClass);
        doTestAnnotation_wildcardImport(notNullAnnotationClass);
        doTestAnnotation_qualifiedInPlaceName(notNullAnnotationClass);
    }

    private void doTestAnnotation_singleImport(@NotNull Class<?> notNullAnnotationClass) {
        String qualifiedAnnotationClassName = notNullAnnotationClass.getName();
        int lastDotIndex = qualifiedAnnotationClassName.lastIndexOf('.');
        String annotationName = qualifiedAnnotationClassName.substring(lastDotIndex + 1);
        String testSource = prepareTestSource(
                qualifiedAnnotationClassName,
                String.format("public void %s(@%s String param) {}",
                              METHOD_NAME, annotationName),
                "null");
        expectNpeFor(testSource, "param");
        doTest(testSource);
    }

    private void doTestAnnotation_wildcardImport(@NotNull Class<?> notNullAnnotationClass) {
        String qualifiedAnnotationClassName = notNullAnnotationClass.getName();
        int lastDotIndex = qualifiedAnnotationClassName.lastIndexOf('.');
        String annotationName = qualifiedAnnotationClassName.substring(lastDotIndex + 1);
        String testSource = prepareTestSource(
                String.format("%s.*", qualifiedAnnotationClassName.substring(0, lastDotIndex)),
                String.format("public void %s(@%s String param) {}",
                              METHOD_NAME, annotationName),
                "null");
        expectNpeFor(testSource, "param");
        doTest(testSource);
    }

    private void doTestAnnotation_qualifiedInPlaceName(@NotNull Class<?> notNullAnnotationClass) {
        String testSource = prepareTestSource(
                null,
                String.format("public void %s(@%s String param) {}",
                              METHOD_NAME, notNullAnnotationClass.getName()),
                "null");
        expectNpeFor(testSource, "param");
        doTest(testSource);
    }

    @ParameterizedTest
    @CsvSource(delimiter = ':', value = {
            "null, 1:i1:0", "1, null:i2:0"
    })
    public void multipleArguments(@NotNull String callArguments, @NotNull String parameterName) {
        String testSource = prepareTestSource(
                NotNull.class.getName(),
                String.format("public void %s(@NotNull Integer i1,\n                   @NotNull Integer i2) {}",
                              METHOD_NAME),
                callArguments
        );
        expectNpeFor(testSource, parameterName);
        doTest(testSource);
    }

    @Test
    public void primitiveMethodArguments() {
        for (String primitiveType : PRIMITIVE_TYPES) {
            String testSource = prepareTestSource(
                    NotNull.class.getName(),
                    String.format("public void %s(@NotNull %s arg) {}", METHOD_NAME, primitiveType),
                    "(" + primitiveType + ")1"
            );
            doTest(testSource);
            // We expect that no instrumentation occurs for primitive types, otherwise we get a compilation
            // error on attempt to compile a check like 'if (arg == null)' where 'arg' is, say, 'int'.
        }
    }

    @Test
    public void subsequentlineNumbersArePreserved() {
        Class<? extends RuntimeException> exceptionClass = IllegalArgumentException.class;
        String testSource = prepareTestSource(
                NotNull.class.getName(),
                String.format("public void %s(@NotNull Integer i) {\n" +
                              "  if (System.currentTimeMillis() > 1) {\n" +
                              "    throw new %s();\n" +
                              "  }\n" +
                              "}", METHOD_NAME, exceptionClass.getName()),
                "1"
        );
        expectRunResult.withExceptionClass(exceptionClass)
                       .atLine(findLineNumber(testSource, exceptionClass.getName()));
        doTest(testSource);
    }

    private void doTest(@NotNull String testSource) {
        CompilationResult compilationResult = compiler.compile(new TestSourceImpl(testSource,
                                                                                  QUALIFIED_CLASS_NAME,
                                                                                  settingsBuilder.build()));
        runner.run(compilationResult, expectRunResult.build());
    }

    /**
     * Applies given data to the {@link #PARAMETER_TEST_CLASS_TEMPLATE test source template}.
     *
     * @param importString  an expression for the {@code import} keyword (if any)
     * @param testMethod    {@value TestConstants#METHOD_NAME}() method body
     * @param callArguments arguments to use for calling the {@value TestConstants#METHOD_NAME}() method
     * @return              complete test source for the given arguments. It's assumed to be properly formatted
     */
    @NotNull
    private static String prepareTestSource(@Nullable String importString,
                                            @NotNull String testMethod,
                                            @NotNull String callArguments)
    {
        String completeImportString = importString == null ? "" : String.format("%nimport %s;%n", importString);

        int declarationEnd = testMethod.indexOf('{') + 1;
        int bodyStart = testMethod.indexOf('\n', declarationEnd);
        if (bodyStart < 0) {
            bodyStart = declarationEnd;
        } else {
            bodyStart++;
        }
        int methodEnd = testMethod.lastIndexOf('}');
        int bodyEnd = methodEnd > 0 ? testMethod.lastIndexOf('\n', methodEnd) : methodEnd;
        if (bodyEnd <= bodyStart) {
            bodyEnd = methodEnd;
        }
        String testMethodDeclaration = testMethod.substring(0, declarationEnd);
        String testMethodBody = testMethod.substring(bodyStart, bodyEnd);
        String indentedTestMethodBody = "  " + testMethodBody.replaceAll("\n", "\n    ");
        String indentedTestMethod = String.format("  %s%n%s%n  }", testMethodDeclaration, indentedTestMethodBody);

        return String.format(PARAMETER_TEST_CLASS_TEMPLATE, completeImportString, indentedTestMethod, callArguments);
    }

    /**
     * Configures expectations for running test code assuming that a plugin-introduced {@code null}-check
     * should be triggered for the target method parameter.
     *
     * @param testSource        test source
     * @param parameterName     name of the {@code NotNull} parameter which should be reported
     */
    private void expectNpeFor(@NotNull String testSource, @NotNull String parameterName) {
        expectRunResult.withExceptionClass(NullPointerException.class);
        expectRunResult.withExceptionMessageSnippet(parameterName);
        int line = findLineNumber(testSource, parameterName);
        expectRunResult.atLine(line);
    }

    private int findLineNumber(@NotNull String text, @NotNull String marker) {
        int i = text.indexOf(marker);
        if (i <= 0) {
            fail(String.format("Can't find line number for the text '%s'. It's not contained in the source:%n%s",
                               marker, text));
        }
        int line = 1;
        for (int j = 0; j < i; j++) {
            if (text.charAt(j) == '\n') {
                line++;
            }
        }
        return line;
    }


}
