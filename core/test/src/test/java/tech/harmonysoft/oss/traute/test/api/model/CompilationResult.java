package tech.harmonysoft.oss.traute.test.api.model;

import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public interface CompilationResult extends Result<TestSource> {

    /**
     * @return      compiled binaries. We use {@link Supplier} instead of raw bytes array because there
     *              is a possible case that we're not interested in them (e.g. when we want to check that
     *              plugin prints non-default settings) and binaries retrieval might be expensive (e.g. during
     *              gradle/maven plugin's testing)
     */
    @NotNull
    Supplier<byte[]> getCompiledBinariesSupplier();

    /**
     * @return      compiler's output generated during processing {@link #getInput() target binaries}
     */
    @NotNull
    String getCompilationOutput();
}
