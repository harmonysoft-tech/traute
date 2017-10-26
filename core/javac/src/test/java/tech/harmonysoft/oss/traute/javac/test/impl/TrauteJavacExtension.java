package tech.harmonysoft.oss.traute.javac.test.impl;

import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.traute.test.api.engine.TestCompiler;
import tech.harmonysoft.oss.traute.test.impl.engine.AbstractTrauteExtension;
import tech.harmonysoft.oss.traute.test.suite.AbstractTrauteTest;

public class TrauteJavacExtension extends AbstractTrauteExtension {

    public TrauteJavacExtension() {
        System.setProperty(AbstractTrauteTest.ACTIVATION_PROPERTY, Boolean.TRUE.toString());
    }

    @Override
    @NotNull
    protected TestCompiler getCompiler() {
        return JavacTestCompiler.INSTANCE;
    }
}
