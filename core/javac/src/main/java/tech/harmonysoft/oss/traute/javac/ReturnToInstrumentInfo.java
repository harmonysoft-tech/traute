package tech.harmonysoft.oss.traute.javac;

import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.tools.javac.tree.JCTree;
import org.jetbrains.annotations.NotNull;

/**
 * A utility data class for describing a 'return' method expression to be instrumented.
 */
public class ReturnToInstrumentInfo {

    @NotNull private final CompilationUnitProcessingContext context;
    @NotNull private final String                           notNullAnnotation;
    @NotNull private final ReturnTree                       returnExpression;
    @NotNull private final JCTree.JCExpression              returnType;
    @NotNull private final String                           tmpVariableName;
    @NotNull private final StatementTree                    parent;

    public ReturnToInstrumentInfo(@NotNull CompilationUnitProcessingContext context,
                                  @NotNull String notNullAnnotation,
                                  @NotNull ReturnTree returnExpression,
                                  @NotNull JCTree.JCExpression returnType,
                                  @NotNull String tmpVariableName,
                                  @NotNull StatementTree parent)
    {
        this.context = context;
        this.notNullAnnotation = notNullAnnotation;
        this.returnExpression = returnExpression;
        this.returnType = returnType;
        this.tmpVariableName = tmpVariableName;
        this.parent = parent;
    }

    /**
     * @return current compilation unit processing context
     */
    @NotNull
    public CompilationUnitProcessingContext getCompilationUnitProcessingContext() {
        return context;
    }

    /**
     * @return  {@code NotNull} annotation used for marking method's return type
     */
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
    public StatementTree getParent() {
        return parent;
    }
}
