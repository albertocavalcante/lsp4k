# Publication model check

`check-publication-model.sh` publishes the protocol, client, and server root and JVM variants to a temporary local Maven
repository. It inspects their coordinates, POM metadata, Gradle module metadata, JVM JARs, and public dependency entries.
The fixture repository is removed when the check finishes. The CI checkout pins the jsonrpc4k source used for the build;
the generated POM still declares the catalog's snapshot dependency.

This check does not publish to a remote registry or establish Java 17 compatibility. A separate clean published-artifact
consumer, with immutable dependencies and no composite or Maven local, is required before claiming consumer readiness.
