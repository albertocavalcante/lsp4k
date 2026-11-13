plugins {
    id("lsp4k.multiplatform")
    id("lsp4k.publication")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(libs.kotlinx.serialization.json)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotest.assertions.core)
            }
        }

        jvmTest {
            dependencies {
                implementation(libs.kotest.runner.junit5)
                // Test only: the meta-model conformance test checks its wire Json against jsonrpc4k's.
                implementation(libs.jsonrpc4k.core)
            }
        }
    }
}

// The protocol module is pure serialization, so the Node run covers the JS target. The headless Chrome run added
// no coverage and failed intermittently with "Test running process exited unexpectedly" on CI.
tasks.matching { it.name == "jsBrowserTest" }.configureEach {
    enabled = false
}
