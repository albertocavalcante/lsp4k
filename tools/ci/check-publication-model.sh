#!/usr/bin/env bash
set -euo pipefail

: "${RUNNER_TEMP:?A private runner temp directory is required}"
: "${GITHUB_RUN_ID:?A unique run ID is required}"
: "${GITHUB_RUN_ATTEMPT:?A unique run attempt is required}"

export LSP4K_FIXTURE_VERSION="0.0.0-fixture-${GITHUB_RUN_ID}-${GITHUB_RUN_ATTEMPT}"
LSP4K_FIXTURE_REPOSITORY=$(mktemp -d "$RUNNER_TEMP/lsp4k-publication.XXXXXXXX")
export LSP4K_FIXTURE_REPOSITORY
trap 'rm -rf "$LSP4K_FIXTURE_REPOSITORY"' EXIT

./gradlew --no-daemon --max-workers=2 -Dorg.gradle.jvmargs=-Xmx1536m \
	-PVERSION_NAME="$LSP4K_FIXTURE_VERSION" \
	-Plsp4k.jsonrpc4kBuild=jsonrpc4k -Plsp4k.enableAppleTargets=false \
	--init-script tools/ci/publication-fixture.init.gradle.kts \
	:lsp4k-protocol:publishKotlinMultiplatformPublicationToFixtureRepository \
	:lsp4k-protocol:publishJvmPublicationToFixtureRepository \
	:lsp4k-client:publishKotlinMultiplatformPublicationToFixtureRepository \
	:lsp4k-client:publishJvmPublicationToFixtureRepository \
	:lsp4k-server:publishKotlinMultiplatformPublicationToFixtureRepository \
	:lsp4k-server:publishJvmPublicationToFixtureRepository

python3 -I - <<'PY'
import json
import os
from pathlib import Path
import struct
import xml.etree.ElementTree as ET
import zipfile

root = Path(os.environ["LSP4K_FIXTURE_REPOSITORY"]) / "io/lsp4k"
version = os.environ["LSP4K_FIXTURE_VERSION"]
ns = {"m": "http://maven.apache.org/POM/4.0.0"}

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
        assert metadata["component"]["module"] == artifact
        assert metadata["component"]["version"] == version
        dependencies = [
            (d.findtext("m:groupId", namespaces=ns), d.findtext("m:artifactId", namespaces=ns), d.findtext("m:version", namespaces=ns))
            for d in pom.findall("m:dependencies/m:dependency", ns)
        ]
        print(f"{artifact}:{version} POM dependencies: {dependencies}")
        print(f"{artifact} variants: {[(v['name'], v['attributes']) for v in metadata['variants']]}")
        if artifact.endswith("-jvm"):
            with zipfile.ZipFile(f"{prefix}.jar") as jar:
                classes = [name for name in jar.namelist() if name.endswith(".class")]
                assert classes, f"{artifact} contains no classes"
                majors = [struct.unpack(">H", jar.read(name)[6:8])[0] for name in classes]
                print(f"{artifact} maximum JVM class major: {max(majors)}")
            if library != "lsp4k-protocol":
                assert any(group == "io.lsp4k" and name.startswith("lsp4k-protocol") and dep_version == version
                           for group, name, dep_version in dependencies), dependencies
                assert any(group == "io.github.albertocavalcante" and name.startswith("jsonrpc4k-transport")
                           for group, name, _ in dependencies), dependencies
PY
