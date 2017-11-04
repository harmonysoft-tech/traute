package tech.harmonysoft.oss.traute.gradle.test.impl

import org.jetbrains.annotations.NotNull
import tech.harmonysoft.oss.traute.test.api.engine.TestCompiler
import tech.harmonysoft.oss.traute.test.impl.engine.AbstractTrauteExtension

class TrauteGradleExtension extends AbstractTrauteExtension {

    @NotNull
    @Override
    protected TestCompiler getCompiler() {
        return GradleTestCompiler.INSTANCE
    }
}
