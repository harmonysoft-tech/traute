package tech.harmonysoft.oss.traute.test.impl.model;

import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.traute.test.api.model.ClassFile;
import tech.harmonysoft.oss.traute.test.api.model.CompilationResult;
import tech.harmonysoft.oss.traute.test.api.model.TestSource;

import java.util.Collection;

public class CompilationResultImpl implements CompilationResult {

    @NotNull private final Collection<ClassFile> compiledClasses;
    @NotNull private final String compilationOutput;
    @NotNull private final TestSource input;

    public CompilationResultImpl(@NotNull Collection<ClassFile> compiledClasses,
                                 @NotNull String compilationOutput,
                                 @NotNull TestSource input)
    {
        this.compiledClasses = compiledClasses;
        this.compilationOutput = compilationOutput;
        this.input = input;
    }

    @Override
    @NotNull
    public Collection<ClassFile> getCompiledClasses() {
        return compiledClasses;
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
