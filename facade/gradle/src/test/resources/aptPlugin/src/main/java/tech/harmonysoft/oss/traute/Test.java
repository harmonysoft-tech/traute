package tech.harmonysoft.oss.traute;

import com.google.auto.value.AutoValue;
import org.jetbrains.annotations.NotNull;

@AutoValue
public abstract class Test {
    public static Test create(@NotNull String name) {
        return new AutoValue_Test(name);
    }

    public abstract String name();

    public static void main(String[] args) {
        Test.create(null);
    }
}