import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.java.TargetJvmVersion

plugins {
    kotlin("jvm") version "2.3.0"
    application
}

kotlin { jvmToolchain(17) }

val fixtureVersion = requireNotNull(System.getenv("LSP4K_FIXTURE_VERSION"))
val form = requireNotNull(System.getenv("LSP4K_FIXTURE_FORM"))
require(form == "root" || form == "jvm")
val modules = listOf("lsp4k-protocol", "lsp4k-client", "lsp4k-server")
val java16Probes =
    modules.flatMap { listOf(it, "$it-jvm") }.associateWith { module ->
        configurations.create("java16-$module") {
            isCanBeConsumed = false
            isCanBeResolved = true
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
                attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
                attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 16)
            }
        }
    }

dependencies {
    listOf("lsp4k-client", "lsp4k-server").forEach { library ->
        val module = if (form == "jvm") "$library-jvm" else library
        implementation("io.lsp4k:$module:$fixtureVersion")
    }
    java16Probes.forEach { (module, configuration) ->
        add(configuration.name, "io.lsp4k:$module:$fixtureVersion")
    }
}

application { mainClass = "MainKt" }

tasks.register("verifyJava16Rejected") {
    doLast {
        java16Probes.forEach { (module, configuration) ->
            val failure =
                runCatching { configuration.resolve() }.exceptionOrNull()
                    ?: error("A Java 16 consumer unexpectedly resolved $module")
            val details = failure.stackTraceToString()
            val requested = "io.lsp4k:$module:$fixtureVersion"
            val noMatchingVariant =
                "No matching variant of $requested" in details && "Java 16" in details && "Java 17" in details
            val jvmTooNew =
                "JVM runtime version 16" in details &&
                    "'$requested' is only compatible with JVM runtime version 17" in details
            check(noMatchingVariant || jvmTooNew) { "Java 16 probe failed for an unrelated reason: $details" }
        }
    }
}
