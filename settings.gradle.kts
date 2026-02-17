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
        mavenCentral()
    }
}

includeBuild("../jsonrpc4k") {
    dependencySubstitution {
        substitute(module("io.jsonrpc4k:jsonrpc4k-core")).using(project(":jsonrpc4k-core"))
        substitute(module("io.jsonrpc4k:jsonrpc4k-transport")).using(project(":jsonrpc4k-transport"))
    }
}

include(":lsp4k-protocol")
include(":lsp4k-server")
include(":lsp4k-client")
include(":example")
