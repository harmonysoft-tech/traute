package tech.harmonysoft.oss.traute.javac.util;

import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;
import org.jetbrains.annotations.NotNull;

import static com.sun.tools.javac.util.List.nil;

public class InstrumentationUtil {

    private InstrumentationUtil() {
    }

    /**
     * Builds an {@code AST 'if'} element which looks as below:
     * <pre>
     *     if ([given-variable-name] == null) {
     *         throw new [given-exception]([given-error-message]);
     *     }
     * </pre>
     *
     * @param factory           an {@code AST} factory to use
     * @param symbolsTable      a symbols table to use
     * @param variableName      a variable name to use
     * @param errorMessage      an error message to use
     * @param exceptionToThrow  an exception to throw in case of failed check
     * @return                  an {@code AST 'if'} for the parameters above
     */
    @NotNull
    public static JCTree.JCIf buildVarCheck(@NotNull TreeMaker factory,
                                            @NotNull Names symbolsTable,
                                            @NotNull String variableName,
                                            @NotNull String errorMessage,
                                            @NotNull String exceptionToThrow)
    {
        return factory.If(
                factory.Parens(
                        factory.Binary(
                                JCTree.Tag.EQ,
                                factory.Ident(
                                        symbolsTable.fromString(variableName)
                                ),
                                factory.Literal(TypeTag.BOT, null))
                ),
                factory.Block(0, List.of(
                        factory.Throw(
                                factory.NewClass(
                                        null,
                                        nil(),
                                        factory.Ident(
                                                symbolsTable.fromString(exceptionToThrow)
                                        ),
                                        List.of(factory.Literal(TypeTag.CLASS, errorMessage)),
                                        null
                                )
                        )
                )),
                null
        );
    }
}
