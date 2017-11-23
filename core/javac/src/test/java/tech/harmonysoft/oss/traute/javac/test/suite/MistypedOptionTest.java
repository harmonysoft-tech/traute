package tech.harmonysoft.oss.traute.javac.test.suite;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import tech.harmonysoft.oss.traute.common.settings.TrautePluginSettings;
import tech.harmonysoft.oss.traute.javac.test.impl.JavacTestCompiler;
import tech.harmonysoft.oss.traute.test.suite.AbstractTrauteTest;

import java.util.Collections;
import java.util.List;

import static tech.harmonysoft.oss.traute.test.util.TestConstants.CLASS_NAME;
import static tech.harmonysoft.oss.traute.test.util.TestConstants.PACKAGE;

public class MistypedOptionTest extends AbstractTrauteTest {

    @Test
    public void mistyped() {
        String key = "traute.log.verboss";
        String value = "true";
        compiler = new JavacTestCompiler() {
            @Override
            protected @NotNull List<String> getAdditionalCompilerArgs(@NotNull TrautePluginSettings settings) {
                List<String> result = super.getAdditionalCompilerArgs(settings);
                result.add(String.format("-A%s=%s", key, value));
                return result;
            }
        };
        String testSource = String.format(
                "package %s;\n" +
                "\n" +
                "import %s;\n" +
                "\n" +
                "public class %s {\n" +
                "\n" +
                "  void test(@NotNull String param) {\n" +
                "  }\n" +
                "}", PACKAGE, NotNull.class.getName(), CLASS_NAME);
        expectCompilationResult.withText(String.format("Found an unknown setting '%s' with value '%s'", key, value));
        doCompile(testSource);
    }
}
