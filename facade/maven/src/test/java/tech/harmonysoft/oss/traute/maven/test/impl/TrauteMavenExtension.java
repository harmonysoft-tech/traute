package tech.harmonysoft.oss.traute.maven.test.impl;

import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.traute.test.api.engine.TestCompiler;
import tech.harmonysoft.oss.traute.test.impl.engine.AbstractTrauteExtension;

public class TrauteMavenExtension extends AbstractTrauteExtension {

    @Override
    protected @NotNull TestCompiler getCompiler() {
        return MavenTestCompiler.INSTANCE;
    }
}
