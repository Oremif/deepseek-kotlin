package org.oremif.deepseek.models

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import org.oremif.deepseek.testing.TestJson
import kotlin.test.Test

class UserMessageContentTests {

    @Test
    fun `a plain-text user message still serializes content as a string`() {
        TestJson.encodeToString<ChatMessage>(UserMessage("Hi")) shouldBe """{"role":"user","content":"Hi"}"""
    }

    @Test
    fun `a multimodal user message serializes content as an array of parts`() {
        val message = UserMessage(
            listOf(
                TextPart("What is in this image?"),
                ImageUrlPart("https://example.com/cat.jpg", ImageDetail.LOW),
            )
        )

        TestJson.encodeToString<ChatMessage>(message) shouldBe
                """{"role":"user","content":[{"type":"text","text":"What is in this image?"},""" +
                """{"type":"image_url","image_url":{"url":"https://example.com/cat.jpg","detail":"low"}}]}"""
    }

    @Test
    fun `a file part serializes flat next to its type discriminator`() {
        val fromId = UserMessage(listOf(FilePart(fileId = "file-api-abc123")))
        TestJson.encodeToString<ChatMessage>(fromId) shouldContain
                """{"type":"file","file_id":"file-api-abc123"}"""

        val inline = UserMessage(listOf(FilePart(fileData = "data:image/png;base64,AAAA", filename = "cat.png")))
        TestJson.encodeToString<ChatMessage>(inline) shouldContain
                """{"type":"file","file_data":"data:image/png;base64,AAAA","filename":"cat.png"}"""
    }

    @Test
    fun `an image url part omits an unset detail`() {
        TestJson.encodeToString<ChatMessage>(UserMessage(listOf(ImageUrlPart("https://example.com/a.jpg")))) shouldBe
                """{"role":"user","content":[{"type":"image_url","image_url":{"url":"https://example.com/a.jpg"}}]}"""
    }

    @Test
    fun `a user message reads back from either content shape`() {
        val text = TestJson.decodeFromString<ChatMessage>("""{"role":"user","content":"Hi"}""")
            .shouldBeInstanceOf<UserMessage>()
        text.content shouldBe "Hi"
        text.parts.shouldBeNull()

        val multimodal = TestJson.decodeFromString<ChatMessage>(
            """{"role":"user","content":[{"type":"text","text":"hey"},""" +
                    """{"type":"image_url","image_url":{"url":"https://example.com/a.jpg","detail":"original"}},""" +
                    """{"type":"file","file_id":"file-api-1"}]}"""
        ).shouldBeInstanceOf<UserMessage>()
        multimodal.content.shouldBeNull()
        multimodal.parts.shouldNotBeNull() shouldBe listOf(
            TextPart("hey"),
            ImageUrlPart("https://example.com/a.jpg", ImageDetail.ORIGINAL),
            FilePart(fileId = "file-api-1"),
        )
    }

    @Test
    fun `a multimodal user message survives a round trip`() {
        val message = UserMessage(
            listOf(TextPart("look"), ImageUrlPart("https://example.com/a.jpg", ImageDetail.HIGH)),
            name = "alice",
        )

        val decoded = TestJson.decodeFromString<ChatMessage>(TestJson.encodeToString<ChatMessage>(message))

        decoded shouldBe message
    }

    @Test
    fun `a file part requires exactly one of fileId and fileData`() {
        val ex = shouldThrow<IllegalArgumentException> { FilePart() }
        ex.message!! shouldContain "exactly one"
        shouldThrow<IllegalArgumentException> {
            FilePart(fileId = "file-api-1", fileData = "data:image/png;base64,AAAA")
        }
    }

    @Test
    fun `a file part rejects a filename without inline data`() {
        val ex = shouldThrow<IllegalArgumentException> {
            FilePart(fileId = "file-api-1", filename = "cat.png")
        }
        ex.message!! shouldContain "filename"
    }

    @Test
    fun `an image url must not be blank`() {
        shouldThrow<IllegalArgumentException> { ImageUrlPart("  ") }
    }

    @Test
    fun `the parts DSL sends parts in the order they were appended`() {
        val parts = ChatCompletionRequest.UserContentBuilder().apply {
            image("https://example.com/before.jpg")
            text("What changed?")
            imageFile("file-api-after")
        }.build()

        parts.map { it::class } shouldBe listOf(ImageUrlPart::class, TextPart::class, FilePart::class)
        TestJson.encodeToString<ChatMessage>(UserMessage(parts)) shouldBe
                """{"role":"user","content":[""" +
                """{"type":"image_url","image_url":{"url":"https://example.com/before.jpg"}},""" +
                """{"type":"text","text":"What changed?"},""" +
                """{"type":"file","file_id":"file-api-after"}]}"""
    }
}
