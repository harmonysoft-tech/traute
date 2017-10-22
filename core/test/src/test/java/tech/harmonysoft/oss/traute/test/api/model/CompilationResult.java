package tech.harmonysoft.oss.traute.test.api.model;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.function.Supplier;

public interface CompilationResult extends Result<TestSource> {

    /**
     * @return      compiled classes. We use a {@link Supplier} here because there is a possible case that
     *              compilation is failed and the test wants to check its {@code stderr}. We return a collection
     *              of {@link ClassFile} because a single source file might result in multiple binaries after
     *              compilation, e.g. when an inner/anonymous classes are involved
     */
    @NotNull
    Supplier<Collection<ClassFile>> getCompiledClassesSupplier();

    /**
     * @return      compiler's output generated during processing {@link #getInput() target binaries}
     */
    @NotNull
    String getCompilationOutput();
}
