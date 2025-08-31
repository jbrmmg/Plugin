package com.jbr.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.*;

@Mojo(name = "calc-release", defaultPhase = LifecyclePhase.INITIALIZE, threadSafe = true)
public class CalcRelease extends AbstractMojo {
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {
        String currentVersion = project.getVersion();
        if (!currentVersion.endsWith("-SNAPSHOT")) {
            throw new MojoExecutionException("Project version must end with -SNAPSHOT: " + currentVersion);
        }

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
            ProcessBuilder pb = new ProcessBuilder(
                    "mvn", "release:prepare",
                    "-B",
                    "-DreleaseVersion=" + releaseVersion,
                    "-DdevelopmentVersion=" + nextDevVersion,
                    "-Dtag=" + tag
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    getLog().info(line);
                }
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new MojoExecutionException("release:prepare failed with exit code " + exitCode);
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to run release:prepare", e);
        }
    }
}
