package tech.harmonysoft.oss.traute.gradle.test.impl

import org.jetbrains.annotations.NotNull
import tech.harmonysoft.oss.traute.common.settings.TrautePluginSettings

class GradlePredefinedProjectCompiler extends GradleTestCompiler {

    String resourceRoot

    @NotNull
    @Override
    protected File createExternalSystemConfig(@NotNull File projectRootDir, @NotNull TrautePluginSettings settings)
            throws IOException
    {
        checkState()
        def file = new File(projectRootDir, 'build.gradle')
        def text = getClass().getClassLoader().getResourceAsStream("$resourceRoot/build.gradle").text
        text = text.replace('TRAUTE_SPEC', getTrauteJavacDependencySpec())
        file.append(text)
        return file
    }

    private void checkState() {
        if (!resourceRoot) {
            throw new IllegalStateException("Expected that resource root is already provided")
        }
    }
}
