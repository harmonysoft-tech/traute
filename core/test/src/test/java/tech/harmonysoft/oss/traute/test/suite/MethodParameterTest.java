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
import static tech.harmonysoft.oss.traute.test.util.TestConstants.CLASS_NAME;
import static tech.harmonysoft.oss.traute.test.util.TestConstants.METHOD_NAME;
import static tech.harmonysoft.oss.traute.test.util.TestConstants.PACKAGE;
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
}
