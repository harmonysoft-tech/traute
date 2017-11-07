package tech.harmonysoft.oss.traute.maven.test.suite;

import org.junit.jupiter.api.extension.ExtendWith;
import tech.harmonysoft.oss.traute.maven.test.impl.TrauteMavenExtension;
import tech.harmonysoft.oss.traute.test.suite.RestrictedInstrumentationTest;

@ExtendWith(TrauteMavenExtension.class)
public class MavenRestrictedInstrumentationTest extends RestrictedInstrumentationTest {
}
