plugins {
    alias(libs.plugins.detekt)
    alias(libs.plugins.spotless)
    alias(libs.plugins.kover)
}

allprojects {
    group = property("GROUP").toString()
    version = property("VERSION_NAME").toString()
}

/** Library subprojects (excludes the example module). */
val libraryProjects = subprojects.filter { it.name != "example" }

// Detekt configuration
detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(files("$rootDir/tools/lint/detekt.yml"))
    source.setFrom(files(libraryProjects.map { "${it.projectDir}/src" }))
}

dependencies {
    detektPlugins(libs.detekt.formatting)
}

// Spotless configuration
spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**", "**/buildSrc/**")
        ktlint()
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**", "**/buildSrc/**")
        ktlint()
    }
}

tasks.named("check") {
    dependsOn(tasks.named("detekt"))
}

// Apply Kover to library subprojects
libraryProjects.forEach { project ->
    project.apply(plugin = "org.jetbrains.kotlinx.kover")
}

// Merge coverage from library subprojects
dependencies {
    libraryProjects.forEach { subproject ->
        kover(subproject)
    }
}

// Kover configuration for code coverage
kover {
    reports {
        // Configure filters to exclude test classes and generated code
        filters {
            excludes {
                classes(
                    "*Test",
                    "*Test$*",
                    "*Tests",
                    "*Tests$*",
                    "*.test.*",
                    "*.BuildConfig",
                )
            }
        }

        // Total (merged) coverage report configuration
        total {
            // HTML report for humans
            html {
                onCheck = false
                htmlDir = layout.buildDirectory.dir("reports/kover/html")
            }

            // XML report for CI integration
            xml {
                onCheck = false
                xmlFile = layout.buildDirectory.file("reports/kover/report.xml")
            }

            // Verification rules
            verify {
                rule("Minimum line coverage") {
                    minBound(90)
                }
            }
        }
    }
}
