package tech.harmonysoft.oss.traute;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import tech.harmonysoft.oss.traute.util.SimpleClassFile;
import tech.harmonysoft.oss.traute.util.SimpleFileManager;
import tech.harmonysoft.oss.traute.util.SimpleSourceFile;
import tech.harmonysoft.oss.traute.util.TestConstants;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.fail;

// TODO den add doc
public abstract class AbstractTrauteTest {

    private static final String METHOD_NAME = "test";

    private static final Object[] MAIN_ARGUMENTS = { new String[0]};

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

    private final Set<String> targetAnnotationsToUse = new HashSet<>();

    @Before
    public void setUp() {
        targetAnnotationsToUse.clear();
    }

    @Test
    public void default_jetBrainsNotNull() {
        doTestArgument("org.jetbrains.annotations.NotNull");
    }

    // TODO den add doc
    @NotNull
    protected abstract List<String> getAdditionalCompilerArgs();

    private void doTestArgument(@NotNull String qualifiedAnnotationClassName) {
        doTestArgument_singleImport(qualifiedAnnotationClassName);
        doTestArgument_wildcardImport(qualifiedAnnotationClassName);
        doTestArgument_qualifiedInPlaceName(qualifiedAnnotationClassName);
    }

    private void doTestArgument_singleImport(@NotNull String qualifiedAnnotationClassName) {
        int lastDotIndex = qualifiedAnnotationClassName.lastIndexOf('.');
        String annotationName = qualifiedAnnotationClassName.substring(lastDotIndex + 1);
        doTest(qualifiedAnnotationClassName,
               String.format("public void %s(@%s String s) {}", METHOD_NAME, annotationName),
               "null"
        );
    }

    private void doTestArgument_wildcardImport(@NotNull String qualifiedAnnotationClassName) {
        int lastDotIndex = qualifiedAnnotationClassName.lastIndexOf('.');
        String annotationName = qualifiedAnnotationClassName.substring(lastDotIndex + 1);
        doTest(String.format("%s.*", qualifiedAnnotationClassName.substring(0, lastDotIndex)),
               String.format("public void %s(@%s String s) {}", METHOD_NAME, annotationName),
               "null"
        );
    }

    private void doTestArgument_qualifiedInPlaceName(@NotNull String qualifiedAnnotationClassName) {
        doTest(null,
               String.format("public void %s(@%s String s) {}", METHOD_NAME, qualifiedAnnotationClassName),
               "null");
    }

    private void doTest(@Nullable String importString, @NotNull String testMethod, @NotNull String callArguments) {
        String testSource = prepareSourceText(importString, testMethod, callArguments);
        byte[] compiledTestSource = compile(testSource);
        boolean gotNpe = false;
        try {
            run(compiledTestSource);
        } catch (NullPointerException e) {
            gotNpe = true;
        }
        if (!gotNpe) {
            fail(String.format("Expected to get a NullPointerException on attempt to call 'new Test().test(%s)' "
                               + "for the source below but that didn't happen.%n%nSource:%n%n%s",
                               callArguments, testSource
            ));
        }
    }

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
        if (bodyEnd < 0) {
            bodyEnd = testMethod.lastIndexOf('}');
        }
        String testMethodDeclaration = testMethod.substring(0, declarationEnd);
        String testMethodBody = testMethod.substring(bodyStart, bodyEnd);
        String indentedTestMethodBody = "  " + testMethodBody.replaceAll("\n", "\n    ");
        String indentedTestMethod = String.format("  %s%n%s%n  }", testMethodDeclaration, indentedTestMethodBody);

        return String.format(CLASS_TEMPLATE, completeImportString, indentedTestMethod, callArguments);
    }

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
