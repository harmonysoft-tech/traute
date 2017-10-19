package tech.harmonysoft.oss.traute.test.api.model;

import org.jetbrains.annotations.NotNull;

/**
 * General output for particular input
 *
 * @param <T>   input type
 */
public interface Result<T> {

    /**
     * @return      current's result input data
     */
    @NotNull
    T getInput();
}
