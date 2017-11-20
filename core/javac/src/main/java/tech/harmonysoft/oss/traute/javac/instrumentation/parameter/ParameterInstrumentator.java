package tech.harmonysoft.oss.traute.javac.instrumentation.parameter;

import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;
import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.traute.javac.text.ExceptionTextGenerator;
import tech.harmonysoft.oss.traute.javac.common.CompilationUnitProcessingContext;
import tech.harmonysoft.oss.traute.javac.instrumentation.AbstractInstrumentator;

import static tech.harmonysoft.oss.traute.common.instrumentation.InstrumentationType.METHOD_PARAMETER;
import static tech.harmonysoft.oss.traute.javac.util.InstrumentationUtil.buildVarCheck;

/**
 * <p>Enhances target method in a way to include a {@code null}-check for the target method parameter.</p>
 * <p>Thread-safe.</p>
 */
public class ParameterInstrumentator extends AbstractInstrumentator<ParameterToInstrumentInfo> {

    @Override
    protected boolean mayBeInstrument(@NotNull ParameterToInstrumentInfo info) {
        String parameterName = info.getMethodParameter().getName().toString();
        CompilationUnitProcessingContext context = info.getContext();
        ExceptionTextGenerator<ParameterToInstrumentInfo> generator =
                context.getExceptionTextGeneratorManager().getGenerator(METHOD_PARAMETER, context.getPluginSettings());
        String errorMessage = generator.generate(info);
        TreeMaker factory = context.getAstFactory();
        Names symbolsTable = context.getSymbolsTable();
        JCTree.JCBlock body = info.getBody();
        String exceptionToThrow = info.getContext().getPluginSettings().getExceptionToThrow(METHOD_PARAMETER);
        JCTree.JCIf varCheck = buildVarCheck(factory, symbolsTable, parameterName, errorMessage, exceptionToThrow);
        if (info.isConstructor() && isFirstStatementThisOrSuperCall(body)) {
            List<JCTree.JCStatement> newStatements = List.of(varCheck);
            List<JCTree.JCStatement> statements = body.getStatements();
            for (int i = 1; i < statements.size(); i++) {
                newStatements = newStatements.append(statements.get(i));
            }
            newStatements = newStatements.prepend(statements.get(0));
            body.stats = newStatements;
        } else {
            body.stats = body.stats.prepend(varCheck);
        }

        if (context.getPluginSettings().isVerboseMode()) {
            String methodName = info.getQualifiedMethodName();
            String methodNotice = methodName == null ? "" : " in the method " + methodName + "()";
            context.getLogger().info(String.format(
                    "added a null-check for argument '%s'%s",
                    parameterName, methodNotice
            ));
        }
        return true;
    }

    private static boolean isFirstStatementThisOrSuperCall(@NotNull JCTree.JCBlock body) {
        List<JCTree.JCStatement> statements = body.getStatements();
        if (statements.isEmpty()) {
            return false;
        }
        JCTree.JCStatement expressionCandidate = statements.get(0);
        if (expressionCandidate instanceof ExpressionStatementTree) {
            ExpressionStatementTree expression = (ExpressionStatementTree) expressionCandidate;
            ExpressionTree methodInvocationCandidate = expression.getExpression();
            if (methodInvocationCandidate instanceof MethodInvocationTree) {
                MethodInvocationTree methodInvocation = (MethodInvocationTree) methodInvocationCandidate;
                ExpressionTree methodSelect = methodInvocation.getMethodSelect();
                if (methodSelect != null) {
                    String select = methodSelect.toString();
                    return "this".equals(select) || "super".equals(select);
                }
            }
        }
        return false;
    }
}
