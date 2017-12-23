package tech.harmonysoft.oss.traute.test.impl.expectation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.harmonysoft.oss.traute.test.api.expectation.Expectation;
import tech.harmonysoft.oss.traute.test.api.model.RunResult;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class RunResultExpectation implements Expectation<RunResult> {

    @Nullable private final String exceptionClass;
    @Nullable private final String   exceptionMessageSnippet;
    @Nullable private final String   exceptionMessageText;
    @Nullable private final Integer  thrownAtLine;

    public RunResultExpectation(@Nullable String exceptionClass,
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
        if (exceptionOptional.isPresent()) {
            if (!expectException) {
                fail(String.format(
                        "Expected that running the program below doesn't produce any exception but got %s.%n%s",
                        exceptionOptional.get(), actual.getInput()
                ));
            }
            matchException(exceptionOptional.get(), actual.getInput());
        } else {
            if (expectException) {
                fail(String.format(
                        "Expected that running the program below produces %s but it is executed successfully.%n%s",
                        getExpectedExceptionDetails(), actual.getInput()));
            }
        }
    }

    private void matchException(@NotNull Throwable actual, @NotNull Object input) {
        String exceptionDescription;

        if (exceptionClass == null) {
            exceptionDescription = "an exception";
        } else {
            if (!exceptionClass.equals(actual.getClass().getName())) {
                fail(String.format(
                        "Expected that running the program below throws a %s but got a %s.%n%s",
                        exceptionClass, actual.getClass().getName(), input
                ));
            }
            exceptionDescription = "a " + exceptionClass;
        }

        if (exceptionMessageSnippet != null) {
            String message = actual.getMessage();
            if (message == null) {
                fail(String.format(
                        "Expected that running the program below throws %s with a text which contains '%s'. "
                        + "However, the exception has no text.%n%s",
                        exceptionDescription, exceptionMessageSnippet, input
                ));
            }
            if (!message.contains(exceptionMessageSnippet)) {
                fail(String.format(
                        "Expected that running the program below throws %s with a text which contains '%s'. "
                        + "However, the text is different: '%s'.%n%s",
                        exceptionDescription, exceptionMessageSnippet, message, input
                ));
            }
        }

        if (exceptionMessageText != null) {
            String message = actual.getMessage();
            if (!exceptionMessageText.equalsIgnoreCase(message)) {
                assertEquals(exceptionMessageText, message, () -> String.format(
                        "Expected that running the program below throws %s with particular error message.%n%s",
                        exceptionDescription, input
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
                        + "from line %d:%n%s%n%s",
                        exceptionDescription, thrownAtLine, lineNumber, stringWriter, input
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
            result.append("an exception of class ").append(exceptionClass);
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
