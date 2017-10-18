package tech.harmonysoft.oss.traute.javac;

import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.traute.AbstractTrauteTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.joining;
import static tech.harmonysoft.oss.traute.javac.TrauteJavacPlugin.*;

/**
 * <p>
 *     Applies {@link AbstractTrauteTest common test scenarios} to the {@code javac} plugin-based approach
 * </p>
 * <p>
 *     General idea:
 *     <ul>
 *       <li>instruct {@code javac} arguments to {@link #getAdditionalCompilerArgs() use our plugin}</li>
 *     </ul>
 * </p>
 */
public class TrauteJavacPluginTest extends AbstractTrauteTest {

    @NotNull
    @Override
    protected List<String> getAdditionalCompilerArgs() {
        List<String> result = new ArrayList<>();
        result.add("-Xplugin:" + NAME);
        Set<String> targetAnnotationsToUse = getNotNullAnnotations();
        if (!targetAnnotationsToUse.isEmpty()) {
            String optionValue = targetAnnotationsToUse.stream().collect(joining(SEPARATOR));
            result.add(String.format("-A%s=%s", OPTION_ANNOTATIONS_NOT_NULL, optionValue));
        }

        Set<String> instrumentationTypes = getInstrumentationTypes();
        if (!instrumentationTypes.isEmpty()) {
            String optionValue = instrumentationTypes.stream().collect(joining(SEPARATOR));
            result.add(String.format("-A%s=%s", OPTION_INSTRUMENTATIONS_TO_USE, optionValue));
        }

        if (isVerboseOutput()) {
            result.add(String.format("-A%s=true", OPTION_LOG_VERBOSE));
        }
        return result;
    }
}
