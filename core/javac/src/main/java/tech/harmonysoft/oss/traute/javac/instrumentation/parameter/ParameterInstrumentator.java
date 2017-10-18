package tech.harmonysoft.oss.traute.javac.instrumentation.parameter;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Names;
import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.traute.javac.common.CompilationUnitProcessingContext;
import tech.harmonysoft.oss.traute.javac.instrumentation.AbstractInstrumentator;

import static tech.harmonysoft.oss.traute.javac.util.InstrumentationUtil.buildVarCheck;

/**
 * <p>Enhances target method in a way to include a {@code null}-check for the target method parameter.</p>
 * <p>Thread-safe.</p>
 */
public class ParameterInstrumentator extends AbstractInstrumentator<ParameterToInstrumentInfo> {

    @Override
    protected boolean mayBeInstrument(@NotNull ParameterToInstrumentInfo info) {
        String parameterName = info.getMethodParameter().getName().toString();
        String errorMessage = String.format(
                "Argument '%s' of type %s (#%d out of %d, zero-based) is marked by @%s but got null for it",
                parameterName, info.getMethodParameter().getType(),
                info.getMethodParameterIndex(), info.getMethodParametersNumber(), info.getNotNullAnnotation()
        );
        CompilationUnitProcessingContext context = info.getContext();
        TreeMaker factory = context.getAstFactory();
        Names symbolsTable = context.getSymbolsTable();
        JCTree.JCBlock body = info.getBody();
        body.stats = body.stats.prepend(buildVarCheck(factory, symbolsTable, parameterName, errorMessage));

        if (context.getPluginSettings().isVerboseLog()) {
            String methodName = info.getQualifiedMethodName();
            String methodNotice = methodName == null ? "" : " in the method " + methodName + "()";
            context.getLogger().info(String.format(
                    "added a null-check for argument '%s'%s",
                    parameterName, methodNotice
            ));
        }
        return true;
    }
}
