package tech.harmonysoft.oss.traute.javac.instrumentation.parameter;

import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.tree.JCTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.harmonysoft.oss.traute.common.instrumentation.InstrumentationType;
import tech.harmonysoft.oss.traute.javac.instrumentation.InstrumentationInfo;
import tech.harmonysoft.oss.traute.javac.common.CompilationUnitProcessingContext;

/**
 * A utility data class for describing a method parameter marked by a {@code NotNull} annotation.
 */
public class ParameterToInstrumentInfo implements InstrumentationInfo {

    @NotNull private final CompilationUnitProcessingContext compilationUnitProcessingContext;
    @NotNull private final VariableTree                     methodParameter;
    @NotNull private final JCTree.JCBlock                   body;

    private final String notNullAnnotation;
    private final String notNullByDefaultAnnotationDescription;

    @Nullable private final String qualifiedMethodName;

    private final int     methodParameterIndex;
    private final int     methodParametersNumber;
    private final boolean constructor;

    public ParameterToInstrumentInfo(@NotNull CompilationUnitProcessingContext compilationUnitProcessingContext,
                                     @Nullable String notNullAnnotation,
                                     @Nullable String notNullByDefaultAnnotationDescription,
                                     @NotNull VariableTree methodParameter,
                                     @NotNull JCTree.JCBlock body,
                                     @Nullable String qualifiedMethodName,
                                     int methodParameterIndex,
                                     int methodParametersNumber,
                                     boolean constructor)
    {
        if (notNullAnnotation == null && notNullByDefaultAnnotationDescription == null) {
            throw new IllegalArgumentException(String.format(
                    "Detected an invalid attempt to instrument a method parameter - either NotNull annotation or "
                    + "NotNullByDefault annotations are undefined. Method: %s(), parameter: %s",
                    qualifiedMethodName, methodParameter.getName()));
        }
        this.compilationUnitProcessingContext = compilationUnitProcessingContext;
        this.notNullAnnotation = notNullAnnotation;
        this.notNullByDefaultAnnotationDescription = notNullByDefaultAnnotationDescription;
        this.methodParameter = methodParameter;
        this.body = body;
        this.qualifiedMethodName = qualifiedMethodName;
        this.methodParameterIndex = methodParameterIndex;
        this.methodParametersNumber = methodParametersNumber;
        this.constructor = constructor;
    }

    @Override
    @NotNull
    public InstrumentationType getType() {
        return InstrumentationType.METHOD_PARAMETER;
    }

    @Override
    @NotNull
    public CompilationUnitProcessingContext getContext() {
        return compilationUnitProcessingContext;
    }

    @Override
    public String getNotNullAnnotation() {
        return notNullAnnotation;
    }

    @Override
    public String getNotNullByDefaultAnnotationDescription() {
        return notNullByDefaultAnnotationDescription;
    }

    /**
     * @return {@code AST} element for the method parameter marked by the {@code NotNull} annotation
     */
    @NotNull
    public VariableTree getMethodParameter() {
        return methodParameter;
    }

    /**
     * @return body of the method which target parameter is marked by the {@code NotNull} annotation
     */
    @NotNull
    public JCTree.JCBlock getBody() {
        return body;
    }

    /**
     * @return  qualified method name which {@code NotNull} parameter should be instrumented
     *          (if that information is available)
     */
    @Nullable
    public String getQualifiedMethodName() {
        return qualifiedMethodName;
    }

    /**
     * @return target parameter's index (zero-based)
     */
    public int getMethodParameterIndex() {
        return methodParameterIndex;
    }

    /**
     * @return total number of the target method's parameters
     */
    public int getMethodParametersNumber() {
        return methodParametersNumber;
    }

    /**
     * @return  {@code true} if target method is a constructor
     */
    public boolean isConstructor() {
        return constructor;
    }
}
