package com.jbr.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import javax.inject.Inject;

import java.time.LocalDate;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

@Mojo(name = "release", defaultPhase = LifecyclePhase.INITIALIZE, threadSafe = true)
public class CalcRelease extends AbstractMojo {
    @Inject
    private MavenProject project;

    @Inject
    private MavenSession session;

    @Inject
    private BuildPluginManager pluginManager;

    @Override
    public void execute() throws MojoExecutionException {
        String currentVersion = project.getVersion();
        if (!currentVersion.endsWith("-SNAPSHOT")) {
            throw new MojoExecutionException("Project version must end with -SNAPSHOT: " + currentVersion);
        }

        // Get current year and month
        LocalDate now = LocalDate.now();
        String releaseVersion = getString(now, currentVersion);
        String nextDevVersion = releaseVersion + "-SNAPSHOT";
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
                            version("3.1.1")
                    ),
                    goal("prepare"),
                    configuration(
                            element(name("releaseVersion"), releaseVersion),
                            element(name("developmentVersion"), nextDevVersion),
                            element(name("tag"), tag),
                            element(name("arguments"),"-DskipTests -DskipITs -Dmaven.test.skip=true")
                    ),
                    executionEnvironment(project, session, pluginManager)
            );

            getLog().info("Release prepare completed successfully.");

            // Run release:perform
            executeMojo(
                    plugin(
                            groupId("org.apache.maven.plugins"),
                            artifactId("maven-release-plugin"),
                            version("3.1.1")
                    ),
                    goal("perform"),
                    configuration(
                            element(name("arguments"),"-DskipTests -DskipITs -Dmaven.test.skip=true")
                    ),
                    executionEnvironment(project, session, pluginManager)
            );

            getLog().info("Release perform completed successfully.");
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to execute release:prepare programmatically", e);
        }
    }

    private static String getString(LocalDate now, String currentVersion) {
        String year = String.valueOf(now.getYear()).substring(2); // last 2 digits
        String month = String.valueOf(now.getMonthValue());       // 1-12

        int buildNumber = 0;

        if (currentVersion != null && !currentVersion.isEmpty()) {
            String[] parts = currentVersion.replace("-SNAPSHOT", "").split("\\.");
            if (parts.length == 3) {
                String currentYear = parts[0];
                String currentMonth = parts[1];
                int currentBuild = Integer.parseInt(parts[2]);

                if (currentYear.equals(year) && currentMonth.equals(month)) {
                    // Same year + month → increment build number
                    buildNumber = currentBuild + 1;
                }
            }
        }

        return year + "." + month + "." + buildNumber;
    }
}
