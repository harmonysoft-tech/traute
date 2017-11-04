package tech.harmonysoft.oss.traute.test.impl.engine;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import tech.harmonysoft.oss.traute.test.api.engine.TestCompiler;
import tech.harmonysoft.oss.traute.test.api.engine.TestRunner;
import tech.harmonysoft.oss.traute.test.suite.AbstractTrauteTest;

import java.lang.reflect.Field;

public abstract class AbstractTrauteExtension implements TestInstancePostProcessor {

    public AbstractTrauteExtension() {
        System.setProperty(AbstractTrauteTest.ACTIVATION_PROPERTY, Boolean.TRUE.toString());
    }

    @Override
    public final void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
        for (Class<?> clazz = testInstance.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                if (field.getType() == TestCompiler.class) {
                    field.setAccessible(true);
                    field.set(testInstance, getCompiler());
                } else if (field.getType() == TestRunner.class) {
                    field.setAccessible(true);
                    field.set(testInstance, TrauteInMemoryTestRunner.INSTANCE);
                }
            }
        }
    }

    @NotNull
    protected abstract TestCompiler getCompiler();
}
