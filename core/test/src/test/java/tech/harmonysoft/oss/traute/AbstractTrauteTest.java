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

import javax.annotation.Nonnull;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static tech.harmonysoft.oss.traute.util.TestConstants.CLASS_NAME;
import static tech.harmonysoft.oss.traute.util.TestConstants.PACKAGE;

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
 *           {@link #getNotNullAnnotations() emulate user's setup for @NotNull annotations}
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
            "}", PACKAGE, CLASS_NAME, NotNull.class.getName());

    private static final String[] PRIMITIVE_TYPES = {
            "byte", "short", "char", "int", "long", "float", "double"
    };

    /** Emulates user's setup for the {@code @NotNull} annotations to use. */
    private final Set<String> notNullAnnotations = new HashSet<>();

    /** Emulates user's setup for the instrumentation types to use. */
    private final Set<String> instrumentationTypes = new HashSet<>();

    private boolean verboseOutput;

    @Before
    public void setUp() {
        notNullAnnotations.clear();
        instrumentationTypes.clear();
        verboseOutput = false;
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
    protected Set<String> getNotNullAnnotations() {
        return notNullAnnotations;
    }

    @NotNull
    protected Set<String> getInstrumentationTypes() {
        return instrumentationTypes;
    }

    protected boolean isVerboseOutput() {
        return verboseOutput;
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
        notNullAnnotations.add(Nonnull.class.getName());
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
        notNullAnnotations.addAll(asList(Nonnull.class.getName(), NotNull.class.getName()));
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
        notNullAnnotations.add(NN.class.getName());
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
                "}", PACKAGE, CLASS_NAME, NotNull.class.getName());
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
                "}", PACKAGE, CLASS_NAME, NotNull.class.getName());
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
                "}", PACKAGE, CLASS_NAME, NotNull.class.getName());
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
                    "}", PACKAGE, CLASS_NAME, NotNull.class.getName(), type, type);
            byte[] compiledTestSource = compile(testSource);
            run(compiledTestSource);
        }
    }

    @Test
    public void logging_instrumentations_verboseOn() {
        doTestVerboseInstrumentationsLogging(true);
    }

    @Test
    public void logging_instrumentations_verboseOff() {
        doTestVerboseInstrumentationsLogging(false);
    }

    private void doTestVerboseInstrumentationsLogging(boolean verbose) {
        verboseOutput = verbose;
        String testSource = String.format(
                "package %s;\n" +
                "\n" +
                "import %s;\n" +
                "\n" +
                "public class %s {\n" +
                "\n" +
                "  @NotNull\n" +
                "  public Integer test(@NotNull Integer i1, @NotNull Integer i2) {\n" +
                "    return i1 + i2;\n" +
                "  }\n" +
                "\n" +
                "  @NotNull\n" +
                "  private Integer negative(@NotNull Integer i) {\n" +
                "      return -1 * i;\n" +
                "  }\n" +
                "\n" +
                "  public static void main(String[] args) {\n" +
                "    new Test().test(1, 2);\n" +
                "  }\n" +
                "}", PACKAGE, NotNull.class.getName(), CLASS_NAME);

        String compilerOutput = compileForFullResult(testSource).compilerOutput;

        String[] targetParameterNames = {"i1", "i2", "i"};
        for (String targetParameterName : targetParameterNames) {
            String filterMessage = String.format("added a null-check for argument '%s'", targetParameterName);
            String description = String.format(
                    "Expected that 'parameter instrumentation' for parameter '%s' from the source below is "
                    + "%smentioned in compiler output when 'verbose mode' is %s%nOutput: %s%nSource:%n%s",
                    targetParameterName, verbose ? "" : "not ", verbose ? "on" : "off", compilerOutput, testSource);
            assertEquals(description, verbose, compilerOutput.contains(filterMessage));
        }

        String methodPrefix = PACKAGE + "." + CLASS_NAME + ".";
        String[] targetMethods = {methodPrefix + "test()", methodPrefix + "negative()"};
        for (String targetMethod : targetMethods) {
            String filterMessage = "added a null-check for 'return' expression in method " + targetMethod;
            String description = String.format(
                    "Expected that 'return instrumentation' for method '%s' from the source below is "
                    + "%smentioned in compiler output when 'verbose mode' is %s%nOutput: %s%nSource:%n%s",
                    targetMethod, verbose ? "" : "not ", verbose ? "on" : "off", compilerOutput, testSource);
            assertEquals(description, verbose, compilerOutput.contains(filterMessage));
        }
    }

    @Test
    public void logging_verbose_classStats() {
        verboseOutput = true;
        String testSource = String.format(
                "package %s;\n" +
                "\n" +
                "import %s;\n" +
                "\n" +
                "public class %s {\n" +
                "\n" +
                "  @NotNull\n" +
                "  public Integer test(@NotNull Integer i1, @NotNull Integer i2) {\n" +
                "    return i1 + i2;\n" +
                "  }\n" +
                "\n" +
                "  class Inner {\n" +
                "    @NotNull\n" +
                "    String inner(@NotNull String s) { return s + 1;}\n" +
                "  }\n" +
                "\n" +
                "  static class StaticInner {\n" +
                "    @NotNull\n" +
                "    String staticInner(@NotNull String s) { return s + 2;}\n" +
                "  }\n" +
                "\n" +
                "  public static void main(String[] args) {\n" +
                "    new Test().test(1, 2);\n" +
                "  }\n" +
                "}", PACKAGE, NotNull.class.getName(), CLASS_NAME);

        String compilerOutput = compileForFullResult(testSource).compilerOutput;
        String filter = String.format(
                "added 7 instrumentations to the /%s/%s%s - METHOD_PARAMETER: 4, METHOD_RETURN: 3",
                PACKAGE, CLASS_NAME, JavaFileObject.Kind.SOURCE.extension
        );
        assertTrue(
                String.format("Expected that class-level instrumentations statistics is mentioned in the compiler "
                              + "output.%nFilter text: '%s'%nCompiler output:%n%s%n%nSource:%n%s",
                              filter, compilerOutput, testSource),
                compilerOutput.contains(filter)
        );
    }

    @Test
    public void logging_customSetting_annotations() {
        notNullAnnotations.add(NN.class.getName());
        String testMethodBody = "  return 1;";
        String testSource = String.format(METHOD_TEST_CLASS_TEMPLATE, testMethodBody);
        String compilerOutput = compileForFullResult(testSource).compilerOutput;
        String filterText = "using the following NotNull annotations: " + notNullAnnotations;
        assertTrue(
                String.format("Expected the custom NotNull annotations are mentioned in the compiler "
                              + "output.%nFilter text: '%s'%nCompiler output: '%s'", filterText, compilerOutput),
                compilerOutput.contains(filterText)
        );
    }

    @Test
    public void logging_customSetting_verbose() {
        verboseOutput = true;
        String testMethodBody = "  return 1;";
        String testSource = String.format(METHOD_TEST_CLASS_TEMPLATE, testMethodBody);
        String compilerOutput = compileForFullResult(testSource).compilerOutput;
        String filterText = "'verbose mode' is set on";
        assertTrue(
                String.format("Expected the active 'verbose mode' is mentioned in the compiler "
                              + "output.%nFilter text: '%s'%nCompiler output: '%s'", filterText, compilerOutput),
                compilerOutput.contains(filterText)
        );
    }

    @Test
    public void restrictedInstrumentation_parameterOnly_noCheckForReturn() {
        instrumentationTypes.add("parameter");
        String testMethodBody = "  return count();";
        String testSource = String.format(METHOD_TEST_CLASS_TEMPLATE, testMethodBody);
        byte[] binaries = compile(testSource);
        // Expecting null-check for return type not to be generated, hence, no exception will be thrown
        run(binaries);
    }

    @Test
    public void restrictedInstrumentation_parameterOnly_active() {
        instrumentationTypes.add("parameter");
        doTestArgument(Nonnull.class.getName());
    }

    @Test
    public void restrictedInstrumentation_returnOnly_noCheckForParameter() {
        instrumentationTypes.add("return");
        String testSource = prepareSourceTextForParameterTest(NotNull.class.getName(),
                                                              "void test(@NotNull String s) {}",
                                                              "null");
        byte[] binaries = compile(testSource);
        // Expecting null-check for parameter not to be generated, hence, no exception will be thrown
        run(binaries);
    }

    @Test
    public void restrictedInstrumentation_returnOnly_active() {
        instrumentationTypes.add("return");
        doMethodTest("return count();");
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
    private byte[] compile(@NotNull String text) {
        CompilationResult result = compileForFullResult(text);
        if (result.binaries == null) {
            fail(String.format("Failed to fetch compiled test class, javac api must have changed, "
                               + "please contact the plugin's author via traute.java@gmail.com.%n"
                               + "Compiler output: %s%n"
                               + "Test source:%n%n%s",
                               result.compilerOutput, text));
        }
        return result.binaries;
    }

    /**
     * Compiles given test source using in-memory {@link JavaCompiler} and returns
     * {@link CompilationResult full result} (e.g. including compiler's output).
     *
     * @param text  test source to compile
     * @return      compilation result
     */
    @NotNull
    private CompilationResult compileForFullResult(@NotNull String text) {
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
        SimpleClassFile classFile = null;
        if (compiled.size() == 1) {
            classFile = compiled.get(0);
        }

        byte[] binaries = null;
        if (classFile != null) {
            Optional<byte[]> o = classFile.getCompiledBinaries();
            if (o.isPresent()) {
                binaries = o.get();
            }
        }
        output.flush();
        return new CompilationResult(binaries, output.toString());
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
            clazz = classLoader.loadClass(String.format("%s.%s", PACKAGE, CLASS_NAME));
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

    private static class CompilationResult {

        @NotNull public final String compilerOutput;

        @Nullable public final byte[] binaries;

        public CompilationResult(@Nullable byte[] binaries, @NotNull String compilerOutput) {
            this.binaries = binaries;
            this.compilerOutput = compilerOutput;
        }
    }
}
