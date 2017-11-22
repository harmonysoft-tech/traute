package tech.harmonysoft.oss.traute.test.suite;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tech.harmonysoft.oss.traute.common.instrumentation.InstrumentationType;

import javax.annotation.Nonnull;

import static tech.harmonysoft.oss.traute.common.util.TrauteConstants.PRIMITIVE_TYPES;
import static tech.harmonysoft.oss.traute.test.util.TestConstants.*;
import static tech.harmonysoft.oss.traute.test.util.TestUtil.*;

/**
 * Holds tests which check {@link InstrumentationType#METHOD_PARAMETER method parameter} instrumentation.
 */
public abstract class MethodParameterTest extends AbstractTrauteTest {

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

    @Test
    public void default_spring() {
        doTestAnnotationWithDifferentDeclarationTypes(org.springframework.lang.NonNull.class);
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
        String testSource = prepareParameterTestSource(
                qualifiedAnnotationClassName,
                String.format("public void %s(@%s String param) {}",
                              METHOD_NAME, annotationName),
                "null");
        expectNpeFromParameterCheck(testSource, "param", expectRunResult);
        doTest(testSource);
    }

    private void doTestAnnotation_wildcardImport(@NotNull Class<?> notNullAnnotationClass) {
        String qualifiedAnnotationClassName = notNullAnnotationClass.getName();
        int lastDotIndex = qualifiedAnnotationClassName.lastIndexOf('.');
        String annotationName = qualifiedAnnotationClassName.substring(lastDotIndex + 1);
        String testSource = prepareParameterTestSource(
                String.format("%s.*", qualifiedAnnotationClassName.substring(0, lastDotIndex)),
                String.format("public void %s(@%s String param) {}",
                              METHOD_NAME, annotationName),
                "null");
        expectNpeFromParameterCheck(testSource, "param", expectRunResult);
        doTest(testSource);
    }

    private void doTestAnnotation_qualifiedInPlaceName(@NotNull Class<?> notNullAnnotationClass) {
        String testSource = prepareParameterTestSource(
                null,
                String.format("public void %s(@%s String param) {}",
                              METHOD_NAME, notNullAnnotationClass.getName()),
                "null");
        expectNpeFromParameterCheck(testSource, "param", expectRunResult);
        doTest(testSource);
    }

    @ParameterizedTest
    @CsvSource(delimiter = ':', value = {
            "null, 1:i1:0", "1, null:i2:0"
    })
    public void multipleArguments(@NotNull String callArguments, @NotNull String parameterName) {
        String testSource = prepareParameterTestSource(
                NotNull.class.getName(),
                String.format("public void %s(@NotNull Integer i1,\n                   @NotNull Integer i2) {}",
                              METHOD_NAME),
                callArguments
        );
        expectNpeFromParameterCheck(testSource, parameterName, expectRunResult);
        doTest(testSource);
    }

    @Test
    public void primitiveMethodArguments() {
        for (String primitiveType : PRIMITIVE_TYPES) {
            String testSource = prepareParameterTestSource(
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
    public void subsequentLineNumbersArePreserved() {
        Class<? extends RuntimeException> exceptionClass = IllegalArgumentException.class;
        String testSource = prepareParameterTestSource(
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

    @DisplayName("interface")
    @Test
    public void interfaceParameters() {
        String testSource = String.format(
                "package %s;\n" +
                "\n" +
                "public interface %s {\n" +
                "  void test(@%s String s);\n" +
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
                "      public boolean contains(@%s Object param) {\n" +
                "        return false;\n" +
                "      }\n" +
                "    }.contains(null);\n" +
                "  }\n" +
                "  public static void main(String[] args) {\n" +
                "    new %s().test();\n" +
                "  }\n" +
                "}", PACKAGE, CLASS_NAME, NotNull.class.getName(), CLASS_NAME);
        expectNpeFromParameterCheck(testSource, "param", expectRunResult);
        doTest(testSource);
    }

    @Test
    public void abstractClass_abstractMethod() {
        String testSource = String.format(
                "package %s;\n" +
                "\n" +
                "public abstract class %s {\n" +
                "\n" +
                "  protected abstract void implementMe(@%s Object param);\n" +
                "\n" +
                "  public static void main(String[] args) {\n" +
                "    new %s() {\n" +
                "      public void implementMe(Object o) {\n" +
                "      }\n" +
                "    }.implementMe(null);\n" +
                "  }\n" +
                "}", PACKAGE, CLASS_NAME, NotNull.class.getName(), CLASS_NAME);
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
                "  protected abstract %s implementMe(@NotNull Object o);\n" +
                "\n" +
                "  public void test(@NotNull Object param) {\n" +
                "  }\n" +
                "\n" +
                "  public static void main(String[] args) {\n" +
                "    new %s() {\n" +
                "      public %s implementMe(Object param) {\n" +
                "        return this;\n" +
                "      }\n" +
                "    }.implementMe(null).test(null);\n" +
                "  }\n" +
                "}", PACKAGE, NotNull.class.getName(), CLASS_NAME, CLASS_NAME, CLASS_NAME, CLASS_NAME);
        expectNpeFromParameterCheck(testSource, "param", expectRunResult);
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
                "  default void test(@NotNull String param) {\n" +
                "  }\n" +
                "\n" +
                "  void implementMe();\n" +
                "\n" +
                "  public static void main(String[] args) {\n" +
                "    new %s() {\n" +
                "      public void implementMe() {\n" +
                "      }\n" +
                "    }.test(null);\n" +
                "  }\n" +
                "}", PACKAGE, NotNull.class.getName(), CLASS_NAME, CLASS_NAME);
        expectNpeFromParameterCheck(testSource, "param", expectRunResult);
        doTest(testSource);
    }

    @Test
    public void staticInterfaceMethod() {
        String testSource = String.format(
                "package %s;\n" +
                "\n" +
                "import %s;\n" +
                "\n" +
                "public interface %s {\n" +
                "\n" +
                "  static void test(@NotNull String param) {\n" +
                "  }\n" +
                "\n" +
                "  public static void main(String[] args) {\n" +
                "    %s.test(null);\n" +
                "  }\n" +
                "}", PACKAGE, NotNull.class.getName(), CLASS_NAME, CLASS_NAME);
        expectNpeFromParameterCheck(testSource, "param", expectRunResult);
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
                "    void test(@NotNull String param) {\n" +
                "    }\n" +
                "  }\n" +
                "\n" +
                "  public static void main(String[] args) {\n" +
                "    %s var = new %s();\n" +
                "    var.new Inner().test(null);\n" +
                "  }\n" +
                "}", PACKAGE, NotNull.class.getName(), CLASS_NAME, CLASS_NAME, CLASS_NAME);
        expectNpeFromParameterCheck(testSource, "param", expectRunResult);
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
                "    void test(@NotNull String param) {\n" +
                "    }\n" +
                "  }\n" +
                "\n" +
                "  public static void main(String[] args) {\n" +
                "    new %s.Inner().test(null);\n" +
                "  }\n" +
                "}", PACKAGE, NotNull.class.getName(), CLASS_NAME, CLASS_NAME);
        expectNpeFromParameterCheck(testSource, "param", expectRunResult);
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
                "  public void test(Object o) {\n" +
                "    class Local {\n" +
                "      void test(@NotNull Object param) {\n" +
                "      }\n" +
                "    }\n" +
                "    new Local().test(o);\n" +
                "  }\n" +
                "\n" +
                "  public static void main(String[] args) {\n" +
                "    new %s().test(null);\n" +
                "  }\n" +
                "}", PACKAGE, NotNull.class.getName(), CLASS_NAME, CLASS_NAME);
        expectNpeFromParameterCheck(testSource, "param", expectRunResult);
        doTest(testSource);
    }

    @Test
    public void constructor_this() {
        String testSource = String.format(
                "package %s;\n" +
                "\n" +
                "import %s;\n" +
                "\n" +
                "public class %s {\n" +
                "\n" +
                "  public %s(@NotNull Integer intParam) {\n" +
                "    this(1.0);\n" +
                "  }\n" +
                "\n" +
                "  public %s(Double numberParam) {\n" +
                "  }\n" +
                "\n" +
                "  public static void main(String[] args) {\n" +
                "    Integer i = null;\n" +
                "    new %s(i);\n" +
                "  }\n" +
                "}", PACKAGE, NotNull.class.getName(), CLASS_NAME, CLASS_NAME, CLASS_NAME, CLASS_NAME);
        expectNpeFromParameterCheck(testSource, "intParam", expectRunResult);
        doTest(testSource);
    }

    @Test
    public void constructor_super() {
        String testSource = String.format(
                "package %s;\n" +
                "\n" +
                "import %s;\n" +
                "\n" +
                "public class %s {\n" +
                "\n" +
                "  public %s(@NotNull Integer intParam) {\n" +
                "    super();\n" +
                "  }\n" +
                "\n" +
                "  public static void main(String[] args) {\n" +
                "    new %s(null);\n" +
                "  }\n" +
                "}", PACKAGE, NotNull.class.getName(), CLASS_NAME, CLASS_NAME, CLASS_NAME);
        expectNpeFromParameterCheck(testSource, "intParam", expectRunResult);
        doTest(testSource);
    }

    @Test
    public void nonDefaultExceptionToThrow() {
        settingsBuilder.withExceptionToThrow(InstrumentationType.METHOD_PARAMETER,
                                             IllegalArgumentException.class.getSimpleName());
        String testSource = prepareParameterTestSource(
                NotNull.class.getName(),
                String.format("public void %s(@NotNull Integer i1) {}", METHOD_NAME),
                "null"
        );
        String parameterName = "i1";
        expectRunResult.withExceptionClass(IllegalArgumentException.class)
                       .withExceptionMessageSnippet(parameterName)
                       .atLine(findLineNumber(testSource, parameterName));
        doTest(testSource);
    }

    @Test
    public void customExceptionText() {
        settingsBuilder.withExceptionTextPattern(InstrumentationType.METHOD_PARAMETER,
                                                 "${capitalize(PARAMETER_NAME)} must not be null");
        String testSource = prepareParameterTestSource(
                NotNull.class.getName(),
                String.format("public void %s(@NotNull Integer arg) {}", METHOD_NAME),
                "null"
        );
        expectRunResult.withExceptionClass(NullPointerException.class)
                       .withExceptionMessageSnippet("Arg must not be null")
                       .atLine(findLineNumber(testSource, "arg"));
        doTest(testSource);
    }

    @Test
    public void springBoot() {
        settingsBuilder.withExceptionToThrow(InstrumentationType.METHOD_PARAMETER,
                                             IllegalArgumentException.class.getSimpleName())
                       .withExceptionTextPattern(InstrumentationType.METHOD_PARAMETER,
                                                 "${capitalize(PARAMETER_NAME)} must not be null");
        String testSource = prepareParameterTestSource(
                NotNull.class.getName(),
                String.format("public void %s(@NotNull Integer myArg) {}", METHOD_NAME),
                "null"
        );
        expectRunResult.withExceptionClass(IllegalArgumentException.class)
                       .withExceptionMessageSnippet("MyArg must not be null")
                       .atLine(findLineNumber(testSource, "myArg"));
        doTest(testSource);
    }
}
