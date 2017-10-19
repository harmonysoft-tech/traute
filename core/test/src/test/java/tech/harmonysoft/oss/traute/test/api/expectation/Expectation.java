package tech.harmonysoft.oss.traute.test.api.expectation;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;

/**
 * Defines a contract for an entity which holds particular expectations about the target data.
 *
 * @param <T>   target data's type
 */
public interface Expectation<T> {

    /**
     * Ensures that given data matches current expectations. Is expected to call {@link Assertions#fail(String)}
     * in case of the mismatch.
     *
     * @param actual    data to match
     */
    void match(@NotNull T actual);
}
