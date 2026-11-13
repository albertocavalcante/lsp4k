import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository

// Every invocation checks actual publication versions; only genuine snapshots skip the release dependency gate.
gradle.projectsEvaluated {
    val root = rootProject
    if (root.name != "lsp4k") return@projectsEvaluated
    val publicationVersions =
        listOf("lsp4k-protocol", "lsp4k-client", "lsp4k-server")
            .flatMap { name ->
                val publications =
                    root
                        .project(":$name")
                        .extensions
                        .getByType<PublishingExtension>()
                        .publications
                        .withType<MavenPublication>()
                        .toList()
                check(publications.isNotEmpty()) { "No Maven publications found for $name" }
                publications.map { it.version }
            }.distinct()
    check(publicationVersions.size == 1) { "Mixed Maven publication versions are not supported: $publicationVersions" }
    val publicationVersion = publicationVersions.single()
    val taggedVersion =
        root.providers
            .environmentVariable("RELEASE_VERSION")
            .orNull
            .orEmpty()
    check(taggedVersion.isEmpty() || taggedVersion == publicationVersion) {
        "Tag version '$taggedVersion' does not match Maven publication version '$publicationVersion'"
    }
    val snapshotVersion = Regex("^[0-9]+(\\.[0-9]+){1,3}(-[A-Za-z0-9][A-Za-z0-9.-]*)?-SNAPSHOT$")
    if (snapshotVersion.matches(publicationVersion)) return@projectsEvaluated
    val catalog = root.extensions.getByType<VersionCatalogsExtension>().named("libs")
    val expected = catalog.findVersion("jsonrpc4k").orElseThrow().requiredVersion
    val exactVersion = Regex("^[0-9]+(\\.[0-9]+){1,3}(-[A-Za-z0-9][A-Za-z0-9.-]*)?$")
    check(exactVersion.matches(publicationVersion) && !publicationVersion.contains("snapshot", ignoreCase = true)) {
        "Publication requires an exact release or SNAPSHOT version, got '$publicationVersion'"
    }
    check(exactVersion.matches(expected) && !expected.contains("snapshot", ignoreCase = true)) {
        "Release publication requires an exact non-SNAPSHOT jsonrpc4k catalog version, got '$expected'"
    }
    // A host-specific model must not publish a partial release as the complete SDK.
    val requiredPublications =
        setOf(
            "kotlinMultiplatform",
            "jvm",
            "js",
            "linuxX64",
            "linuxArm64",
            "mingwX64",
            "macosX64",
            "macosArm64",
        )
    listOf("lsp4k-protocol", "lsp4k-client", "lsp4k-server").forEach { name ->
        val publications =
            root
                .project(":$name")
                .extensions
                .getByType<PublishingExtension>()
                .publications
                .withType<MavenPublication>()
                .map { it.name }
                .toSet()
        val missing = requiredPublications - publications
        check(missing.isEmpty()) { "Release publication for $name is missing required publications: ${missing.sorted()}" }
    }
    val graphs =
        listOf("lsp4k-client", "lsp4k-server").map { name ->
            root
                .project(":$name")
                .configurations
                .getByName("jvmRuntimeClasspath")
                .incoming.resolutionResult.rootComponent
                .map { start ->
                    val seen = mutableSetOf<ComponentIdentifier>()
                    val queue = java.util.ArrayDeque<ResolvedComponentResult>()
                    val found = mutableListOf<String>()
                    queue.add(start)
                    while (queue.isNotEmpty()) {
                        val node = queue.removeFirst()
                        if (!seen.add(node.id)) continue
                        val id = node.id
                        if (id is ModuleComponentIdentifier && id.group == "io.github.albertocavalcante" &&
                            (
                                id.module == "jsonrpc4k-core" || id.module.startsWith("jsonrpc4k-core-") ||
                                    id.module == "jsonrpc4k-transport" || id.module.startsWith("jsonrpc4k-transport-")
                            )
                        ) {
                            found.add("${id.module}:${id.version}")
                        }
                        for (dependency in node.dependencies) {
                            if (dependency is ResolvedDependencyResult) {
                                val requested = dependency.requested
                                if (requested is ModuleComponentSelector && requested.group == "io.github.albertocavalcante" &&
                                    (
                                        requested.module == "jsonrpc4k-core" || requested.module.startsWith("jsonrpc4k-core-") ||
                                            requested.module == "jsonrpc4k-transport" || requested.module.startsWith("jsonrpc4k-transport-")
                                    )
                                ) {
                                    found.add("requested:${requested.module}:${requested.version}")
                                }
                                queue.add(dependency.selected)
                            } else {
                                throw GradleException("Release publication has an unresolved dependency: $dependency")
                            }
                        }
                    }
                    found.sorted()
                }
        }
    val guard =
        root.tasks.register("verifyReleaseJsonrpcDependencies") {
            inputs.property("expectedJsonrpcVersion", expected)
            graphs.forEachIndexed { index, graph -> inputs.property("resolvedJsonrpc$index", graph) }
            doLast {
                graphs.forEach { graph ->
                    val dependencies = graph.get()
                    check(dependencies.any { it.startsWith("jsonrpc4k-core:") || it.startsWith("jsonrpc4k-core-") }) {
                        "Release publication did not resolve jsonrpc4k-core: $dependencies"
                    }
                    check(dependencies.any { it.startsWith("jsonrpc4k-transport:") || it.startsWith("jsonrpc4k-transport-") }) {
                        "Release publication did not resolve jsonrpc4k-transport: $dependencies"
                    }
                    check(dependencies.all { it.substringAfterLast(':') == expected }) {
                        "Release publication requires exact jsonrpc4k $expected in requested and selected dependencies: $dependencies"
                    }
                }
            }
        }
    root.allprojects {
        tasks.withType<PublishToMavenRepository>().configureEach { dependsOn(guard) }
    }
}
