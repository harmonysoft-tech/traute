package tech.harmonysoft.oss.traute.test.api.model;

import org.jetbrains.annotations.NotNull;

public interface ClassFile {

    @NotNull String getName();

    @NotNull byte[] getBinaries();
}
