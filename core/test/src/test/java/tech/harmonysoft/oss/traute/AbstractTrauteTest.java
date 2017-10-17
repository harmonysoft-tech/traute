package tech.harmonysoft.oss.traute;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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
    private static final String CLASS_TEMPLATE =
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
        String testSource = prepareSourceText(importString, testMethod, "null");
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
        String testSource = prepareSourceText(
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
        String testSource = prepareSourceText(
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
        String[] primitiveTypes = {
                "byte", "short", "char", "int", "long", "float", "double"
        };
        for (String primitiveType : primitiveTypes) {
            String testSource = prepareSourceText(
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
        String testSource = prepareSourceText(
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

    /**
     * Applies given data to the {@link #CLASS_TEMPLATE test source template}.
     *
     * @param importString  an expression for the {@code import} keyword (if any)
     * @param testMethod    {@value #METHOD_NAME}() method body
     * @param callArguments arguments to use for calling the {@value #METHOD_NAME}() method
     * @return              complete test source for the given arguments. It's assumed to be properly formatted
     */
    @NotNull
    private static String prepareSourceText(@Nullable String importString,
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

        return String.format(CLASS_TEMPLATE, completeImportString, indentedTestMethod, callArguments);
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
