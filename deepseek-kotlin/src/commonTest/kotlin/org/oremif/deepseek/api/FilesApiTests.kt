package org.oremif.deepseek.api

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldNotContain
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import org.oremif.deepseek.errors.DeepSeekException
import org.oremif.deepseek.models.ChatModel
import org.oremif.deepseek.models.FilePurpose
import org.oremif.deepseek.models.SortOrder
import org.oremif.deepseek.testing.mockEngine
import org.oremif.deepseek.testing.testClient
import kotlin.test.Test

class FilesApiTests {

    private val imageBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())

    private val fileBody = """
        {
            "id": "file-api-abc123",
            "object": "file",
            "bytes": 102400,
            "created_at": 1700000000,
            "filename": "cat.jpg",
            "purpose": "user_data",
            "expires_at": 1700003600
        }
    """.trimIndent()

    private fun MockRequestHandleScope.jsonResponse(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ): HttpResponseData = respond(
        content = body,
        status = status,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    )

    @Test
    fun `uploadFile posts multipart form data to the files endpoint`() = runTest {
        var capturedMethod: HttpMethod? = null
        var capturedPath: String? = null
        var capturedContentType: ContentType? = null
        var capturedBody: String? = null
        val engine = mockEngine { request ->
            capturedMethod = request.method
            capturedPath = request.url.encodedPath
            capturedContentType = request.body.contentType
            capturedBody = request.body.toByteArray().decodeToString()
            jsonResponse(fileBody)
        }
        val client = testClient(engine)

        val file = client.uploadFile(imageBytes, "cat.jpg")

        capturedMethod shouldBe HttpMethod.Post
        capturedPath.shouldNotBeNull().shouldEndWith("/files")

        // The body's own content type has to win over the JSON default the client pins on
        // every request, boundary included, or the server cannot parse the parts.
        val contentType = capturedContentType.shouldNotBeNull()
        contentType.withoutParameters() shouldBe ContentType.MultiPart.FormData
        contentType.parameter("boundary").shouldNotBeNull()

        val body = capturedBody.shouldNotBeNull()
        body shouldContain """Content-Disposition: form-data; name="file"; filename="cat.jpg""""
        body shouldContain """Content-Disposition: form-data; name="purpose""""
        body shouldContain "user_data"

        file.id shouldBe "file-api-abc123"
        file.`object` shouldBe "file"
        file.bytes shouldBe 102400L
        file.createdAt shouldBe 1700000000L
        file.filename shouldBe "cat.jpg"
        file.purpose shouldBe FilePurpose.USER_DATA
        file.expiresAt shouldBe 1700003600L
    }

    @Test
    fun `uploadFile omits the expiry fields when no lifetime is given`() = runTest {
        var capturedBody: String? = null
        val engine = mockEngine { request ->
            capturedBody = request.body.toByteArray().decodeToString()
            jsonResponse(fileBody)
        }
        val client = testClient(engine)

        client.uploadFile(imageBytes, "cat.jpg")

        capturedBody.shouldNotBeNull() shouldNotContain "expires_after"
    }

    @Test
    fun `uploadFile sends the expiry anchor alongside the lifetime`() = runTest {
        var capturedBody: String? = null
        val engine = mockEngine { request ->
            capturedBody = request.body.toByteArray().decodeToString()
            jsonResponse(fileBody)
        }
        val client = testClient(engine)

        client.uploadFile(imageBytes, "cat.jpg", expiresAfterSeconds = 3600)

        val body = capturedBody.shouldNotBeNull()
        body shouldContain "expires_after[anchor]"
        body shouldContain "created_at"
        body shouldContain "expires_after[seconds]"
        body shouldContain "3600"
    }

    @Test
    fun `uploadFile parses a file without an expiry`() = runTest {
        val engine = mockEngine {
            jsonResponse(
                """
                    {
                        "id": "file-api-abc123",
                        "object": "file",
                        "bytes": 12,
                        "created_at": 1700000000,
                        "filename": "cat.jpg",
                        "purpose": "user_data"
                    }
                """.trimIndent()
            )
        }
        val client = testClient(engine)

        client.uploadFile(imageBytes, "cat.jpg").expiresAt.shouldBeNull()
    }

    @Test
    fun `uploadFile escapes a quote in the filename`() = runTest {
        var capturedBody: String? = null
        val engine = mockEngine { request ->
            capturedBody = request.body.toByteArray().decodeToString()
            jsonResponse(fileBody)
        }
        val client = testClient(engine)

        client.uploadFile(imageBytes, """say "hi".jpg""")

        capturedBody.shouldNotBeNull() shouldContain """filename="say \"hi\".jpg""""
    }

    @Test
    fun `uploadFile rejects an unusable file or filename`() = runTest {
        val client = testClient(mockEngine { jsonResponse(fileBody) })

        shouldThrow<IllegalArgumentException> { client.uploadFile(byteArrayOf(), "cat.jpg") }
        shouldThrow<IllegalArgumentException> { client.uploadFile(imageBytes, "  ") }
        shouldThrow<IllegalArgumentException> { client.uploadFile(imageBytes, "cat\n.jpg") }
    }

    @Test
    fun `uploadFile rejects a lifetime outside the accepted range`() = runTest {
        val client = testClient(mockEngine { jsonResponse(fileBody) })

        shouldThrow<IllegalArgumentException> {
            client.uploadFile(imageBytes, "cat.jpg", expiresAfterSeconds = 3599)
        }
        shouldThrow<IllegalArgumentException> {
            client.uploadFile(imageBytes, "cat.jpg", expiresAfterSeconds = 2_592_001)
        }
    }

    @Test
    fun `listFiles GETs the files endpoint and parses the page`() = runTest {
        var capturedMethod: HttpMethod? = null
        var capturedPath: String? = null
        var capturedQuery: String? = null
        val engine = mockEngine { request ->
            capturedMethod = request.method
            capturedPath = request.url.encodedPath
            capturedQuery = request.url.encodedQuery
            jsonResponse(
                """
                    {
                        "object": "list",
                        "data": [
                            {
                                "id": "file-api-abc123",
                                "object": "file",
                                "bytes": 102400,
                                "created_at": 1700000000,
                                "filename": "cat.jpg",
                                "purpose": "user_data"
                            },
                            {
                                "id": "file-api-def456",
                                "object": "file",
                                "bytes": 2048,
                                "created_at": 1700000100,
                                "filename": "dog.png",
                                "purpose": "user_data",
                                "expires_at": 1700003700
                            }
                        ],
                        "first_id": "file-api-abc123",
                        "last_id": "file-api-def456",
                        "has_more": true
                    }
                """.trimIndent()
            )
        }
        val client = testClient(engine)

        val page = client.listFiles()

        capturedMethod shouldBe HttpMethod.Get
        capturedPath.shouldNotBeNull().shouldEndWith("/files")
        capturedQuery shouldBe ""
        page.`object` shouldBe "list"
        page.data shouldHaveSize 2
        page.data.map { it.id } shouldBe listOf("file-api-abc123", "file-api-def456")
        page.data[0].expiresAt.shouldBeNull()
        page.data[1].expiresAt shouldBe 1700003700L
        page.firstId shouldBe "file-api-abc123"
        page.lastId shouldBe "file-api-def456"
        page.hasMore.shouldBeTrue()
    }

    @Test
    fun `listFiles passes every query parameter it is given`() = runTest {
        var capturedParameters: Parameters? = null
        val engine = mockEngine { request ->
            capturedParameters = request.url.parameters
            jsonResponse("""{"object": "list", "data": [], "has_more": false}""")
        }
        val client = testClient(engine)

        val page = client.listFiles(
            after = "file-api-abc123",
            limit = 50,
            order = SortOrder.DESC,
            purpose = FilePurpose.USER_DATA,
        )

        val parameters = capturedParameters.shouldNotBeNull()
        parameters["after"] shouldBe "file-api-abc123"
        parameters["limit"] shouldBe "50"
        parameters["order"] shouldBe "desc"
        parameters["purpose"] shouldBe "user_data"
        page.data shouldHaveSize 0
        page.hasMore.shouldBeFalse()
        page.firstId.shouldBeNull()
        page.lastId.shouldBeNull()
    }

    @Test
    fun `listFiles rejects a page size outside the accepted range`() = runTest {
        val client = testClient(mockEngine { jsonResponse("""{"object": "list", "data": [], "has_more": false}""") })

        shouldThrow<IllegalArgumentException> { client.listFiles(limit = 0) }
        shouldThrow<IllegalArgumentException> { client.listFiles(limit = 1001) }
    }

    @Test
    fun `retrieveFile GETs a single file by id`() = runTest {
        var capturedMethod: HttpMethod? = null
        var capturedPath: String? = null
        val engine = mockEngine { request ->
            capturedMethod = request.method
            capturedPath = request.url.encodedPath
            jsonResponse(fileBody)
        }
        val client = testClient(engine)

        val file = client.retrieveFile("file-api-abc123")

        capturedMethod shouldBe HttpMethod.Get
        capturedPath.shouldNotBeNull().shouldEndWith("/files/file-api-abc123")
        file.id shouldBe "file-api-abc123"
    }

    @Test
    fun `deleteFile DELETEs the file and parses the outcome`() = runTest {
        var capturedMethod: HttpMethod? = null
        var capturedPath: String? = null
        val engine = mockEngine { request ->
            capturedMethod = request.method
            capturedPath = request.url.encodedPath
            jsonResponse("""{"id": "file-api-abc123", "object": "file", "deleted": true}""")
        }
        val client = testClient(engine)

        val deleted = client.deleteFile("file-api-abc123")

        capturedMethod shouldBe HttpMethod.Delete
        capturedPath.shouldNotBeNull().shouldEndWith("/files/file-api-abc123")
        deleted.id shouldBe "file-api-abc123"
        deleted.`object` shouldBe "file"
        deleted.deleted.shouldBeTrue()
    }

    @Test
    fun `retrieveFile and deleteFile reject a blank id`() = runTest {
        val client = testClient(mockEngine { jsonResponse(fileBody) })

        shouldThrow<IllegalArgumentException> { client.retrieveFile("  ") }
        shouldThrow<IllegalArgumentException> { client.deleteFile("") }
    }

    @Test
    fun `an uploaded file id can be referenced from a vision message`() = runTest {
        var capturedChatBody: String? = null
        val engine = mockEngine { request ->
            if (request.url.encodedPath.endsWith("/files")) {
                jsonResponse(fileBody)
            } else {
                capturedChatBody = request.body.toByteArray().decodeToString()
                jsonResponse(
                    """
                        {
                            "id": "abc-123",
                            "choices": [
                                {
                                    "finish_reason": "stop",
                                    "index": 0,
                                    "message": {"content": "A cat.", "role": "assistant"}
                                }
                            ],
                            "created": 1705651092,
                            "model": "deepseek-v4-flash-vision-exp",
                            "object": "chat.completion",
                            "usage": {"completion_tokens": 3, "prompt_tokens": 8, "total_tokens": 11}
                        }
                    """.trimIndent()
                )
            }
        }
        val client = testClient(engine)

        val uploaded = client.uploadFile(imageBytes, "cat.jpg")
        client.chatCompletion {
            params { model = ChatModel.DEEPSEEK_V4_FLASH_VISION_EXP }
            messages {
                user {
                    text("What is in this image?")
                    imageFile(uploaded.id)
                }
            }
        }

        val body = capturedChatBody.shouldNotBeNull()
        body shouldContain "\"type\":\"file\""
        body shouldContain "\"file_id\":\"file-api-abc123\""
    }

    @Test
    fun `retrieveFile maps 404 to NotFoundException with the parsed error`() = runTest {
        val engine = mockEngine {
            jsonResponse(
                """{"error": {"message": "No such file", "type": "invalid_request_error"}}""",
                HttpStatusCode.NotFound,
            )
        }
        val client = testClient(engine)

        val ex = shouldThrow<DeepSeekException.NotFoundException> { client.retrieveFile("file-api-missing") }

        ex.statusCode shouldBe 404
        ex.error?.error?.message shouldBe "No such file"
    }
}
