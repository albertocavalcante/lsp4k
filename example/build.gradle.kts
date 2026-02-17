plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

application {
    mainClass.set("io.lsp4k.example.MainKt")
}

dependencies {
    implementation(projects.lsp4kServer)
}
