package tech.harmonysoft.oss.traute.javac.instrumentation.method;

import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.tree.JCTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.harmonysoft.oss.traute.common.instrumentation.InstrumentationType;
import tech.harmonysoft.oss.traute.javac.instrumentation.InstrumentationInfo;
import tech.harmonysoft.oss.traute.javac.common.CompilationUnitProcessingContext;

/**
 * A utility data class for describing a 'return' method expression to be instrumented.
 */
public class ReturnToInstrumentInfo implements InstrumentationInfo {

    @NotNull private final CompilationUnitProcessingContext context;
    @NotNull private final ReturnTree                       returnExpression;
    @NotNull private final JCTree.JCExpression              returnType;
    @NotNull private final String                           tmpVariableName;
    @NotNull private final Tree                             parent;

    @Nullable private final String qualifiedMethodName;

    private final String notNullAnnotation;
    private final String notNullByDefaultAnnotationDescription;

    public ReturnToInstrumentInfo(@NotNull CompilationUnitProcessingContext context,
                                  @Nullable String notNullAnnotation,
                                  @Nullable String notNullByDefaultAnnotationDescription,
                                  @NotNull ReturnTree returnExpression,
                                  @NotNull JCTree.JCExpression returnType,
                                  @NotNull String tmpVariableName,
                                  @NotNull Tree parent,
                                  @Nullable String qualifiedMethodName)
    {
        if (notNullAnnotation == null && notNullByDefaultAnnotationDescription == null) {
            throw new IllegalArgumentException(String.format(
                    "Detected an invalid attempt to instrument a method return - either NotNull annotation or "
                    + "NotNullByDefault annotations are undefined. Method: %s()", qualifiedMethodName));
        }
        this.context = context;
        this.notNullAnnotation = notNullAnnotation;
        this.notNullByDefaultAnnotationDescription = notNullByDefaultAnnotationDescription;
        this.returnExpression = returnExpression;
        this.returnType = returnType;
        this.tmpVariableName = tmpVariableName;
        this.parent = parent;
        this.qualifiedMethodName = qualifiedMethodName;
    }

    @Override
    @NotNull
    public InstrumentationType getType() {
        return InstrumentationType.METHOD_RETURN;
    }

    @Override
    @NotNull
    public CompilationUnitProcessingContext getContext() {
        return context;
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
     * @return  '{@code return}' {@code AST} expression to instrument
     */
    @NotNull
    public ReturnTree getReturnExpression() {
        return returnExpression;
    }

    /**
     * @return  target method's return type
     */
    @NotNull
    public JCTree.JCExpression getReturnType() {
        return returnType;
    }

    /**
     * <p>Allows to get a name of the variable to use for storing {@code return}'s expression value (if necessary).</p>
     * <p>E.g. we don't need a temp variable in code like below:</p>
     * <pre>
     * String s = ...;
     * ...
     * return s;
     * </pre>
     * <p>I.e resulting code might be written as follows:</p>
     * <pre>
     * String s = ...;
     * ...
     * if (s == null) {
     *     throw new NullPointerException("...");
     * }
     * return s;
     *     </pre>
     * <p>However, there is a possible situation that we do need a temp variable:</p>
     * <pre>
     * return compute();
     * </pre>
     * <p>This code should be transformed at the following way:</p>
     * <pre>
     * String tmpVar = compute();
     * if (tmpVar == null) {
     *     throw new NullPointerException("...");
     * }
     * return tmpVar;
     * </pre>
     *
     * @return  name of the variable to use for storing {@code return}'s expression value (if necessary)
     */
    @NotNull
    public String getTmpVariableName() {
        return tmpVariableName;
    }

    /**
     * @return  parent {@code AST} element for the target {@code return} expression to check
     */
    @NotNull
    public Tree getParent() {
        return parent;
    }

    /**
     * @return  qualified method name which {@code NotNull} parameter should be instrumented
     *          (if that information is available)
     */
    @Nullable
    public String getQualifiedMethodName() {
        return qualifiedMethodName;
    }
}
