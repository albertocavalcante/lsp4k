# Publication model check

`check-publication-model.sh` publishes the protocol, client, and server root and JVM variants to a temporary local Maven
repository. It first publishes the CI-pinned jsonrpc4k source there at its declared snapshot version, then disables the
composite build and resolves that group exclusively from the fixture. It inspects LSP coordinates, POM metadata, Gradle
module metadata, JVM JARs, and public dependency entries. The fixture repository is removed when the check finishes; the
generated POM still declares the catalog's snapshot dependency.

This check does not publish to a remote registry or establish Java 17 compatibility. A separate clean published-artifact
consumer, with immutable dependencies and no composite or Maven local, is required before claiming consumer readiness.
