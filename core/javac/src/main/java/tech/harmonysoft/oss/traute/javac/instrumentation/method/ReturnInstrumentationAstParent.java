package tech.harmonysoft.oss.traute.javac.instrumentation.method;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * <p>
 *     Wraps underlying {@link Tree AST element(s)} and provides an API to apply {@code method instrumentation} on it.
 * </p>
 * <p>
 *     The rationale is that we generally have two {@code AST} parent types:
 *     <ul>
 *       <li>
 *           code block like below
 *           <pre>
 *               if (something) {
 *                   return result; <- instrumentation should be applied to the instruction inside a code block ({})
 *               }
 *           </pre>
 *       </li>
 *       <li>
 *           {@code 'case/default'} group:
 *           <pre>
 *               switch (var) {
 *                   case 1:
 *                     // do something
 *                     return result; <- instrumentation should be applied to the instruction inside a {@code 'case'}
 *                   default:
 *                     return defaultValue; <- an instruction inside a {@code 'default'}
 *               }
 *           </pre>
 *       </li>
 *     </ul>
 * </p>
 */
public interface ReturnInstrumentationAstParent {

    /**
     * @return  statements in the current {@code AST} parent
     */
    @NotNull
    List<JCTree.JCStatement> getStatements();

    /**
     * Replaces all {@link #getStatements() current statements} by the given statements.
     *
     * @param statements statements to use for the underlying {@code AST} element
     */
    void setStatements(@NotNull List<JCTree.JCStatement> statements);
}
