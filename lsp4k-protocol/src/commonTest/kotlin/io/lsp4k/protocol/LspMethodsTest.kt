package io.lsp4k.protocol

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class LspMethodsTest {
    @Test
    fun `LspMethods contains standard lifecycle methods`() {
        LspMethods.INITIALIZE shouldBe "initialize"
        LspMethods.INITIALIZED shouldBe "initialized"
        LspMethods.SHUTDOWN shouldBe "shutdown"
        LspMethods.EXIT shouldBe "exit"
    }

    @Test
    fun `LspMethods contains text document methods`() {
        LspMethods.TEXT_DOCUMENT_DID_OPEN shouldBe "textDocument/didOpen"
        LspMethods.TEXT_DOCUMENT_DID_CLOSE shouldBe "textDocument/didClose"
        LspMethods.TEXT_DOCUMENT_DID_CHANGE shouldBe "textDocument/didChange"
        LspMethods.TEXT_DOCUMENT_COMPLETION shouldBe "textDocument/completion"
        LspMethods.TEXT_DOCUMENT_HOVER shouldBe "textDocument/hover"
        LspMethods.TEXT_DOCUMENT_DEFINITION shouldBe "textDocument/definition"
    }

    @Test
    fun `LspMethods contains workspace methods`() {
        LspMethods.WORKSPACE_DID_CHANGE_CONFIGURATION shouldBe "workspace/didChangeConfiguration"
        LspMethods.WORKSPACE_SYMBOL shouldBe "workspace/symbol"
        LspMethods.WORKSPACE_EXECUTE_COMMAND shouldBe "workspace/executeCommand"
    }

    @Test
    fun `LspMethods contains window methods`() {
        LspMethods.WINDOW_SHOW_MESSAGE shouldBe "window/showMessage"
        LspMethods.WINDOW_LOG_MESSAGE shouldBe "window/logMessage"
    }

    @Test
    fun `LspMethods contains general methods`() {
        LspMethods.CANCEL_REQUEST shouldBe "\$/cancelRequest"
        LspMethods.PROGRESS shouldBe "\$/progress"
    }
}
