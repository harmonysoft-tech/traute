package tech.harmonysoft.oss.traute.test.impl.expectation;

import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.traute.test.api.expectation.Expectation;
import tech.harmonysoft.oss.traute.test.api.model.CompilationResult;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.fail;

public class CompilationResultExpectation implements Expectation<CompilationResult> {

    @NotNull private final Set<String> included;
    @NotNull private final Set<String> excluded;

    public CompilationResultExpectation(@NotNull Set<String> included, @NotNull Set<String> excluded) {
        this.included = Collections.unmodifiableSet(new HashSet<>(included));
        this.excluded = Collections.unmodifiableSet(new HashSet<>(excluded));
    }

    @Override
    public void match(@NotNull CompilationResult actual) {
        String output = actual.getCompilationOutput();
        for (String s : included) {
            Pattern pattern = Pattern.compile(s);
            if (!pattern.matcher(output).find()) {
                fail(String.format("Expected to find text '%s' in the compilation output:%n%n%s", s, output));
            }
        }
        for (String s : excluded) {
            Pattern pattern = Pattern.compile(s);
            if (pattern.matcher(output).find()) {
                fail(String.format("Expected that text '%s' is not contained in the compilation output:%n%n%s",
                                   s, output));
            }
        }
    }
}
