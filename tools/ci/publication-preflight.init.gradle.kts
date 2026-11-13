import org.gradle.api.publish.PublishingExtension

allprojects {
    plugins.withId("maven-publish") {
        extensions.configure<PublishingExtension> {
            repositories.maven {
                name = "Preflight"
                url = uri(requireNotNull(System.getenv("LSP4K_PREFLIGHT_REPOSITORY")))
            }
        }
    }
}
