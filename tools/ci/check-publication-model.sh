#!/usr/bin/env bash
set -euo pipefail

: "${RUNNER_TEMP:?A private runner temp directory is required}"
: "${GITHUB_RUN_ID:?A unique run ID is required}"
: "${GITHUB_RUN_ATTEMPT:?A unique run attempt is required}"

export LSP4K_FIXTURE_VERSION="0.0.0-fixture-${GITHUB_RUN_ID}-${GITHUB_RUN_ATTEMPT}"
LSP4K_FIXTURE_REPOSITORY=$(mktemp -d "$RUNNER_TEMP/lsp4k-publication.XXXXXXXX")
export LSP4K_FIXTURE_REPOSITORY
trap 'rm -rf "$LSP4K_FIXTURE_REPOSITORY"' EXIT

./jsonrpc4k/gradlew -p jsonrpc4k --no-daemon --max-workers=2 -Dorg.gradle.jvmargs=-Xmx1536m \
	--init-script "$PWD/tools/ci/publication-fixture.init.gradle.kts" \
	:jsonrpc4k-core:publishAllPublicationsToFixtureRepository \
	:jsonrpc4k-transport:publishAllPublicationsToFixtureRepository

./gradlew --no-daemon --max-workers=2 -Dorg.gradle.jvmargs=-Xmx1536m \
	-PVERSION_NAME="$LSP4K_FIXTURE_VERSION" \
	-Plsp4k.useLocalJsonrpc4k=false -Plsp4k.enableAppleTargets=false \
	--init-script tools/ci/publication-fixture.init.gradle.kts \
	:lsp4k-protocol:publishKotlinMultiplatformPublicationToFixtureRepository \
	:lsp4k-protocol:publishJvmPublicationToFixtureRepository \
	:lsp4k-client:publishKotlinMultiplatformPublicationToFixtureRepository \
	:lsp4k-client:publishJvmPublicationToFixtureRepository \
	:lsp4k-server:publishKotlinMultiplatformPublicationToFixtureRepository \
	:lsp4k-server:publishJvmPublicationToFixtureRepository

status=0
python3 -I - <<'PY' || status=1
import json
import os
from pathlib import Path
import struct
import xml.etree.ElementTree as ET
import zipfile

root = Path(os.environ["LSP4K_FIXTURE_REPOSITORY"]) / "io/lsp4k"
version = os.environ["LSP4K_FIXTURE_VERSION"]
ns = {"m": "http://maven.apache.org/POM/4.0.0"}
java17_failure = False
expected_api = {
    "lsp4k-protocol": [("org.jetbrains.kotlinx", "kotlinx-serialization-json", "1.9.0")],
    "lsp4k-client": [
        ("io.lsp4k", "lsp4k-protocol", version),
        ("io.github.albertocavalcante", "jsonrpc4k-transport", "0.1.0-SNAPSHOT"),
        ("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.10.2"),
    ],
    "lsp4k-server": [
        ("io.lsp4k", "lsp4k-protocol", version),
        ("io.github.albertocavalcante", "jsonrpc4k-transport", "0.1.0-SNAPSHOT"),
        ("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.10.2"),
    ],
}

for library in ("lsp4k-protocol", "lsp4k-client", "lsp4k-server"):
    for artifact in (library, f"{library}-jvm"):
        directory = root / artifact / version
        prefix = directory / f"{artifact}-{version}"
        pom = ET.parse(f"{prefix}.pom").getroot()
        assert pom.findtext("m:groupId", namespaces=ns) == "io.lsp4k"
        assert pom.findtext("m:artifactId", namespaces=ns) == artifact
        assert pom.findtext("m:version", namespaces=ns) == version
        assert pom.findtext("m:licenses/m:license/m:name", namespaces=ns) == "Apache License 2.0"
        assert pom.findtext("m:scm/m:url", namespaces=ns) == "https://github.com/albertocavalcante/lsp4k"
        metadata = json.loads(Path(f"{prefix}.module").read_text())
        assert metadata["component"]["group"] == "io.lsp4k"
        assert metadata["component"]["module"] == library, metadata["component"]
        assert metadata["component"]["version"] == version
        if artifact.endswith("-jvm"):
            owner = metadata["component"].get("url")
            assert owner, metadata["component"]
            assert (directory / owner).resolve() == (root / library / version / f"{library}-{version}.module").resolve(), owner
        dependencies = [
            (d.findtext("m:groupId", namespaces=ns), d.findtext("m:artifactId", namespaces=ns),
             d.findtext("m:version", namespaces=ns), d.findtext("m:scope", default="compile", namespaces=ns))
            for d in pom.findall("m:dependencies/m:dependency", ns)
        ]
        print(f"{artifact}:{version} POM dependencies: {dependencies}")
        print(f"{artifact} variants: {[(v['name'], v['attributes']) for v in metadata['variants']]}")
        if os.environ.get("LSP4K_VERIFY_JAVA17") == "true":
            variants = [v for v in metadata["variants"]
                        if v["attributes"].get("org.gradle.category") == "library"
                        and v["attributes"].get("org.jetbrains.kotlin.platform.type") == "jvm"
                        and v["attributes"].get("org.gradle.usage") in ("java-api", "java-runtime")]
            if ({v["attributes"]["org.gradle.usage"] for v in variants} != {"java-api", "java-runtime"}
                    or any(v["attributes"].get("org.gradle.jvm.version") != 17 for v in variants)):
                print(f"{artifact}: missing or incompatible Java 17 variant metadata: {variants}")
                java17_failure = True
        if artifact == library:
            redirects = [v["available-at"] for v in metadata["variants"] if "available-at" in v]
            assert any(target.get("group") == "io.lsp4k" and target.get("module") == f"{library}-jvm"
                       and target.get("version") == version for target in redirects), redirects
        if artifact.endswith("-jvm"):
            with zipfile.ZipFile(f"{prefix}.jar") as jar:
                classes = [name for name in jar.namelist() if name.endswith(".class")]
                assert classes, f"{artifact} contains no classes"
                majors = [struct.unpack(">H", jar.read(name)[6:8])[0] for name in classes]
                print(f"{artifact} maximum JVM class major: {max(majors)}")
                if os.environ.get("LSP4K_VERIFY_JAVA17") == "true" and max(majors) != 61:
                    print(f"{artifact}: expected Java 17 class major 61")
                    java17_failure = True
            api_variants = [v for v in metadata["variants"]
                            if v["attributes"].get("org.gradle.usage") == "java-api"]
            assert api_variants, f"{artifact} has no JVM API variant"
            for group, module, dep_version in expected_api[library]:
                assert any(g == group and name in (module, f"{module}-jvm") and v == dep_version and scope == "compile"
                           for g, name, v, scope in dependencies), dependencies
                for variant in api_variants:
                    api_dependencies = variant.get("dependencies", [])
                    print(f"{artifact} API dependencies: {api_dependencies}")
                    assert any(d["group"] == group and d["module"] in (module, f"{module}-jvm")
                               and d.get("version", {}).get("requires") == dep_version
                               for d in api_dependencies), api_dependencies
if java17_failure:
    raise SystemExit(1)
PY

if [[ ${LSP4K_VERIFY_JAVA17:-false} == true ]]; then
	: "${JAVA_HOME_17_X64:?JDK 17 is required for the consumer}"
	for form in root jvm; do
		consumer_dir="$LSP4K_FIXTURE_REPOSITORY/consumer-$form"
		mkdir -p "$consumer_dir"
		cp -R tools/ci/java17-consumer/. "$consumer_dir/"
		LSP4K_FIXTURE_FORM="$form" JAVA_HOME="$JAVA_HOME_17_X64" PATH="$JAVA_HOME_17_X64/bin:$PATH" \
			./gradlew --no-daemon --max-workers=2 -Dorg.gradle.jvmargs=-Xmx1536m -p "$consumer_dir" \
			run || status=1
		if [[ $form == root ]]; then
			LSP4K_FIXTURE_FORM="$form" JAVA_HOME="$JAVA_HOME_17_X64" PATH="$JAVA_HOME_17_X64/bin:$PATH" \
				./gradlew --no-daemon --max-workers=2 -Dorg.gradle.jvmargs=-Xmx1536m -p "$consumer_dir" \
				verifyJava16Rejected || status=1
		fi
	done
	./gradlew --no-daemon --max-workers=2 -Dorg.gradle.jvmargs=-Xmx1536m \
		-Plsp4k.jsonrpc4kBuild=jsonrpc4k -Plsp4k.enableAppleTargets=false \
		--init-script tools/ci/publication-fixture.init.gradle.kts \
		:lsp4k-protocol:jvmTest :lsp4k-client:jvmTest :lsp4k-server:jvmTest || status=1
fi
exit "$status"
