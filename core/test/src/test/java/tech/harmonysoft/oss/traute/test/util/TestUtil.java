package tech.harmonysoft.oss.traute.test.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.harmonysoft.oss.traute.test.api.model.TestSource;
import tech.harmonysoft.oss.traute.test.impl.expectation.RunResultExpectationBuilder;

import java.util.Collection;

import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.fail;
import static tech.harmonysoft.oss.traute.test.util.TestConstants.CLASS_NAME;
import static tech.harmonysoft.oss.traute.test.util.TestConstants.METHOD_NAME;
import static tech.harmonysoft.oss.traute.test.util.TestConstants.PACKAGE;

public class TestUtil {

    public static final String PARAMETER_TEST_CLASS_TEMPLATE =
            "package %s;\n" +
            "%s\n" +
            "public class " + CLASS_NAME + " {\n" +
            "\n" +
            "%s\n" +
            "\n" +
            "  public static void main(String[] args) {\n" +
            "    new " + CLASS_NAME + "()." + METHOD_NAME + "(%s);\n" +
            "  }\n" +
            "}";

    public static final String METHOD_TEST_CLASS_TEMPLATE = String.format(
            "package %s;\n" +
            "\n" +
            "public class %s {\n" +
            "\n" +
            "  @%%s\n" +
            "  public Integer test() {\n" +
            "    %%s\n" +
            "  }\n" +
            "\n" +
            "  private Integer count() {\n" +
            "      return null;\n" +
            "  }\n" +
            "\n" +
            "  public static void main(String[] args) {\n" +
            "    new Test().test();\n" +
            "  }\n" +
            "}", PACKAGE, CLASS_NAME);

    public static final String QUALIFIED_CLASS_NAME = PACKAGE + "." + CLASS_NAME;

    private TestUtil() {
    }

    /**
     * Delegates to the {@link #prepareParameterTestSource(String, String, String, String)} with
     * {@link TestConstants#PACKAGE} as a package name.
     *
     * @param importString  an expression for the {@code import} keyword (if any)
     * @param testMethod    {@value TestConstants#METHOD_NAME}() method body
     * @param callArguments arguments to use for calling the {@value TestConstants#METHOD_NAME}() method
     * @return              complete test source for the given arguments. It's assumed to be properly formatted
     */
    @NotNull
    public static String prepareParameterTestSource(@Nullable String importString,
                                                    @NotNull String testMethod,
                                                    @NotNull String callArguments)
    {
        return prepareParameterTestSource(PACKAGE, importString, testMethod, callArguments);
    }

    /**
     * Applies given data to the {@link TestUtil#PARAMETER_TEST_CLASS_TEMPLATE test source template}.
     *
     * @param packageName   package name to use
     * @param importString  an expression for the {@code import} keyword (if any)
     * @param testMethod    {@value TestConstants#METHOD_NAME}() method body
     * @param callArguments arguments to use for calling the {@value TestConstants#METHOD_NAME}() method
     * @return              complete test source for the given arguments. It's assumed to be properly formatted
     */
    @NotNull
    public static String prepareParameterTestSource(@NotNull String packageName,
                                                    @Nullable String importString,
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

        return String.format(PARAMETER_TEST_CLASS_TEMPLATE,
                             packageName, completeImportString, indentedTestMethod, callArguments);
    }

    @NotNull
    public static String prepareReturnTestSource(@NotNull String methodBody) {
        return prepareReturnTestSource(NotNull.class, methodBody);
    }

    @NotNull
    public static String prepareReturnTestSource(@NotNull Class<?> notNullAnnotationClass, @NotNull String methodBody) {
        return String.format(METHOD_TEST_CLASS_TEMPLATE,
                             notNullAnnotationClass.getName(),
                             methodBody.replaceAll("\n", "\n    "));
    }

    public static int findLineNumber(@NotNull String text, @NotNull String marker) {
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

    /**
     * Configures expectations for running test code assuming that a plugin-introduced {@code null}-check
     * should be triggered for the target method parameter.
     *
     * @param testSource        test source
     * @param parameterName     name of the {@code NotNull} parameter which should be reported
     * @param expectRunResult   test run expectations
     */
    public static void expectNpeFromParameterCheck(@NotNull String testSource,
                                                   @NotNull String parameterName,
                                                   @NotNull RunResultExpectationBuilder expectRunResult)
    {
        expectRunResult.withExceptionClass(NullPointerException.class)
                       .withExceptionMessageSnippet(parameterName)
                       .atLine(findLineNumber(testSource, parameterName));
    }

    public static void expectNpeFromReturnCheck(@NotNull String testSource,
                                                @NotNull String returnExpression,
                                                @NotNull RunResultExpectationBuilder expectRunResult)
    {
        expectRunResult.withExceptionClass(NullPointerException.class)
                       .withExceptionMessageSnippet("Detected an attempt to return null from a method")
                       .atLine(findLineNumber(testSource, returnExpression));
    }

    @NotNull
    public static String getSources(@NotNull Collection<TestSource> sources) {
        return sources.stream()
                      .map(s -> s.getQualifiedClassName() + ".java:\n\n" + s.getSourceText())
                      .collect(joining("\n\n"));
    }
}
