package tech.harmonysoft.oss.traute.test.api.model;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface CompilationResult extends Result<TestSource> {

    /**
     * @return      compiled classes. We return a collection of {@link ClassFile} because
     *              a single source file might result in multiple binaries after compilation, e.g. when
     *              an inner/anonymous classes are involved
     */
    @NotNull
    Collection<ClassFile> getCompiledClasses();

    /**
     * @return      compiler's output generated during processing {@link #getInput() target binaries}
     */
    @NotNull
    String getCompilationOutput();
}
