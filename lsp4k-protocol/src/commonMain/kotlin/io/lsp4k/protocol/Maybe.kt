package io.lsp4k.protocol

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * The value of an optional property typed `T | null` in the meta model (`x?: T | null`), which tells three
 * states apart: the property is [Absent], explicitly [Null], or carries a value ([Of]).
 *
 * A property of this type defaults to [Absent] and is never written while it is [Absent], whatever the
 * `encodeDefaults` setting of the `Json` instance.
 */
public sealed interface Maybe<out T> {
    /** The value when this is [Of], otherwise `null` (for both [Absent] and [Null]). */
    public val valueOrNull: T?

    /** The property is not present. */
    public data object Absent : Maybe<Nothing> {
        override val valueOrNull: Nothing? get() = null
    }

    /** The property is present with the JSON value `null`. */
    public data object Null : Maybe<Nothing> {
        override val valueOrNull: Nothing? get() = null
    }

    /** The property is present with [value]. */
    public data class Of<out T>(
        public val value: T,
    ) : Maybe<T> {
        override val valueOrNull: T get() = value
    }
}

/**
 * Serializer for a [Maybe] property. Its descriptor is nullable so that `coerceInputValues` never turns a JSON
 * `null` into the default ([Maybe.Absent]); [Maybe.Absent] itself is never encoded.
 */
public open class MaybeSerializer<T>(
    private val valueSerializer: KSerializer<T>,
) : KSerializer<Maybe<T>> {
    override val descriptor: SerialDescriptor = valueSerializer.descriptor.nullable

    override fun serialize(
        encoder: Encoder,
        value: Maybe<T>,
    ) {
        when (value) {
            Maybe.Absent -> throw SerializationException("Maybe.Absent has no JSON form; leave the property at its default")
            Maybe.Null -> encoder.encodeNull()
            is Maybe.Of -> encoder.encodeSerializableValue(valueSerializer, value.value)
        }
    }

    override fun deserialize(decoder: Decoder): Maybe<T> =
        if (decoder.decodeNotNullMark()) {
            Maybe.Of(decoder.decodeSerializableValue(valueSerializer))
        } else {
            decoder.decodeNull()
            Maybe.Null
        }
}

internal object MaybeIntSerializer : MaybeSerializer<Int>(Int.serializer())

internal object MaybeStringSerializer : MaybeSerializer<String>(String.serializer())

internal object MaybeWorkspaceFoldersSerializer : MaybeSerializer<List<WorkspaceFolder>>(ListSerializer(WorkspaceFolder.serializer()))
