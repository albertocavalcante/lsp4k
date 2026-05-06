plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

fun appleTargetsEnabled(): Boolean {
    val requested = providers.gradleProperty("lsp4k.enableAppleTargets").orNull?.toBooleanStrict()
    if (requested == false) return false

    val isMacHost = System.getProperty("os.name").contains("Mac", ignoreCase = true)
    if (!isMacHost) {
        check(requested != true) {
            "Apple native targets require a macOS host."
        }
        return false
    }

    val xcodeAvailable = providers.exec {
        commandLine("xcrun", "xcodebuild", "-version")
        isIgnoreExitValue = true
    }.result.get().exitValue == 0

    check(requested != true || xcodeAvailable) {
        "Apple native targets were requested, but full Xcode is not selected. " +
            "Select Xcode with xcode-select or run Gradle with DEVELOPER_DIR pointing to Xcode.app/Contents/Developer."
    }

    return xcodeAvailable
}

kotlin {
    explicitApi()

    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    jvmToolchain(21)

    js(IR) {
        browser()
        nodejs()
    }

    // Native targets
    if (appleTargetsEnabled()) {
        macosX64()
        macosArm64()
    } else {
        logger.lifecycle("Skipping Apple native targets because Xcode is not available")
    }
    linuxX64()
    linuxArm64()
    mingwX64()

    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xexpect-actual-classes",
            "-opt-in=kotlin.ExperimentalStdlibApi",
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
        )
    }
}
