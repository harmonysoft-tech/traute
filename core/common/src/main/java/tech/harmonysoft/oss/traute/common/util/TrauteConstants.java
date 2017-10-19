package tech.harmonysoft.oss.traute.common.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;

public class TrauteConstants {

    public static final Set<String> PRIMITIVE_TYPES = Collections.unmodifiableSet(new HashSet<>(asList(
            "byte", "short", "char", "int", "long", "float", "double"
    )));

    public static final Set<String> METHOD_RETURN_TYPES_TO_SKIP;
    static {
        Set<String> set = new HashSet<>(PRIMITIVE_TYPES);
        set.add("void");
        set.add("Void");
        METHOD_RETURN_TYPES_TO_SKIP = set;
    }

    private TrauteConstants() {
    }
}
