package tech.harmonysoft.oss.traute.gradle.test.suite

import org.junit.jupiter.api.extension.ExtendWith
import tech.harmonysoft.oss.traute.gradle.test.impl.TrauteGradleExtension
import tech.harmonysoft.oss.traute.test.suite.RestrictedInstrumentationTest

@ExtendWith(TrauteGradleExtension)
class GradleRestrictedInstrumentationTest extends RestrictedInstrumentationTest {
}
