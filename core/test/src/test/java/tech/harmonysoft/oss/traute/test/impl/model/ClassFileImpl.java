package tech.harmonysoft.oss.traute.test.impl.model;

import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.traute.test.api.model.ClassFile;

public class ClassFileImpl implements ClassFile {

    @NotNull private final String name;
    @NotNull private final byte[] binaries;

    public ClassFileImpl(@NotNull String name, @NotNull byte[] binaries) {
        this.name = name;
        this.binaries = binaries;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public byte[] getBinaries() {
        return binaries;
    }
}
