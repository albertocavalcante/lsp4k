plugins {
    alias(libs.plugins.detekt)
    alias(libs.plugins.spotless)
}

allprojects {
    group = property("GROUP").toString()
    version = property("VERSION_NAME").toString()
}

// Detekt configuration
detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(files("$rootDir/tools/lint/detekt.yml"))
    source.setFrom(files(subprojects.map { "${it.projectDir}/src" }))
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
