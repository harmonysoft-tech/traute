package tech.harmonysoft.oss.traute.test.util;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class TestDurationPrinter implements BeforeTestExecutionCallback, AfterTestExecutionCallback {

    private static final ThreadLocal<Long> START_TIME_MS = ThreadLocal.withInitial(System::currentTimeMillis);

    @Override
    public void beforeTestExecution(ExtensionContext extensionContext) throws Exception {
        START_TIME_MS.set(System.currentTimeMillis());
    }

    @Override
    public void afterTestExecution(ExtensionContext extensionContext) throws Exception {
        System.out.println(String.format(
                "Executed %s.%s in %d ms",
                extensionContext.getRequiredTestClass().getSimpleName(),
                extensionContext.getDisplayName(),
                System.currentTimeMillis() - START_TIME_MS.get()
        ));
    }
}
