package io.lsp4k.server

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.lsp4k.protocol.DidChangeTextDocumentParams
import io.lsp4k.protocol.DidCloseTextDocumentParams
import io.lsp4k.protocol.DidOpenTextDocumentParams
import io.lsp4k.protocol.Position
import io.lsp4k.protocol.PositionEncodingKind
import io.lsp4k.protocol.Range
import io.lsp4k.protocol.TextDocumentContentChangeEvent
import io.lsp4k.protocol.TextDocumentIdentifier
import io.lsp4k.protocol.TextDocumentItem
import io.lsp4k.protocol.VersionedTextDocumentIdentifier
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class TextDocumentsTest {
    private val uri = "file:///a.txt"

    private suspend fun manager(
        text: String,
        encoding: PositionEncodingKind = PositionEncodingKind.UTF16,
    ): TextDocuments =
        TextDocuments(encoding).also {
            it.didOpen(DidOpenTextDocumentParams(TextDocumentItem(uri, "text", 1, text)))
        }

    private fun range(
        sl: Int,
        sc: Int,
        el: Int,
        ec: Int,
    ) = Range(Position(sl, sc), Position(el, ec))

    private suspend fun TextDocuments.edit(
        version: Int,
        vararg changes: TextDocumentContentChangeEvent,
    ): ChangeResult = didChange(DidChangeTextDocumentParams(VersionedTextDocumentIdentifier(uri, version), changes.toList()))

    private fun replace(
        r: Range,
        text: String,
    ) = TextDocumentContentChangeEvent(range = r, text = text)

    private suspend fun TextDocuments.text(): String? = get(uri)?.text

    @Test
        docs.get(uri)?.languageId shouldBe "text"
        docs.version(uri) shouldBe 1
        docs.size() shouldBe 1
        docs.all().map { it.uri } shouldBe listOf(uri)
    }

    @Test
    fun `close removes document`() = runTest {
        val docs = manager("hello")
        docs.didClose(DidCloseTextDocumentParams(TextDocumentIdentifier(uri)))?.text shouldBe "hello"
        docs.get(uri).shouldBeNull()
        docs.version(uri).shouldBeNull()
        docs.didClose(DidCloseTextDocumentParams(TextDocumentIdentifier(uri))).shouldBeNull()
    }

    @Test
    fun `reopen replaces document`() = runTest {
        val docs = manager("hello")
        docs.didOpen(DidOpenTextDocumentParams(TextDocumentItem(uri, "text", 1, "fresh")))
        docs.text() shouldBe "fresh"
        docs.size() shouldBe 1
    }

    @Test
    fun `full sync replaces text and bumps version`() = runTest {
        val docs = manager("hello")
        docs.edit(2, TextDocumentContentChangeEvent(text = "world")) shouldBe ChangeResult.APPLIED
        docs.text() shouldBe "world"
        docs.version(uri) shouldBe 2
    }

    @Test
    fun `incremental edit within a line`() = runTest {
        val docs = manager("hello world")
        docs.edit(2, replace(range(0, 6, 0, 11), "kotlin"))
        docs.text() shouldBe "hello kotlin"
    }

    @Test
    fun `edit spanning lines`() = runTest {
        val docs = manager("one\ntwo\nthree")
        docs.edit(2, replace(range(0, 2, 2, 2), "X"))
        docs.text() shouldBe "onXree"
    }

    @Test
    fun `insertion at line end goes before the terminator`() = runTest {
        val docs = manager("ab\ncd")
        docs.edit(2, replace(range(0, 2, 0, 2), "!"))
        docs.text() shouldBe "ab!\ncd"
    }

    @Test
    fun `character past line end clamps before the terminator`() = runTest {
        val docs = manager("ab\ncd")
        docs.edit(2, replace(range(0, 50, 0, 50), "!"))
        docs.text() shouldBe "ab!\ncd"
    }

    @Test
    fun `deletion at end of document`() = runTest {
        val docs = manager("ab\ncd")
        docs.edit(2, replace(range(1, 1, 1, 2), ""))
        docs.text() shouldBe "ab\nc"
        docs.edit(3, replace(range(0, 2, 1, 1), ""))
        docs.text() shouldBe "ab"
    }

    @Test
    fun `line past the end appends`() = runTest {
        val docs = manager("ab")
        docs.edit(2, replace(range(9, 0, 9, 0), "z"))
        docs.text() shouldBe "abz"
    }

    @Test
    fun `reversed range is normalised`() = runTest {
        val docs = manager("abcdef")
        docs.edit(2, replace(range(0, 4, 0, 1), ""))
        docs.text() shouldBe "aef"
    }

    @Test
    fun `changes in one notification apply in order`() = runTest {
        val docs = manager("abc")
        docs.edit(
            2,
            replace(range(0, 0, 0, 0), "12"),
            replace(range(0, 2, 0, 3), "X"),
            replace(range(0, 5, 0, 5), "!"),
        )
        docs.text() shouldBe "12Xbc!"
    }

    @Test
    fun `full change then incremental in one notification`() = runTest {
        val docs = manager("old")
        docs.edit(2, TextDocumentContentChangeEvent(text = "new"), replace(range(0, 0, 0, 1), "N"))
        docs.text() shouldBe "New"
    }

    @Test
    fun `empty change list only bumps version`() = runTest {
        val docs = manager("abc")
        docs.edit(5) shouldBe ChangeResult.APPLIED
        docs.version(uri) shouldBe 5
        docs.text() shouldBe "abc"
    }

    @Test
    fun `stale and duplicate versions are dropped`() = runTest {
        val docs = manager("abc")
        docs.edit(3, replace(range(0, 0, 0, 0), "x")) shouldBe ChangeResult.APPLIED
        docs.edit(2, replace(range(0, 0, 0, 0), "y")) shouldBe ChangeResult.STALE_VERSION
        docs.edit(3, replace(range(0, 0, 0, 0), "z")) shouldBe ChangeResult.STALE_VERSION
        docs.text() shouldBe "xabc"
        docs.version(uri) shouldBe 3
    }

    @Test
    fun `version gaps are accepted`() = runTest {
        val docs = manager("abc")
        docs.edit(10, replace(range(0, 0, 0, 0), "x")) shouldBe ChangeResult.APPLIED
        docs.version(uri) shouldBe 10
    }

    @Test
    fun `change for unknown document is dropped`() = runTest {
        val docs = TextDocuments()
        docs.edit(1, TextDocumentContentChangeEvent(text = "x")) shouldBe ChangeResult.UNKNOWN_DOCUMENT
        docs.size() shouldBe 0
    }

    @Test
    fun `earlier snapshots are not mutated`() = runTest {
        val docs = manager("abc")
        val before = docs.get(uri)
        docs.edit(2, TextDocumentContentChangeEvent(text = "zzz"))
        before?.text shouldBe "abc"
        before?.version shouldBe 1
    }

    @Test
    fun `all keeps open order and clear forgets`() = runTest {
        val docs = manager("a")
        docs.didOpen(DidOpenTextDocumentParams(TextDocumentItem("file:///b.txt", "text", 1, "b")))
        docs.all().map { it.uri } shouldBe listOf(uri, "file:///b.txt")
        docs.clear()
        docs.all().shouldBeEmpty()
    }

    // a e-acute euro G-clef(surrogate pair) CJK b
    private val multi = "aé€𝄞中b"

    @Test
    fun `utf16 edit after a surrogate pair`() = runTest {
        val docs = manager(multi)
        docs.edit(2, replace(range(0, 5, 0, 6), "X"))
        docs.text() shouldBe "aé€𝄞Xb"
    }

    @Test
    fun `utf8 edit after multibyte characters`() = runTest {
        val docs = manager(multi, PositionEncodingKind.UTF8)
        docs.edit(2, replace(range(0, 10, 0, 13), "X"))
        docs.text() shouldBe "aé€𝄞Xb"
        val doc = docs.get(uri)
        doc?.positionAt(doc.text.length) shouldBe Position(0, 12)
    }

    @Test
    fun `utf32 edit counts code points`() = runTest {
        val docs = manager(multi, PositionEncodingKind.UTF32)
        docs.edit(2, replace(range(0, 3, 0, 4), ""))
        docs.text() shouldBe "aé€中b"
    }

    @Test
    fun `deleting the surrogate pair per encoding`() = runTest {
        val expected = "aé€中b"
        val ranges =
            mapOf(
                PositionEncodingKind.UTF8 to range(0, 6, 0, 10),
                PositionEncodingKind.UTF16 to range(0, 3, 0, 5),
                PositionEncodingKind.UTF32 to range(0, 3, 0, 4),
            )
        for ((encoding, r) in ranges) {
            val docs = manager(multi, encoding)
            docs.edit(2, replace(r, ""))
            docs.text() shouldBe expected
        }
    }

    @Test
    fun `crlf document edits across lines`() = runTest {
        val docs = manager("ab\r\ncd\r\nef")
        docs.edit(2, replace(range(0, 1, 1, 1), "-"))
        docs.text() shouldBe "a-d\r\nef"
    }

    @Test
    fun `crlf insertion at line end stays before the terminator`() = runTest {
        val docs = manager("ab\r\ncd")
        docs.edit(2, replace(range(0, 2, 0, 2), "!"))
        docs.text() shouldBe "ab!\r\ncd"
    }

    @Test
    fun `lone cr is a line break`() = runTest {
        val docs = manager("ab\rcd")
        docs.get(uri)?.lineCount shouldBe 2
        docs.edit(2, replace(range(1, 0, 1, 1), "C"))
        docs.text() shouldBe "ab\rCd"
    }

    @Test
    fun `offsetAt and positionAt round trip`() = runTest {
        val doc = manager("aé\n𝄞x\r\nend").get(uri)!!
        for (offset in listOf(0, 1, 2, 3, 5, 6, 8, 9, 11)) {
            doc.offsetAt(doc.positionAt(offset)) shouldBe offset
        }
        doc.positionAt(3) shouldBe Position(1, 0)
        doc.positionAt(5) shouldBe Position(1, 2)
        doc.positionAt(7) shouldBe Position(1, 3)
        doc.positionAt(8) shouldBe Position(2, 0)
        doc.offsetAt(Position(2, 1)) shouldBe 9
    }

    @Test
    fun `offsetAt and positionAt clamp`() = runTest {
        val doc = manager("ab\ncd").get(uri)!!
        doc.positionAt(-5) shouldBe Position(0, 0)
        doc.positionAt(99) shouldBe Position(1, 2)
        doc.offsetAt(Position(-1, 0)) shouldBe 0
        doc.offsetAt(Position(7, 0)) shouldBe 5
        doc.offsetAt(Position(0, -3)) shouldBe 0
    }

    @Test
    fun `positionAt in utf8 and utf32`() = runTest {
        val u8 = manager(multi, PositionEncodingKind.UTF8).get(uri)!!
        val u32 = manager(multi, PositionEncodingKind.UTF32).get(uri)!!
        u8.positionAt(5) shouldBe Position(0, 10)
        u32.positionAt(5) shouldBe Position(0, 4)
        u8.offsetAt(Position(0, 6)) shouldBe 3
        u32.offsetAt(Position(0, 4)) shouldBe 5
    }

    @Test
    fun `getText for a range and line text`() = runTest {
        val doc = manager("one\r\ntwo\r\nthree").get(uri)!!
        doc.getText(range(0, 1, 1, 2)) shouldBe "ne\r\ntw"
        doc.getText() shouldBe "one\r\ntwo\r\nthree"
        doc.lineText(1) shouldBe "two"
        doc.lineText(9) shouldBe ""
        doc.lineCount shouldBe 3
    }

    @Test
    fun `trailing newline yields an empty last line`() = runTest {
        val doc = manager("a\n").get(uri)!!
        doc.lineCount shouldBe 2
        doc.positionAt(2) shouldBe Position(1, 0)
    }

    @Test
    fun `empty document`() = runTest {
        val doc = manager("").get(uri)!!
        doc.lineCount shouldBe 1
        doc.positionAt(0) shouldBe Position(0, 0)
        doc.offsetAt(Position(0, 4)) shouldBe 0
    }
}
