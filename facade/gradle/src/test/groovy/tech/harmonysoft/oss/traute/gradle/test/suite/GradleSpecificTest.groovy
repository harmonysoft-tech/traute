package tech.harmonysoft.oss.traute.gradle.test.suite

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import tech.harmonysoft.oss.traute.gradle.test.impl.GradlePredefinedProjectCompiler
import tech.harmonysoft.oss.traute.gradle.test.impl.TrautePredefinedProjectGradleExtension
import tech.harmonysoft.oss.traute.test.suite.AbstractTrauteTest

import static tech.harmonysoft.oss.traute.test.util.TestUtil.findInClassPath

@ExtendWith(TrautePredefinedProjectGradleExtension)
class GradleSpecificTest extends AbstractTrauteTest {

    @Test
    void aptPlugin() {
        ((GradlePredefinedProjectCompiler) compiler).resourceRoot = 'aptPlugin'
        expectRunResult.withExceptionClass(IllegalArgumentException)
                .withExceptionMessageSnippet('name')
        doTest(findInClassPath('aptPlugin/src/main/java', 'tech.harmonysoft.oss.traute.Test'))
    }
}
