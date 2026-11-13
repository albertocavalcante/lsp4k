plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

application {
    mainClass.set("io.lsp4k.example.NoteLanguageServerKt")
}

dependencies {
    implementation(projects.lsp4kServer)
}
