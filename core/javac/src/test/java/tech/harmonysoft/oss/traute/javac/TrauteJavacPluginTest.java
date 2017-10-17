package tech.harmonysoft.oss.traute.javac;

import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.traute.AbstractTrauteTest;

import java.util.ArrayList;
import java.util.List;

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
        // TODO den deliver target annotations
        List<String> result = new ArrayList<>();
        result.add("-Xplugin:" + TrauteJavacPlugin.NAME);
        return result;
    }
}
