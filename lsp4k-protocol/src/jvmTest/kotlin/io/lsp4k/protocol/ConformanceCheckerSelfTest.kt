package io.lsp4k.protocol

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@Serializable
private data class SeedPosition(
    val line: Int,
    val character: Int,
)

@Serializable
private data class SeedRenamedPosition(
    val line: Int,
    @SerialName("char") val character: Int,
)

@Serializable
private data class SeedBegin(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val kind: String = "begin",
    val title: String,
    val message: String? = null,
    @Serializable(with = MaybeIntSerializer::class)
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val percentage: Maybe<Int> = Maybe.Absent,
)

/** Drops the literal `kind`: it has a default but no `@EncodeDefault`, so `encodeDefaults = false` omits it. */
@Serializable
private data class SeedDroppedKindBegin(
    val kind: String = "begin",
    val title: String,
    val message: String? = null,
    @Serializable(with = MaybeIntSerializer::class)
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val percentage: Maybe<Int> = Maybe.Absent,
)

/** A nullable required property, a required optional property, a defaulted required one and a collapsed tri-state. */
@Serializable
private data class SeedLooseBegin(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val kind: String = "begin",
    val title: String?,
    val message: String,
    val percentage: Int? = null,
)

@Serializable
private data class SeedDefaultedBegin(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val kind: String = "begin",
    val title: String = "untitled",
    val message: String? = null,
    @Serializable(with = MaybeIntSerializer::class)
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val percentage: Maybe<Int> = Maybe.Absent,
)

@Serializable(with = SeedTriggerKindSerializer::class)
private enum class SeedTriggerKind(
    val value: Int,
) {
    Invoked(1),
    Automatic(2),
}

private object SeedTriggerKindSerializer : IntEnumSerializer<SeedTriggerKind>(
    "SeedTriggerKind",
    { value -> SeedTriggerKind.entries.first { it.value == value } },
    { it.value },
)

/** The values of `InlineCompletionTriggerKind` before they were fixed: shifted down by one. */
@Serializable(with = SeedShiftedTriggerKindSerializer::class)
private enum class SeedShiftedTriggerKind(
    val value: Int,
) {
    Invoked(0),
    Automatic(1),
}

private object SeedShiftedTriggerKindSerializer : IntEnumSerializer<SeedShiftedTriggerKind>(
    "SeedShiftedTriggerKind",
    { value -> SeedShiftedTriggerKind.entries.first { it.value == value } },
    { it.value },
)

@Serializable
private enum class SeedClosedKind {
    @SerialName("comment")
    Comment,
}

private object SeedOpenKind {
    const val COMMENT: String = "comment"
}

/** Proves on every run that [ConformanceChecker] reports each class of defect, on a seeded meta model. */
class ConformanceCheckerSelfTest {
    private val meta = MetaModel.parse(SEED_MODEL)

    private val conforming =
        ConformanceRegistry(
            structures =
                mapOf(
                    "Position" to StructureBinding.Exact(SeedPosition.serializer()),
                    "Begin" to StructureBinding.Exact(SeedBegin.serializer()),
                ),
            enumerations =
                mapOf(
                    "TriggerKind" to EnumerationBinding.Closed(SeedTriggerKindSerializer, SeedTriggerKind.entries),
                    "Kind" to EnumerationBinding.Open(String.serializer(), SeedOpenKind::class.java),
                ),
            typeAliases = mapOf("Token" to ProgressTokenSerializer),
            messages =
                mapOf(
                    "seed/position" to
                        MessageBinding(
                            MessageDirection.ClientToServer,
                            params = SeedPosition.serializer(),
                            result = SeedPosition.serializer().nullable,
                        ),
                ),
            methodConstants = setOf("seed/position"),
        )

    private fun check(registry: ConformanceRegistry): Set<String> = ConformanceChecker(meta, registry).check().map { it.key }.toSet()

    private fun withStructure(
        name: String,
        serializer: KSerializer<*>,
    ): ConformanceRegistry = conforming.copy(structures = conforming.structures + (name to StructureBinding.Exact(serializer)))

    @Test
    fun `a conforming registry has no violations`() {
        assertEquals(emptySet(), check(conforming))
    }

    @Test
    fun `reports unmapped items`() {
        assertEquals(
            setOf("structure:Position", "structure:Begin", "enum:TriggerKind", "enum:Kind", "alias:Token", "message:seed/position"),
            check(ConformanceRegistry(emptyMap(), emptyMap(), emptyMap(), emptyMap(), setOf("seed/position"))),
        )
    }

    @Test
    fun `reports registry rows the meta model does not declare`() {
        val ghosts =
            conforming.copy(
                structures = conforming.structures + ("Ghost" to StructureBinding.Exact(SeedPosition.serializer())),
                messages = conforming.messages + ("seed/ghost" to MessageBinding(MessageDirection.Both)),
            )
        assertEquals(setOf("registry:structure:Ghost", "registry:message:seed/ghost"), check(ghosts))
    }

    @Test
    fun `reports a renamed property`() {
        assertEquals(
            setOf("property:Position.character:missing", "property:Position.char:extra", "roundtrip:structure:Position"),
            check(withStructure("Position", SeedRenamedPosition.serializer())),
        )
    }

    @Test
    fun `reports a literal discriminator dropped on the wire`() {
        assertEquals(
            setOf(
                "property:Begin.kind:optionality",
                "roundtrip:structure:Begin",
                "roundtrip:structure:Begin+message",
                "roundtrip:structure:Begin.percentage=uinteger",
                "roundtrip:structure:Begin.percentage=null",
            ),
            check(withStructure("Begin", SeedDroppedKindBegin.serializer())),
        )
    }

    @Test
    fun `reports a default on a required property that is not a literal`() {
        assertEquals(setOf("property:Begin.title:optionality"), check(withStructure("Begin", SeedDefaultedBegin.serializer())))
    }

    @Test
    fun `reports optionality nullability and tri-state defects`() {
        val violations = check(withStructure("Begin", SeedLooseBegin.serializer()))
        val expected =
            setOf(
                "property:Begin.title:nullability",
                "property:Begin.message:optionality",
                "roundtrip:structure:Begin.percentage=null",
            )
        assertTrue(violations.containsAll(expected), "expected $expected in $violations")
    }

    @Test
    fun `reports shifted enumeration values`() {
        val shifted = EnumerationBinding.Closed(SeedShiftedTriggerKindSerializer, SeedShiftedTriggerKind.entries)
        assertEquals(
            setOf("enumValue:TriggerKind=2", "enumExtra:TriggerKind=0"),
            check(conforming.copy(enumerations = conforming.enumerations + ("TriggerKind" to shifted))),
        )
    }

    @Test
    fun `reports a closed enum for an enumeration that supports custom values`() {
        val closed = EnumerationBinding.Closed(SeedClosedKind.serializer(), SeedClosedKind.entries)
        assertEquals(setOf("enumCustom:Kind"), check(conforming.copy(enumerations = conforming.enumerations + ("Kind" to closed))))
    }

    @Test
    fun `reports message method direction registration and payload defects`() {
        val wrong =
            MessageBinding(
                MessageDirection.ServerToClient,
                params = SeedPosition.serializer(),
                result = SeedPosition.serializer(),
                registrationMethod = "seed/register",
                errorData = SeedPosition.serializer(),
            )
        assertEquals(
            setOf(
                "method:seed/position",
                "direction:seed/position",
                "registrationMethod:seed/position",
                "roundtrip:result:seed/position#null",
                "errorData:seed/position:extra",
            ),
            check(conforming.copy(messages = mapOf("seed/position" to wrong), methodConstants = emptySet())),
        )
    }

    @Test
    fun `reports a payload without a serializer`() {
        val noResult = MessageBinding(MessageDirection.ClientToServer, params = SeedPosition.serializer())
        assertEquals(setOf("result:seed/position"), check(conforming.copy(messages = mapOf("seed/position" to noResult))))
    }

    @Test
    fun `reports an alias serializer that rejects a member`() {
        assertEquals(setOf("roundtrip:alias:Token#string"), check(conforming.copy(typeAliases = mapOf("Token" to Int.serializer()))))
    }

    @Test
    fun `ratchet reports unexpected violations and stale allow-list entries`() {
        assertEquals(
            Ratchet(unexpected = setOf("structure:A"), stale = setOf("structure:B")),
            ratchet(setOf("structure:A", "structure:C"), setOf("structure:B", "structure:C")),
        )
    }

    @Test
    fun `allow-list parser reads grouped entries`() {
        val entries =
            ConformanceAllowList.parse(
                "# comment\n\n[LP-1] First gap\nstructure:A reason one\n[LP-2] Second gap\nenum:B reason two\n",
            )
        assertEquals(
            listOf(AllowListEntry("structure:A", "LP-1", "reason one"), AllowListEntry("enum:B", "LP-2", "reason two")),
            entries,
        )
    }

    @Test
    fun `allow-list parser rejects malformed files`() {
        val malformed =
            listOf(
                "structure:A reason before any gap",
                "[LP-1] Gap\nstructure:A",
                "[LP-1] Gap\nstructure:A reason\nstructure:A again",
                "[LP-1] Gap\nstructure:A reason\n[LP-1] Gap again\nstructure:B reason",
                "[LP-1] Gap without entries",
                "[gap] Not a gap id\nstructure:A reason",
            )
        malformed.forEach { text -> assertFailsWith<IllegalArgumentException>(text) { ConformanceAllowList.parse(text) } }
    }

    private companion object {
        val SEED_MODEL =
            """
            {
              "metaData": {"version": "seed"},
              "requests": [
                {"method": "seed/position", "messageDirection": "clientToServer",
                 "params": {"kind": "reference", "name": "Position"},
                 "result": {"kind": "or", "items": [{"kind": "reference", "name": "Position"}, {"kind": "base", "name": "null"}]}}
              ],
              "notifications": [],
              "structures": [
                {"name": "Position", "properties": [
                  {"name": "line", "type": {"kind": "base", "name": "uinteger"}},
                  {"name": "character", "type": {"kind": "base", "name": "uinteger"}}]},
                {"name": "Begin", "properties": [
                  {"name": "kind", "type": {"kind": "stringLiteral", "value": "begin"}},
                  {"name": "title", "type": {"kind": "base", "name": "string"}},
                  {"name": "message", "type": {"kind": "base", "name": "string"}, "optional": true},
                  {"name": "percentage", "optional": true, "type": {"kind": "or", "items": [
                    {"kind": "base", "name": "uinteger"}, {"kind": "base", "name": "null"}]}}]}
              ],
              "enumerations": [
                {"name": "TriggerKind", "type": {"kind": "base", "name": "uinteger"},
                 "values": [{"name": "Invoked", "value": 1}, {"name": "Automatic", "value": 2}]},
                {"name": "Kind", "type": {"kind": "base", "name": "string"}, "supportsCustomValues": true,
                 "values": [{"name": "Comment", "value": "comment"}]}
              ],
              "typeAliases": [
                {"name": "Token", "type": {"kind": "or", "items": [{"kind": "base", "name": "integer"}, {"kind": "base", "name": "string"}]}}
              ]
            }
            """.trimIndent()
    }
}
