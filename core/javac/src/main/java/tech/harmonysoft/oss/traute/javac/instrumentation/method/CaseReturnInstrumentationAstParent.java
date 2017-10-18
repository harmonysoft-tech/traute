package tech.harmonysoft.oss.traute.javac.instrumentation.method;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Implements {@link ReturnInstrumentationAstParent} for a situation when the check should be
 * located in a {@code 'case'} block, e.g.
 * <pre>
 *     switch (something) {
 *       case variant:
 *         return result;
 *     }
 * </pre>
 */
public class CaseReturnInstrumentationAstParent implements ReturnInstrumentationAstParent {

    @NotNull private final JCTree.JCCase jcCase;

    public CaseReturnInstrumentationAstParent(@NotNull JCTree.JCCase jcCase) {
        this.jcCase = jcCase;
    }

    @Override
    public @NotNull List<JCTree.JCStatement> getStatements() {
        return jcCase.stats == null ? List.nil() : jcCase.stats;
    }

    @Override
    public void setStatements(@NotNull List<JCTree.JCStatement> statements) {
        jcCase.stats = statements;
    }
}
