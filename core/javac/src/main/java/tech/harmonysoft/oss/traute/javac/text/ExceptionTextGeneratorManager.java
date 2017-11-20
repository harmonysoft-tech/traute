package tech.harmonysoft.oss.traute.javac.text;

import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.traute.common.instrumentation.InstrumentationType;
import tech.harmonysoft.oss.traute.common.settings.TrautePluginSettings;
import tech.harmonysoft.oss.traute.javac.log.TrautePluginLogger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ExceptionTextGeneratorManager {

    private static final Map<InstrumentationType, ExceptionTextGenerator<?>> DEFAULT_GENERATORS = new HashMap<>();
    static {
        DEFAULT_GENERATORS.put(InstrumentationType.METHOD_PARAMETER, new DefaultParameterExceptionTextGenerator());
        DEFAULT_GENERATORS.put(InstrumentationType.METHOD_RETURN, new DefaultReturnExceptionTextGenerator());
        if (DEFAULT_GENERATORS.size() != InstrumentationType.values().length) {
            throw new RuntimeException(String.format(
                    "Default exception text generators for failed checks are not registered for all "
                    + "instrumentation types. All types: %s, registered types: %s",
                    Arrays.toString(InstrumentationType.values()), DEFAULT_GENERATORS.keySet()
            ));
        }
    }

    private final ConcurrentMap<InstrumentationType, ExceptionTextGenerator<?>> generators = new ConcurrentHashMap<>();

    private final ExceptionTextGeneratorFactory factory = new ExceptionTextGeneratorFactory();

    @NotNull private final TrautePluginLogger logger;

    public ExceptionTextGeneratorManager(@NotNull TrautePluginLogger logger) {
        this.logger = logger;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public <T> ExceptionTextGenerator<T> getGenerator(@NotNull InstrumentationType type,
                                                      @NotNull TrautePluginSettings settings)
    {
        return (ExceptionTextGenerator<T>) generators.computeIfAbsent(type, k -> {
            String pattern = settings.getExceptionTextPattern(type);
            if (pattern == null) {
                return DEFAULT_GENERATORS.get(type);
            }
            Optional<ExceptionTextGenerator<?>> o = factory.build(type, pattern, logger);
            return o.orElse(DEFAULT_GENERATORS.get(type));
        });
    }
}
