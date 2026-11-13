package io.lsp4k.server

import io.lsp4k.protocol.DocumentUri
import io.lsp4k.protocol.LineEndings
import io.lsp4k.protocol.Position
import io.lsp4k.protocol.PositionEncodingKind
import io.lsp4k.protocol.PositionEncodings
import io.lsp4k.protocol.Range

/**
 * An immutable snapshot of an open text document (all state is fixed at construction, so a snapshot
 * is safe to share across coroutines). Instances are created by [TextDocuments].
 *
 * [Position] characters follow [encoding]; offsets returned by [offsetAt] and accepted by [positionAt]
 * are always UTF-16 indices into [text] (Kotlin string indices). Out-of-range input is clamped:
 * lines past the end map to the end of the text, characters past the end of a line map to the line end
 * (before its terminator), as the specification prescribes.
 */
public class TextDocument internal constructor(
    public val uri: DocumentUri,
    public val languageId: String,
    public val version: Int,
    public val text: String,
    public val encoding: PositionEncodingKind = PositionEncodingKind.UTF16,
) {
    private val lineStarts: IntArray  = LineEndings.lineStarts(text)

    /** Number of lines, counting a final empty line after a trailing terminator. */
    public val lineCount: Int get() = lineStarts.size

    /** Text of [line] without its terminator, or an empty string when [line] does not exist. */
    public fun lineText(line: Int): String {
        if (line < 0 || line >= lineCount) return ""
        return text.substring(lineStarts[line], contentEnd(line))
    }

    /** Converts [position] (in [encoding]) to a UTF-16 offset into [text]. */
    public fun offsetAt(position: Position): Int {
        if (position.line >= lineCount) return text.length
        if (position.line < 0) return 0
        val start = lineStarts[position.line]
        return start + PositionEncodings.toUtf16Index(lineText(position.line), position.character, encoding)
    }

    /**
     * Converts a UTF-16 [offset] into [text] to a [Position] (in [encoding]).
     * An offset inside a line terminator maps to the end of that line.
     */
    public fun positionAt(offset: Int): Position {
        val clamped = offset.coerceIn(0, text.length)
        val line = lineContaining(clamped)
        val within = (clamped - lineStarts[line]).coerceAtMost(contentEnd(line) - lineStarts[line])
        return Position(line, PositionEncodings.fromUtf16Index(lineText(line), within, encoding))
    }

    /** Returns the text inside [range], or the whole text when [range] is null. */
    public fun getText(range: Range? = null): String {
        if (range == null) return text
        val (start, end) = orderedOffsets(range)
        return text.substring(start, end)
    }

    internal fun withText(
        version: Int,
        newText: String,
    ): TextDocument = TextDocument(uri, languageId, version, newText, encoding)

    internal fun withEdit(
        range: Range,
        replacement: String,
    ): TextDocument {
        val (start, end) = orderedOffsets(range)
        return withText(version, text.substring(0, start) + replacement + text.substring(end))
    }

    private fun orderedOffsets(range: Range): Pair<Int, Int> {
        val a = offsetAt(range.start)
        val b = offsetAt(range.end)
        return if (a <= b) a to b else b to a
    }

    private fun contentEnd(line: Int): Int {
        val end = if (line + 1 < lineCount) lineStarts[line + 1] else text.length
        return end - terminatorLengthBefore(lineStarts[line], end)
    }

    private fun terminatorLengthBefore(
        start: Int,
        end: Int,
    ): Int {
        if (end <= start) return 0
        val last = text[end - 1]
        return when {
            last == '\n' && end - 1 > start && text[end - 2] == '\r' -> 2
            last == '\n' || last == '\r' -> 1
            else -> 0
        }
    }

    private fun lineContaining(offset: Int): Int {
        var low = 0
        var high = lineCount - 1
        while (low < high) {
            val mid = (low + high + 1) ushr 1
            if (lineStarts[mid] <= offset) low = mid else high = mid - 1
        }
        return low
    }

    override fun toString(): String = "TextDocument(uri=$uri, version=$version, languageId=$languageId)"
}
