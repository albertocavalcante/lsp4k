plugins {
    id("lsp4k.multiplatform")
    id("lsp4k.publication")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(libs.jsonrpc4k.transport)
                api(projects.lsp4kProtocol)
                api(libs.kotlinx.coroutines.core)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotest.assertions.core)
                implementation(libs.kotlinx.coroutines.test)
                implementation(projects.lsp4kClient)
            }
        }

        jvmTest {
            dependencies {
                implementation(libs.kotest.runner.junit5)
            }
        }
    }
}
