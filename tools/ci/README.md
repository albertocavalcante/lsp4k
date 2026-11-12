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
