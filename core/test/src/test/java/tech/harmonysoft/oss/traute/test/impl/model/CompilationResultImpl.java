package tech.harmonysoft.oss.traute.test.impl.model;

import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.traute.test.api.model.ClassFile;
import tech.harmonysoft.oss.traute.test.api.model.CompilationResult;
import tech.harmonysoft.oss.traute.test.api.model.TestSource;

import java.util.Collection;
import java.util.function.Supplier;

public class CompilationResultImpl implements CompilationResult {

    @NotNull private final Supplier<Collection<ClassFile>> compiledBinariesSupplier;
    @NotNull private final String compilationOutput;
    @NotNull private final TestSource input;

    public CompilationResultImpl(@NotNull Supplier<Collection<ClassFile>> compiledBinariesSupplier,
                                 @NotNull String compilationOutput,
                                 @NotNull TestSource input)
    {
        this.compiledBinariesSupplier = compiledBinariesSupplier;
        this.compilationOutput = compilationOutput;
        this.input = input;
    }

    @Override
    @NotNull
    public Supplier<Collection<ClassFile>> getCompiledClassesSupplier() {
        return compiledBinariesSupplier;
    }

    @Override
    @NotNull
    public String getCompilationOutput() {
        return compilationOutput;
    }

    @NotNull
    @Override
    public TestSource getInput() {
        return input;
    }
}