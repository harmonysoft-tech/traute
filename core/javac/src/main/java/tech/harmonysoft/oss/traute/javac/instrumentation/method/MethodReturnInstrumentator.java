package tech.harmonysoft.oss.traute.javac.instrumentation.method;

import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;
import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.traute.javac.common.CompilationUnitProcessingContext;
import tech.harmonysoft.oss.traute.javac.instrumentation.AbstractInstrumentator;
import tech.harmonysoft.oss.traute.javac.util.InstrumentationUtil;

import java.util.Optional;

/**
 * <p>
 *     Enhances target method annotated by {@code NotNull} in a way to insert {@code null}-checks for
 *     its return points.
 * </p>
 * <p>
 *     Example.
 *     Original code:
 *     <pre>
 *         &#064;NotNull
 *         public Integer compute() {
 *           return doCompute();
 *         }
 *     </pre>
 *     Instrumented code:
 *     <pre>
 *         &#064;NotNull
 *         public Integer compute() {
 *           Integer tmpVar = doCompute();
 *           if (tmpVar == null) {
 *               throw new NullPointerException("[the details]");
 *           }
 *           return tmpVar;
 *         }
 *     </pre>
 * </p>
 * <p>Thread-safe.</p>
 */
public class MethodReturnInstrumentator extends AbstractInstrumentator<ReturnToInstrumentInfo> {

    @Override
    protected boolean mayBeInstrument(@NotNull ReturnToInstrumentInfo info) {
        ReturnInstrumentationAstParent parent
                = info.getParent().accept(new MethodInstrumentationParentFinder(info), null);
        if (parent == null) {
            return false;
        }
        Optional<List<JCTree.JCStatement>> returnCheckOptional = buildReturnCheck(info);
        if (!returnCheckOptional.isPresent()) {
            return false;
        }
        List<JCTree.JCStatement> statements = parent.getStatements();
        for (int i = 0; i < statements.size(); i++) {
            JCTree.JCStatement statement = statements.get(i);
            if (statement == info.getReturnExpression()) {
                List<JCTree.JCStatement> newStatements = returnCheckOptional.get();
                for (int j = i + 1; j < statements.size(); j++) {
                    newStatements = newStatements.append(statements.get(j));
                }
                for (int j = i - 1; j >= 0; j--) {
                    newStatements = newStatements.prepend(statements.get(j));
                }
                parent.setStatements(newStatements);
                mayBeLogInstrumentation(info);
                return true;
            }
        }
        // When control flow reaches this place, that means that the AST parent doesn't contain any statments, so,
        // we just populate it with new instructions.
        parent.setStatements(returnCheckOptional.get());
        mayBeLogInstrumentation(info);
        return true;
    }

    @NotNull
    private static Optional<List<JCTree.JCStatement>> buildReturnCheck(@NotNull ReturnToInstrumentInfo info) {
        CompilationUnitProcessingContext context = info.getContext();
        ExpressionTree returnExpression = info.getReturnExpression().getExpression();
        if (!(returnExpression instanceof JCTree.JCExpression)) {
            context.getLogger().reportDetails(String.format(
                    "find a 'return' expression of type %s but got %s",
                    JCTree.JCExpression.class.getName(), returnExpression.getClass().getName()
            ));
            return Optional.empty();
        }
        JCTree.JCExpression returnJcExpression = (JCTree.JCExpression) returnExpression;

        TreeMaker factory = context.getAstFactory();
        Names symbolsTable = context.getSymbolsTable();
        String errorMessage = String.format("Detected an attempt to return null from a method marked by %s",
                                            info.getNotNullAnnotation()
        );

        List<JCTree.JCStatement> result = List.of(
                factory.VarDef(
                        factory.Modifiers(0),
                        symbolsTable.fromString(info.getTmpVariableName()),
                        info.getReturnType(),
                        returnJcExpression
                )
        );
        result = result.append(InstrumentationUtil.buildVarCheck(factory,
                                                                 symbolsTable,
                                                                 info.getTmpVariableName(),
                                                                 errorMessage));
        result = result.append(
                factory.Return(
                        factory.Ident(symbolsTable.fromString(info.getTmpVariableName()))));
        return Optional.of(result);
    }

    private void mayBeLogInstrumentation(@NotNull ReturnToInstrumentInfo info) {
        CompilationUnitProcessingContext context = info.getContext();
        if (context.getPluginSettings().isVerboseMode()) {
            String methodName = info.getQualifiedMethodName();
            if (methodName != null) {
                context.getLogger().info("added a null-check for 'return' expression in method " + methodName + "()");
            }
        }
    }
}
