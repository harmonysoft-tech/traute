package tech.harmonysoft.oss.traute.test.suite;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tech.harmonysoft.oss.traute.common.util.TrauteConstants;
import tech.harmonysoft.oss.traute.test.util.TestUtil;

import static tech.harmonysoft.oss.traute.test.util.TestConstants.CLASS_NAME;
import static tech.harmonysoft.oss.traute.test.util.TestConstants.PACKAGE;
import static tech.harmonysoft.oss.traute.test.util.TestUtil.expectNpeFromReturnCheck;
import static tech.harmonysoft.oss.traute.test.util.TestUtil.prepareReturnTestSource;

public abstract class MethodReturnTest extends AbstractTrauteTest {

    @Test
    public void noDoubleEvaluation() {
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
        expectRunResult.withExceptionClass(IllegalStateException.class)
                       .withExceptionMessage("1");
        doTest(testSource);
    }

    @Test
    public void if_withBraces() {
        doMethodReturnTest(
                "" +
                "if (true) {\n" +
                "  return count();\n" +
                "}\n" +
                "return 10;"
        );
    }

    @Test
    public void if_withoutBraces() {
        doMethodReturnTest(
                "" +
                "if (true) \n" +
                "  return count();\n" +
                "return 10;"
        );
    }

    @Test
    public void if_withoutBracesSameLine() {
        doMethodReturnTest(
                "" +
                "if (true) return count();\n" +
                "return 10;"
        );
    }

    @Test
    public void else_withBraces() {
        doMethodReturnTest(
                "" +
                "if (false) {\n" +
                "  return 1;\n" +
                "} else {\n" +
                "  return count();\n" +
                "}"
        );
    }

    @Test
    public void else_withoutBraces() {
        doMethodReturnTest(
                "" +
                "if (false) {\n" +
                "  return 1;\n" +
                "} else\n" +
                "  return count();"
        );
    }

    @Test
    public void else_withoutBracesSameLine() {
        doMethodReturnTest(
                "" +
                "if (false) {\n" +
                "  return 1;\n" +
                "} else return count();"
        );
    }

    @Test
    public void for_withBraces() {
        doMethodReturnTest(
                "" +
                "for (int i = 0; i < 2; i++) {\n" +
                "  return count();\n" +
                "}\n" +
                "return 10;"
        );
    }

    @Test
    public void for_withoutBraces() {
        doMethodReturnTest(
                "" +
                "for (int i = 0; i < 2; i++)\n" +
                "  return count();\n" +
                "return 10;"
        );
    }

    @Test
    public void forEach_withBraces() {
        doMethodReturnTest(
                "" +
                "int[] data = {1, 2};\n" +
                "for (int i : data) {\n" +
                "  return count();\n" +
                "}\n" +
                "return 10;"
        );
    }

    @Test
    public void forEach_withoutBraces() {
        doMethodReturnTest(
                "" +
                "int[] data = {1, 2};\n" +
                "for (int i : data)\n" +
                "  return count();\n" +
                "return 10;"
        );
    }

    @Test
    public void while_withBraces() {
        doMethodReturnTest(
                "" +
                "while (System.currentTimeMillis() > 1) {\n" +
                "  return count();\n" +
                "}\n" +
                "return 10;"
        );
    }

    @Test
    public void while_withoutBraces() {
        doMethodReturnTest(
                "" +
                "while (System.currentTimeMillis() > 1) return count();\n" +
                "return 10;"
        );
    }

    @Test
    public void doWhile_withBraces() {
        doMethodReturnTest(
                "" +
                "do {\n" +
                "  return count();\n" +
                "} while (true);"
        );
    }

    @Test
    public void doWhile_withoutBraces() {
        doMethodReturnTest(
                "" +
                "do\n" +
                "  return count();\n" +
                "while (true);"
        );
    }

    @DisplayName("try")
    @Test
    public void fromTry() {
        doMethodReturnTest(
                "" +
                "try {\n" +
                "  return count();\n" +
                "} finally {}"
        );
    }

    @DisplayName("catch")
    @Test
    public void fromCatch() {
        doMethodReturnTest(
                "" +
                "try {\n" +
                "  return 10 / 0;\n" +
                "} catch (Exception e) {\n" +
                "  return count();\n" +
                "}"
        );
    }

    @DisplayName("finally")
    @Test
    public void fromFinally() {
        doMethodReturnTest(
                "" +
                "try {\n" +
                "  return 1;\n" +
                "} finally {\n" +
                "  return count();\n" +
                "}"
        );
    }

    @Test
    public void case_singleInstruction() {
        doMethodReturnTest(
                "" +
                "switch (System.currentTimeMillis() > 1 ? 1 : 0) {\n" +
                "  case 1:\n" +
                "    return count();\n" +
                "}\n" +
                "return 2;"
        );
    }

    private void doMethodReturnTest(@NotNull String testMethodBody) {
        String testSource = TestUtil.prepareReturnTestSource(testMethodBody);
        expectNpeFromReturnCheck(testSource, "return count()", expectRunResult);
        doTest(testSource);
    }

    @Test
    public void case_multipleInstruction() {
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
        expectRunResult.withExceptionClass(IllegalStateException.class)
                       .withExceptionMessage("1");
        doTest(testSource);
    }

    @Test
    public void case_doesNotBreakLogicInAnotherCase() {
        String testMethodBody =
                "switch (System.currentTimeMillis() > 1 ? 1 : 0) {\n" +
                "  case 0:\n" +
                "    return count();\n" +
                "  case 1:\n" +
                "    throw new IllegalStateException();\n" +
                "}\n" +
                "return 2;";
        String testSource = prepareReturnTestSource(testMethodBody);
        expectRunResult.withExceptionClass(IllegalStateException.class);
        doTest(testSource);
    }

    @Test
    public void case_doesNotBreakLogicInDefault() {
        String testMethodBody =
                "switch (System.currentTimeMillis() > 1 ? 1 : 0) {\n" +
                "  case 0:\n" +
                "    return count();\n" +
                "  default:\n" +
                "    throw new IllegalStateException();\n" +
                "}";
        String testSource = prepareReturnTestSource(testMethodBody);
        expectRunResult.withExceptionClass(IllegalStateException.class);
        doTest(testSource);
    }

    @Test
    public void default_singleInstruction() {
        doMethodReturnTest(
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
    public void default_multipleInstructions() {
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
        expectRunResult.withExceptionClass(IllegalStateException.class)
                       .withExceptionMessage("1");
        doTest(testSource);
    }

    @Test
    public void default_doesNotBreakLogicInAnotherCase() {
        String testMethodBody =
                "switch (System.currentTimeMillis() > 1 ? 1 : 0) {\n" +
                "  case 1:\n" +
                "    throw new IllegalStateException();\n" +
                "  default:\n" +
                "    return count();\n" +
                "}\n";
        String testSource = prepareReturnTestSource(testMethodBody);
        expectRunResult.withExceptionClass(IllegalStateException.class);
        doTest(testSource);
    }

    @Test
    public void noAttemptToInstrumentInappropriateReturnType() {
        for (String type : TrauteConstants.PRIMITIVE_TYPES) {

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
            doTest(testSource);
        }
    }

    @DisplayName("interface")
    @Test
    public void interfaceReturn() {
        String testSource = String.format(
                "package %s;\n" +
                "\n" +
                "public interface %s {\n" +
                "  @%s\n" +
                "  String test();\n" +
                "}", PACKAGE, CLASS_NAME, NotNull.class.getName());

        // We expect that no instrumentation occurs for interface parameters, hence, compilation is fine.
        doCompile(testSource);
    }

    @Test
    public void anonymousClass() {
        String testSource = String.format(
                "package %s;\n" +
                "\n" +
                "import java.util.ArrayList;\n" +
                "\n" +
                "public class %s {\n" +
                "  public void test() {\n" +
                "    new ArrayList() {\n" +
                "      @%s\n" +
                "      public Object get(int index) {\n" +
                "        return null;\n" +
                "      }\n" +
                "    }.get(1);\n" +
                "  }\n" +
                "  public static void main(String[] args) {\n" +
                "    new %s().test();\n" +
                "  }\n" +
                "}", PACKAGE, CLASS_NAME, NotNull.class.getName(), CLASS_NAME);
        expectNpeFromReturnCheck(testSource, "return", expectRunResult);
        doTest(testSource);
    }

    @Test
    public void abstractClass_abstractMethod() {
        String testSource = String.format(
                "package %s;\n" +
                "\n" +
                "import %s;\n" +
                "\n" +
                "public abstract class %s {\n" +
                "\n" +
                "  @NotNull\n" +
                "  protected abstract Object implementMe();\n" +
                "\n" +
                "  public static void main(String[] args) {\n" +
                "    new %s() {\n" +
                "      public Object implementMe() {\n" +
                "        return null;\n" +
                "      }\n" +
                "    }.implementMe();\n" +
                "  }\n" +
                "}", PACKAGE, NotNull.class.getName(), CLASS_NAME, CLASS_NAME);
        // Expecting that compilation is fine as there is no attempt to add a null-check into an abstract method
        doTest(testSource);
    }

    @Test
    public void abstractClass_nonAbstractMethod() {
        String testSource = String.format(
                "package %s;\n" +
                "\n" +
                "import %s;\n" +
                "\n" +
                "public abstract class %s {\n" +
                "\n" +
                "  @NotNull\n" +
                "  protected abstract %s implementMe();\n" +
                "\n" +
                "  @NotNull\n" +
                "  public Object test() {\n" +
                "    return (String)null;\n" +
                "  }\n" +
                "\n" +
                "  public static void main(String[] args) {\n" +
                "    %s var = new %s() {\n" +
                "      public %s implementMe() {\n" +
                "        return null;\n" +
                "      }\n" +
                "    };\n" +
                "    var.implementMe();\n" +
                "    var.test();\n" +
                "  }\n" +
                "}", PACKAGE, NotNull.class.getName(), CLASS_NAME, CLASS_NAME, CLASS_NAME, CLASS_NAME, CLASS_NAME);
        expectNpeFromReturnCheck(testSource, "return (String)null", expectRunResult);
        doTest(testSource);
    }

    @Test
    public void defaultInterfaceMethod() {
        String testSource = String.format(
                "package %s;\n" +
                "\n" +
                "import %s;\n" +
                "\n" +
                "public interface %s {\n" +
                "\n" +
                "  @NotNull\n" +
                "  default Object test() {\n" +
                "    return null;\n" +
                "  }\n" +
                "\n" +
                "  void implementMe();\n" +
                "\n" +
                "  public static void main(String[] args) {\n" +
                "    new %s() {\n" +
                "      public void implementMe() {\n" +
                "      }\n" +
                "    }.test();\n" +
                "  }\n" +
                "}", PACKAGE, NotNull.class.getName(), CLASS_NAME, CLASS_NAME);
        expectNpeFromReturnCheck(testSource, "return null", expectRunResult);
        doTest(testSource);
    }

    @Test
    public void innerClass() {
        String testSource = String.format(
                "package %s;\n" +
                "\n" +
                "import %s;\n" +
                "\n" +
                "public class %s {\n" +
                "\n" +
                "  class Inner {\n" +
                "    @NotNull\n" +
                "    Object test() {\n" +
                "      return null;\n" +
                "    }\n" +
                "  }\n" +
                "\n" +
                "  public static void main(String[] args) {\n" +
                "    %s var = new %s();\n" +
                "    var.new Inner().test();\n" +
                "  }\n" +
                "}", PACKAGE, NotNull.class.getName(), CLASS_NAME, CLASS_NAME, CLASS_NAME);
        expectNpeFromReturnCheck(testSource, "return null", expectRunResult);
        doTest(testSource);
    }

    @Test
    public void staticInnerClass() {
        String testSource = String.format(
                "package %s;\n" +
                "\n" +
                "import %s;\n" +
                "\n" +
                "public class %s {\n" +
                "\n" +
                "  static class Inner {\n" +
                "    @NotNull\n" +
                "    Object test() {\n" +
                "      return null;\n" +
                "    }\n" +
                "  }\n" +
                "\n" +
                "  public static void main(String[] args) {\n" +
                "    new %s.Inner().test();\n" +
                "  }\n" +
                "}", PACKAGE, NotNull.class.getName(), CLASS_NAME, CLASS_NAME);
        expectNpeFromReturnCheck(testSource, "return null", expectRunResult);
        doTest(testSource);
    }

    @Test
    public void localClass() {
        String testSource = String.format(
                "package %s;\n" +
                "\n" +
                "import %s;\n" +
                "\n" +
                "public class %s {\n" +
                "\n" +
                "  public void test() {\n" +
                "    class Local {\n" +
                "      @NotNull\n" +
                "      Object test() {\n" +
                "        return null;\n" +
                "      }\n" +
                "    }\n" +
                "    new Local().test();\n" +
                "  }\n" +
                "\n" +
                "  public static void main(String[] args) {\n" +
                "    new %s().test();\n" +
                "  }\n" +
                "}", PACKAGE, NotNull.class.getName(), CLASS_NAME, CLASS_NAME);
        expectNpeFromReturnCheck(testSource, "return null", expectRunResult);
        doTest(testSource);
    }
}
