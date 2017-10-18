package tech.harmonysoft.oss.traute.javac.parameter;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Names;
import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.traute.javac.Instrumentator;

import static tech.harmonysoft.oss.traute.javac.util.InstrumentationUtil.buildVarCheck;

/**
 * <p>Enhances target method in a way to include a {@code null}-check for the target method parameter.</p>
 * <p>Thread-safe.</p>
 */
public class ParameterInstrumentator implements Instrumentator<ParameterToInstrumentInfo> {

    @Override
    public void instrument(@NotNull ParameterToInstrumentInfo info) {
        String parameterName = info.getMethodParameter().getName().toString();
        String errorMessage = String.format(
                "Argument '%s' of type %s (#%d out of %d, zero-based) is marked by @%s but got null for it",
                parameterName, info.getMethodParameter().getType(),
                info.getMethodParameterIndex(), info.getMethodParametersNumber(), info.getNotNullAnnotation()
        );
        TreeMaker factory = info.getContext().getAstFactory();
        Names symbolsTable = info.getContext().getSymbolsTable();
        JCTree.JCBlock body = info.getBody();
        body.stats = body.stats.prepend(buildVarCheck(factory, symbolsTable, parameterName, errorMessage));
    }
}
