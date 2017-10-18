package tech.harmonysoft.oss.traute.javac.parameter;

import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.tree.JCTree;
import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.traute.javac.InstrumentationInfo;
import tech.harmonysoft.oss.traute.javac.common.CompilationUnitProcessingContext;

/**
 * A utility data class for describing a method parameter marked by a {@code NotNull} annotation.
 */
public class ParameterToInstrumentInfo implements InstrumentationInfo {

    @NotNull private final CompilationUnitProcessingContext compilationUnitProcessingContext;
    @NotNull private final String                           notNullAnnotation;
    @NotNull private final VariableTree                     methodParameter;
    @NotNull private final JCTree.JCBlock                   body;

    private final int methodParameterIndex;
    private final int methodParametersNumber;

    public ParameterToInstrumentInfo(@NotNull CompilationUnitProcessingContext compilationUnitProcessingContext,
                                     @NotNull String notNullAnnotation,
                                     @NotNull VariableTree methodParameter,
                                     @NotNull JCTree.JCBlock body,
                                     int methodParameterIndex,
                                     int methodParametersNumber)
    {
        this.compilationUnitProcessingContext = compilationUnitProcessingContext;
        this.notNullAnnotation = notNullAnnotation;
        this.methodParameter = methodParameter;
        this.body = body;
        this.methodParameterIndex = methodParameterIndex;
        this.methodParametersNumber = methodParametersNumber;
    }

    @Override
    @NotNull
    public CompilationUnitProcessingContext getContext() {
        return compilationUnitProcessingContext;
    }

    @Override
    @NotNull
    public String getNotNullAnnotation() {
        return notNullAnnotation;
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
}
