package tech.harmonysoft.oss.traute.javac.text;

import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.traute.common.util.TrauteConstants;
import tech.harmonysoft.oss.traute.javac.instrumentation.parameter.ParameterToInstrumentInfo;

import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.singletonList;

public class ParameterCheckExceptionTextGeneratorSpi implements ExceptionTextGeneratorSpi<ParameterToInstrumentInfo> {

    private final Set<String> supportedVariables = new HashSet<>(singletonList(
            TrauteConstants.VARIABLE_PARAMETER_NAME
    ));

    @NotNull
    @Override
    public Set<String> getSupportedVariables() {
        return supportedVariables;
    }

    @NotNull
    @Override
    public String getVariableValue(@NotNull String variableName, @NotNull ParameterToInstrumentInfo context) {
        switch (variableName) {
            case TrauteConstants.VARIABLE_PARAMETER_NAME:
                return context.getMethodParameter().getName().toString();
            default:
                throw new IllegalArgumentException(String.format(
                        "Can't map variable with name '%s' to data from the %s. Make sure to use only supported "
                        + "variables: %s", variableName, context.getClass().getName(), getSupportedVariables()
                ));
        }
    }
}
