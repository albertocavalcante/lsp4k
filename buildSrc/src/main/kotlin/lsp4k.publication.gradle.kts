plugins {
    `maven-publish`
}

val repositoryUrl = "https://github.com/albertocavalcante/lsp4k"

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set(artifactId)
            description.set("Kotlin Multiplatform Language Server Protocol library")
            url.set(repositoryUrl)
            licenses {
                license {
                    name.set("Apache License 2.0")
                    url.set("$repositoryUrl/blob/main/LICENSE")
                }
            }
            scm {
                url.set(repositoryUrl)
                connection.set("scm:git:$repositoryUrl.git")
                developerConnection.set("scm:git:ssh://git@github.com/albertocavalcante/lsp4k.git")
            }
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/albertocavalcante/lsp4k")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR") ?: ""
                password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN") ?: ""
            }
        }
    }
}
