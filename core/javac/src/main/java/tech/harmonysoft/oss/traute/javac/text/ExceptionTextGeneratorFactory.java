package tech.harmonysoft.oss.traute.javac.text;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.harmonysoft.oss.traute.common.instrumentation.InstrumentationType;
import tech.harmonysoft.oss.traute.common.util.TrauteConstants;
import tech.harmonysoft.oss.traute.javac.instrumentation.parameter.ParameterToInstrumentInfo;
import tech.harmonysoft.oss.traute.javac.log.TrautePluginLogger;

import java.util.*;
import java.util.function.Function;

public class ExceptionTextGeneratorFactory {

    private final Map<InstrumentationType, ExceptionTextGeneratorSpi<?>> spis           = new HashMap<>();
    private final Map<InstrumentationType, Class<?>>                     contextClasses = new HashMap<>();

    public ExceptionTextGeneratorFactory() {
        spis.put(InstrumentationType.METHOD_PARAMETER, new ParameterCheckExceptionTextGeneratorSpi());
        contextClasses.put(InstrumentationType.METHOD_PARAMETER, ParameterToInstrumentInfo.class);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public Optional<ExceptionTextGenerator<?>> build(@NotNull InstrumentationType type,
                                                     @NotNull String pattern,
                                                     @Nullable TrautePluginLogger logger)
    {
        Class<?> contextClass = contextClasses.get(type);
        if (contextClass == null) {
            if (logger != null) {
                logger.report(String.format(
                        "Unsupported instrumentation type %s - no context class for it is configured. "
                        + "Known mappings: %s. Skipping custom exception text pattern", type, contextClasses
                ));
            }
            return Optional.empty();
        }
        int start = 0;
        List<Function<Object, String>> generators = new ArrayList<>();
        while (start < pattern.length()) {
            int end = pattern.indexOf("${", start);
            if (end < 0) {
                String snippet = pattern.substring(start);
                if (!snippet.isEmpty()) {
                    generators.add(c -> snippet);
                }
                break;
            }
            if (end > start) {
                String snippet = pattern.substring(start, end);
                generators.add(c -> snippet);
            }
            start = end + 2;
            end = pattern.indexOf("}");
            if (end < 0) {
                if (logger != null) {
                    logger.report(String.format(
                            "Invalid custom error text pattern for type '%s' - '%s'. It contains unclosed "
                            + "variable - '${'. Skipping custom exception text pattern", type.getShortName(), pattern
                    ));
                }
                return Optional.empty();
            }
            Function<Object, String> mapper = map(type, pattern.substring(start, end), logger);
            if (mapper == null) {
                return Optional.empty();
            }
            generators.add(mapper);
            start = end + 1;
        }
        return Optional.of(new ExceptionTextGeneratorImpl(contextClass, generators));
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private Function<Object, String> map(@NotNull InstrumentationType type,
                                         @NotNull String pattern,
                                         @Nullable TrautePluginLogger logger)
    {
        ExceptionTextGeneratorSpi spi = spis.get(type);
        if (spi == null) {
            if (logger != null) {
                logger.report(String.format(
                        "Can't prepare a custom exception text pattern for unsupported instrumentation type '%s'. "
                        + "Supported types: %s", type, spis.keySet()
                ));
            }
            return null;
        }

        List<Function<String, String>> functions = new ArrayList<>();
        int start = 0;
        String varName = null;
        while (start < pattern.length()) {
            int end = pattern.indexOf('(', start);
            if (end < 0) {
                end = pattern.indexOf(')');
                varName = pattern.substring(start, end > start ? end : pattern.length());
                break;
            }
            String functionName = pattern.substring(start, end);
            Function<String, String> function = null;
            switch (functionName) {
                case TrauteConstants.FUNCTION_CAPITALIZE:
                    function = ExceptionTextGeneratorFactory::capitalize;
                    break;
            }
            if (function == null) {
                if (logger != null) {
                    logger.report(String.format(
                            "Can't prepare a custom exception text pattern for instrumentation type '%s' "
                            + "- unknown function '%s' in '%s'", type.getShortName(), functionName, pattern
                    ));
                }
                return null;
            }
            functions.add(function);
            start = end + 1;
        }

        if (varName == null) {
            if (logger != null) {
                logger.report(String.format(
                        "Can't prepare a custom exception text pattern for instrumentation type '%s' "
                        + "- no variable name is defined in '%s'", type.getShortName(), pattern
                ));
            }
            return null;
        }

        if (!spi.getSupportedVariables().contains(varName)) {
            if (logger != null) {
                logger.report(String.format(
                        "Can't prepare a custom exception text pattern for instrumentation type '%s' "
                        + "- unsupported variable '%s'. Supported variables: %s",
                        type.getShortName(), varName, spi.getSupportedVariables()
                ));
            }
            return null;
        }

        String finalVarName = varName;
        Collections.reverse(functions);
        return context -> {
            String s = spi.getVariableValue(finalVarName, context);
            for (Function<String, String> function : functions) {
                s = function.apply(s);
            }
            return s;
        };
    }

    @NotNull
    private static String capitalize(@NotNull String s) {
        if (s.isEmpty()) {
            return s;
        } else if (s.length() == 1) {
            return s.toUpperCase();
        } else {
            return s.substring(0, 1).toUpperCase() + s.substring(1);
        }
    }
}
