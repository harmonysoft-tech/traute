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
    @NotNull private final String                           notNullAnnotation;
    @NotNull private final ReturnTree                       returnExpression;
    @NotNull private final JCTree.JCExpression              returnType;
    @NotNull private final String                           tmpVariableName;
    @NotNull private final Tree                             parent;

    @Nullable private final String qualifiedMethodName;

    public ReturnToInstrumentInfo(@NotNull CompilationUnitProcessingContext context,
                                  @NotNull String notNullAnnotation,
                                  @NotNull ReturnTree returnExpression,
                                  @NotNull JCTree.JCExpression returnType,
                                  @NotNull String tmpVariableName,
                                  @NotNull Tree parent,
                                  @Nullable String qualifiedMethodName)
    {
        this.context = context;
        this.notNullAnnotation = notNullAnnotation;
        this.returnExpression = returnExpression;
        this.returnType = returnType;
        this.tmpVariableName = tmpVariableName;
        this.parent = parent;
        this.qualifiedMethodName = qualifiedMethodName;
    }

    @Override
    public @NotNull InstrumentationType getType() {
        return InstrumentationType.METHOD_RETURN;
    }

    @Override
    @NotNull
    public CompilationUnitProcessingContext getContext() {
        return context;
    }

    @Override
    @NotNull
    public String getNotNullAnnotation() {
        return notNullAnnotation;
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
     * <p>
     *     E.g. we don't need a temp variable in code like below:
     *     <pre>
     *         String s = ...;
     *         ...
     *         return s;
     *     </pre>
     *     I.e resulting code might be written as follows:
     *     <pre>
     *         String s = ...;
     *         ...
     *         if (s == null) {
     *             throw new NullPointerException("...");
     *         }
     *         return s;
     *     </pre>
     *     However, there is a possible situation that we do need a temp variable:
     *     <pre>
     *         return compute();
     *     </pre>
     *     This code should be transformed at the following way:
     *     <pre>
     *         String tmpVar = compute();
     *         if (tmpVar == null) {
     *             throw new NullPointerException("...");
     *         }
     *         return tmpVar;
     *     </pre>
     * </p>
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
