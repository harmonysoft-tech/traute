package tech.harmonysoft.oss.traute.test.impl.expectation;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("UnusedReturnValue")
public class CompilationResultExpectationBuilder {

    private final Set<String> included = new HashSet<>();
    private final Set<String> excluded = new HashSet<>();

    @NotNull
    public static CompilationResultExpectationBuilder expectCompilationResult() {
        return new CompilationResultExpectationBuilder();
    }

    @NotNull
    public CompilationResultExpectationBuilder withText(@NotNull String text) {
        return withText(text, true);
    }

    @NotNull
    public CompilationResultExpectationBuilder withText(@NotNull String text, boolean expected) {
        (expected ? included : excluded).add(text);
        return this;
    }

    @NotNull
    public CompilationResultExpectation build() {
        return new CompilationResultExpectation(included, excluded);
    }
}
