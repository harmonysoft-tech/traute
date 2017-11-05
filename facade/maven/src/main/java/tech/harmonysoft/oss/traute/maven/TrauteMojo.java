package tech.harmonysoft.oss.traute.maven;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mojo(name = "traute", defaultPhase = LifecyclePhase.VALIDATE)
public class TrauteMojo extends AbstractMojo {

    private static final String KEY_PROJECT = "project";

    @SuppressWarnings("unchecked")
//    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        addTrauteDependency();
        propagateSettings();
    }

    @SuppressWarnings("unchecked")
    private void addTrauteDependency() {
        MavenProject project = getMavenProject();
        List<Dependency> newDependencies = new ArrayList<>(project.getDependencies());
        Dependency trauteDependency = new Dependency();
        trauteDependency.setGroupId("tech.harmonysoft");
        trauteDependency.setArtifactId("traute-javac");
        trauteDependency.setVersion("1.0.5");
        trauteDependency.setScope("compile");
        trauteDependency.setType("jar");
        newDependencies.add(trauteDependency);
        project.setDependencies(newDependencies);
    }

    @NotNull
    private MavenProject getMavenProject() {
        Object p = getPluginContext().get(KEY_PROJECT);
        if (!(p instanceof MavenProject)) {
            throw new IllegalStateException(String.format(
                    "Expected to find a %s instance under the key '%s' in the plugin context but got %s for that. "
                    + "The context: %s", MavenProject.class.getName(), KEY_PROJECT, p, getPluginContext()));
        }
        return (MavenProject) p;
    }

    @SuppressWarnings("unchecked")
    private void propagateSettings() {
        Optional<Plugin> o = getMavenProject().getBuildPlugins()
                                    .stream()
                                    .filter(p -> p instanceof Plugin
                                                 && "maven-compiler-plugin".equals(((Plugin) p).getArtifactId()))
                                    .findFirst();
        if (!o.isPresent()) {
            // TODO den implement
            throw new IllegalStateException();
        }

        Xpp3Dom configuration = (Xpp3Dom) o.get().getConfiguration();
        Xpp3Dom source = configuration.getChild("source");// get parameter
        source.setValue("1.5");
    }
}
