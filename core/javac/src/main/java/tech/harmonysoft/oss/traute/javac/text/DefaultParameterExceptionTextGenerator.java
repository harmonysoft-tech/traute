package tech.harmonysoft.oss.traute.javac.text;

import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.traute.javac.instrumentation.parameter.ParameterToInstrumentInfo;

public class DefaultParameterExceptionTextGenerator implements ExceptionTextGenerator<ParameterToInstrumentInfo> {

    @NotNull
    @Override
    public String generate(@NotNull ParameterToInstrumentInfo context) {
        String parameterName = context.getMethodParameter().getName().toString();
        String notNullAnnotation = context.getNotNullAnnotation();
        if (notNullAnnotation == null) {
            return String.format(
                    "Argument '%s' of type %s (#%d out of %d, zero-based) must be not-null (implied by the %s) "
                    + "but got null for it",
                    parameterName, context.getMethodParameter().getType(), context.getMethodParameterIndex(),
                    context.getMethodParametersNumber(), context.getNotNullByDefaultAnnotationDescription()
            );

        } else {
            return String.format(
                    "Argument '%s' of type %s (#%d out of %d, zero-based) is marked by @%s but got null for it",
                    parameterName, context.getMethodParameter().getType(), context.getMethodParameterIndex(),
                    context.getMethodParametersNumber(), notNullAnnotation
            );

        }
    }
}
