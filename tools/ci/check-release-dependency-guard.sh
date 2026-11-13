#!/usr/bin/env bash
set -euo pipefail

: "${RUNNER_TEMP:?A private runner temp directory is required}"
fixture_root=$(mktemp -d "$RUNNER_TEMP/lsp4k-release-guard.XXXXXXXX")
trap 'rm -rf "$fixture_root"' EXIT
fixture_project="$fixture_root/project"
export LSP_GUARD_REPO="$fixture_root/repository"
export LSP_GUARD_OUTPUT="$fixture_root/output"
mkdir -p "$fixture_project/lsp4k-protocol" "$fixture_project/lsp4k-client" "$fixture_project/lsp4k-server" "$LSP_GUARD_REPO"
cat >"$fixture_project/settings.gradle.kts" <<'SETTINGS'
rootProject.name = "lsp4k"
include(":lsp4k-protocol", ":lsp4k-client", ":lsp4k-server")
dependencyResolutionManagement {
    repositories { maven { url = uri(requireNotNull(System.getenv("LSP_GUARD_REPO"))) } }
    versionCatalogs { create("libs") { version("jsonrpc4k", requireNotNull(System.getenv("LSP_GUARD_EXPECTED"))) } }
}
SETTINGS
cat >"$fixture_project/build.gradle.kts" <<'BUILD'
subprojects {
    group = "io.lsp4k.fixture"
    version = if (name == "lsp4k-protocol") System.getenv("LSP_GUARD_PROTOCOL_VERSION") else System.getenv("LSP_GUARD_SDK_VERSION")
    apply(plugin = "java")
    apply(plugin = "maven-publish")
    val runtime = configurations.create("jvmRuntimeClasspath")
    dependencies.add(runtime.name, "io.github.albertocavalcante:jsonrpc4k-transport:${System.getenv("LSP_GUARD_REQUESTED")}")
    extensions.configure<org.gradle.api.publish.PublishingExtension> {
        val missing = System.getenv("LSP_GUARD_MISSING_PUBLICATION")
        val names = listOf("kotlinMultiplatform", "jvm", "js", "linuxX64", "linuxArm64", "mingwX64", "macosX64", "macosArm64")
        names.filter { it != missing }.forEach { publication ->
            publications.create<org.gradle.api.publish.maven.MavenPublication>(publication) {
                artifactId = "${project.name}-$publication"
            }
        }
        repositories.maven { name = "Fixture"; url = uri(requireNotNull(System.getenv("LSP_GUARD_OUTPUT"))) }
    }
}
BUILD
artifact() {
	local module=$1 version=$2 dependency=${3:-} directory
	directory="$LSP_GUARD_REPO/io/github/albertocavalcante/$module/$version"
	mkdir -p "$directory"
	cat >"$directory/$module-$version.pom" <<POM
<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion><groupId>io.github.albertocavalcante</groupId><artifactId>$module</artifactId><version>$version</version>$dependency</project>
POM
}
core_dep='<dependencies><dependency><groupId>io.github.albertocavalcante</groupId><artifactId>jsonrpc4k-core</artifactId><version>1.2.3</version></dependency></dependencies>'
artifact jsonrpc4k-core 1.2.3
artifact jsonrpc4k-transport 1.2.3 "$core_dep"
artifact jsonrpc4k-transport 1.2.4 "$core_dep"
guard="$PWD/tools/ci/release-dependency-guard.init.gradle.kts"
run_guard() {
	LSP_GUARD_EXPECTED="$1" LSP_GUARD_REQUESTED="$2" LSP_GUARD_SDK_VERSION="${3:-1.0}" \
		LSP_GUARD_PROTOCOL_VERSION="${4:-${3:-1.0}}" RELEASE_VERSION="${5:-}" \
		./gradlew --no-daemon --configuration-cache --max-workers=2 -Dorg.gradle.jvmargs=-Xmx1536m \
		-p "$fixture_project" --init-script "$guard" publishAllPublicationsToFixtureRepository
}
run_guard 0.1.0-SNAPSHOT 0.1.0-SNAPSHOT 0.1.0-SNAPSHOT
[[ -d $LSP_GUARD_OUTPUT ]]
rm -rf "$LSP_GUARD_OUTPUT"
run_guard 1.2.3 1.2.3
run_guard 1.2.3 1.2.3 >"$fixture_root/reuse.log" 2>&1
if ! grep -Fq 'Reusing configuration cache.' "$fixture_root/reuse.log"; then
	cat "$fixture_root/reuse.log" >&2
	printf 'Release guard did not reuse the configuration cache\n' >&2
	exit 1
fi
rm -rf "$LSP_GUARD_OUTPUT"
for version in 1.2.3-SNAPSHOT '1.+' latest.release '[1.0,2.0)'; do
	if run_guard "$version" "$version" >"$fixture_root/rejected.log" 2>&1; then
		printf 'Mutable release dependency unexpectedly accepted: %s\n' "$version" >&2
		exit 1
	fi
	grep -Fq 'requires an exact non-SNAPSHOT jsonrpc4k catalog version' "$fixture_root/rejected.log"
	[[ ! -e $LSP_GUARD_OUTPUT ]]
done
# Release checks use actual MavenPublication names. Test both tagged and manual paths;
# an absent Apple publication must fail before any file repository output is created.
for missing in macosX64 macosArm64; do
	for tag in '' 1.0; do
		if LSP_GUARD_MISSING_PUBLICATION="$missing" run_guard 1.2.3 1.2.3 1.0 1.0 "$tag" >"$fixture_root/missing-target.log" 2>&1; then
			printf 'Release unexpectedly accepted missing publication: %s\n' "$missing" >&2
			exit 1
		fi
		grep -Fq "missing required publications: [$missing]" "$fixture_root/missing-target.log"
		[[ ! -e $LSP_GUARD_OUTPUT ]]
	done
done
# A manual snapshot remains scoped to whichever targets are configured on the host.
LSP_GUARD_MISSING_PUBLICATION=macosArm64 run_guard 0.1.0-SNAPSHOT 0.1.0-SNAPSHOT 0.1.0-SNAPSHOT
[[ -d $LSP_GUARD_OUTPUT ]]
rm -rf "$LSP_GUARD_OUTPUT"
if run_guard 1.2.3 1.2.4 >"$fixture_root/mismatch.log" 2>&1; then
	printf 'Mismatched resolved JSON-RPC version unexpectedly accepted\n' >&2
	exit 1
fi
grep -Fq 'requires exact jsonrpc4k 1.2.3 in requested and selected dependencies' "$fixture_root/mismatch.log"
[[ ! -e $LSP_GUARD_OUTPUT ]]
if run_guard 1.2.3 1.2.3 1.0 0.1.0-SNAPSHOT >"$fixture_root/mixed.log" 2>&1; then
	printf 'Mixed publication versions unexpectedly accepted\n' >&2
	exit 1
fi
grep -Fq 'Mixed Maven publication versions are not supported' "$fixture_root/mixed.log"
[[ ! -e $LSP_GUARD_OUTPUT ]]
if run_guard 1.2.3 1.2.3 0.1.0-SNAPSHOT 0.1.0-SNAPSHOT 1.0 >"$fixture_root/tag-mismatch.log" 2>&1; then
	printf 'Tag/publication version mismatch unexpectedly accepted\n' >&2
	exit 1
fi
grep -Fq 'does not match Maven publication version' "$fixture_root/tag-mismatch.log"
[[ ! -e $LSP_GUARD_OUTPUT ]]
