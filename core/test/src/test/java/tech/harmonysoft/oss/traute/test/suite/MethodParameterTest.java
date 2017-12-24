package tech.harmonysoft.oss.traute.test.suite;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NonNullType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tech.harmonysoft.oss.traute.common.instrumentation.InstrumentationType;
import tech.harmonysoft.oss.traute.test.fixture.NN;
import tech.harmonysoft.oss.traute.test.impl.model.TestSourceImpl;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import static java.util.Collections.singleton;
import static tech.harmonysoft.oss.traute.common.util.TrauteConstants.PACKAGE_INFO;
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

    @Test
    public void default_checker() {
        doTestAnnotationWithDifferentDeclarationTypes(org.checkerframework.checker.nullness.qual.NonNull.class);
        doTestAnnotationWithDifferentDeclarationTypes(NonNullDecl.class);
        doTestAnnotationWithDifferentDeclarationTypes(NonNullType.class);
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
                    "boolean".equals(primitiveType) ? "true" : "(" + primitiveType + ")1"
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

    @Test
    public void defaultPackage() {
        String testSource = String.format(
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
                "}", NotNull.class.getName(), CLASS_NAME, CLASS_NAME, CLASS_NAME);
        expectNpeFromParameterCheck(testSource, "intParam", expectRunResult);
        doTest(CLASS_NAME, testSource);
    }

    @Test
    public void notNullByDefault_package() {
        String packageInfoSource = String.format(
                "@ParametersAreNonnullByDefault\n" +
                "package %s;\n" +
                "\n" +
                "import javax.annotation.ParametersAreNonnullByDefault;",
                PACKAGE);
        String testSource = String.format(
                "package %s;\n" +
                "" +
                "public class %s {\n" +
                "\n" +
                "  public %s(Integer intParam) {\n" +
                "  }\n" +
                "\n" +
                "  public static void main(String[] args) {\n" +
                "    new %s(null);\n" +
                "  }\n" +
                "}", PACKAGE, CLASS_NAME, CLASS_NAME, CLASS_NAME);
        expectNpeFromParameterCheck(testSource, "intParam", expectRunResult);
        doTest(new TestSourceImpl(testSource, PACKAGE + "." + CLASS_NAME),
               new TestSourceImpl(packageInfoSource, PACKAGE + "." + PACKAGE_INFO));
    }

    @Test
    public void notNullByDefault_class() {
        String testSource = String.format(
                "@%s\n" +
                "public class %s {\n" +
                "\n" +
                "  public %s(Integer intParam) {\n" +
                "  }\n" +
                "\n" +
                "  public static void main(String[] args) {\n" +
                "    new %s(null);\n" +
                "  }\n" +
                "}", ParametersAreNonnullByDefault.class.getName(), CLASS_NAME, CLASS_NAME, CLASS_NAME);
        expectNpeFromParameterCheck(testSource, "intParam", expectRunResult);
        doTest(CLASS_NAME, testSource);
    }

    @Test
    public void notNullByDefault_method() {
        String testSource = String.format(
                "public class %s {\n" +
                "\n" +
                "  @%s\n" +
                "  public %s(Integer intParam) {\n" +
                "  }\n" +
                "\n" +
                "  public static void main(String[] args) {\n" +
                "    new %s(null);\n" +
                "  }\n" +
                "}", CLASS_NAME, ParametersAreNonnullByDefault.class.getName(), CLASS_NAME, CLASS_NAME);
        expectNpeFromParameterCheck(testSource, "intParam", expectRunResult);
        doTest(CLASS_NAME, testSource);
    }

    @Test
    public void notNullByDefault_custom() {
        String testSource = String.format(
                "@%s\n" +
                "public class %s {\n" +
                "\n" +
                "  public %s(Integer intParam) {\n" +
                "  }\n" +
                "\n" +
                "  public static void main(String[] args) {\n" +
                "    new %s(null);\n" +
                "  }\n" +
                "}", NN.class.getName(), CLASS_NAME, CLASS_NAME, CLASS_NAME);
        settingsBuilder.withNotNullByDefaultAnnotations(InstrumentationType.METHOD_PARAMETER,
                                                        singleton(NN.class.getName()));
        expectNpeFromParameterCheck(testSource, "intParam", expectRunResult);
        doTest(CLASS_NAME, testSource);
    }

    @Test
    public void notNullByDefault_nullableArgument() {
        String testSource = String.format(
                "@%s\n" +
                "public class %s {\n" +
                "\n" +
                "  public %s(@%s Integer intParam) {\n" +
                "  }\n" +
                "\n" +
                "  public static void main(String[] args) {\n" +
                "    new %s(null);\n" +
                "  }\n" +
                "}", ParametersAreNonnullByDefault.class.getName(), CLASS_NAME, CLASS_NAME, Nullable.class.getName(),
                CLASS_NAME);
        doTest(CLASS_NAME, testSource);
    }

    @Test
    public void notNullByDefault_customNullableAnnotation() {
        String testSource = String.format(
                "@%s\n" +
                "public class %s {\n" +
                "\n" +
                "  public %s(@%s Integer intParam) {\n" +
                "  }\n" +
                "\n" +
                "  public static void main(String[] args) {\n" +
                "    new %s(null);\n" +
                "  }\n" +
                "}", ParametersAreNonnullByDefault.class.getName(), CLASS_NAME, CLASS_NAME, NN.class.getName(),
                CLASS_NAME);
        settingsBuilder.withNullableAnnotations(NN.class.getName());
        doTest(CLASS_NAME, testSource);
    }

    @Test
    public void customNotBuiltinException() {
        String testClassSource = String.format(
                "package %s;\n" +
                "\n" +
                "@%s\n" +
                "public class %s {\n" +
                "\n" +
                "  public %s(Integer intParam) {\n" +
                "  }\n" +
                "\n" +
                "  public static void main(String[] args) {\n" +
                "    new %s(null);\n" +
                "  }\n" +
                "}", PACKAGE, ParametersAreNonnullByDefault.class.getName(), CLASS_NAME, CLASS_NAME, CLASS_NAME);

        String exceptionPackage = PACKAGE + ".util";
        String exceptionClass = "MyException";
        String qualifiedExceptionClass = exceptionPackage + "." + exceptionClass;
        String exceptionSource = String.format(
                "package %s;\n"+
                "\n" +
                "public class %s extends RuntimeException {\n" +
                "    public %s(String message) {\n" +
                "        super(message);\n" +
                "    }\n" +
                "}\n" +
                "\n",
                exceptionPackage, exceptionClass, exceptionClass);
        settingsBuilder.withExceptionToThrow(InstrumentationType.METHOD_PARAMETER, qualifiedExceptionClass);
        expectRunResult.withExceptionClass(qualifiedExceptionClass)
                       .withExceptionMessageSnippet("intParam")
                       .atLine(findLineNumber(testClassSource, "intParam"));
        doTest(new TestSourceImpl(testClassSource, PACKAGE + "." + CLASS_NAME),
               new TestSourceImpl(exceptionSource, qualifiedExceptionClass));
    }

    @Test
    public void parametersOrder_primitiveTypes() {
        String testSource = String.format(
                "public class %s {\n" +
                "\n" +
                "  public %s(int i, @%s Integer intParam) {\n" +
                "  }\n" +
                "\n" +
                "  public static void main(String[] args) {\n" +
                "    new %s(1, null);\n" +
                "  }\n" +
                "}", CLASS_NAME, CLASS_NAME, NotNull.class.getName(), CLASS_NAME);
        expectRunResult.withExceptionClass(NullPointerException.class)
                       .withExceptionMessageSnippet("Argument 'intParam' of type Integer (#1 out of 2, zero-based)")
                       .atLine(findLineNumber(testSource, "intParam"));
        doTest(CLASS_NAME, testSource);
    }

    @Test
    public void booleanParameter() {
        String testSource = String.format(
                "package %s;\n" +
                "\n" +
                "@%s\n" +
                "public class %s {\n" +
                "\n" +
                "  public %s(boolean expression) {\n" +
                "  }\n" +
                "\n" +
                "  public static void main(String[] args) {\n" +
                "    new %s(true);\n" +
                "  }\n" +
                "}", PACKAGE, ParametersAreNonnullByDefault.class.getName(), CLASS_NAME, CLASS_NAME, CLASS_NAME);
        doTest(testSource);
    }
}
