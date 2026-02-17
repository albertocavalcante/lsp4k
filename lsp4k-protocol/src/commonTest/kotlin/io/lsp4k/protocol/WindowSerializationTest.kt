package io.lsp4k.protocol

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * Tests for Window-related types including:
 * - MessageType (integer-serialized enum)
 * - ShowMessageParams, LogMessageParams
 * - ShowMessageRequestParams, MessageActionItem
 * - WorkDoneProgress types (Begin, Report, End)
 * - ProgressParams
 */
class WindowSerializationTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

    // ==================== MessageType Tests ====================

    @Test
    fun `MessageType Error serializes to 1`() {
        val encoded = json.encodeToString(MessageType.Error)
        encoded shouldBe "1"
    }

    @Test
    fun `MessageType Warning serializes to 2`() {
        val encoded = json.encodeToString(MessageType.Warning)
        encoded shouldBe "2"
    }

    @Test
    fun `MessageType Info serializes to 3`() {
        val encoded = json.encodeToString(MessageType.Info)
        encoded shouldBe "3"
    }

    @Test
    fun `MessageType Log serializes to 4`() {
        val encoded = json.encodeToString(MessageType.Log)
        encoded shouldBe "4"
    }

    @Test
    fun `MessageType deserializes from integers`() {
        json.decodeFromString<MessageType>("1") shouldBe MessageType.Error
        json.decodeFromString<MessageType>("2") shouldBe MessageType.Warning
        json.decodeFromString<MessageType>("3") shouldBe MessageType.Info
        json.decodeFromString<MessageType>("4") shouldBe MessageType.Log
    }

    @Test
    fun `MessageType fromValue throws for unknown value`() {
        assertFailsWith<IllegalArgumentException> {
            MessageType.fromValue(99)
        }
    }

    // ==================== ShowMessageParams Tests ====================

    @Test
    fun `ShowMessageParams serialization`() {
        val params =
            ShowMessageParams(
                type = MessageType.Error,
                message = "An error occurred",
            )
        val encoded = json.encodeToString(params)
        encoded shouldContain "\"type\":1"
        encoded shouldContain "\"message\":\"An error occurred\""
    }

    @Test
    fun `ShowMessageParams round-trip`() {
        val params =
            ShowMessageParams(
                type = MessageType.Warning,
                message = "This is a warning",
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<ShowMessageParams>(encoded)
        decoded shouldBe params
    }

    @Test
    fun `ShowMessageParams with unicode message`() {
        val params =
            ShowMessageParams(
                type = MessageType.Info,
                message = "情報メッセージ 🎉",
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<ShowMessageParams>(encoded)
        decoded.message shouldBe "情報メッセージ 🎉"
    }

    // ==================== LogMessageParams Tests ====================

    @Test
    fun `LogMessageParams serialization`() {
        val params =
            LogMessageParams(
                type = MessageType.Log,
                message = "Debug log entry",
            )
        val encoded = json.encodeToString(params)
        encoded shouldContain "\"type\":4"
        encoded shouldContain "\"message\":\"Debug log entry\""
    }

    @Test
    fun `LogMessageParams round-trip`() {
        val params =
            LogMessageParams(
                type = MessageType.Info,
                message = "Information log",
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<LogMessageParams>(encoded)
        decoded shouldBe params
    }

    // ==================== ShowMessageRequestParams Tests ====================

    @Test
    fun `ShowMessageRequestParams without actions`() {
        val params =
            ShowMessageRequestParams(
                type = MessageType.Warning,
                message = "Do you want to continue?",
            )
        val encoded = json.encodeToString(params)
        encoded shouldContain "\"type\":2"
        encoded shouldContain "\"message\":\"Do you want to continue?\""
    }

    @Test
    fun `ShowMessageRequestParams with actions`() {
        val params =
            ShowMessageRequestParams(
                type = MessageType.Error,
                message = "Operation failed",
                actions =
                    listOf(
                        MessageActionItem(title = "Retry"),
                        MessageActionItem(title = "Cancel"),
                        MessageActionItem(title = "Open Log"),
                    ),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<ShowMessageRequestParams>(encoded)
        decoded.actions?.size shouldBe 3
        decoded.actions?.get(0)?.title shouldBe "Retry"
        decoded.actions?.get(1)?.title shouldBe "Cancel"
        decoded.actions?.get(2)?.title shouldBe "Open Log"
    }

    @Test
    fun `ShowMessageRequestParams round-trip`() {
        val params =
            ShowMessageRequestParams(
                type = MessageType.Info,
                message = "Select an option",
                actions =
                    listOf(
                        MessageActionItem(title = "Yes"),
                        MessageActionItem(title = "No"),
                    ),
            )
        val encoded = json.encodeToString(params)
        val decoded = json.decodeFromString<ShowMessageRequestParams>(encoded)
        decoded shouldBe params
    }

    // ==================== MessageActionItem Tests ====================

    @Test
    fun `MessageActionItem serialization`() {
        val item = MessageActionItem(title = "Retry")
        val encoded = json.encodeToString(item)
        encoded shouldBe """{"title":"Retry"}"""
    }

    @Test
    fun `MessageActionItem with special characters`() {
        val item = MessageActionItem(title = "Save & Continue")
        val encoded = json.encodeToString(item)
        val decoded = json.decodeFromString<MessageActionItem>(encoded)
        decoded.title shouldBe "Save & Continue"
    }

    // ==================== WorkDoneProgressBegin Tests ====================

    @Test
    fun `WorkDoneProgressBegin minimal`() {
        val progress =
            WorkDoneProgressBegin(
                title = "Indexing",
            )
        val encoded = json.encodeToString(progress)
        encoded shouldContain "\"title\":\"Indexing\""
        // kind has default value "begin" and may not be in output when encodeDefaults=false
        val decoded = json.decodeFromString<WorkDoneProgressBegin>(encoded)
        decoded.kind shouldBe "begin"
        decoded.title shouldBe "Indexing"
    }

    @Test
    fun `WorkDoneProgressBegin full`() {
        val progress =
            WorkDoneProgressBegin(
                title = "Building project",
                cancellable = true,
                message = "Compiling sources...",
                percentage = 0,
            )
        val encoded = json.encodeToString(progress)
        val decoded = json.decodeFromString<WorkDoneProgressBegin>(encoded)
        decoded.kind shouldBe "begin"
        decoded.title shouldBe "Building project"
        decoded.cancellable shouldBe true
        decoded.message shouldBe "Compiling sources..."
        decoded.percentage shouldBe 0
    }

    @Test
    fun `WorkDoneProgressBegin percentage boundaries`() {
        val progress0 = WorkDoneProgressBegin(title = "Test", percentage = 0)
        val progress100 = WorkDoneProgressBegin(title = "Test", percentage = 100)

        json.decodeFromString<WorkDoneProgressBegin>(json.encodeToString(progress0)).percentage shouldBe 0
        json.decodeFromString<WorkDoneProgressBegin>(json.encodeToString(progress100)).percentage shouldBe 100
    }

    // ==================== WorkDoneProgressReport Tests ====================

    @Test
    fun `WorkDoneProgressReport minimal`() {
        val progress = WorkDoneProgressReport()
        val encoded = json.encodeToString(progress)
        // kind has default value "report" and may not be in output when encodeDefaults=false
        val decoded = json.decodeFromString<WorkDoneProgressReport>(encoded)
        decoded.kind shouldBe "report"
    }

    @Test
    fun `WorkDoneProgressReport full`() {
        val progress =
            WorkDoneProgressReport(
                cancellable = false,
                message = "50% complete",
                percentage = 50,
            )
        val encoded = json.encodeToString(progress)
        val decoded = json.decodeFromString<WorkDoneProgressReport>(encoded)
        decoded.kind shouldBe "report"
        decoded.cancellable shouldBe false
        decoded.message shouldBe "50% complete"
        decoded.percentage shouldBe 50
    }

    @Test
    fun `WorkDoneProgressReport round-trip`() {
        val progress =
            WorkDoneProgressReport(
                message = "Processing files...",
                percentage = 75,
            )
        val encoded = json.encodeToString(progress)
        val decoded = json.decodeFromString<WorkDoneProgressReport>(encoded)
        decoded shouldBe progress
    }

    // ==================== WorkDoneProgressEnd Tests ====================

    @Test
    fun `WorkDoneProgressEnd minimal`() {
        val progress = WorkDoneProgressEnd()
        val encoded = json.encodeToString(progress)
        // kind has default value "end" and may not be in output when encodeDefaults=false
        val decoded = json.decodeFromString<WorkDoneProgressEnd>(encoded)
        decoded.kind shouldBe "end"
    }

    @Test
    fun `WorkDoneProgressEnd with message`() {
        val progress =
            WorkDoneProgressEnd(
                message = "Build completed successfully",
            )
        val encoded = json.encodeToString(progress)
        val decoded = json.decodeFromString<WorkDoneProgressEnd>(encoded)
        decoded.kind shouldBe "end"
        decoded.message shouldBe "Build completed successfully"
    }

    @Test
    fun `WorkDoneProgressEnd round-trip`() {
        val progress = WorkDoneProgressEnd(message = "Done!")
        val encoded = json.encodeToString(progress)
        val decoded = json.decodeFromString<WorkDoneProgressEnd>(encoded)
        decoded shouldBe progress
    }

    // ==================== WorkDoneProgressCreateParams Tests ====================

    @Test
    fun `WorkDoneProgressCreateParams with string token`() {
        val params =
            WorkDoneProgressCreateParams(
                token = Either.right("unique-token-123"),
            )
        val encoded = json.encodeToString(params)
        encoded shouldContain "\"token\":\"unique-token-123\""
    }

    // ==================== ProgressParams Tests ====================

    @Test
    fun `ProgressParams with WorkDoneProgressBegin`() {
        val params =
            ProgressParams(
                token = Either.right("build-progress"),
                value = WorkDoneProgressBegin(title = "Building"),
            )
        val encoded = json.encodeToString(params)
        encoded shouldContain "\"token\":\"build-progress\""
        encoded shouldContain "\"title\":\"Building\""
    }

    @Test
    fun `ProgressParams with WorkDoneProgressReport`() {
        val params =
            ProgressParams(
                token = Either.right("task-1"),
                value = WorkDoneProgressReport(percentage = 25),
            )
        val encoded = json.encodeToString(params)
        encoded shouldContain "\"token\":\"task-1\""
        encoded shouldContain "\"percentage\":25"
    }

    @Test
    fun `ProgressParams with WorkDoneProgressEnd`() {
        val params =
            ProgressParams(
                token = Either.right("task-2"),
                value = WorkDoneProgressEnd(message = "Complete"),
            )
        val encoded = json.encodeToString(params)
        encoded shouldContain "\"token\":\"task-2\""
        encoded shouldContain "\"message\":\"Complete\""
    }
}
