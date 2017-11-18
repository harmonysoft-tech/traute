package tech.harmonysoft.oss.traute.common.util;

import tech.harmonysoft.oss.traute.common.instrumentation.InstrumentationType;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;

public class TrauteConstants {

    public static final String PLUGIN_NAME = "Traute";

    public static final Set<String> PRIMITIVE_TYPES = Collections.unmodifiableSet(new HashSet<>(asList(
            "byte", "short", "char", "int", "long", "float", "double"
    )));

    public static final Set<String> METHOD_RETURN_TYPES_TO_SKIP;
    /**
     * <p>
     *     Compiler's option name to use for specifying custom {@code @NotNull} annotations to use
     *     ({@value #SEPARATOR}-separated).
     * </p>
     * <p>
     *     This is not mandatory setting as default annotations are used otherwise. Only given annotations are
     *     checked if this argument is specified.
     * </p>
     * <p>
     *     Example: consider a situation when given parameter's value is
     *     {@code 'org.eclipse.jdt.annotation.NonNull:android.support.annotation.NonNull'} (eclipse and android
     *     annotations, defined as
     *     {@code -Atraute.annotations.not.null=org.eclipse.jdt.annotation.NonNull:android.support.annotation.NonNull}
     *     in the {@code javac} command line). That means that a method which parameter is marked by, say
     *     {@code org.jetbrains.annotations.NotNull} won't trigger {@code null}-check generation by the plugin.
     * </p>
     */
    public static final String OPTION_ANNOTATIONS_NOT_NULL = "traute.annotations.not.null";
    /**
     * <p>
     *     Compiler's option name to use for specifying if the plugin should produce verbose output
     *     for its processing
     * </p>
     * <p>
     *     Verbose output is produced if this option's value is {@code 'true'}, e.g.
     *     {@code '-Atraute.log.verbose=true'} in the {@code javac} command line. Any other value (or now value)
     *     mean that no verbose output should be provided.
     * </p>
     */
    public static final String OPTION_LOG_VERBOSE = "traute.log.verbose";
    /**
     * <p>Compiler's option name for specifying a path to a file to store plugin's logs.</p>
     * <p>The logs are printed to the standard compiler's output by default.</p>
     */
    public static final String OPTION_LOG_FILE = "traute.log.file";
    /**
     * Compiler's option name to use for specifying instrumentation types to use
     *
     * @see InstrumentationType
     * @see InstrumentationType#getShortName()
     */
    public static final String OPTION_INSTRUMENTATIONS_TO_USE = "traute.instrumentations";

    /**
     * Separator to use for composite properties, e.g. when a user want to specify more than one
     * {@code NonNull} annotation.
     */
    public static final String SEPARATOR = ":";

    static {
        Set<String> set = new HashSet<>(PRIMITIVE_TYPES);
        set.add("void");
        set.add("Void");
        METHOD_RETURN_TYPES_TO_SKIP = set;
    }

    private TrauteConstants() {
    }
}
