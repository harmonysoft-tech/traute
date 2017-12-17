package tech.harmonysoft.oss.traute.javac.text;

import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.tree.JCTree;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import tech.harmonysoft.oss.traute.javac.common.CompilationUnitProcessingContext;
import tech.harmonysoft.oss.traute.javac.instrumentation.parameter.ParameterToInstrumentInfo;

import javax.lang.model.element.Name;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static tech.harmonysoft.oss.traute.common.instrumentation.InstrumentationType.METHOD_PARAMETER;
import static tech.harmonysoft.oss.traute.common.util.TrauteConstants.FUNCTION_CAPITALIZE;
import static tech.harmonysoft.oss.traute.common.util.TrauteConstants.VARIABLE_PARAMETER_NAME;

@SuppressWarnings("unchecked")
class ExceptionTextGeneratorFactoryTest {

    private final ExceptionTextGeneratorFactory factory = new ExceptionTextGeneratorFactory();

    @Test
    public void staticText() {
        doTestParameter("xxx", "a", "xxx");
    }

    @Test
    public void varNameOnly() {
        doTestParameter(String.format("${%s}", VARIABLE_PARAMETER_NAME), "xyz", "xyz");
    }

    @Test
    public void varNameAtBeginning() {
        doTestParameter(String.format("abc ${%s}", VARIABLE_PARAMETER_NAME), "xyz", "abc xyz");
    }

    @Test
    public void varNameAtEnd() {
        doTestParameter(String.format("${%s} abc", VARIABLE_PARAMETER_NAME), "xyz", "xyz abc");
    }

    @Test
    public void varNameInTheMiddle() {
        doTestParameter(String.format("abc ${%s} xyz", VARIABLE_PARAMETER_NAME), "arg", "abc arg xyz");
    }

    @Test
    public void capitalize_manyLetters() {
        doTestParameter(String.format("abc ${%s(%s)} xyz", FUNCTION_CAPITALIZE, VARIABLE_PARAMETER_NAME),
                        "arg",
                        "abc Arg xyz");
    }

    @Test
    public void capitalize_singleLetter() {
        doTestParameter(String.format("abc ${%s(%s)} xyz", FUNCTION_CAPITALIZE, VARIABLE_PARAMETER_NAME),
                        "a",
                        "abc A xyz");
    }

    private void doTestParameter(@NotNull String pattern, @NotNull String variableName, @NotNull String expected) {
        Optional<ExceptionTextGenerator<?>> o = factory.build(METHOD_PARAMETER, pattern, null);
        assertTrue(o.isPresent());
        ExceptionTextGenerator generator = o.get();
        assertEquals(expected, generator.generate(getInfo(variableName)));
    }

    @NotNull
    private static ParameterToInstrumentInfo getInfo(@NotNull String argumentName) {
        VariableTree variableMock = mock(VariableTree.class);
        Name nameMock = mock(Name.class);
        when(variableMock.getName()).thenReturn(nameMock);
        when(nameMock.toString()).thenReturn(argumentName);
        return new ParameterToInstrumentInfo(mock(CompilationUnitProcessingContext.class),
                                             NotNull.class.getName(),
                                             null,
                                             variableMock,
                                             mock(JCTree.JCBlock.class),
                                             "test",
                                             0,
                                             1,
                                             false);
    }
}