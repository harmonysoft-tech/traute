package tech.harmonysoft.oss.traute.javac.instrumentation.method;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Implements {@link ReturnInstrumentationAstParent} for a situation when the check should be
 * located in a code block ({@code {}}), e.g.
 * <pre>
 *     if (something) {
 *         return result;
 *     }
 * </pre>
 */
public class BlockReturnInstrumentationAstParent implements ReturnInstrumentationAstParent {

    @NotNull private final JCTree.JCBlock block;

    public BlockReturnInstrumentationAstParent(@NotNull JCTree.JCBlock block) {
        this.block = block;
    }

    @Override
    @NotNull
    public List<JCTree.JCStatement> getStatements() {
        return block.stats == null ? List.nil() : block.stats;
    }

    @Override
    public void setStatements(@NotNull List<JCTree.JCStatement> statements) {
        block.stats = statements;
    }
}
