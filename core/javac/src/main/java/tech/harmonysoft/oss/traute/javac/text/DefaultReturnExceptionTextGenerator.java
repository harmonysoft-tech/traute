package tech.harmonysoft.oss.traute.javac.text;

import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.traute.javac.instrumentation.method.ReturnToInstrumentInfo;

public class DefaultReturnExceptionTextGenerator implements ExceptionTextGenerator<ReturnToInstrumentInfo> {

    @NotNull
    @Override
    public String generate(@NotNull ReturnToInstrumentInfo context) {
        String notNullAnnotation = context.getNotNullAnnotation();
        if (notNullAnnotation == null) {
            return String.format("Detected an attempt to return null from a method %s() but that is incorrect "
                                 + "due to %s",
                                 context.getQualifiedMethodName(), context.getNotNullByDefaultAnnotationDescription());
        } else {
            return String.format("Detected an attempt to return null from a method %s() marked by %s",
                                 context.getQualifiedMethodName(), notNullAnnotation);
        }
    }
}
