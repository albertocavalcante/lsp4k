import org.gradle.api.publish.PublishingExtension
import org.gradle.api.tasks.testing.Test

settingsEvaluated {
    val fixtureRepositories = dependencyResolutionManagement.repositories
    fixtureRepositories.exclusiveContent {
        forRepository {
            fixtureRepositories.maven { url = uri(requireNotNull(System.getenv("LSP4K_FIXTURE_REPOSITORY"))) }
        }
        filter { includeGroup("io.github.albertocavalcante") }
    }
}

allprojects {
    if (System.getenv("LSP4K_VERIFY_JAVA17") == "true") {
        tasks.withType<Test>().configureEach {
            doFirst {
                val javaVersion = javaLauncher.get().metadata.languageVersion.asInt()
                check(javaVersion == 17) { "JVM tests must run on Java 17, got $javaVersion" }
                println("JVM tests use Java $javaVersion")
            }
        }
    }
    plugins.withId("maven-publish") {
        extensions.configure<PublishingExtension> {
            repositories.maven {
                name = "Fixture"
                url = uri(requireNotNull(System.getenv("LSP4K_FIXTURE_REPOSITORY")))
            }
        }
    }
}
