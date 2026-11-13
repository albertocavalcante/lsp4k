package io.lsp4k.protocol

/**
 * Pure helpers for LSP line terminators.
 *
 * The specification recognises three terminators: `\n`, `\r\n` and a lone `\r`.
 * A `\r\n` pair is a single terminator, never two.
 */
public object LineEndings {
    /**
     * Returns the UTF-16 offset at which each line of [text] starts.
     * The first entry is always `0`; a trailing terminator yields a final empty line.
     */
    public fun lineStarts(text: CharSequence): IntArray {
        val starts = mutableListOf(0)
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c == '\r' && i + 1 < text.length && text[i + 1] == '\n') {
                i++
            }
            if (c == '\r' || c == '\n') {
                starts.add(i + 1)
            }
            i++
        }
        return starts.toIntArray()
    }

    /**
     * Returns the length of the terminator at the end of [line], or `0` when there is none.
     */
    public fun terminatorLength(line: CharSequence): Int =
        when {
            line.endsWith("\r\n") -> 2
            line.endsWith('\n') || line.endsWith('\r') -> 1
            else -> 0
        }
}

/**
 * Pure conversions between the three LSP position encodings for the text of a single line.
 *
 * A line is the text of one line **without** its terminator. Character offsets count code units of
 * the given encoding: UTF-16 code units, UTF-8 bytes or Unicode code points (UTF-32).
 *
 * Policies:
 * - Offsets below zero are treated as zero; offsets past the end of the line are clamped to the line end.
 * - An offset that falls inside a code point (for example between the two UTF-16 halves of a surrogate
 *   pair, or inside a multi-byte UTF-8 sequence) snaps to the start of that code point.
 * - A lone surrogate counts as one code point and is encoded as the 3 byte UTF-8 replacement sequence.
 * - An unknown [PositionEncodingKind] is treated as UTF-16, the mandatory default.
 */
public object PositionEncodings {
    private const val SUPPLEMENTARY_BASE = 0x10000
    private const val HIGH_SURROGATE_MIN = 0xD800
    private const val LOW_SURROGATE_MIN = 0xDC00
    private const val SURROGATE_SHIFT = 10
    private const val UTF8_ONE_BYTE_LIMIT = 0x80
    private const val UTF8_TWO_BYTE_LIMIT = 0x800

    /**
     * Converts [character], expressed in [encoding], to a UTF-16 index into [line].
     */
    public fun toUtf16Index(
        line: String,
        character: Int,
        encoding: PositionEncodingKind,
    ): Int {
        var index = 0
        var units = 0
        while (index < line.length && units < character) {
            val width = unitWidth(codePointOf(line, index), encoding)
            if (units + width > character) break
            units += width
            index += codePointLength(line, index)
        }
        return index
    }

    /**
     * Converts a UTF-16 [index] into [line] to a character offset in [encoding].
     */
    public fun fromUtf16Index(
        line: String,
        index: Int,
        encoding: PositionEncodingKind,
    ): Int {
        val limit = index.coerceIn(0, line.length)
        var i = 0
        var units = 0
        while (i < limit) {
            val length = codePointLength(line, i)
            if (i + length > limit) break
            units += unitWidth(codePointOf(line, i), encoding)
            i += length
        }
        return units
    }

    /**
     * Converts [character] on [line] from encoding [from] to encoding [to].
     */
    public fun convert(
        line: String,
        character: Int,
        from: PositionEncodingKind,
        to: PositionEncodingKind,
    ): Int = fromUtf16Index(line, toUtf16Index(line, character, from), to)

    /**
     * Length of [line] in [encoding] units.
     */
    public fun lineLength(
        line: String,
        encoding: PositionEncodingKind,
    ): Int = fromUtf16Index(line, line.length, encoding)

    private fun codePointLength(
        text: String,
        index: Int,
    ): Int = if (text[index].isHighSurrogate() && index + 1 < text.length && text[index + 1].isLowSurrogate()) 2 else 1

    private fun codePointOf(
        text: String,
        index: Int,
    ): Int {
        val high = text[index]
        return if (codePointLength(text, index) == 2) {
            SUPPLEMENTARY_BASE + ((high.code - HIGH_SURROGATE_MIN) shl SURROGATE_SHIFT) +
                (text[index + 1].code - LOW_SURROGATE_MIN)
        } else {
            high.code
        }
    }

    private fun unitWidth(
        codePoint: Int,
        encoding: PositionEncodingKind,
    ): Int =
        when (encoding) {
            PositionEncodingKind.UTF32 -> 1
            PositionEncodingKind.UTF8 -> utf8Width(codePoint)
            else -> if (codePoint >= SUPPLEMENTARY_BASE) 2 else 1
        }

    private fun utf8Width(codePoint: Int): Int =
        when {
            codePoint < UTF8_ONE_BYTE_LIMIT -> 1
            codePoint < UTF8_TWO_BYTE_LIMIT -> 2
            codePoint < SUPPLEMENTARY_BASE -> 3
            else -> 4
        }
}
