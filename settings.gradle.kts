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

include(":lsp4k-protocol")
include(":lsp4k-jsonrpc")
include(":lsp4k-transport")
include(":lsp4k-server")
include(":lsp4k-client")
