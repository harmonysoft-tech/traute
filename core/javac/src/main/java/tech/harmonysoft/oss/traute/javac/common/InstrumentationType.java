package tech.harmonysoft.oss.traute.javac.common;

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
    METHOD_PARAMETER,

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
    METHOD_RETURN
}
