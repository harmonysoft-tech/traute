package tech.harmonysoft.oss.traute.test.api.model;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.function.Supplier;

public interface CompilationResult extends Result<TestSource> {

    /**
     * @return      compiled classes. We use {@link Supplier} instead of raw bytes array because there
     *              is a possible case that we're not interested in them (e.g. when we want to check that
     *              plugin prints non-default settings) and binaries retrieval might be expensive (e.g. during
     *              gradle/maven plugin's testing). Also we return a collection of {@link ClassFile} because
     *              a single source file might result in multiple binaries after compilation, e.g. when
     *              an inner/anonymous classes are involved
     */
    @NotNull
    Supplier<Collection<ClassFile>> getCompiledClassesSupplier();

    /**
     * @return      compiler's output generated during processing {@link #getInput() target binaries}
     */
    @NotNull
    String getCompilationOutput();
}
