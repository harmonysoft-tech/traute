package tech.harmonysoft.oss.traute.javac.text;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ExceptionTextGeneratorImpl<T> implements ExceptionTextGenerator<T> {

    private final List<Function<T, String>> generators = new ArrayList<>();

    @NotNull private final Class<T> contextClass;

    public ExceptionTextGeneratorImpl(@NotNull Class<T> contextClass, @NotNull List<Function<T, String>> generators) {
        this.contextClass = contextClass;
        this.generators.addAll(generators);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public String generate(@NotNull Object context) {
        if (!contextClass.isInstance(context)) {
            throw new IllegalStateException(String.format(
                    "Can't generate exception text - expected to get a context of type %s but got %s",
                    contextClass.getName(), context.getClass().getName()
            ));
        }
        T typedContext = (T) context;
        StringBuilder buffer = new StringBuilder();
        for (Function<T, String> generator : generators) {
            buffer.append(generator.apply(typedContext));
        }
        return buffer.toString();
    }
}
