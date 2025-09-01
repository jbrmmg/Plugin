package com.jbr.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

@Mojo(name = "calc-release", defaultPhase = LifecyclePhase.INITIALIZE, threadSafe = true)
public class CalcRelease extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    @Component
    private BuildPluginManager pluginManager;

    @Override
    public void execute() throws MojoExecutionException {
        String currentVersion = project.getVersion();
        if (!currentVersion.endsWith("-SNAPSHOT")) {
            throw new MojoExecutionException("Project version must end with -SNAPSHOT: " + currentVersion);
        }

        // Compute release and next dev versions
        String releaseVersion = currentVersion.replace("-SNAPSHOT", "");
        String[] parts = releaseVersion.split("\\.");
        int patch = Integer.parseInt(parts[2]);
        String nextDevVersion = parts[0] + "." + parts[1] + "." + (patch + 1) + "-SNAPSHOT";
        String tag = "v" + releaseVersion;

        getLog().info("Preparing release:");
        getLog().info("  releaseVersion = " + releaseVersion);
        getLog().info("  developmentVersion = " + nextDevVersion);
        getLog().info("  tag = " + tag);

        try {
            session.getSystemProperties().setProperty("maven.batchMode","true");

            executeMojo(
                    plugin(
                            groupId("org.apache.maven.plugins"),
                            artifactId("maven-release-plugin"),
                            version("3.0.0-M1") // use your release plugin version
                    ),
                    goal("prepare"),
                    configuration(
                            element(name("releaseVersion"), releaseVersion),
                            element(name("developmentVersion"), nextDevVersion),
                            element(name("tag"), tag)
                    ),
                    executionEnvironment(project, session, pluginManager)
            );

            getLog().info("Release prepare completed successfully.");

            // Run release:perform
            executeMojo(
                    plugin(
                            groupId("org.apache.maven.plugins"),
                            artifactId("maven-release-plugin"),
                            version("3.0.0-M1")
                    ),
                    goal("perform"),
                    configuration(),
                    executionEnvironment(project, session, pluginManager)
            );

            getLog().info("Release perform completed successfully.");
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to execute release:prepare programmatically", e);
        }
    }
}
