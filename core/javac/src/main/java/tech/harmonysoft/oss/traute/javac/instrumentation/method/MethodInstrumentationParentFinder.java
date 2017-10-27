package tech.harmonysoft.oss.traute.javac.instrumentation.method;

import com.sun.source.tree.*;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * <p>
 *     Tries to find a {@link ReturnInstrumentationAstParent} which should be used as a generated
 *     {@code null}-check holder.
 * </p>
 * <p>
 *     The trick here is that there is a possible case that we need to instrument a {@code 'return'} expression
 *     which is not contained in a code block, like below:
 * </p>
 * <pre>
 * if (something)
 *     return result;
 * </pre>
 * <p>
 *     Here we need to insert a code block instead of the {@code 'return'} expression and provide it back.
 * </p>
 */
public class MethodInstrumentationParentFinder extends SimpleTreeVisitor<ReturnInstrumentationAstParent, Void> {

    @NotNull private final ReturnToInstrumentInfo info;

    public MethodInstrumentationParentFinder(@NotNull ReturnToInstrumentInfo info) {
        this.info = info;
    }

    @Override
    public ReturnInstrumentationAstParent visitBlock(BlockTree node, Void aVoid) {
        if (node instanceof JCTree.JCBlock) {
            return new BlockReturnInstrumentationAstParent((JCTree.JCBlock) node);
        } else {
            report(JCTree.JCBlock.class, node);
            return null;
        }
    }

    @Override
    public ReturnInstrumentationAstParent visitIf(IfTree node, Void aVoid) {
        if (!(node instanceof JCTree.JCIf)) {
            report(JCTree.JCIf.class, node);
            return null;
        }
        JCTree.JCIf jcIf = (JCTree.JCIf) node;
        JCTree.JCBlock block = null;
        if (jcIf.thenpart == info.getReturnExpression()) {
            jcIf.thenpart = (block = buildBlock());
        } else if (jcIf.elsepart == info.getReturnExpression()) {
            jcIf.elsepart = (block = buildBlock());
        }
        return block == null ? null : new BlockReturnInstrumentationAstParent(block);
    }

    @Override
    public ReturnInstrumentationAstParent visitForLoop(ForLoopTree node, Void aVoid) {
        if (node instanceof JCTree.JCForLoop) {
            JCTree.JCForLoop loop = (JCTree.JCForLoop) node;
            if (loop.body == info.getReturnExpression()) {
                JCTree.JCBlock block = buildBlock();
                loop.body = block;
                return new BlockReturnInstrumentationAstParent(block);
            }
        } else {
            report(JCTree.JCForLoop.class, node);
        }
        return null;
    }

    @Override
    public ReturnInstrumentationAstParent visitEnhancedForLoop(EnhancedForLoopTree node, Void aVoid) {
        if (node instanceof JCTree.JCEnhancedForLoop) {
            JCTree.JCEnhancedForLoop loop = (JCTree.JCEnhancedForLoop) node;
            if (loop.body == info.getReturnExpression()) {
                JCTree.JCBlock block = buildBlock();
                loop.body = block;
                return new BlockReturnInstrumentationAstParent(block);
            }
        } else {
            report(JCTree.JCEnhancedForLoop.class, node);
        }
        return null;
    }

    @Override
    public ReturnInstrumentationAstParent visitWhileLoop(WhileLoopTree node, Void aVoid) {
        if (node instanceof JCTree.JCWhileLoop) {
            JCTree.JCWhileLoop loop = (JCTree.JCWhileLoop) node;
            if (loop.body == info.getReturnExpression()) {
                JCTree.JCBlock block = buildBlock();
                loop.body = block;
                return new BlockReturnInstrumentationAstParent(block);
            }
        } else {
            report(JCTree.JCWhileLoop.class, node);
        }
        return null;
    }

    @Override
    public ReturnInstrumentationAstParent visitDoWhileLoop(DoWhileLoopTree node, Void aVoid) {
        if (node instanceof JCTree.JCDoWhileLoop) {
            JCTree.JCDoWhileLoop loop = (JCTree.JCDoWhileLoop) node;
            if (loop.body == info.getReturnExpression()) {
                JCTree.JCBlock block = buildBlock();
                loop.body = block;
                return new BlockReturnInstrumentationAstParent(block);
            }
        } else {
            report(JCTree.JCDoWhileLoop.class, node);
        }
        return null;
    }

    @Override
    public ReturnInstrumentationAstParent visitCase(CaseTree node, Void aVoid) {
        if (node instanceof JCTree.JCCase) {
            return new CaseReturnInstrumentationAstParent((JCTree.JCCase)node);
        } else {
            report(JCTree.JCCase.class, node);
            return null;
        }
    }

    @NotNull
    private JCTree.JCBlock buildBlock() {
        return info.getContext().getAstFactory().Block(0, List.nil());
    }

    private void report(@NotNull Class<?> expectedClass, @NotNull Object actual) {
        info.getContext().getLogger().reportDetails(
                String.format("find an AST element of type %s but got %s",
                              expectedClass.getName(), actual.getClass().getName())
        );
    }
}
