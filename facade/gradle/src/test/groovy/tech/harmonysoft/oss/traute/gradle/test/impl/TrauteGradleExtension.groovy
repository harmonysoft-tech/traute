package tech.harmonysoft.oss.traute.gradle.test.impl

import tech.harmonysoft.oss.traute.test.api.engine.TestCompiler
import tech.harmonysoft.oss.traute.test.impl.engine.AbstractTrauteExtension
import tech.harmonysoft.oss.traute.test.suite.AbstractTrauteTest

class TrauteGradleExtension extends AbstractTrauteExtension {

    TrauteGradleExtension() {
        System.setProperty(AbstractTrauteTest.ACTIVATION_PROPERTY, Boolean.TRUE.toString())
    }

    @Override
    protected TestCompiler getCompiler() {
        return GradleTestCompiler.INSTANCE
    }
}
