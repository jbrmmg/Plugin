# auto-version-release Maven Plugin

A Maven plugin that automatically generates date-based version numbers and orchestrates the Maven release process.

## Overview

This plugin eliminates manual version management by calculating release versions using the current date. It then drives the full Maven release lifecycle (`prepare` and `perform`) automatically.

## Version Scheme

Versions follow the format `YY.M.N`:

| Component | Description                                       | Example |
|-----------|---------------------------------------------------|---------|
| `YY`      | Last 2 digits of the current year                 | `26`    |
| `M`       | Current month (no leading zero)                   | `3`     |
| `N`       | Build number, starting at `0`, incremented if the year and month match the current SNAPSHOT version | `0`     |

**Examples:**
- First release in March 2026: `26.3.0`
- Second release in the same month: `26.3.1`
- First release in April 2026: `26.4.0`

## Requirements

- Java 17+
- Maven 3.9.11+
- The project version must end with `-SNAPSHOT`

## Installation

Add the plugin to your project's `pom.xml`:

```xml
<plugin>
    <groupId>com.jbr.plugin</groupId>
    <artifactId>auto-version-release</artifactId>
    <version>1.10</version>
</plugin>
```

## Usage

Run the `release` goal from the command line:

```bash
mvn auto-release:release
```

The plugin will:

1. Validate that the current project version ends with `-SNAPSHOT`
2. Calculate the release version from the current date
3. Determine the next development version (`releaseVersion-SNAPSHOT`)
4. Create a Git tag in the format `v{releaseVersion}`
5. Execute `maven-release-plugin:prepare`
6. Execute `maven-release-plugin:perform`

Tests are skipped during the release process (`-DskipTests -DskipITs`).

## How Build Numbers Work

The build number (`N`) is derived from the current SNAPSHOT version:

- If the current SNAPSHOT version matches the current year and month (e.g. `26.3.0-SNAPSHOT` in March 2026), the build number is incremented by 1.
- Otherwise (different year or month), the build number resets to `0`.

This allows multiple releases within the same calendar month.

## Plugin Details

| Property        | Value                        |
|-----------------|------------------------------|
| Group ID        | `com.jbr.plugin`             |
| Artifact ID     | `auto-version-release`       |
| Goal            | `release`                    |
| Goal Prefix     | `auto-release`               |
| Lifecycle Phase | `initialize`                 |
| Thread Safe     | Yes                          |

## Source

[https://github.com/jbrmmg/plugin](https://github.com/jbrmmg/plugin)