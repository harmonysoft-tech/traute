package tech.harmonysoft.oss.traute.javac.test.impl;

import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.traute.test.api.engine.TestCompiler;
import tech.harmonysoft.oss.traute.test.impl.engine.AbstractTrauteExtension;

public class TrauteJavacExtension extends AbstractTrauteExtension {

    @Override
    @NotNull
    protected TestCompiler getCompiler() {
        return JavacTestCompiler.INSTANCE;
    }
}
