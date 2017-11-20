package tech.harmonysoft.oss.traute.javac.text;

import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.traute.javac.instrumentation.method.ReturnToInstrumentInfo;

public class DefaultReturnExceptionTextGenerator implements ExceptionTextGenerator<ReturnToInstrumentInfo> {

    @NotNull
    @Override
    public String generate(@NotNull ReturnToInstrumentInfo context) {
        return String.format("Detected an attempt to return null from a method marked by %s",
                             context.getNotNullAnnotation()
        );
    }
}
