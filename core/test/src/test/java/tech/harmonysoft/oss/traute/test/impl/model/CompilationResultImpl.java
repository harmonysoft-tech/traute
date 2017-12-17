package tech.harmonysoft.oss.traute.test.impl.model;

import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.traute.test.api.model.ClassFile;
import tech.harmonysoft.oss.traute.test.api.model.CompilationResult;
import tech.harmonysoft.oss.traute.test.api.model.TestSource;
import tech.harmonysoft.oss.traute.test.util.TestUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static java.util.Collections.emptyMap;

public class CompilationResultImpl implements CompilationResult {

    private final Map<String, String>    additionalInfo = new HashMap<>();
    private final Collection<TestSource> input          = new ArrayList<>();

    @NotNull private final Supplier<Collection<ClassFile>> compiledClassesSupplier;
    @NotNull private final String compilationOutput;


    public CompilationResultImpl(@NotNull Supplier<Collection<ClassFile>> compiledClassesSupplier,
                                 @NotNull String compilationOutput,
                                 @NotNull Collection<TestSource> input)
    {
        this(compiledClassesSupplier, compilationOutput, input, emptyMap());
    }

    public CompilationResultImpl(@NotNull Supplier<Collection<ClassFile>> compiledClassesSupplier,
                                 @NotNull String compilationOutput,
                                 @NotNull Collection<TestSource> input,
                                 @NotNull Map<String, String> additionalInfo)
    {
        this.compiledClassesSupplier = compiledClassesSupplier;
        this.compilationOutput = compilationOutput;
        this.input.addAll(input);
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
    public Collection<TestSource> getInput() {
        return input;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("Test sources:\n\n").append(TestUtil.getSources(getInput())).append("\n\n");
        buffer.append("Compile output:\n").append(getCompilationOutput());

        for (Map.Entry<String, String> entry : additionalInfo.entrySet()) {
            buffer.append("\n\n").append(entry.getKey()).append(":\n").append(entry.getValue());
        }
        return buffer.toString();
    }
}
