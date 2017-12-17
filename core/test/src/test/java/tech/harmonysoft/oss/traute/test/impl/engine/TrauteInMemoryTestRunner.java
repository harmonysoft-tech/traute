package tech.harmonysoft.oss.traute.test.impl.engine;

import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.traute.test.api.engine.TestRunner;
import tech.harmonysoft.oss.traute.test.api.model.ClassFile;
import tech.harmonysoft.oss.traute.test.api.model.CompilationResult;
import tech.harmonysoft.oss.traute.test.api.model.RunResult;
import tech.harmonysoft.oss.traute.test.api.model.TestSource;
import tech.harmonysoft.oss.traute.test.impl.model.RunResultImpl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

import static java.util.stream.Collectors.toMap;
import static tech.harmonysoft.oss.traute.common.util.TrauteConstants.PACKAGE_INFO;

public class TrauteInMemoryTestRunner implements TestRunner {

    public static final TrauteInMemoryTestRunner INSTANCE = new TrauteInMemoryTestRunner();

    /** Arguments to call {@code main()} in the compiled test sources. */
    private static final Object[] MAIN_ARGUMENTS = { new String[0]};

    @Override
    @NotNull
    public RunResult run(@NotNull CompilationResult compilationResult) {
        Map<String, @NotNull byte[]> compiled = compilationResult.getCompiledClassesSupplier().get()
                                                                 .stream()
                                                                 .collect(toMap(ClassFile::getName,
                                                                                ClassFile::getBinaries));
        ClassLoader classLoader = new ClassLoader() {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                byte[] compiledBinaries = compiled.get(name);
                if (compiledBinaries == null) {
                    throw new IllegalArgumentException(String.format(
                            "Can't find compiled binaries for class %s. Available binaries found for %s",
                            name, compiled.keySet()
                    ));
                }
                return defineClass(name, compiledBinaries, 0, compiledBinaries.length);
            }
        };
        Class<?> clazz;
        try {
            clazz = classLoader.loadClass(getStartClass(compilationResult.getInput()));
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
            return new RunResultImpl(compilationResult, null);
        } catch (IllegalAccessException e) {
            return new RunResultImpl(compilationResult, e);
        } catch (InvocationTargetException e) {
            return new RunResultImpl(compilationResult, e.getCause());
        }
    }

    @NotNull
    private static String getStartClass(@NotNull Collection<TestSource> testSources) {
        for (TestSource testSource : testSources) {
            String candidate = testSource.getQualifiedClassName();
            if (!PACKAGE_INFO.equals(candidate)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException(String.format("Can't find a start class to run in test sources: %s",
                                                         testSources));
    }
}
