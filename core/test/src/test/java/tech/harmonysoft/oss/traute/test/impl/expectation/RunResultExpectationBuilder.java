package tech.harmonysoft.oss.traute.test.impl.expectation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnusedReturnValue")
public class RunResultExpectationBuilder {

    @Nullable private String  expectedExceptionClass;
    @Nullable private String  expectedExceptionMessageSnippet;
    @Nullable private String  expectedExceptionMessageText;
    @Nullable private Integer thrownAtLine;

    @NotNull
    public static RunResultExpectationBuilder expectRunResult() {
        return new RunResultExpectationBuilder();
    }

    @NotNull
    public RunResultExpectationBuilder withExceptionClass(@NotNull Class<?> clazz) {
        return withExceptionClass(clazz.getName());
    }

    @NotNull
    public RunResultExpectationBuilder withExceptionClass(@NotNull String clazz) {
        expectedExceptionClass = clazz;
        return this;
    }

    @NotNull
    public RunResultExpectationBuilder withExceptionMessageSnippet(@NotNull String snippet) {
        expectedExceptionMessageSnippet = snippet;
        return this;
    }

    @NotNull
    public RunResultExpectationBuilder withExceptionMessage(@NotNull String message) {
        expectedExceptionMessageText = message;
        return this;
    }

    @NotNull
    public RunResultExpectationBuilder atLine(int line) {
        thrownAtLine = line;
        return this;
    }

    @NotNull
    public RunResultExpectation build() {
        return new RunResultExpectation(expectedExceptionClass,
                                        expectedExceptionMessageSnippet,
                                        expectedExceptionMessageText,
                                        thrownAtLine);
    }
}
