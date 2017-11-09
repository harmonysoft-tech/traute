package tech.harmonysoft.oss.traute.ant.sample;

import org.jetbrains.annotations.NotNull;

public class Test {
    public static void main(String[] args) {
        doTest(args.length > 0 ? Integer.valueOf(args[0]) : null);
    }

    private static void doTest(@NotNull Integer i) {
        System.out.println(i);
    }
}