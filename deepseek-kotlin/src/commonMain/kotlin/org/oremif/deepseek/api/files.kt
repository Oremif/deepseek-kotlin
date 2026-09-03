package org.oremif.deepseek.api

import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import org.oremif.deepseek.client.DeepSeekClientBase
import org.oremif.deepseek.errors.DeepSeekException
import org.oremif.deepseek.models.*
import org.oremif.deepseek.utils.validateResponse

/** Shortest and longest lifetime the API accepts for an uploaded file: one hour to 30 days. */
private val EXPIRY_RANGE = 3600..2_592_000

/** Narrowest and widest page the listing endpoint serves. */
private val LIMIT_RANGE = 1..1000

/**
 * Uploads an image for a vision model to read, and returns the stored [FileObject].
 *
 * The file is sent as `multipart/form-data` with purpose [FilePurpose.USER_DATA]. Its format
 * is detected from the bytes themselves — JPEG, PNG, GIF and WebP are supported — so
 * [filename] is only a label. The API caps an upload at 64 MiB.
 *
 * Uploads use [uploadTimeout][org.oremif.deepseek.client.DeepSeekClientConfig.uploadTimeout]
 * rather than the client's default request timeout, since a large file on a slow link needs
 * longer than a chat call.
 *
 * Example:
 * ```kotlin
 * val uploaded = client.uploadFile(imageBytes, "cat.jpg", expiresAfterSeconds = 3600)
 * client.chatCompletion {
 *     params { model = ChatModel.DEEPSEEK_V4_FLASH_VISION_EXP }
 *     messages {
 *         user {
 *             text("What is in this image?")
 *             imageFile(uploaded.id)
 *         }
 *     }
 * }
 * ```
 *
 * @param bytes Raw file content; must not be empty
 * @param filename Name to store the file under; must not be blank or contain a line break
 * @param expiresAfterSeconds Lifetime of the file, anchored at its creation, between 3600
 * and 2592000 seconds. Left `null`, the file is kept indefinitely.
 * @return The stored [FileObject], whose `id` can be referenced from a chat message
 * @throws IllegalArgumentException if [bytes] is empty, [filename] is unusable, or
 * [expiresAfterSeconds] falls outside 3600..2592000
 * @throws DeepSeekException if the API returns a non-2xx status
 */
public suspend fun DeepSeekClientBase.uploadFile(
    bytes: ByteArray,
    filename: String,
    expiresAfterSeconds: Int? = null,
): FileObject {
    require(bytes.isNotEmpty()) { "file must not be empty" }
    require(filename.isNotBlank()) { "filename must not be blank" }
    require(filename.none { it == '\r' || it == '\n' }) { "filename must not contain a line break" }
    expiresAfterSeconds?.let {
        require(it in EXPIRY_RANGE) {
            "expiresAfterSeconds must be between ${EXPIRY_RANGE.first} and ${EXPIRY_RANGE.last}, was $it"
        }
    }

    val parts = formData {
        append("file", bytes, Headers.build {
            append(HttpHeaders.ContentDisposition, "filename=\"${filename.escapeQuotes()}\"")
        })
        append("purpose", FilePurpose.USER_DATA.value)
        expiresAfterSeconds?.let {
            append("expires_after[anchor]", "created_at")
            append("expires_after[seconds]", it)
        }
    }

    val response = client.post("files") {
        setBody(MultiPartFormDataContent(parts))
        timeout {
            requestTimeoutMillis = config.uploadTimeout
        }
    }
    validateResponse(response)
    return response.body()
}

/**
 * Lists stored files, oldest first, one page at a time.
 *
 * Example — walk every page:
 * ```kotlin
 * var page = client.listFiles(limit = 100)
 * while (page.hasMore) {
 *     page.data.forEach { println("${it.id} ${it.filename}") }
 *     page = client.listFiles(after = page.lastId, limit = 100)
 * }
 * ```
 *
 * @param after Identifier of the file to resume after, typically [FileList.lastId] of the
 * previous page; `null` starts from the beginning
 * @param limit How many files to return, between 1 and 1000; the API defaults to 1000
 * @param order Whether to sort by creation time ascending or descending; the API defaults
 * to [SortOrder.ASC]
 * @param purpose Restricts the listing to files with this purpose
 * @return One [FileList] page
 * @throws IllegalArgumentException if [limit] falls outside 1..1000
 * @throws DeepSeekException if the API returns a non-2xx status
 */
public suspend fun DeepSeekClientBase.listFiles(
    after: String? = null,
    limit: Int? = null,
    order: SortOrder? = null,
    purpose: FilePurpose? = null,
): FileList {
    limit?.let {
        require(it in LIMIT_RANGE) {
            "limit must be between ${LIMIT_RANGE.first} and ${LIMIT_RANGE.last}, was $it"
        }
    }

    // `parameter` drops a null value, so an unset argument leaves the query string alone.
    val response = client.get("files") {
        parameter("after", after)
        parameter("limit", limit)
        parameter("order", order?.value)
        parameter("purpose", purpose?.value)
    }
    validateResponse(response)
    return response.body()
}

/**
 * Fetches the metadata of a single stored file.
 *
 * Example:
 * ```kotlin
 * val file = client.retrieveFile("file-api-abc123")
 * println("${file.filename}: ${file.bytes} bytes")
 * ```
 *
 * @param fileId Identifier of the file, of the form `file-api-...`
 * @return The stored [FileObject]
 * @throws IllegalArgumentException if [fileId] is blank
 * @throws DeepSeekException if the API returns a non-2xx status, including when no such
 * file exists
 */
public suspend fun DeepSeekClientBase.retrieveFile(fileId: String): FileObject {
    require(fileId.isNotBlank()) { "fileId must not be blank" }

    val response = client.get("files/${fileId.encodeURLPathPart()}")
    validateResponse(response)
    return response.body()
}

/**
 * Deletes a stored file.
 *
 * Example:
 * ```kotlin
 * if (client.deleteFile("file-api-abc123").deleted) println("gone")
 * ```
 *
 * @param fileId Identifier of the file, of the form `file-api-...`
 * @return A [FileDeleted] confirming the outcome
 * @throws IllegalArgumentException if [fileId] is blank
 * @throws DeepSeekException if the API returns a non-2xx status, including when no such
 * file exists
 */
public suspend fun DeepSeekClientBase.deleteFile(fileId: String): FileDeleted {
    require(fileId.isNotBlank()) { "fileId must not be blank" }

    val response = client.delete("files/${fileId.encodeURLPathPart()}")
    validateResponse(response)
    return response.body()
}

/**
 * Escapes a filename for the quoted-string form of the `Content-Disposition` header, so a
 * quote in the name cannot break out of the multipart part.
 */
private fun String.escapeQuotes(): String = replace("\\", "\\\\").replace("\"", "\\\"")
