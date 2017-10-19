package tech.harmonysoft.oss.traute.javac.test.impl;

import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.traute.test.api.engine.TestCompiler;
import tech.harmonysoft.oss.traute.test.impl.engine.AbstractTrauteExtension;
import tech.harmonysoft.oss.traute.test.suite.AbstractTrauteTest;

public class JavacExtension extends AbstractTrauteExtension {

    public JavacExtension() {
        System.setProperty(AbstractTrauteTest.ACTIVATION_PROPERTY, Boolean.TRUE.toString());
    }

    @Override
    protected @NotNull TestCompiler getCompiler() {
        return TrauteJavacTestCompiler.INSTANCE;
    }
}
