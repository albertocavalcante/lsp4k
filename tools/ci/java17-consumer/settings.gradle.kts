pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        exclusiveContent {
            forRepository {
                maven { url = uri(requireNotNull(System.getenv("LSP4K_FIXTURE_REPOSITORY"))) }
            }
            filter {
                includeGroup("io.lsp4k")
                includeGroup("io.github.albertocavalcante")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "lsp4k-java17-consumer"
