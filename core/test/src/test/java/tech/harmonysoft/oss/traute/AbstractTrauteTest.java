package tech.harmonysoft.oss.traute;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import tech.harmonysoft.oss.traute.fixture.NN;
import tech.harmonysoft.oss.traute.util.SimpleClassFile;
import tech.harmonysoft.oss.traute.util.SimpleFileManager;
import tech.harmonysoft.oss.traute.util.SimpleSourceFile;
import tech.harmonysoft.oss.traute.util.TestConstants;

import javax.annotation.Nonnull;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;

/**
 * <p>Defines common scenarios and checks for all {@code @NotNull}-instrumentation approaches (javac, asm).</p>
 * <p>
 *     The main idea is to do the following:
 *     <ul>
 *       <li>define a number of test scenarios (sources to be instrumented)</li>
 *       <li>compile every test source in-memory using {@link JavaCompiler} api</li>
 *       <li>load compiled class in the current process</li>
 *       <li>
 *           start {@code main()} method in the compiled class and ensure that an {@link NullPointerException}
 *           is thrown
 *       </li>
 *       <li>
 *           delegate to concrete implementation test setup details, e.g. allow to
 *           {@link #getAdditionalCompilerArgs() modify javac arguments}
 *           (necessary for the implementation based on {@code javac} plugins api) and
 *           {@link #getTargetAnnotationsToUse() emulate user's setup for @NotNull annotations}
 *       </li>
 *     </ul>
 * </p>
 */
public abstract class AbstractTrauteTest {

    /** Name of the method to be instrumented in the test sources. */
    private static final String METHOD_NAME = "test";

    /** Arguments to call {@code main()} in the compiled test sources. */
    private static final Object[] MAIN_ARGUMENTS = { new String[0]};

    /** Test sources template. All concrete tests fulfill it. */
    private static final String PARAMETER_TEST_CLASS_TEMPLATE =
            "package " + TestConstants.PACKAGE + ";\n" +
            "%s\n" +
            "public class " + TestConstants.CLASS_NAME + " {\n" +
            "\n" +
            "%s\n" +
            "\n" +
            "  public static void main(String[] args) {\n" +
            "    new " + TestConstants.CLASS_NAME + "()." + METHOD_NAME + "(%s);\n" +
            "  }\n" +
            "}";

    private static final String METHOD_TEST_CLASS_TEMPLATE = String.format(
            "package %s;\n" +
            "\n" +
            "public class %s {\n" +
            "\n" +
            "  @%s\n" +
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
            "}", TestConstants.PACKAGE, TestConstants.CLASS_NAME, NotNull.class.getName());

    private static final String[] PRIMITIVE_TYPES = {
            "byte", "short", "char", "int", "long", "float", "double"
    };

    /** Emulates user's setup for the {@code @NotNull} annotations to use. */
    private final Set<String> targetAnnotationsToUse = new HashSet<>();

    @Before
    public void setUp() {
        targetAnnotationsToUse.clear();
    }

    /**
     * @return      additional {@code javac} arguments to use. It's allowed to return an empty list
     */
    @NotNull
    protected List<String> getAdditionalCompilerArgs() {
        return emptyList();
    }

    /**
     * @return  {@code @NotNull} annotations defined by the end-user
     */
    @NotNull
    protected Set<String> getTargetAnnotationsToUse() {
        return targetAnnotationsToUse;
    }

    @Test
    public void default_jetBrains() {
        doTestArgument(NotNull.class.getName());
    }

    @Test
    public void default_jsr305() {
        doTestArgument(Nonnull.class.getName());
    }

    @Test
    public void default_javaEE() {
        doTestArgument(javax.validation.constraints.NotNull.class.getName());
    }

    @Test
    public void default_findBugs() {
        doTestArgument(NonNull.class.getName());
    }

    @Test
    public void default_android() {
        doTestArgument(android.support.annotation.NonNull.class.getName());
    }

    @Test
    public void default_eclipse() {
        doTestArgument(org.eclipse.jdt.annotation.NonNull.class.getName());
    }

    private void doTestArgument(@NotNull String qualifiedAnnotationClassName) {
        doTestArgument_singleImport(qualifiedAnnotationClassName);
        doTestArgument_wildcardImport(qualifiedAnnotationClassName);
        doTestArgument_qualifiedInPlaceName(qualifiedAnnotationClassName);
    }

    private void doTestArgument_singleImport(@NotNull String qualifiedAnnotationClassName) {
        int lastDotIndex = qualifiedAnnotationClassName.lastIndexOf('.');
        String annotationName = qualifiedAnnotationClassName.substring(lastDotIndex + 1);
        doTestExpectNpe(qualifiedAnnotationClassName,
                        String.format("public void %s(@%s String s) {}", METHOD_NAME, annotationName));
    }

    private void doTestArgument_wildcardImport(@NotNull String qualifiedAnnotationClassName) {
        int lastDotIndex = qualifiedAnnotationClassName.lastIndexOf('.');
        String annotationName = qualifiedAnnotationClassName.substring(lastDotIndex + 1);
        doTestExpectNpe(String.format("%s.*", qualifiedAnnotationClassName.substring(0, lastDotIndex)),
                        String.format("public void %s(@%s String s) {}", METHOD_NAME, annotationName));
    }

    private void doTestArgument_qualifiedInPlaceName(@NotNull String qualifiedAnnotationClassName) {
        doTestExpectNpe(null,
                        String.format("public void %s(@%s String s) {}", METHOD_NAME, qualifiedAnnotationClassName));
    }

    private void doTestExpectNpe(@Nullable String importString,
                                 @NotNull String testMethod)
    {
        String testSource = prepareSourceTextForParameterTest(importString, testMethod, "null");
        byte[] compiledTestSource = compile(testSource);
        boolean gotNpe = false;
        try {
            run(compiledTestSource);
        } catch (NullPointerException e) {
            gotNpe = true;
        }
        if (!gotNpe) {
            fail(String.format("Expected to get a NullPointerException on attempt to call 'new Test().test(null)' "
                               + "for the source below but that didn't happen.%n%nSource:%n%n%s",
                               testSource
            ));
        }
    }

    @Test
    public void argumentIndex() {
        doTestArgumentIndex("null, 1", 0);
        doTestArgumentIndex("1, null", 1);
    }

    private void doTestArgumentIndex(@NonNull String callArguments, int expectedIndexToBeReported) {
        String annotation = NotNull.class.getName();
        String testSource = prepareSourceTextForParameterTest(
                annotation,
                String.format("public void %s(@NotNull Integer i1, @NotNull Integer i2) {}", METHOD_NAME),
                callArguments
        );
        byte[] compiledTestSource = compile(testSource);
        boolean passed = false;
        try {
            run(compiledTestSource);
        } catch (NullPointerException e) {
            String message = e.getMessage();
            assert message != null;
            assert message.contains("#" + expectedIndexToBeReported);
            passed = true;
        }
        if (!passed) {
            fail(String.format("Expected to get a NPE on attempt to execute the source below but that "
                               + "didn't happen:%n%n%s", testSource));
        }
    }

    @Test
    public void lineNumberInNpeTrace() {
        String testSource = prepareSourceTextForParameterTest(
                NotNull.class.getName(),
                String.format("public void %s(@NotNull Integer i,\n                   @NotNull Integer i2) {}",
                              METHOD_NAME),
                "1, null"
        );
        int i = testSource.indexOf("@NotNull Integer i2");
        long expectedLine = testSource.substring(0, i).chars().filter(c -> c == '\n').count() + 1;
        byte[] compiledTestSource = compile(testSource);
        boolean passed = false;
        try {
            run(compiledTestSource);
        } catch (NullPointerException e) {
            StackTraceElement[] stackTrace = e.getStackTrace();
            assert stackTrace.length > 0;
            assertEquals("Expected that a NPE thrown from the plugin-introduced check points to the valid line",
                         expectedLine, stackTrace[0].getLineNumber());
            passed = true;
        }
        if (!passed) {
            fail(String.format("Expected to get a NPE on attempt to execute the source below but that "
                               + "didn't happen:%n%n%s", testSource));
        }
    }

    @Test
    public void primitiveMethodArguments() {
        for (String primitiveType : PRIMITIVE_TYPES) {
            String testSource = prepareSourceTextForParameterTest(
                    NotNull.class.getName(),
                    String.format("public void %s(@NotNull %s arg) {}", METHOD_NAME, primitiveType),
                    "(" + primitiveType + ")1"
            );
            byte[] compiledTestSource = compile(testSource);
            run(compiledTestSource);
            // We expect that no instrumentation occurs for primitive types, otherwise we get a compilation
            // error on attempt to compile a check like 'if (arg == null)' where 'arg' is, say, 'int'.
        }
    }

    @Test
    public void lineNumbersAfterNullCheck() {
        String testSource = prepareSourceTextForParameterTest(
                NotNull.class.getName(),
                String.format("public void %s(@NotNull Integer i) {\n" +
                              "  if (System.currentTimeMillis() > 1) {\n" +
                              "    throw new IllegalArgumentException();\n" +
                              "  }\n" +
                              "}", METHOD_NAME),
                "1"
        );
        byte[] compiledTestSource = compile(testSource);
        int i = testSource.indexOf("new IllegalArgumentException()");
        long expectedLine = testSource.substring(0, i).chars().filter(c -> c == '\n').count() + 1;
        boolean passed = false;
        try {
            run(compiledTestSource);
        } catch (IllegalArgumentException e) {
            StackTraceElement[] stackTrace = e.getStackTrace();
            assert stackTrace.length > 0;
            assertEquals("Expected that an exception thrown after a plugin-introduced check points to "
                         + "the valid line", expectedLine, stackTrace[0].getLineNumber());
            passed = true;
        }
        if (!passed) {
            fail(String.format("Expected to get an IllegalArgumentException exception on attempt to execute the "
                               + "source below but that didn't happen:%n%n%s", testSource));
        }
    }

    @Test
    public void customAnnotations_reducedNumber() {
        targetAnnotationsToUse.add(Nonnull.class.getName());
        String testSource = prepareSourceTextForParameterTest(
                null,
                String.format("public void %s(@%s Integer i1, @%s Integer i2) {}",
                              METHOD_NAME, NotNull.class.getName(), Nonnull.class.getName()),
                "null, null"
        );
        byte[] compiledTestSource = compile(testSource);
        boolean passed = false;
        try {
            run(compiledTestSource);
        } catch (NullPointerException e) {
            assertTrue("Expected that no null-check is generated for a default annotation which is "
                       + "not mentioned in the custom @NotNull annotations setting", e.getMessage().contains("i2"));
            passed = true;
        }
        if (!passed) {
            fail(String.format("Expected to get a NPE on attempt to execute the source below but that "
                               + "didn't happen:%n%n%s", testSource));
        }
    }

    @Test
    public void customAnnotations_moreThanOne() {
        targetAnnotationsToUse.addAll(asList(Nonnull.class.getName(), NotNull.class.getName()));
        String testSource = prepareSourceTextForParameterTest(
                null,
                String.format("public void %s(@%s Integer i1, @%s Integer i2) {}",
                              METHOD_NAME, org.eclipse.jdt.annotation.NonNull.class.getName(), Nonnull.class.getName()),
                "null, null"
        );
        byte[] compiledTestSource = compile(testSource);
        boolean passed = false;
        try {
            run(compiledTestSource);
        } catch (NullPointerException e) {
            assertTrue("Expected that no null-check is generated for a default annotation which is "
                       + "not mentioned in the custom @NotNull annotations setting", e.getMessage().contains("i2"));
            passed = true;
        }
        if (!passed) {
            fail(String.format("Expected to get a NPE on attempt to execute the source below but that "
                               + "didn't happen:%n%n%s", testSource));
        }
    }

    @Test
    public void customAnnotations_trulyCustom() {
        targetAnnotationsToUse.add(NN.class.getName());
        String testSource = prepareSourceTextForParameterTest(
                NN.class.getName(),
                String.format("public void %s(@%s Integer i1) {}", METHOD_NAME, NN.class.getSimpleName()),
                "null"
        );
        byte[] compiledTestSource = compile(testSource);
        boolean passed = false;
        try {
            run(compiledTestSource);
        } catch (NullPointerException e) {
            passed = true;
        }
        if (!passed) {
            fail(String.format("Expected to get a NPE on attempt to execute the source below but that "
                               + "didn't happen:%n%n%s", testSource));
        }
    }

    @Test
    public void methodReturn_noDoubleEvaluation() {
        String testSource = String.format(
                "package %s;\n" +
                "\n" +
                "public class %s {\n" +
                "\n" +
                "  static int counter;\n" +
                "\n" +
                "  @%s\n" +
                "  public Integer test() {\n" +
                "    return count();\n" +
                "  }\n" +
                "\n" +
                "  private Integer count() {\n" +
                "      counter++;\n" +
                "      return null;\n" +
                "  }\n" +
                "\n" +
                "  public static void main(String[] args) {\n" +
                "    try {\n" +
                "      new Test().test();\n" +
                "    } catch (NullPointerException e) {\n" +
                "      throw new IllegalStateException(String.valueOf(counter));\n" +
                "    }\n" +
                "  }\n" +
                "}", TestConstants.PACKAGE, TestConstants.CLASS_NAME, NotNull.class.getName());
        byte[] compiledTestSource = compile(testSource);
        boolean passed = false;
        try {
            run(compiledTestSource);
        } catch (IllegalStateException e) {
            assertEquals("'return' expression should be evaluated only once", "1", e.getMessage());
            passed = true;
        }
        if (!passed) {
            fail(String.format("Expected to get an IllegalStateException on attempt to execute the source "
                               + "below but that didn't happen:%n%n%s", testSource));
        }
    }

    private void doMethodTest(@NotNull String testMethodBody) {
        String testSource = String.format(METHOD_TEST_CLASS_TEMPLATE,
                                          testMethodBody.replaceAll("\n", "\n    "));
        byte[] compiledTestSource = compile(testSource);
        boolean passed = false;
        try {
            run(compiledTestSource);
        } catch (NullPointerException e) {
            passed = true;
        }
        if (!passed) {
            fail(String.format("Expected to get an NullPointerException on attempt to execute the source "
                               + "below but that didn't happen:%n%n%s", testSource));
        }
    }

    @Test
    public void methodReturn_fromIfWithBraces() {
        doMethodTest(
                "" +
                "if (true) {\n" +
                "  return count();\n" +
                "}\n" +
                "return 10;"
        );
    }

    @Test
    public void methodReturn_fromIfWithoutBraces() {
        doMethodTest(
                "" +
                "if (true) \n" +
                "  return count();\n" +
                "return 10;"
        );
    }

    @Test
    public void methodReturn_fromIfWithoutBracesSameLine() {
        doMethodTest(
                "" +
                "if (true) return count();\n" +
                "return 10;"
        );
    }

    @Test
    public void methodReturn_fromElseWithBraces() {
        doMethodTest(
                "" +
                "if (false) {\n" +
                "  return 1;\n" +
                "} else {\n" +
                "  return count();\n" +
                "}"
        );
    }

    @Test
    public void methodReturn_fromElseWithoutBraces() {
        doMethodTest(
                "" +
                "if (false) {\n" +
                "  return 1;\n" +
                "} else\n" +
                "  return count();"
        );
    }

    @Test
    public void methodReturn_fromElseWithoutBracesSameLine() {
        doMethodTest(
                "" +
                "if (false) {\n" +
                "  return 1;\n" +
                "} else return count();"
        );
    }

    @Test
    public void methodReturn_fromForWithBraces() {
        doMethodTest(
                "" +
                "for (int i = 0; i < 2; i++) {\n" +
                "  return count();\n" +
                "}\n" +
                "return 10;"
        );
    }

    @Test
    public void methodReturn_fromForWithoutBraces() {
        doMethodTest(
                "" +
                "for (int i = 0; i < 2; i++)\n" +
                "  return count();\n" +
                "return 10;"
        );
    }

    @Test
    public void methodReturn_fromForEachWithBraces() {
        doMethodTest(
                "" +
                "int[] data = {1, 2};\n" +
                "for (int i : data) {\n" +
                "  return count();\n" +
                "}\n" +
                "return 10;"
        );
    }

    @Test
    public void methodReturn_fromForEachWithoutBraces() {
        doMethodTest(
                "" +
                "int[] data = {1, 2};\n" +
                "for (int i : data)\n" +
                "  return count();\n" +
                "return 10;"
        );
    }

    @Test
    public void methodReturn_fromWhileWithBraces() {
        doMethodTest(
                "" +
                "while (System.currentTimeMillis() > 1) {\n" +
                "  return count();\n" +
                "}\n" +
                "return 10;"
        );
    }

    @Test
    public void methodReturn_fromWhileWithoutBraces() {
        doMethodTest(
                "" +
                "while (System.currentTimeMillis() > 1) return count();\n" +
                "return 10;"
        );
    }

    @Test
    public void methodReturn_fromDoWhileWithBraces() {
        doMethodTest(
                "" +
                "do {\n" +
                "  return count();\n" +
                "} while (true);"
        );
    }

    @Test
    public void methodReturn_fromDoWhileWithoutBraces() {
        doMethodTest(
                "" +
                "do\n" +
                "  return count();\n" +
                "while (true);"
        );
    }

    @Test
    public void methodReturn_fromTry() {
        doMethodTest(
                "" +
                "try {\n" +
                "  return count();\n" +
                "} finally {}"
        );
    }

    @Test
    public void methodReturn_fromCatch() {
        doMethodTest(
                "" +
                "try {\n" +
                "  return 10 / 0;\n" +
                "} catch (Exception e) {\n" +
                "  return count();\n" +
                "}"
        );
    }

    @Test
    public void methodReturn_fromFinally() {
        doMethodTest(
                "" +
                "try {\n" +
                "  return 1;\n" +
                "} finally {\n" +
                "  return count();\n" +
                "}"
        );
    }

    @Test
    public void methodReturn_fromCase_singleInstruction() {
        doMethodTest(
                "" +
                "switch (System.currentTimeMillis() > 1 ? 1 : 0) {\n" +
                "  case 1:\n" +
                "    return count();\n" +
                "}\n" +
                "return 2;"
        );
    }

    @Test
    public void methodReturn_fromCase_multipleInstruction() {
        String testSource = String.format(
                "package %s;\n" +
                "\n" +
                "public class %s {\n" +
                "\n" +
                "  static int counter;\n" +
                "\n" +
                "  @%s\n" +
                "  public Integer test() {\n" +
                "    switch (System.currentTimeMillis() > 0 ? 1 : 0) {\n" +
                "      case 1:\n" +
                "        counter++;\n" +
                "        return count();\n" +
                "    }\n" +
                "    return 2;\n" +
                "  }\n" +
                "\n" +
                "  private Integer count() {\n" +
                "      return null;\n" +
                "  }\n" +
                "\n" +
                "  public static void main(String[] args) {\n" +
                "    try {\n" +
                "      new Test().test();\n" +
                "    } catch (NullPointerException e) {\n" +
                "      throw new IllegalStateException(String.valueOf(counter));\n" +
                "    }\n" +
                "  }\n" +
                "}", TestConstants.PACKAGE, TestConstants.CLASS_NAME, NotNull.class.getName());
        byte[] compiledTestSource = compile(testSource);
        boolean passed = false;
        try {
            run(compiledTestSource);
        } catch (IllegalStateException e) {
            assertEquals("'case' expressions over than 'return' shouldn't be swallowed",
                         "1",
                         e.getMessage());
            passed = true;
        }
        if (!passed) {
            fail(String.format("Expected to get an IllegalStateException on attempt to execute the source "
                               + "below but that didn't happen:%n%n%s", testSource));
        }
    }

    @Test
    public void methodReturn_fromCase_doesNotBreakLogicInAnotherCase() {
        String testMethodBody =
                "switch (System.currentTimeMillis() > 1 ? 1 : 0) {\n" +
                "  case 0:\n" +
                "    return count();\n" +
                "  case 1:\n" +
                "    throw new IllegalStateException();\n" +
                "}\n" +
                "return 2;";
        String testSource = String.format(METHOD_TEST_CLASS_TEMPLATE,
                                          testMethodBody.replaceAll("\n", "\n    "));
        byte[] compiledTestSource = compile(testSource);
        boolean passed = false;
        try {
            run(compiledTestSource);
        } catch (IllegalStateException e) {
            passed = true;
        } catch (Exception e) {
            fail(String.format("Expected to get an IllegalStateException on attempt to execute the source "
                               + "below but got a %s instead:%n%n%s", e.getClass().getName(), testSource));
        }
        if (!passed) {
            fail(String.format("Expected to get an IllegalStateException on attempt to execute the source "
                               + "below but that didn't happen:%n%n%s", testSource));
        }
    }

    @Test
    public void methodReturn_fromCase_doesNotBreakLogicInDefault() {
        String testMethodBody =
                "switch (System.currentTimeMillis() > 1 ? 1 : 0) {\n" +
                "  case 0:\n" +
                "    return count();\n" +
                "  default:\n" +
                "    throw new IllegalStateException();\n" +
                "}";
        String testSource = String.format(METHOD_TEST_CLASS_TEMPLATE,
                                          testMethodBody.replaceAll("\n", "\n    "));
        byte[] compiledTestSource = compile(testSource);
        boolean passed = false;
        try {
            run(compiledTestSource);
        } catch (IllegalStateException e) {
            passed = true;
        } catch (Exception e) {
            fail(String.format("Expected to get an IllegalStateException on attempt to execute the source "
                               + "below but got a %s instead:%n%n%s", e.getClass().getName(), testSource));
        }
        if (!passed) {
            fail(String.format("Expected to get an IllegalStateException on attempt to execute the source "
                               + "below but that didn't happen:%n%n%s", testSource));
        }
    }

    @Test
    public void methodReturn_fromDefault_singleInstruction() {
        doMethodTest(
                "" +
                "switch (System.currentTimeMillis() > 1 ? 1 : 0) {\n" +
                "  case 0:\n" +
                "    return 1;\n" +
                "  default:\n" +
                "    return count();\n" +
                "}"
        );
    }

    @Test
    public void methodReturn_fromDefault_multipleInstruction() {
        String testSource = String.format(
                "package %s;\n" +
                "\n" +
                "public class %s {\n" +
                "\n" +
                "  static int counter;\n" +
                "\n" +
                "  @%s\n" +
                "  public Integer test() {\n" +
                "    switch (System.currentTimeMillis() > 0 ? 1 : 0) {\n" +
                "      case 0:\n" +
                "        return 0;\n" +
                "      default:\n" +
                "        counter++;\n" +
                "        return count();\n" +
                "    }\n" +
                "  }\n" +
                "\n" +
                "  private Integer count() {\n" +
                "      return null;\n" +
                "  }\n" +
                "\n" +
                "  public static void main(String[] args) {\n" +
                "    try {\n" +
                "      new Test().test();\n" +
                "    } catch (NullPointerException e) {\n" +
                "      throw new IllegalStateException(String.valueOf(counter));\n" +
                "    }\n" +
                "  }\n" +
                "}", TestConstants.PACKAGE, TestConstants.CLASS_NAME, NotNull.class.getName());
        byte[] compiledTestSource = compile(testSource);
        boolean passed = false;
        try {
            run(compiledTestSource);
        } catch (IllegalStateException e) {
            assertEquals("'default' expressions over than 'return' shouldn't be swallowed",
                         "1",
                         e.getMessage());
            passed = true;
        }
        if (!passed) {
            fail(String.format("Expected to get an IllegalStateException on attempt to execute the source "
                               + "below but that didn't happen:%n%n%s", testSource));
        }
    }

    @Test
    public void methodReturn_fromDefault_doesNotBreakLogicInAnotherCase() {
        String testMethodBody =
                "switch (System.currentTimeMillis() > 1 ? 1 : 0) {\n" +
                "  case 1:\n" +
                "    throw new IllegalStateException();\n" +
                "  default:\n" +
                "    return count();\n" +
                "}\n";
        String testSource = String.format(METHOD_TEST_CLASS_TEMPLATE,
                                          testMethodBody.replaceAll("\n", "\n    "));
        byte[] compiledTestSource = compile(testSource);
        boolean passed = false;
        try {
            run(compiledTestSource);
        } catch (IllegalStateException e) {
            passed = true;
        } catch (Exception e) {
            fail(String.format("Expected to get an IllegalStateException on attempt to execute the source "
                               + "below but got a %s instead:%n%n%s", e.getClass().getName(), testSource));
        }
        if (!passed) {
            fail(String.format("Expected to get an IllegalStateException on attempt to execute the source "
                               + "below but that didn't happen:%n%n%s", testSource));
        }
    }

    @Test
    public void methodReturn_noAttemptToInstrumentPrimitiveReturnType() {
        for (String type : PRIMITIVE_TYPES) {

            // Ensure that compilation is fine as we don't instrument primitive types and class binaries
            // are correctly loaded and executed.

            String testSource = String.format(
                    "package %s;\n" +
                    "\n" +
                    "public class %s {\n" +
                    "\n" +
                    "  @%s\n" +
                    "  public %s test() {\n" +
                    "    return (%s)1;\n" +
                    "  }\n" +
                    "\n" +
                    "  public static void main(String[] args) {\n" +
                    "    new Test().test();\n" +
                    "  }\n" +
                    "}", TestConstants.PACKAGE, TestConstants.CLASS_NAME, NotNull.class.getName(), type, type);
            byte[] compiledTestSource = compile(testSource);
            run(compiledTestSource);
        }
    }

    /**
     * Applies given data to the {@link #PARAMETER_TEST_CLASS_TEMPLATE test source template}.
     *
     * @param importString  an expression for the {@code import} keyword (if any)
     * @param testMethod    {@value #METHOD_NAME}() method body
     * @param callArguments arguments to use for calling the {@value #METHOD_NAME}() method
     * @return              complete test source for the given arguments. It's assumed to be properly formatted
     */
    @NotNull
    private static String prepareSourceTextForParameterTest(@Nullable String importString,
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
     * Compiles given test source using in-memory {@link JavaCompiler}.
     *
     * @param text      test source to compile
     * @return          compiled binaries for the given source
     */
    @NotNull
    private byte[] compile(@NotNull String text) {
        StringWriter output = new StringWriter();

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        SimpleFileManager fileManager = new SimpleFileManager(compiler.getStandardFileManager(
                null,
                null,
                null
        ));
        List<SimpleSourceFile> compilationUnits = singletonList(new SimpleSourceFile(text));
        List<String> arguments = new ArrayList<>();
        arguments.addAll(asList("-classpath", System.getProperty("java.class.path")));
        arguments.addAll(getAdditionalCompilerArgs());
        JavaCompiler.CompilationTask task = compiler.getTask(output,
                                                             fileManager,
                                                             null,
                                                             arguments,
                                                             null,
                                                             compilationUnits);
        Boolean successfulCompilation = task.call();
        if (!successfulCompilation) {
            fail(String.format("Failed to compile test class source.%nCompiler output: %s%nTest source:%n%n%s",
                               output, text));
        }

        List<SimpleClassFile> compiled = fileManager.getCompiled();
        if (compiled.size() != 1) {
            fail(String.format("Failed to fetch compiled test class, javac api must have changed, "
                               + "please contact the plugin's author via traute.java@gmail.com.%n"
                               + "Compiler output: %s%n"
                               + "Test source:%n%n%s",
                               output, text));
        }

        Optional<byte[]> o = compiled.get(0).getCompiledBinaries();
        if (!o.isPresent()) {
            fail(String.format("Failed to fetch compiled test class content, javac api must have changed, "
                               + "please contact the plugin's author via traute.java@gmail.com.%n"
                               + "Compiler output: %s%n"
                               + "Test source:%n%n%s",
                               output, text));
        }
        return o.get();
    }

    /**
     * Calls {@code main()} method on the given compiled test source binaries.
     *
     * @param compiledClass     compiled test source binaries
     */
    private static void run(@NotNull byte[] compiledClass) {
        ClassLoader classLoader = new ClassLoader() {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                return defineClass(name, compiledClass, 0, compiledClass.length);
            }
        };
        Class<?> clazz;
        try {
            clazz = classLoader.loadClass(String.format("%s.%s", TestConstants.PACKAGE, TestConstants.CLASS_NAME));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Can't load compiled test class", e);
        }

        Method method;
        try {
            method = clazz.getMethod("main", String[].class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Can't find the 'main()' method in the compiled test class", e);
        }

        try {
            method.invoke(null, MAIN_ARGUMENTS);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Unexpected exception on calling 'main()' in the test class", e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException)cause;
            } else {
                throw new RuntimeException("Unexpected exception on calling 'main()' in the test class", e);
            }
        }
    }
}
