package io.lsp4k.protocol

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertFailsWith

class EitherTest {
    private val json = Json { ignoreUnknownKeys = true }

    // ===== Either.Left Tests =====

    @Test
    fun `Either Left holds value correctly`() {
        val either: Either<String, Int> = Either.left("hello")

        either.shouldBeInstanceOf<Either.Left<String>>()
        either.left shouldBe "hello"
        either.right shouldBe null
        either.isLeft shouldBe true
        either.isRight shouldBe false
        either.get() shouldBe "hello"
    }

    @Test
    fun `Either Left equality`() {
        val either1: Either<String, Int> = Either.left("test")
        val either2: Either<String, Int> = Either.left("test")
        val either3: Either<String, Int> = Either.left("other")

        either1 shouldBe either2
        (either1 == either3) shouldBe false
    }

    @Test
    fun `Either Left fold applies left function`() {
        val either: Either<String, Int> = Either.left("hello")

        val result =
            either.fold(
                ifLeft = { it.length },
                ifRight = { it * 2 },
            )

        result shouldBe 5
    }

    // ===== Either.Right Tests =====

    @Test
    fun `Either Right holds value correctly`() {
        val either: Either<String, Int> = Either.right(42)

        either.shouldBeInstanceOf<Either.Right<Int>>()
        either.left shouldBe null
        either.right shouldBe 42
        either.isLeft shouldBe false
        either.isRight shouldBe true
        either.get() shouldBe 42
    }

    @Test
    fun `Either Right equality`() {
        val either1: Either<String, Int> = Either.right(42)
        val either2: Either<String, Int> = Either.right(42)
        val either3: Either<String, Int> = Either.right(99)

        either1 shouldBe either2
        (either1 == either3) shouldBe false
    }

    @Test
    fun `Either Right fold applies right function`() {
        val either: Either<String, Int> = Either.right(21)

        val result =
            either.fold(
                ifLeft = { it.length },
                ifRight = { it * 2 },
            )

        result shouldBe 42
    }

    // ===== Either Left and Right not equal =====

    @Test
    fun `Either Left and Right are not equal even with same value type`() {
        val left: Either<Int, Int> = Either.left(42)
        val right: Either<Int, Int> = Either.right(42)

        (left == right) shouldBe false
    }

    // ===== Either companion functions =====

    @Test
    fun `Either companion left creates Left`() {
        val either = Either.left("test")
        either.shouldBeInstanceOf<Either.Left<String>>()
    }

    @Test
    fun `Either companion right creates Right`() {
        val either = Either.right(42)
        either.shouldBeInstanceOf<Either.Right<Int>>()
    }

    // ===== Either with null values =====

    @Test
    fun `Either Left can hold null`() {
        val either: Either<String?, Int> = Either.left(null)

        either.isLeft shouldBe true
        either.left shouldBe null
        either.get() shouldBe null
    }

    @Test
    fun `Either Right can hold null`() {
        val either: Either<String, Int?> = Either.right(null)

        either.isRight shouldBe true
        either.right shouldBe null
        either.get() shouldBe null
    }

    // ===== EitherSerializer Tests =====

    @Test
    fun `EitherSerializer base throws on serialize`() {
        val either: Either<String, Int> = Either.left("test")

        assertFailsWith<UnsupportedOperationException> {
            json.encodeToString(EitherSerializer, either)
        }
    }

    @Test
    fun `EitherSerializer base throws on deserialize`() {
        assertFailsWith<UnsupportedOperationException> {
            json.decodeFromString(EitherSerializer, "\"test\"")
        }
    }

    @Test
    fun `EitherSerializer create works with string discriminator`() {
        // Discriminator: if it's a string, it's left; otherwise right
        val serializer =
            EitherSerializer.create(
                String.serializer(),
                Int.serializer(),
                discriminator = { element -> element.jsonPrimitive.isString },
            )

        // Test serialization
        val leftValue: Either<String, Int> = Either.left("hello")
        val leftEncoded = json.encodeToString(serializer, leftValue)
        leftEncoded shouldBe "\"hello\""

        val rightValue: Either<String, Int> = Either.right(42)
        val rightEncoded = json.encodeToString(serializer, rightValue)
        rightEncoded shouldBe "42"

        // Test deserialization
        val decodedLeft = json.decodeFromString(serializer, "\"world\"")
        decodedLeft.isLeft shouldBe true
        decodedLeft.left shouldBe "world"

        val decodedRight = json.decodeFromString(serializer, "100")
        decodedRight.isRight shouldBe true
        decodedRight.right shouldBe 100
    }

    // ===== Either3 Tests =====

    @Test
    fun `Either3 First holds value correctly`() {
        val either: Either3<String, Int, Boolean> = Either3.first("hello")

        either.shouldBeInstanceOf<Either3.First<String>>()
        either.first shouldBe "hello"
        either.second shouldBe null
        either.third shouldBe null
        either.get() shouldBe "hello"
    }

    @Test
    fun `Either3 Second holds value correctly`() {
        val either: Either3<String, Int, Boolean> = Either3.second(42)

        either.shouldBeInstanceOf<Either3.Second<Int>>()
        either.first shouldBe null
        either.second shouldBe 42
        either.third shouldBe null
        either.get() shouldBe 42
    }

    @Test
    fun `Either3 Third holds value correctly`() {
        val either: Either3<String, Int, Boolean> = Either3.third(true)

        either.shouldBeInstanceOf<Either3.Third<Boolean>>()
        either.first shouldBe null
        either.second shouldBe null
        either.third shouldBe true
        either.get() shouldBe true
    }

    @Test
    fun `Either3 First fold applies first function`() {
        val either: Either3<String, Int, Boolean> = Either3.first("hello")

        val result =
            either.fold(
                ifFirst = { it.length },
                ifSecond = { it * 2 },
                ifThird = { if (it) 1 else 0 },
            )

        result shouldBe 5
    }

    @Test
    fun `Either3 Second fold applies second function`() {
        val either: Either3<String, Int, Boolean> = Either3.second(21)

        val result =
            either.fold(
                ifFirst = { it.length },
                ifSecond = { it * 2 },
                ifThird = { if (it) 1 else 0 },
            )

        result shouldBe 42
    }

    @Test
    fun `Either3 Third fold applies third function`() {
        val either: Either3<String, Int, Boolean> = Either3.third(true)

        val result =
            either.fold(
                ifFirst = { it.length },
                ifSecond = { it * 2 },
                ifThird = { if (it) 1 else 0 },
            )

        result shouldBe 1
    }

    @Test
    fun `Either3 equality for First`() {
        val e1: Either3<String, Int, Boolean> = Either3.first("test")
        val e2: Either3<String, Int, Boolean> = Either3.first("test")
        val e3: Either3<String, Int, Boolean> = Either3.first("other")

        e1 shouldBe e2
        (e1 == e3) shouldBe false
    }

    @Test
    fun `Either3 equality for Second`() {
        val e1: Either3<String, Int, Boolean> = Either3.second(42)
        val e2: Either3<String, Int, Boolean> = Either3.second(42)
        val e3: Either3<String, Int, Boolean> = Either3.second(99)

        e1 shouldBe e2
        (e1 == e3) shouldBe false
    }

    @Test
    fun `Either3 equality for Third`() {
        val e1: Either3<String, Int, Boolean> = Either3.third(true)
        val e2: Either3<String, Int, Boolean> = Either3.third(true)
        val e3: Either3<String, Int, Boolean> = Either3.third(false)

        e1 shouldBe e2
        (e1 == e3) shouldBe false
    }

    @Test
    fun `Either3 variants are not equal`() {
        val first: Either3<Int, Int, Int> = Either3.first(1)
        val second: Either3<Int, Int, Int> = Either3.second(1)
        val third: Either3<Int, Int, Int> = Either3.third(1)

        (first == second) shouldBe false
        (second == third) shouldBe false
        (first == third) shouldBe false
    }

    // ===== Either3Serializer Tests =====

    @Test
    fun `Either3Serializer base throws on serialize`() {
        val either: Either3<String, Int, Boolean> = Either3.first("test")

        assertFailsWith<UnsupportedOperationException> {
            json.encodeToString(Either3Serializer, either)
        }
    }

    @Test
    fun `Either3Serializer base throws on deserialize`() {
        assertFailsWith<UnsupportedOperationException> {
            json.decodeFromString(Either3Serializer, "\"test\"")
        }
    }

    // ===== Either with complex types =====

    @Test
    fun `Either works with complex left type`() {
        data class Person(
            val name: String,
            val age: Int,
        )

        val either: Either<Person, String> = Either.left(Person("Alice", 30))

        either.isLeft shouldBe true
        either.left?.name shouldBe "Alice"
        either.left?.age shouldBe 30
    }

    @Test
    fun `Either works with complex right type`() {
        data class Error(
            val code: Int,
            val message: String,
        )

        val either: Either<String, Error> = Either.right(Error(404, "Not found"))

        either.isRight shouldBe true
        either.right?.code shouldBe 404
        either.right?.message shouldBe "Not found"
    }

    @Test
    fun `Either3 works with complex types`() {
        data class Success(
            val value: String,
        )

        data class Failure(
            val error: String,
        )

        data class Pending(
            val progress: Int,
        )

        val success: Either3<Success, Failure, Pending> = Either3.first(Success("done"))
        val failure: Either3<Success, Failure, Pending> = Either3.second(Failure("oops"))
        val pending: Either3<Success, Failure, Pending> = Either3.third(Pending(50))

        success.first?.value shouldBe "done"
        failure.second?.error shouldBe "oops"
        pending.third?.progress shouldBe 50
    }

    // ===== Either covariance tests =====

    @Test
    fun `Either is covariant in both type parameters`() {
        val stringLeft: Either<String, Nothing> = Either.left("hello")
        val anyLeft: Either<Any, Int> = stringLeft // Should compile due to covariance

        anyLeft.left shouldBe "hello"

        val intRight: Either<Nothing, Int> = Either.right(42)
        val anyRight: Either<String, Number> = intRight // Should compile due to covariance

        anyRight.right shouldBe 42
    }

    @Test
    fun `Either3 is covariant in all type parameters`() {
        val first: Either3<String, Nothing, Nothing> = Either3.first("test")
        val any: Either3<Any, Int, Boolean> = first

        any.first shouldBe "test"
    }
}
