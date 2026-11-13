package io.lsp4k.protocol

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class PositionEncodingTest {
    private val u8 = PositionEncodingKind.UTF8
    private val u16 = PositionEncodingKind.UTF16
    private val u32 = PositionEncodingKind.UTF32

    // a e-acute euro G-clef(surrogate pair) CJK b
    private val line = "aé€𝄞中b"

    @Test
    fun `utf16 index equals character for utf16`() {
        for (i in listOf(0, 1, 2, 3, 5, 6, 7)) {
            PositionEncodings.toUtf16Index(line, i, u16) shouldBe i
            PositionEncodings.fromUtf16Index(line, i, u16) shouldBe i
        }
    }

    @Test
    fun `utf8 byte offsets map to utf16 indices`() {
        val pairs = listOf(0 to 0, 1 to 1, 3 to 2, 6 to 3, 10 to 5, 13 to 6, 14 to 7)
        for ((bytes, utf16) in pairs) {
            PositionEncodings.toUtf16Index(line, bytes, u8) shouldBe utf16
            PositionEncodings.fromUtf16Index(line, utf16, u8) shouldBe bytes
        }
    }

    @Test
    fun `utf32 code point offsets map to utf16 indices`() {
        val pairs = listOf(0 to 0, 1 to 1, 2 to 2, 3 to 3, 4 to 5, 5 to 6, 6 to 7)
        for ((points, utf16) in pairs) {
            PositionEncodings.toUtf16Index(line, points, u32) shouldBe utf16
            PositionEncodings.fromUtf16Index(line, utf16, u32) shouldBe points
        }
    }

    @Test
    fun `convert between all encodings`() {
        PositionEncodings.convert(line, 6, u8, u16) shouldBe 3
        PositionEncodings.convert(line, 3, u16, u8) shouldBe 6
        PositionEncodings.convert(line, 5, u16, u32) shouldBe 4
        PositionEncodings.convert(line, 4, u32, u8) shouldBe 10
        PositionEncodings.convert(line, 14, u8, u32) shouldBe 6
    }

    @Test
    fun `line length per encoding`() {
        PositionEncodings.lineLength(line, u16) shouldBe 7
        PositionEncodings.lineLength(line, u8) shouldBe 14
        PositionEncodings.lineLength(line, u32) shouldBe 6
    }

    @Test
    fun `offsets past the end clamp and negatives become zero`() {
        PositionEncodings.toUtf16Index(line, 99, u16) shouldBe 7
        PositionEncodings.toUtf16Index(line, 99, u8) shouldBe 7
        PositionEncodings.toUtf16Index(line, -4, u32) shouldBe 0
        PositionEncodings.fromUtf16Index(line, 99, u8) shouldBe 14
        PositionEncodings.fromUtf16Index(line, -1, u8) shouldBe 0
    }

    @Test
    fun `offset inside a code point snaps to its start`() {
        // utf16 index 4 is between the surrogate halves
        PositionEncodings.fromUtf16Index(line, 4, u8) shouldBe 6
        PositionEncodings.fromUtf16Index(line, 4, u32) shouldBe 3
        // utf8 byte 8 is inside the 4 byte G-clef, utf16 character 4 as well
        PositionEncodings.toUtf16Index(line, 8, u8) shouldBe 3
        PositionEncodings.toUtf16Index(line, 4, u16) shouldBe 3
        // utf8 byte 2 is inside the euro sign
        PositionEncodings.toUtf16Index(line, 2, u8) shouldBe 1
    }

    @Test
    fun `lone surrogates count as one code point`() {
        val broken = "\uD834x\uDD1E"
        PositionEncodings.lineLength(broken, u32) shouldBe 3
        PositionEncodings.lineLength(broken, u16) shouldBe 3
        PositionEncodings.lineLength(broken, u8) shouldBe 7
    }

    @Test
    fun `empty line`() {
        PositionEncodings.toUtf16Index("", 3, u8) shouldBe 0
        PositionEncodings.lineLength("", u32) shouldBe 0
    }

    @Test
    fun `unknown encoding behaves like utf16`() {
        val custom = PositionEncodingKind("x-custom")
        PositionEncodings.lineLength(line, custom) shouldBe 7
    }

    @Test
    fun `line starts handle lf crlf and lone cr`() {
        LineEndings.lineStarts("a\nb\r\nc\rd").toList() shouldBe listOf(0, 2, 5, 7)
    }

    @Test
    fun `line starts with trailing terminators add an empty last line`() {
        LineEndings.lineStarts("a\n").toList() shouldBe listOf(0, 2)
        LineEndings.lineStarts("a\r\n").toList() shouldBe listOf(0, 3)
        LineEndings.lineStarts("a\r").toList() shouldBe listOf(0, 2)
        LineEndings.lineStarts("").toList() shouldBe listOf(0)
    }

    @Test
    fun `consecutive carriage returns are separate terminators`() {
        LineEndings.lineStarts("a\r\rb").toList() shouldBe listOf(0, 2, 3)
        LineEndings.lineStarts("\n\r\n").toList() shouldBe listOf(0, 1, 3)
    }

    @Test
    fun `terminator length`() {
        LineEndings.terminatorLength("a\r\n") shouldBe 2
        LineEndings.terminatorLength("a\n") shouldBe 1
        LineEndings.terminatorLength("a\r") shouldBe 1
        LineEndings.terminatorLength("a") shouldBe 0
        LineEndings.terminatorLength("") shouldBe 0
    }
}
