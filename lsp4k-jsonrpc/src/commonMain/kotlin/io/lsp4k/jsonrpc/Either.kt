package io.lsp4k.jsonrpc

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder

/**
 * A disjoint union type representing a value that is either [L] (left) or [R] (right).
 *
 * This is commonly used in LSP for fields that can have multiple types, such as:
 * - `textDocumentSync: TextDocumentSyncOptions | TextDocumentSyncKind`
 * - `completion: CompletionList | CompletionItem[]`
 *
 * @param L The left type
 * @param R The right type
 */
@Serializable(with = EitherSerializer::class)
public sealed interface Either<out L, out R> {
    /**
     * Returns the left value if this is a [Left], otherwise null.
     */
    public val left: L?

    /**
     * Returns the right value if this is a [Right], otherwise null.
     */
    public val right: R?

    /**
     * Returns true if this is a [Left].
     */
    public val isLeft: Boolean

    /**
     * Returns true if this is a [Right].
     */
    public val isRight: Boolean

    /**
     * Returns the value, regardless of whether it's left or right.
     */
    public fun get(): Any?

    /**
     * Maps both sides of the either.
     */
    public fun <T> fold(
        ifLeft: (L) -> T,
        ifRight: (R) -> T,
    ): T

    /**
     * The left variant of Either.
     */
    public data class Left<out L>(
        public val value: L,
    ) : Either<L, Nothing> {
        override val left: L get() = value
        override val right: Nothing? get() = null
        override val isLeft: Boolean get() = true
        override val isRight: Boolean get() = false

        override fun get(): L = value

        override fun <T> fold(
            ifLeft: (L) -> T,
            ifRight: (Nothing) -> T,
        ): T = ifLeft(value)
    }

    /**
     * The right variant of Either.
     */
    public data class Right<out R>(
        public val value: R,
    ) : Either<Nothing, R> {
        override val left: Nothing? get() = null
        override val right: R get() = value
        override val isLeft: Boolean get() = false
        override val isRight: Boolean get() = true

        override fun get(): R = value

        override fun <T> fold(
            ifLeft: (Nothing) -> T,
            ifRight: (R) -> T,
        ): T = ifRight(value)
    }

    public companion object {
        /**
         * Creates a Left value.
         */
        public fun <L> left(value: L): Either<L, Nothing> = Left(value)

        /**
         * Creates a Right value.
         */
        public fun <R> right(value: R): Either<Nothing, R> = Right(value)
    }
}

/**
 * A disjoint union type for three types.
 *
 * Used in LSP for fields like `prepareRename` which can return:
 * - `Range`
 * - `PrepareRenameResult`
 * - `PrepareRenameDefaultBehavior`
 */
@Serializable(with = Either3Serializer::class)
public sealed interface Either3<out A, out B, out C> {
    public val first: A?
    public val second: B?
    public val third: C?

    public fun get(): Any?

    public fun <T> fold(
        ifFirst: (A) -> T,
        ifSecond: (B) -> T,
        ifThird: (C) -> T,
    ): T

    public data class First<out A>(
        public val value: A,
    ) : Either3<A, Nothing, Nothing> {
        override val first: A get() = value
        override val second: Nothing? get() = null
        override val third: Nothing? get() = null

        override fun get(): A = value

        override fun <T> fold(
            ifFirst: (A) -> T,
            ifSecond: (Nothing) -> T,
            ifThird: (Nothing) -> T,
        ): T = ifFirst(value)
    }

    public data class Second<out B>(
        public val value: B,
    ) : Either3<Nothing, B, Nothing> {
        override val first: Nothing? get() = null
        override val second: B get() = value
        override val third: Nothing? get() = null

        override fun get(): B = value

        override fun <T> fold(
            ifFirst: (Nothing) -> T,
            ifSecond: (B) -> T,
            ifThird: (Nothing) -> T,
        ): T = ifSecond(value)
    }

    public data class Third<out C>(
        public val value: C,
    ) : Either3<Nothing, Nothing, C> {
        override val first: Nothing? get() = null
        override val second: Nothing? get() = null
        override val third: C get() = value

        override fun get(): C = value

        override fun <T> fold(
            ifFirst: (Nothing) -> T,
            ifSecond: (Nothing) -> T,
            ifThird: (C) -> T,
        ): T = ifThird(value)
    }

    public companion object {
        public fun <A> first(value: A): Either3<A, Nothing, Nothing> = First(value)

        public fun <B> second(value: B): Either3<Nothing, B, Nothing> = Second(value)

        public fun <C> third(value: C): Either3<Nothing, Nothing, C> = Third(value)
    }
}

/**
 * Base serializer for Either types.
 * Concrete implementations should provide type-specific deserialization logic.
 *
 * Note: For proper serialization/deserialization, use type-specific serializers
 * that know how to distinguish between L and R types.
 */
public object EitherSerializer : KSerializer<Either<*, *>> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("Either")

    override fun serialize(
        encoder: Encoder,
        value: Either<*, *>,
    ) {
        val jsonEncoder = encoder as JsonEncoder
        // This is a simplified serializer - real usage needs type-specific serializers
        val element = value.get()
        if (element != null) {
            jsonEncoder.encodeSerializableValue(
                kotlinx.serialization.serializer(element::class.java),
                element,
            )
        }
    }

    override fun deserialize(decoder: Decoder): Either<*, *> {
        // Cannot deserialize without knowing the concrete types
        throw UnsupportedOperationException(
            "Either deserialization requires a type-specific serializer. " +
                "Use EitherSerializer.create(leftSerializer, rightSerializer, discriminator) instead.",
        )
    }

    /**
     * Creates a type-specific serializer for Either<L, R>.
     *
     * @param leftSerializer Serializer for the left type
     * @param rightSerializer Serializer for the right type
     * @param discriminator Function to determine if a JSON element is the left type
     */
    public fun <L, R> create(
        leftSerializer: KSerializer<L>,
        rightSerializer: KSerializer<R>,
        discriminator: (JsonElement) -> Boolean,
    ): KSerializer<Either<L, R>> =
        object : KSerializer<Either<L, R>> {
            override val descriptor: SerialDescriptor =
                buildClassSerialDescriptor("Either<${leftSerializer.descriptor.serialName}, ${rightSerializer.descriptor.serialName}>")

            override fun serialize(
                encoder: Encoder,
                value: Either<L, R>,
            ) {
                val jsonEncoder = encoder as JsonEncoder
                when (value) {
                    is Either.Left -> jsonEncoder.encodeSerializableValue(leftSerializer, value.value)
                    is Either.Right -> jsonEncoder.encodeSerializableValue(rightSerializer, value.value)
                }
            }

            override fun deserialize(decoder: Decoder): Either<L, R> {
                val jsonDecoder = decoder as JsonDecoder
                val element = jsonDecoder.decodeJsonElement()
                return if (discriminator(element)) {
                    Either.Left(jsonDecoder.json.decodeFromJsonElement(leftSerializer, element))
                } else {
                    Either.Right(jsonDecoder.json.decodeFromJsonElement(rightSerializer, element))
                }
            }
        }
}

/**
 * Base serializer for Either3 types.
 */
public object Either3Serializer : KSerializer<Either3<*, *, *>> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("Either3")

    override fun serialize(
        encoder: Encoder,
        value: Either3<*, *, *>,
    ) {
        val jsonEncoder = encoder as JsonEncoder
        val element = value.get()
        if (element != null) {
            jsonEncoder.encodeSerializableValue(
                kotlinx.serialization.serializer(element::class.java),
                element,
            )
        }
    }

    override fun deserialize(decoder: Decoder): Either3<*, *, *> =
        throw UnsupportedOperationException(
            "Either3 deserialization requires a type-specific serializer.",
        )
}
