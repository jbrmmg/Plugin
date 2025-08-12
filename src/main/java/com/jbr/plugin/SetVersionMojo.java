package com.jbr.plugin;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

@Mojo(name = "set-version", defaultPhase = LifecyclePhase.INITIALIZE, threadSafe = true)
public class SetVersionMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.file}", readonly = true)
    private File pomFile;

    @Override
    public void execute() throws MojoExecutionException {
        // Get the major and minor version numbers.
        String majorVersion = System.getenv("major.version");
        String minorVersion = System.getenv("minor.version");

        getLog().info("Major version: " + majorVersion);
        getLog().info("Minor version: " + minorVersion);

        // Get the team city properties file.
        String teamcityPropsFile = System.getenv("TEAMCITY_BUILD_PROPERTIES_FILE");
        Properties teamcityProps = new Properties();

        if (teamcityPropsFile != null) {
            try (InputStream in = Files.newInputStream(Paths.get(teamcityPropsFile))) {
                teamcityProps.load(in);
            } catch (IOException e) {
                getLog().error("Failed to determine team city properties");
            }
        } else {
            getLog().info("TEAMCITY_BUILD_PROPERTIES_FILE not set!");
        }

        boolean isPersonal = "true".equalsIgnoreCase(teamcityProps.getProperty("build.is.personal"));
        String branch = System.getenv("teamcity.build.branch");
        String buildNumber = System.getenv("build.number");

        // Determine the build version number
        String newVersion;

        if(isPersonal) {
            getLog().info("Personal build - 1.0-Beta-SNAPSHOT");
            newVersion = "1.0-Beta-SNAPSHOT";
        } else if("refs/heads/Development".equals(branch)) {
            getLog().info("Development build - " + buildNumber + "-SNAPSHOT");
            newVersion = buildNumber + "-SNAPSHOT";
        } else if("refs/heads/Release".equals(branch)) {
            getLog().info("Release build- " + buildNumber);
            newVersion = buildNumber;
        } else {
            getLog().info("Feature or Bug Fix branch");
            newVersion = buildNumber + "-" + branch + "-SNAPSHOT";
        }

        try (FileReader reader = new FileReader(pomFile)) {
            MavenXpp3Reader xpp3Reader = new MavenXpp3Reader();
            Model model = xpp3Reader.read(reader);
            model.setVersion(newVersion);

            try (FileWriter writer = new FileWriter(pomFile)) {
                new MavenXpp3Writer().write(writer, model);
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to update POM version", e);
        }
    }
}
