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
     *     Compiler's option name to use for specifying custom {@code @Nullable} annotations to use
     *     ({@value #SEPARATOR}-separated).
     * </p>
     * <p>
     *     This is not mandatory setting as default annotations are used otherwise. Only given annotations are
     *     checked if this argument is specified.
     * </p>
     * <p>
     *     Example: consider a situation when given parameter's value is
     *     {@code 'org.eclipse.jdt.annotation.Nullable:android.support.annotation.Nullable'} (eclipse and android
     *     annotations, defined as
     *     {@code -Atraute.annotations.nullable=org.eclipse.jdt.annotation.Nullable:android.support.annotation.Nullable}
     *     in the {@code javac} command line). That means that a method which parameter is marked by, say
     *     {@code org.jetbrains.annotations.Nullable} won't be considered as {@code nullable} by the plugin.
     * </p>
     */
    public static final String OPTION_ANNOTATIONS_NULLABLE = "traute.annotations.nullable";

    /**
     * <p>
     *     Prefix for compiler's option prefix for specifying a set of {@code not null by default} annotations
     *     (separated by {@value #SEPARATOR}). Resulting option is constructed from the current prefix
     *     and {@link InstrumentationType#getShortName()}.
     * </p>
     * <p>
     *     E.g. {@code -Atraute.not.null.by.default.parameter=javax.annotation.ParametersAreNonnullByDefault}
     *     instructs the plugin to consider all method parameters not marked as {@link #OPTION_ANNOTATIONS_NULLABLE}
     *     to be treated as {@code not-null} for a package where {@code package-info.java} is marked by the
     *     {@code ParametersAreNonnullByDefault} annotation.
     * </p>
     */
    public static final String OPTION_PREFIX_ANNOTATIONS_NOT_NULL_BY_DEFAULT = "traute.not.null.by.default.";

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
     * <p>
     *     Prefix for compiler's option prefix for specifying an exception to throw on failed
     *     {@code null}-check. Resulting option is constructed from the current prefix and
     *     {@link InstrumentationType#getShortName()}.
     * </p>
     * <p>
     *     E.g. {@code -Atraute.exception.parameter=IllegalArgumentException} instructs the plugin
     *     to generated a parameter check where an {@link IllegalArgumentException} is thrown in case
     *     of failure.
     * </p>
     */
    public static final String OPTION_PREFIX_EXCEPTION_TO_THROW = "traute.exception.";

    /**
     * <p>
     *     Prefix for compiler's option prefix for specifying a text to use in an exception thrown from
     *     a failed {@code null}-check. Resulting option is constructed from the current prefix and
     *     {@link InstrumentationType#getShortName()}.
     * </p>
     * <p>
     *     E.g. {@code "-Atraute.failure.text.parameter=${PARAMETER_NAME} must not be null"} instructs the plugin
     *     to generated a check for a parameter named, say, {@code 'arg'} with exception text
     *     {@code "arg must not be null"}.
     * </p>
     */
    public static final String OPTION_PREFIX_EXCEPTION_TEXT = "traute.failure.text.";

    /**
     * This text is replaced by the actual parameter name in the
     * {@link InstrumentationType#METHOD_PARAMETER parametere check}.
     */
    public static final String VARIABLE_PARAMETER_NAME = "PARAMETER_NAME";

    /**
     * This function capitalizes the variable inside it, e.g. {@code ${capitalize(PARAMETER_NAME)}} for
     * parameter {@code 'arg'} produced {@code 'Arg'}.
     */
    public static final String FUNCTION_CAPITALIZE = "capitalize";

    /**
     * Separator to use for composite properties, e.g. when a user want to specify more than one
     * {@code NonNull} annotation.
     */
    public static final String SEPARATOR = ":";

    public static final String PACKAGE_INFO = "package-info";

    static {
        Set<String> set = new HashSet<>(PRIMITIVE_TYPES);
        set.add("void");
        set.add("Void");
        METHOD_RETURN_TYPES_TO_SKIP = set;
    }

    private TrauteConstants() {
    }
}
