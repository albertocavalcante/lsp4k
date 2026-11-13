#!/usr/bin/env bash
set -euo pipefail

: "${RUNNER_TEMP:?A private runner temp directory is required}"
fixture_root=$(mktemp -d "$RUNNER_TEMP/lsp4k-publish-check.XXXXXXXX")
trap 'rm -rf "$fixture_root"' EXIT
workflow=.github/workflows/publish.yml

extract_run() {
	local label=$1 target=$2
	awk -v label="$label" '
		$0 == "      - name: " label { step = 1; next }
		step && /^        run: \|$/ { body = 1; next }
		body && /^        env:$/ { exit }
		body && /^      - name:/ { exit }
		body { sub(/^          /, ""); print }
	' "$workflow" >"$target"
	[[ -s $target ]]
}
check_token_env() {
	local label=$1
	awk -v label="$label" '
		$0 == "      - name: " label { step = 1; next }
		step && /^      - name:/ { exit }
		step && $0 == "          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}" { found = 1 }
		END { if (!found) exit 1 }
	' "$workflow"
}
preflight='Preflight configured publication to a private file repository'
extract_run 'Select tagged publication version' "$fixture_root/version.sh"
extract_run "$preflight" "$fixture_root/preflight.sh"
extract_run 'Publish' "$fixture_root/publish.sh"
check_token_env "$preflight"
check_token_env 'Publish'
for tag in v1.2.3 v2.0.0-rc.1; do
	output="$fixture_root/version-output"
	: >"$output"
	RELEASE_TAG="$tag" GITHUB_OUTPUT="$output" bash "$fixture_root/version.sh"
	grep -Fxq "version=${tag#v}" "$output"
done
for tag in bad v1.0.0-SNAPSHOT 'v1;touch-bad'; do
	if RELEASE_TAG="$tag" GITHUB_OUTPUT="$fixture_root/version-output" bash "$fixture_root/version.sh" >/dev/null 2>&1; then
		printf 'Invalid tag was accepted: %s\n' "$tag" >&2
		exit 1
	fi
done
cat >"$fixture_root/gradlew" <<'STUB'
#!/usr/bin/env bash
set -euo pipefail
printf '%s\n' "$@" > "$TRACE_FILE"
[[ ${GITHUB_TOKEN:-} == fixture-token ]]
STUB
chmod +x "$fixture_root/gradlew"

for mode in preflight publish; do
	for version in '' 7.1.2; do
		trace="$fixture_root/$mode-${version:-manual}.args"
		(
			cd "$fixture_root"
			export RUNNER_TEMP="$fixture_root" TRACE_FILE="$trace" RELEASE_VERSION="$version" GITHUB_TOKEN=fixture-token
			bash "$fixture_root/$mode.sh"
		)
		grep -Fxq -- '-Plsp4k.useLocalJsonrpc4k=false' "$trace"
		grep -Fxq -- '-Pjsonrpc4k.repository=github' "$trace"
		grep -Fxq -- tools/ci/release-dependency-guard.init.gradle.kts "$trace"
		if [[ -n $version ]]; then
			grep -Fxq -- "-PVERSION_NAME=$version" "$trace"
		elif grep -Fq -- '-PVERSION_NAME=' "$trace"; then
			printf 'Manual publication unexpectedly overrides version\n' >&2
			exit 1
		fi
		if [[ $mode == preflight ]]; then
			grep -Fxq -- publishAllPublicationsToPreflightRepository "$trace"
		else
			grep -Fxq -- publishAllPublicationsToGitHubPackagesRepository "$trace"
		fi
	done
done
