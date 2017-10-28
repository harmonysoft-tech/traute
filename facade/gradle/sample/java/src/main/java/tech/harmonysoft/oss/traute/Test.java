package tech.harmonysoft.oss.traute;

import org.jetbrains.annotations.NotNull;

public class Test {

    public static void main(String[] args) {
        System.out.println(getInt());
    }

    @NotNull
    private static Integer getInt() {
        return System.currentTimeMillis() > 1 ? null : 42;
    }
}
