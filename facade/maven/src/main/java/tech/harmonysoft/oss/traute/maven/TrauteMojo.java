package tech.harmonysoft.oss.traute.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "traute", defaultPhase = LifecyclePhase.INITIALIZE)
public class TrauteMojo extends AbstractMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // TODO den remove
        getLog().info( "xxxxxxxxxxxxxxxxxxxx Hello, world." );
    }
}
