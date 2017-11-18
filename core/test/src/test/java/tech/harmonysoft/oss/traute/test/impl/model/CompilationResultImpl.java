package tech.harmonysoft.oss.traute.test.impl.model;

import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.traute.test.api.model.ClassFile;
import tech.harmonysoft.oss.traute.test.api.model.CompilationResult;
import tech.harmonysoft.oss.traute.test.api.model.TestSource;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static java.util.Collections.emptyMap;

public class CompilationResultImpl implements CompilationResult {

    private final Map<String, String> additionalInfo = new HashMap<>();

    @NotNull private final Supplier<Collection<ClassFile>> compiledClassesSupplier;
    @NotNull private final String compilationOutput;
    @NotNull private final TestSource input;

    public CompilationResultImpl(@NotNull Supplier<Collection<ClassFile>> compiledClassesSupplier,
                                 @NotNull String compilationOutput,
                                 @NotNull TestSource input)
    {
        this(compiledClassesSupplier, compilationOutput, input, emptyMap());
    }

    public CompilationResultImpl(@NotNull Supplier<Collection<ClassFile>> compiledClassesSupplier,
                                 @NotNull String compilationOutput,
                                 @NotNull TestSource input,
                                 @NotNull Map<String, String> additionalInfo)
    {
        this.compiledClassesSupplier = compiledClassesSupplier;
        this.compilationOutput = compilationOutput;
        this.input = input;
        this.additionalInfo.putAll(additionalInfo);
    }

    @Override
    @NotNull
    public Supplier<Collection<ClassFile>> getCompiledClassesSupplier() {
        return compiledClassesSupplier;
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

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("Test source:\n\n").append(getInput().getSourceText()).append("\n\n");
        buffer.append("Compile output:\n").append(getCompilationOutput());

        for (Map.Entry<String, String> entry : additionalInfo.entrySet()) {
            buffer.append("\n\n").append(entry.getKey()).append(":\n").append(entry.getValue());
        }
        return buffer.toString();
    }
}
