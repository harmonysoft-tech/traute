package tech.harmonysoft.oss.traute.test.impl.expectation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.harmonysoft.oss.traute.test.api.expectation.Expectation;
import tech.harmonysoft.oss.traute.test.api.model.RunResult;
import tech.harmonysoft.oss.traute.test.api.model.TestSource;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class RunResultExpectation implements Expectation<RunResult> {

    @Nullable private final Class<?> exceptionClass;
    @Nullable private final String   exceptionMessageSnippet;
    @Nullable private final String   exceptionMessageText;
    @Nullable private final Integer  thrownAtLine;

    public RunResultExpectation(@Nullable Class<?> exceptionClass,
                                @Nullable String exceptionMessageSnippet,
                                @Nullable String exceptionMessageText,
                                @Nullable Integer thrownAtLine)
    {
        this.exceptionClass = exceptionClass;
        this.exceptionMessageSnippet = exceptionMessageSnippet;
        this.exceptionMessageText = exceptionMessageText;
        this.thrownAtLine = thrownAtLine;
    }

    @Override
    public void match(@NotNull RunResult actual) {
        boolean expectException = exceptionClass != null
                                  || exceptionMessageSnippet != null
                                  || exceptionMessageText != null;
        Optional<Throwable> exceptionOptional = actual.getException();
        TestSource testSource = actual.getInput().getInput();
        if (exceptionOptional.isPresent()) {
            if (!expectException) {
                fail(String.format(
                        "Expected that running the program below doesn't produce any exception but got %s.%n" +
                        "Test source:%n%n%s",
                        exceptionOptional.get(), testSource.getSourceText()
                ));
            }
            matchException(exceptionOptional.get(), testSource);
        } else {
            if (expectException) {
                fail(String.format(
                        "Expected that running the program below produces %s but it is executed successfully."
                        + "%nTest source:%n%n%s", getExpectedExceptionDetails(), testSource.getSourceText()));
            }
        }
    }

    private void matchException(@NotNull Throwable actual, @NotNull TestSource testSource) {
        String exceptionDescription;

        if (exceptionClass == null) {
            exceptionDescription = "an exception";
        } else {
            if (exceptionClass != actual.getClass()) {
                fail(String.format(
                        "Expected that running the program below throws a %s but got a %s.%nSource:%n%n%s",
                        exceptionClass.getName(), actual.getClass().getName(), testSource.getSourceText()
                ));
            }
            exceptionDescription = "a " + exceptionClass.getName();
        }

        if (exceptionMessageSnippet != null) {
            String message = actual.getMessage();
            if (!message.contains(exceptionMessageSnippet)) {
                fail(String.format(
                        "Expected that running the program below throws %s with a text which contains '%s'. "
                        + "However, the text is different: '%s'.%nSource:%n%n%s",
                        exceptionDescription, exceptionMessageSnippet, exceptionMessageText,
                        testSource.getSourceText()
                ));
            }
        }

        if (exceptionMessageText != null) {
            String message = actual.getMessage();
            if (!exceptionMessageText.equalsIgnoreCase(message)) {
                assertEquals(exceptionMessageText, message, () -> String.format(
                        "Expected that running the program below throws %s with particular error message."
                        + "%nSource:%n%n%s",
                        exceptionDescription, testSource.getSourceText()
                ));
            }
        }

        if (thrownAtLine != null) {
            StringWriter stringWriter = new StringWriter();
            actual.printStackTrace(new PrintWriter(stringWriter));
            stringWriter.flush();
            int lineNumber = actual.getStackTrace()[0].getLineNumber();
            if (lineNumber != thrownAtLine) {
                fail(String.format(
                        "Expected that running the program below throws %s exception from line %d but it's thrown "
                        + "from line %d:%n%s%nTest source:%n%n%s",
                        exceptionDescription, thrownAtLine, lineNumber, stringWriter, testSource.getSourceText()
                ));
            }
        }
    }

    @NotNull
    private String getExpectedExceptionDetails() {
        StringBuilder result = new StringBuilder();

        if (exceptionClass == null) {
            result.append("an exception");
        } else {
            result.append("an exception of class ").append(exceptionClass.getName());
        }

        if (exceptionMessageSnippet != null) {
            result.append(" which message has '").append(exceptionMessageSnippet).append("' snippet");
        }

        if (exceptionMessageText != null) {
            result.append(" with message '").append(exceptionMessageText).append("'");
        }

        return result.toString();
    }
}
