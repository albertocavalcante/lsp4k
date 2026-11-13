# Publication model check

`check-publication-model.sh` publishes the protocol, client, and server root and JVM variants to a temporary local Maven
repository. It first publishes the host-available variants of the CI-pinned jsonrpc4k source there at its declared
snapshot version, then disables the composite build and resolves that group exclusively from the fixture. The native
variants are needed for Gradle's common metadata transformation. It inspects LSP coordinates, POM metadata, Gradle module
metadata, JVM JARs, and public dependency entries. The fixture repository is removed when the check finishes; the generated
POM still declares the catalog's snapshot dependency.

With `LSP4K_VERIFY_JAVA17=true`, the script also checks every JVM API/runtime variant and JAR class version, runs the
SDK's JVM tests on Java 17, and copies separate standalone Gradle consumers for root and JVM coordinates. Those consumers
declare only the client and server artifacts, use the temporary repository exclusively for LSP4K and jsonrpc4k, reject
Java 16 variants, and perform a public initialization and shutdown on Java 17. Set `JAVA_HOME_17_X64` to the Java 17 JDK
and use JDK 21 for the producer Gradle daemon. The fixture is deleted on exit.

This check does not publish to a remote registry, prove a Maven consumer, or establish an immutable published dependency.
The JSON-RPC artifact remains a snapshot from the pinned source checkout; an independent published-artifact consumer is
still required before claiming release readiness.

## Publication path

The tag/manual publication workflow preflights the Linux host publication tasks in a private file repository before
uploading to the fixed LSP4K GitHub Packages repository. Both steps disable sibling composite substitution and select
the exclusive JSON-RPC GitHub Packages route with the same version and credentials. Tagged runs derive the publication
version from the validated tag; manual runs use `VERSION_NAME`. The file preflight proves only dependencies resolved by
the Linux publication task graph, not all metadata-only runtime or native variants, live package access, or atomic upload.
Both steps check the actual Maven publication versions, reject mixed versions, and verify that tagged versions match.
Consistent snapshot publications retain the manual snapshot behavior. Every non-snapshot publication, including a
manual run, requires a fixed non-SNAPSHOT JSON-RPC catalog version and verifies requested and resolved core and
transport versions on client/server JVM runtime graphs before any Maven publication task. The current catalog uses a
snapshot, so release publication fails until core provides an independently resolvable version and the catalog is
updated. This version check does not prove registry immutability.

For development, existing repository selection and automatic sibling composites remain the default. To resolve JSON-RPC
artifacts exclusively from its GitHub Packages repository, select `-Pjsonrpc4k.repository=github` and disable the composite
with `-Plsp4k.useLocalJsonrpc4k=false`. The route uses `gpr.user` and `gpr.key` user properties or `GITHUB_ACTOR` and
`GITHUB_TOKEN`; credentials belong in private configuration or the environment. Registry availability, cross-repository
read permission, and immutable published JSON-RPC versions remain unverified. The local tests redirect only to private
file repositories and do not authenticate to GitHub or publish remotely.
