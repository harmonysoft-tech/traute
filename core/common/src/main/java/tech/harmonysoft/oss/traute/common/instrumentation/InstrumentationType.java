package tech.harmonysoft.oss.traute.common.instrumentation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumerates instrumentation types supported by the current plugin.
 */
public enum InstrumentationType {

    /**
     * Before:
     * <pre>
     *     public void test(&#064;NotNull String s) {
     *         // body
     *     }
     * </pre>
     * After:
     * <pre>
     *     public void test(&#064;NotNull String s) {
     *         if (s == null) {
     *             throw new NullPointerException("[problem details]");
     *         }
     *         // body
     *     }
     * </pre>
     */
    METHOD_PARAMETER("parameter"),

    /**
     * Before:
     * <pre>
     *     &#064;NotNull
     *     public String compute() {
     *         return doCompute();
     *     }
     * </pre>
     * After:
     * <pre>
     *     &#064;NotNull
     *     public String compute() {
     *         String tmpVar = doCompute();
     *         if (tmpVar == null) {
     *             throw new NullPointerException("[problem details]");
     *         }
     *         return tmpVar;
     *     }
     * </pre>
     */
    METHOD_RETURN("return");

    private static Map<String, InstrumentationType> BY_SHORT_NAME = new HashMap<>();
    static {
        for (InstrumentationType type : values()) {
            BY_SHORT_NAME.put(type.getShortName(), type);
        }
    }

    @NotNull private final String shortName;

    InstrumentationType(@NotNull String shortName) {
        this.shortName = shortName;
    }

    @Nullable
    public static InstrumentationType byShortName(@NotNull String shortName) {
        return BY_SHORT_NAME.get(shortName);
    }

    @NotNull
    public String getShortName() {
        return shortName;
    }
}
