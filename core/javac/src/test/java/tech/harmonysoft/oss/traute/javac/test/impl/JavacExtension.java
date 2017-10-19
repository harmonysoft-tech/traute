package tech.harmonysoft.oss.traute.javac.test.impl;

import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.traute.test.api.engine.TestCompiler;
import tech.harmonysoft.oss.traute.test.impl.engine.AbstractTrauteExtension;

public class JavacExtension extends AbstractTrauteExtension {

    @Override
    protected @NotNull TestCompiler getCompiler() {
        return TrauteJavacTestCompiler.INSTANCE;
    }
}
