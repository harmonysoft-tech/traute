package tech.harmonysoft.oss.traute.javac.test.suite;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import tech.harmonysoft.oss.traute.javac.test.impl.TrauteJavacExtension;
import tech.harmonysoft.oss.traute.test.impl.model.TestSourceImpl;
import tech.harmonysoft.oss.traute.test.suite.MethodParameterTest;

import static tech.harmonysoft.oss.traute.test.util.TestConstants.CLASS_NAME;
import static tech.harmonysoft.oss.traute.test.util.TestUtil.expectNpeFromParameterCheck;

@ExtendWith(TrauteJavacExtension.class)
public class JavacMethodParameterTest extends MethodParameterTest {

    @Test
    public void notNullByDefault_compiledPackage() {
        String myPackage = "tech.harmonysoft.oss.traute.test.fixture";
        String testSource = String.format(
                "package %s;\n" +
                "" +
                "public class %s {\n" +
                "\n" +
                "  public %s(Integer intParam) {\n" +
                "  }\n" +
                "\n" +
                "  public static void main(String[] args) {\n" +
                "    new %s(null);\n" +
                "  }\n" +
                "}", myPackage, CLASS_NAME, CLASS_NAME, CLASS_NAME);
        expectNpeFromParameterCheck(testSource, "intParam", expectRunResult);
        doTest(new TestSourceImpl(testSource, myPackage + "." + CLASS_NAME));
    }
}
