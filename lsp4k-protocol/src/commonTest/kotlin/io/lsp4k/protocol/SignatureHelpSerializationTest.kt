package io.lsp4k.protocol

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * Tests for SignatureHelp-related types including:
 * - SignatureHelpTriggerKind (integer-serialized enum)
 * - SignatureHelpParams
 * - SignatureHelpContext
 * - SignatureHelp
 * - SignatureInformation
 * - ParameterInformation
 */
class SignatureHelpSerializationTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

    // ==================== SignatureHelpTriggerKind Tests ====================

    @Test
    fun `SignatureHelpTriggerKind Invoked serializes to 1`() {
        val encoded = json.encodeToString(SignatureHelpTriggerKind.Invoked)
        encoded shouldBe "1"
    }

    @Test
    fun `SignatureHelpTriggerKind TriggerCharacter serializes to 2`() {
        val encoded = json.encodeToString(SignatureHelpTriggerKind.TriggerCharacter)
        encoded shouldBe "2"
    }

    @Test
    fun `SignatureHelpTriggerKind ContentChange serializes to 3`() {
        val encoded = json.encodeToString(SignatureHelpTriggerKind.ContentChange)
        encoded shouldBe "3"
    }

    @Test
    fun `SignatureHelpTriggerKind deserializes from integers`() {
        json.decodeFromString<SignatureHelpTriggerKind>("1") shouldBe SignatureHelpTriggerKind.Invoked
        json.decodeFromString<SignatureHelpTriggerKind>("2") shouldBe SignatureHelpTriggerKind.TriggerCharacter
        json.decodeFromString<SignatureHelpTriggerKind>("3") shouldBe SignatureHelpTriggerKind.ContentChange
    }

    @Test
    fun `SignatureHelpTriggerKind fromValue throws for unknown value`() {
        assertFailsWith<IllegalArgumentException> {
            SignatureHelpTriggerKind.fromValue(99)
        }
    }

    // ==================== SignatureHelpParams Tests ====================

    @Test
    fun `SignatureHelpParams minimal`() {
        val params =
            SignatureHelpParams(
                textDocument = TextDocumentIdentifier(uri = "file:///test.kt"),
                position = Position(line = 10, character = 5),
            )
        val encoded = json.encodeToString(params)
        encoded shouldContain "\"uri\":\"file:///test.kt\""
        encoded shouldContain "\"line\":10"
        encoded shouldContain "\"character\":5"
    }

    @Test
    fun `SignatureHelpParams with context`() {
        val params =
            SignatureHelpParams(
                textDocument = TextDocumentIdentifier(uri = "file:///main.kt"),
                position = Position(line = 20, character = 15),
                context =
                    SignatureHelpContext(
                        triggerKind = SignatureHelpTriggerKind.TriggerCharacter,
                        triggerCharacter = "(",
                        isRetrigger = false,
                    ),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<SignatureHelpParams>(encoded)
        decoded.context?.triggerKind shouldBe SignatureHelpTriggerKind.TriggerCharacter
        decoded.context?.triggerCharacter shouldBe "("
        decoded.context?.isRetrigger shouldBe false
    }

    @Test
    fun `SignatureHelpParams round-trip`() {
        val params =
            SignatureHelpParams(
                textDocument = TextDocumentIdentifier(uri = "file:///code.kt"),
                position = Position(line = 5, character = 10),
                context =
                    SignatureHelpContext(
                        triggerKind = SignatureHelpTriggerKind.Invoked,
                        isRetrigger = true,
                    ),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<SignatureHelpParams>(encoded)
        decoded shouldBe params
    }

    // ==================== SignatureHelpContext Tests ====================

    @Test
    fun `SignatureHelpContext minimal`() {
        val context =
            SignatureHelpContext(
                triggerKind = SignatureHelpTriggerKind.Invoked,
                isRetrigger = false,
            )
        val encoded = json.encodeToString(context)
        val decoded = json.decodeFromString<SignatureHelpContext>(encoded)
        decoded.triggerKind shouldBe SignatureHelpTriggerKind.Invoked
        decoded.isRetrigger shouldBe false
        decoded.triggerCharacter shouldBe null
        decoded.activeSignatureHelp shouldBe null
    }

    @Test
    fun `SignatureHelpContext with trigger character`() {
        val context =
            SignatureHelpContext(
                triggerKind = SignatureHelpTriggerKind.TriggerCharacter,
                triggerCharacter = ",",
                isRetrigger = true,
            )
        val encoded = json.encodeToString(context)
        val decoded = json.decodeFromString<SignatureHelpContext>(encoded)
        decoded.triggerCharacter shouldBe ","
    }

    @Test
    fun `SignatureHelpContext with active signature help`() {
        val activeHelp =
            SignatureHelp(
                signatures =
                    listOf(
                        SignatureInformation(label = "fun test(a: Int)"),
                    ),
                activeSignature = 0,
            )
        val context =
            SignatureHelpContext(
                triggerKind = SignatureHelpTriggerKind.ContentChange,
                isRetrigger = true,
                activeSignatureHelp = activeHelp,
            )
        val encoded = json.encodeToString(context)
        val decoded = json.decodeFromString<SignatureHelpContext>(encoded)
        decoded.activeSignatureHelp?.signatures?.size shouldBe 1
    }

    // ==================== SignatureHelp Tests ====================

    @Test
    fun `SignatureHelp minimal`() {
        val help =
            SignatureHelp(
                signatures = emptyList(),
            )
        val encoded = json.encodeToString(help)
        val decoded = json.decodeFromString<SignatureHelp>(encoded)
        decoded.signatures shouldBe emptyList()
        decoded.activeSignature shouldBe null
        decoded.activeParameter shouldBe null
    }

    @Test
    fun `SignatureHelp with single signature`() {
        val help =
            SignatureHelp(
                signatures =
                    listOf(
                        SignatureInformation(
                            label = "fun greet(name: String): String",
                        ),
                    ),
                activeSignature = 0,
            )
        val encoded = json.encodeToString(help)
        val decoded = json.decodeFromString<SignatureHelp>(encoded)
        decoded.signatures.size shouldBe 1
        decoded.activeSignature shouldBe 0
    }

    @Test
    fun `SignatureHelp with multiple signatures and active parameter`() {
        val help =
            SignatureHelp(
                signatures =
                    listOf(
                        SignatureInformation(label = "fun test()"),
                        SignatureInformation(label = "fun test(a: Int)"),
                        SignatureInformation(label = "fun test(a: Int, b: String)"),
                    ),
                activeSignature = 2,
                activeParameter = 1,
            )
        val encoded = json.encodeToString(help)
        val decoded = json.decodeFromString<SignatureHelp>(encoded)
        decoded.signatures.size shouldBe 3
        decoded.activeSignature shouldBe 2
        decoded.activeParameter shouldBe 1
    }

    @Test
    fun `SignatureHelp round-trip`() {
        val help =
            SignatureHelp(
                signatures =
                    listOf(
                        SignatureInformation(
                            label = "fun calc(x: Int, y: Int): Int",
                            documentation =
                                Either.right(
                                    MarkupContent(
                                        kind = MarkupKind.Markdown,
                                        value = "Calculates something",
                                    ),
                                ),
                            parameters =
                                listOf(
                                    ParameterInformation(label = Either.left("x: Int")),
                                    ParameterInformation(label = Either.left("y: Int")),
                                ),
                        ),
                    ),
                activeSignature = 0,
                activeParameter = 0,
            )
        val encoded = json.encodeToString(help)
        val decoded = json.decodeFromString<SignatureHelp>(encoded)
        decoded shouldBe help
    }

    // ==================== SignatureInformation Tests ====================

    @Test
    fun `SignatureInformation minimal`() {
        val info =
            SignatureInformation(
                label = "fun example()",
            )
        val encoded = json.encodeToString(info)
        val decoded = json.decodeFromString<SignatureInformation>(encoded)
        decoded.label shouldBe "fun example()"
        decoded.documentation shouldBe null
        decoded.parameters shouldBe null
        decoded.activeParameter shouldBe null
    }

    @Test
    fun `SignatureInformation with documentation`() {
        val info =
            SignatureInformation(
                label = "fun format(template: String, vararg args: Any): String",
                documentation =
                    Either.right(
                        MarkupContent(
                            kind = MarkupKind.PlainText,
                            value = "Formats a string using the provided template and arguments",
                        ),
                    ),
            )
        val encoded = json.encodeToString(info)
        val decoded = json.decodeFromString<SignatureInformation>(encoded)
        decoded.documentation?.right?.kind shouldBe MarkupKind.PlainText
        decoded.documentation?.right?.value shouldBe "Formats a string using the provided template and arguments"
    }

    @Test
    fun `SignatureInformation with parameters`() {
        val info =
            SignatureInformation(
                label = "fun copy(src: File, dest: File, overwrite: Boolean = false)",
                parameters =
                    listOf(
                        ParameterInformation(label = Either.left("src: File")),
                        ParameterInformation(label = Either.left("dest: File")),
                        ParameterInformation(label = Either.left("overwrite: Boolean = false")),
                    ),
            )
        val encoded = json.encodeToString(info)
        val decoded = json.decodeFromString<SignatureInformation>(encoded)
        decoded.parameters?.size shouldBe 3
    }

    @Test
    fun `SignatureInformation full`() {
        val info =
            SignatureInformation(
                label = "fun process(data: ByteArray): Result",
                documentation =
                    Either.right(
                        MarkupContent(
                            kind = MarkupKind.Markdown,
                            value = "Processes the binary data.\n\n**Returns:** Processing result",
                        ),
                    ),
                parameters =
                    listOf(
                        ParameterInformation(
                            label = Either.left("data: ByteArray"),
                            documentation =
                                Either.right(
                                    MarkupContent(
                                        kind = MarkupKind.PlainText,
                                        value = "The data to process",
                                    ),
                                ),
                        ),
                    ),
                activeParameter = 0,
            )
        val encoded = json.encodeToString(info)
        val decoded = json.decodeFromString<SignatureInformation>(encoded)
        decoded.activeParameter shouldBe 0
        decoded.parameters
            ?.first()
            ?.documentation
            ?.right
            ?.value shouldBe "The data to process"
    }

    // ==================== ParameterInformation Tests ====================

    @Test
    fun `ParameterInformation minimal`() {
        val param =
            ParameterInformation(
                label = Either.left("count: Int"),
            )
        val encoded = json.encodeToString(param)
        val decoded = json.decodeFromString<ParameterInformation>(encoded)
        decoded.label shouldBe Either.left("count: Int")
        decoded.documentation shouldBe null
    }

    @Test
    fun `ParameterInformation with documentation`() {
        val param =
            ParameterInformation(
                label = Either.left("timeout: Duration"),
                documentation =
                    Either.right(
                        MarkupContent(
                            kind = MarkupKind.Markdown,
                            value = "The maximum time to wait for completion",
                        ),
                    ),
            )
        val encoded = json.encodeToString(param)
        val decoded = json.decodeFromString<ParameterInformation>(encoded)
        decoded.documentation?.right?.kind shouldBe MarkupKind.Markdown
    }

    @Test
    fun `ParameterInformation round-trip`() {
        val param =
            ParameterInformation(
                label = Either.left("options: Map<String, Any>"),
                documentation =
                    Either.right(
                        MarkupContent(
                            kind = MarkupKind.PlainText,
                            value = "Configuration options",
                        ),
                    ),
            )
        val encoded = json.encodeToString(param)
        val decoded = json.decodeFromString<ParameterInformation>(encoded)
        decoded shouldBe param
    }

    @Test
    fun `ParameterInformation with complex label`() {
        val param =
            ParameterInformation(
                label = Either.left("callback: (result: Result<T>, error: Throwable?) -> Unit"),
            )
        val encoded = json.encodeToString(param)
        val decoded = json.decodeFromString<ParameterInformation>(encoded)
        decoded.label shouldBe Either.left("callback: (result: Result<T>, error: Throwable?) -> Unit")
    }
}
