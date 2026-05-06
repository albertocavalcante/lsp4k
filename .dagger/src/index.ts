/**
 * Dagger CI module for lsp4k — Kotlin Multiplatform LSP library.
 *
 * Provides portable, containerised CI functions that mirror the project's
 * Gradle tasks.  Apple-native targets are disabled (use the dedicated
 * GitHub Actions macOS job for those).
 */
import {
  dag,
  Container,
  Directory,
  Platform,
  object,
  func,
} from "@dagger.io/dagger"

@object()
export class Lsp4K {
  /**
   * Base Gradle container with JDK 21, source mounted, and caches configured.
   */
  @func()
  base(source: Directory, jsonrpc4k: Directory): Container {
    return dag
      .container({ platform: "linux/amd64" as Platform })
      .from("eclipse-temurin:21-jdk-noble")
      .withMountedCache("/root/.gradle/caches", dag.cacheVolume("gradle-caches"))
      .withMountedCache(
        "/root/.gradle/build-cache",
        dag.cacheVolume("gradle-build-cache"),
      )
      .withMountedCache("/root/.kotlin", dag.cacheVolume("kotlin-cache"))
      .withEnvVariable("GRADLE_OPTS", "-Xmx2g -XX:+UseParallelGC")
      .withDirectory("/src", source)
      .withDirectory("/src/jsonrpc4k", jsonrpc4k)
      .withWorkdir("/src")
  }

  /**
   * Run full Gradle check (compile + test + detekt + spotless)
   * with Apple targets disabled.
   */
  @func()
  async check(source: Directory, jsonrpc4k: Directory): Promise<string> {
    return this.base(source, jsonrpc4k)
      .withExec([
        "./gradlew",
        "check",
        "-Plsp4k.jsonrpc4kBuild=jsonrpc4k",
        "-Plsp4k.enableAppleTargets=false",
      ])
      .stdout()
  }

  /**
   * Run Spotless + Detekt only (fast lint check).
   */
  @func()
  async lint(source: Directory, jsonrpc4k: Directory): Promise<string> {
    return this.base(source, jsonrpc4k)
      .withExec([
        "./gradlew",
        "spotlessCheck",
        "detekt",
        "-Plsp4k.jsonrpc4kBuild=jsonrpc4k",
        "-Plsp4k.enableAppleTargets=false",
      ])
      .stdout()
  }

  /**
   * Generate Kover coverage reports and verify the 74 % baseline.
   */
  @func()
  async coverage(source: Directory, jsonrpc4k: Directory): Promise<string> {
    return this.base(source, jsonrpc4k)
      .withExec([
        "./gradlew",
        "koverHtmlReport",
        "koverXmlReport",
        "koverVerify",
        "-Plsp4k.jsonrpc4kBuild=jsonrpc4k",
        "-Plsp4k.enableAppleTargets=false",
      ])
      .stdout()
  }

  /**
   * Umbrella: runs check (compile + test + lint) then coverage.
   * Single entry point for CI.
   */
  @func()
  async ci(source: Directory, jsonrpc4k: Directory): Promise<string> {
    await this.check(source, jsonrpc4k)
    return this.coverage(source, jsonrpc4k)
  }
}
