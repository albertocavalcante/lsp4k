package io.lsp4k.protocol

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * Wire-shape tests for LSP 3.18 text document edits, workspace edit metadata, the
 * `workspace/textDocumentContent` family and document filters. They use a Json equivalent to
 * jsonrpc4k `JsonRpcCodec.defaultJson`.
 */
class EditsContentFiltersSerializationTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
            isLenient = true
            coerceInputValues = true
        }

    private val range = Range(Position(0, 0), Position(0, 1))
    private val rangeJson = """{"start":{"line":0,"character":0},"end":{"line":0,"character":1}}"""

    // ==================== TextDocumentEdit.edits ====================

    @Test
    fun `text edit encodes without annotation or snippet`() {
        json.encodeToString<DocumentTextEdit>(TextEdit(range, "x")) shouldBe
            """{"range":$rangeJson,"newText":"x"}"""
    }

    @Test
    fun `annotated text edit encodes its annotation id`() {
        json.encodeToString<DocumentTextEdit>(AnnotatedTextEdit(range, "x", "a1")) shouldBe
            """{"range":$rangeJson,"newText":"x","annotationId":"a1"}"""
    }

    @Test
    fun `snippet text edit always encodes the snippet kind`() {
        json.encodeToString<DocumentTextEdit>(SnippetTextEdit(range, StringValue(value = "\${1:x}"), "a1")) shouldBe
            """{"range":$rangeJson,"snippet":{"kind":"snippet","value":"${'$'}{1:x}"},"annotationId":"a1"}"""
        json.encodeToString<DocumentTextEdit>(SnippetTextEdit(range, StringValue(value = "y"))) shouldBe
            """{"range":$rangeJson,"snippet":{"kind":"snippet","value":"y"}}"""
    }

    @Test
    fun `text document edit decodes a mixed list of the three edit forms`() {
        val text =
            """
            {"textDocument":{"uri":"file:///a","version":null},"edits":[
              {"range":$rangeJson,"newText":"t"},
              {"range":$rangeJson,"newText":"a","annotationId":"id"},
              {"range":$rangeJson,"snippet":{"kind":"snippet","value":"s"}}
            ]}
            """.trimIndent()
        val decoded = json.decodeFromString<TextDocumentEdit>(text)
        decoded.edits shouldBe
            listOf(
                TextEdit(range, "t"),
                AnnotatedTextEdit(range, "a", "id"),
                SnippetTextEdit(range, StringValue(value = "s")),
            )
        json.decodeFromString<TextDocumentEdit>(json.encodeToString(decoded)) shouldBe decoded
        decoded.edits[2].shouldBeInstanceOf<SnippetTextEdit>()
    }

    @Test
    fun `document text edit rejects invalid shapes`() {
        listOf("""{"range":$rangeJson}""", """{"range":$rangeJson,"newText":"x","snippet":{"value":"y"}}""", "\"x\"", "1")
            .forEach { text ->
                assertFailsWith<SerializationException> { json.decodeFromString(DocumentTextEditSerializer, text) }
            }
    }

    // ==================== Workspace edit metadata ====================

    @Test
    fun `apply workspace edit params carries metadata`() {
        val params =
            ApplyWorkspaceEditParams(
                edit = WorkspaceEdit(),
                metadata = WorkspaceEditMetadata(isRefactoring = true),
            )
        val encoded = json.encodeToString(params)
        encoded shouldBe """{"edit":{},"metadata":{"isRefactoring":true}}"""
        json.decodeFromString<ApplyWorkspaceEditParams>(encoded) shouldBe params
        json.decodeFromString<ApplyWorkspaceEditParams>("""{"edit":{}}""").metadata shouldBe null
    }

    @Test
    fun `workspace edit client capabilities carry metadata and snippet support`() {
        val capabilities = WorkspaceEditClientCapabilities(metadataSupport = true, snippetEditSupport = false)
        val encoded = json.encodeToString(capabilities)
        encoded shouldBe """{"metadataSupport":true,"snippetEditSupport":false}"""
        json.decodeFromString<WorkspaceEditClientCapabilities>(encoded) shouldBe capabilities
    }

    // ==================== workspace/textDocumentContent ====================

    @Test
    fun `text document content messages use the spec property names`() {
        json.encodeToString(TextDocumentContentParams("git:/a")) shouldBe """{"uri":"git:/a"}"""
        json.encodeToString(TextDocumentContentResult("body")) shouldBe """{"text":"body"}"""
        json.encodeToString(TextDocumentContentRefreshParams("git:/a")) shouldBe """{"uri":"git:/a"}"""
        json.encodeToString(TextDocumentContentOptions(listOf("git"))) shouldBe """{"schemes":["git"]}"""
        json.encodeToString(TextDocumentContentRegistrationOptions(listOf("git"), "1")) shouldBe
            """{"schemes":["git"],"id":"1"}"""
        json.encodeToString(TextDocumentContentClientCapabilities(dynamicRegistration = true)) shouldBe
            """{"dynamicRegistration":true}"""
    }

    @Test
    fun `text document content method constants match the spec`() {
        LspMethods.WORKSPACE_TEXT_DOCUMENT_CONTENT shouldBe "workspace/textDocumentContent"
        LspMethods.WORKSPACE_TEXT_DOCUMENT_CONTENT_REFRESH shouldBe "workspace/textDocumentContent/refresh"
    }

    @Test
    fun `server workspace capabilities decode both text document content forms`() {
        val options =
            json.decodeFromString<ServerWorkspaceCapabilities>("""{"textDocumentContent":{"schemes":["git"]}}""")
        options.textDocumentContent shouldBe Either.Left(TextDocumentContentOptions(listOf("git")))

        val registration =
            json.decodeFromString<ServerWorkspaceCapabilities>(
                """{"textDocumentContent":{"schemes":["git"],"id":"1"}}""",
            )
        registration.textDocumentContent shouldBe
            Either.Right(TextDocumentContentRegistrationOptions(listOf("git"), "1"))

        json.encodeToString(options) shouldBe """{"textDocumentContent":{"schemes":["git"]}}"""
        json.encodeToString(registration) shouldBe """{"textDocumentContent":{"schemes":["git"],"id":"1"}}"""
    }

    @Test
    fun `server workspace capabilities reject a non object text document content`() {
        assertFailsWith<SerializationException> {
            json.decodeFromString<ServerWorkspaceCapabilities>("""{"textDocumentContent":true}""")
        }
    }

    @Test
    fun `workspace client capabilities carry text document content`() {
        val capabilities =
            WorkspaceClientCapabilities(
                textDocumentContent = TextDocumentContentClientCapabilities(dynamicRegistration = true),
            )
        val encoded = json.encodeToString(capabilities)
        encoded shouldBe """{"textDocumentContent":{"dynamicRegistration":true}}"""
        json.decodeFromString<WorkspaceClientCapabilities>(encoded) shouldBe capabilities
    }

    // ==================== GlobPattern and RelativePattern ====================

    @Test
    fun `glob pattern decodes a string and a relative pattern`() {
        json.decodeFromString(GlobPatternSerializer, "\"**/*.kt\"") shouldBe Either.Left("**/*.kt")

        val withUri = json.decodeFromString(GlobPatternSerializer, """{"baseUri":"file:///w","pattern":"*.kt"}""")
        withUri shouldBe Either.Right(RelativePattern(Either.Right("file:///w"), "*.kt"))

        val withFolder =
            json.decodeFromString(
                GlobPatternSerializer,
                """{"baseUri":{"uri":"file:///w","name":"w"},"pattern":"*.kt"}""",
            )
        withFolder shouldBe
            Either.Right(RelativePattern(Either.Left(WorkspaceFolder("file:///w", "w")), "*.kt"))
    }

    @Test
    fun `glob pattern round trips on the wire`() {
        val folder: GlobPattern = Either.Right(RelativePattern(Either.Left(WorkspaceFolder("file:///w", "w")), "*.kt"))
        val uri: GlobPattern = Either.Right(RelativePattern(Either.Right("file:///w"), "*.kt"))
        json.encodeToString(GlobPatternSerializer, folder) shouldBe
            """{"baseUri":{"uri":"file:///w","name":"w"},"pattern":"*.kt"}"""
        json.encodeToString(GlobPatternSerializer, uri) shouldBe """{"baseUri":"file:///w","pattern":"*.kt"}"""
        json.encodeToString(GlobPatternSerializer, Either.Left("*.kt")) shouldBe "\"*.kt\""
        listOf(folder, uri, Either.Left("*.kt")).forEach { pattern ->
            json.decodeFromString(GlobPatternSerializer, json.encodeToString(GlobPatternSerializer, pattern)) shouldBe
                pattern
        }
    }

    @Test
    fun `glob pattern rejects numbers and booleans and arrays`() {
        listOf("5", "true", "[]", "null").forEach { text ->
            assertFailsWith<SerializationException> { json.decodeFromString(GlobPatternSerializer, text) }
        }
    }

    @Test
    fun `relative pattern rejects a base uri that is neither a folder nor a string`() {
        assertFailsWith<SerializationException> {
            json.decodeFromString<RelativePattern>("""{"baseUri":5,"pattern":"*.kt"}""")
        }
    }

    @Test
    fun `file system watcher accepts a relative pattern`() {
        val watcher = FileSystemWatcher(Either.Right(RelativePattern(Either.Right("file:///w"), "*.kt")), kind = 7)
        val encoded = json.encodeToString(watcher)
        encoded shouldBe """{"globPattern":{"baseUri":"file:///w","pattern":"*.kt"},"kind":7}"""
        json.decodeFromString<FileSystemWatcher>(encoded) shouldBe watcher
        json.decodeFromString<FileSystemWatcher>("""{"globPattern":"**/*.kt"}""").globPattern shouldBe
            Either.Left("**/*.kt")
    }

    // ==================== DocumentFilter ====================

    @Test
    fun `document filter decodes text and notebook cell forms`() {
        json.decodeFromString<DocumentFilter>("""{"language":"kotlin"}""") shouldBe
            TextDocumentFilter(language = "kotlin")
        json.decodeFromString<DocumentFilter>("""{"scheme":"file"}""") shouldBe TextDocumentFilter(scheme = "file")
        json.decodeFromString<DocumentFilter>("""{"pattern":"**/*.kt"}""") shouldBe
            TextDocumentFilter(pattern = Either.Left("**/*.kt"))
        json.decodeFromString<DocumentFilter>("""{"language":"python","notebook":"jupyter"}""") shouldBe
            NotebookCellTextDocumentFilter(Either.Left("jupyter"), "python")
        json.decodeFromString<DocumentFilter>("""{"notebook":{"notebookType":"jupyter","scheme":"file"}}""") shouldBe
            NotebookCellTextDocumentFilter(Either.Right(NotebookDocumentFilter(notebookType = "jupyter", scheme = "file")))
    }

    @Test
    fun `document filter encodes both forms without extra members`() {
        json.encodeToString<DocumentFilter>(TextDocumentFilter(language = "kotlin", scheme = "file")) shouldBe
            """{"language":"kotlin","scheme":"file"}"""
        json.encodeToString<DocumentFilter>(NotebookCellTextDocumentFilter(Either.Left("*"), "python")) shouldBe
            """{"notebook":"*","language":"python"}"""
    }

    @Test
    fun `document filter supports relative patterns`() {
        val filter =
            TextDocumentFilter(
                language = "kotlin",
                pattern = Either.Right(RelativePattern(Either.Right("file:///w"), "*.kt")),
            )
        val encoded = json.encodeToString<DocumentFilter>(filter)
        encoded shouldBe """{"language":"kotlin","pattern":{"baseUri":"file:///w","pattern":"*.kt"}}"""
        json.decodeFromString<DocumentFilter>(encoded) shouldBe filter
    }

    @Test
    fun `document filter rejects empty shapes and non objects`() {
        listOf("{}", """{"unknown":1}""", "\"typescript\"", "1", "[]").forEach { text ->
            assertFailsWith<IllegalArgumentException> { json.decodeFromString<DocumentFilter>(text) }
        }
        assertFailsWith<IllegalArgumentException> { TextDocumentFilter() }
    }

    @Test
    fun `notebook cell filter requires a notebook`() {
        assertFailsWith<SerializationException> {
            json.decodeFromString<NotebookCellTextDocumentFilter>("""{"language":"python"}""")
        }
        assertFailsWith<SerializationException> {
            json.decodeFromString<NotebookCellTextDocumentFilter>("""{"notebook":5}""")
        }
    }

    @Test
    fun `notebook document filter rejects an empty shape and accepts a glob pattern`() {
        assertFailsWith<IllegalArgumentException> { NotebookDocumentFilter() }
        assertFailsWith<IllegalArgumentException> { json.decodeFromString<NotebookDocumentFilter>("{}") }
        val filter = NotebookDocumentFilter(pattern = Either.Right(RelativePattern(Either.Right("file:///w"), "*.ipynb")))
        json.decodeFromString<NotebookDocumentFilter>(json.encodeToString(filter)) shouldBe filter
    }

    @Test
    fun `notebook selector decodes strict notebook forms`() {
        json.decodeFromString<NotebookSelector>("""{"notebook":"jupyter"}""").notebook shouldBe Either.Left("jupyter")
        json.decodeFromString<NotebookSelector>("""{"cells":[{"language":"python"}]}""").cells?.size shouldBe 1
        assertFailsWith<SerializationException> { json.decodeFromString<NotebookSelector>("""{"notebook":5}""") }
        assertFailsWith<IllegalArgumentException> { json.decodeFromString<NotebookSelector>("{}") }
    }

    // ==================== TextDocumentFilterClientCapabilities ====================

    @Test
    fun `text document client capabilities carry filter capabilities`() {
        val capabilities =
            TextDocumentClientCapabilities(filters = TextDocumentFilterClientCapabilities(relativePatternSupport = true))
        val encoded = json.encodeToString(capabilities)
        encoded shouldBe """{"filters":{"relativePatternSupport":true}}"""
        json.decodeFromString<TextDocumentClientCapabilities>(encoded) shouldBe capabilities
    }
}
