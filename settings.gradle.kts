pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "lsp4k"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
    repositories {
        if (providers.gradleProperty("lsp4k.useMavenLocal").orNull?.toBooleanStrict() == true) {
            mavenLocal()
        }
        mavenCentral()
        maven {
            name = "Jsonrpc4kGitHubPackages"
            url = uri("https://maven.pkg.github.com/albertocavalcante/jsonrpc4k")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR") ?: ""
                password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN") ?: ""
            }
            content {
                includeGroup("io.github.albertocavalcante")
            }
        }
    }
}

val localJsonrpc4kBuild =
    providers.gradleProperty("lsp4k.jsonrpc4kBuild").orNull?.let(::file)
        ?: file("../jsonrpc4k")
val useLocalJsonrpc4k =
    providers.gradleProperty("lsp4k.useLocalJsonrpc4k").orNull?.toBooleanStrict()
        ?: localJsonrpc4kBuild.isDirectory

if (useLocalJsonrpc4k) {
    check(localJsonrpc4kBuild.isDirectory) {
        "Local jsonrpc4k build does not exist: ${localJsonrpc4kBuild.absolutePath}"
    }

    includeBuild(localJsonrpc4kBuild) {
        dependencySubstitution {
            substitute(module("io.github.albertocavalcante:jsonrpc4k-core")).using(project(":jsonrpc4k-core"))
            substitute(module("io.github.albertocavalcante:jsonrpc4k-transport")).using(project(":jsonrpc4k-transport"))
        }
    }
}

include(":lsp4k-protocol")
include(":lsp4k-server")
include(":lsp4k-client")
include(":example")
