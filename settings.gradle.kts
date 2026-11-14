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

val jsonrpcRepository = providers.gradleProperty("jsonrpc4k.repository").orNull
require(jsonrpcRepository == null || jsonrpcRepository == "github") {
    "jsonrpc4k.repository must be github when set"
}

dependencyResolutionManagement {
    repositories {
        if (providers.gradleProperty("lsp4k.useMavenLocal").orNull?.toBooleanStrict() == true) {
            mavenLocal()
        }
        mavenCentral()
        val configureJsonrpcRepository: MavenArtifactRepository.() -> Unit = {
            name = "Jsonrpc4kGitHubPackages"
            url = uri("https://maven.pkg.github.com/albertocavalcante/jsonrpc4k")
            val registryUser = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
            val registryToken = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
            if (jsonrpcRepository != "github" || !registryToken.isNullOrEmpty()) {
                if (jsonrpcRepository == "github") {
                    require(!registryUser.isNullOrEmpty()) { "A GitHub Packages username is required with a token" }
                }
                credentials {
                    username = registryUser ?: ""
                    password = registryToken ?: ""
                }
            }
        }
        if (jsonrpcRepository == "github") {
            exclusiveContent {
                forRepository { maven { configureJsonrpcRepository() } }
                filter { includeModuleByRegex("io\\.github\\.albertocavalcante", "jsonrpc4k-(core|transport)(-.+)?") }
            }
        } else {
            maven {
                configureJsonrpcRepository()
                content { includeGroup("io.github.albertocavalcante") }
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

/**
 * Compares the sibling jsonrpc4k checkout's git HEAD with the revision pinned as `lsp4k.jsonrpc4kPin` in
 * gradle.properties. A mismatch (or an unreadable HEAD) fails the build when running in CI and warns locally.
 * `-Plsp4k.allowJsonrpc4kPinMismatch=true` opts out, for local work against another jsonrpc4k revision.
 */
fun verifyJsonrpc4kPin(jsonrpcDirectory: File) {
    val pin = providers.gradleProperty("lsp4k.jsonrpc4kPin").orNull
    require(pin != null && Regex("[0-9a-f]{40}").matches(pin)) {
        "lsp4k.jsonrpc4kPin in gradle.properties must be a full 40-character lowercase commit SHA, got '$pin'"
    }
    if (providers.gradleProperty("lsp4k.allowJsonrpc4kPinMismatch").orNull?.toBooleanStrict() == true) return

    val revision = providers.exec {
        commandLine("git", "-C", jsonrpcDirectory.absolutePath, "rev-parse", "HEAD")
        isIgnoreExitValue = true
    }
    val head =
        runCatching {
            if (revision.result.get().exitValue == 0) revision.standardOutput.asText.get().trim() else null
        }.getOrNull()
    if (head == pin) return

    val found = head ?: "unknown (not a readable git checkout)"
    val message =
        "The jsonrpc4k checkout at ${jsonrpcDirectory.absolutePath} is at $found, but lsp4k pins $pin " +
            "(lsp4k.jsonrpc4kPin in gradle.properties). Check out the pinned revision, or pass " +
            "-Plsp4k.allowJsonrpc4kPinMismatch=true to build against another revision."
    check(System.getenv("CI") != "true") { message }
    Logging.getLogger("lsp4k").warn("WARNING: $message")
}

if (useLocalJsonrpc4k) {
    check(localJsonrpc4kBuild.isDirectory) {
        "Local jsonrpc4k build does not exist: ${localJsonrpc4kBuild.absolutePath}"
    }

    verifyJsonrpc4kPin(localJsonrpc4kBuild)

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
