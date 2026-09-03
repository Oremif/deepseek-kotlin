package org.oremif.deepseek.models

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * A file stored by the DeepSeek Files API.
 *
 * Feed [id] to a [FilePart] — or to the `imageFile(...)` step of the `user { }` DSL — to let
 * a vision model read the file:
 * ```kotlin
 * val uploaded = client.uploadFile(imageBytes, "cat.jpg")
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
 * @property id File identifier, of the form `file-api-...`.
 * @property object Object type discriminator; always `file`.
 * @property bytes Size of the file in bytes.
 * @property createdAt Unix timestamp, in seconds, of when the file was created.
 * @property filename Name the file was uploaded under.
 * @property purpose What the file may be used for; currently always [FilePurpose.USER_DATA].
 * @property expiresAt Unix timestamp, in seconds, of when the file expires. `null` for a
 * file uploaded without an expiry, which the API keeps indefinitely.
 * @see <a href="https://api-docs.deepseek.com/api/create-file">Create File</a>
 */
@Serializable
public class FileObject(
    public val id: String,
    public val `object`: String,
    public val bytes: Long,
    public val createdAt: Long,
    public val filename: String,
    public val purpose: FilePurpose,
    public val expiresAt: Long? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FileObject) return false
        return id == other.id &&
                `object` == other.`object` &&
                bytes == other.bytes &&
                createdAt == other.createdAt &&
                filename == other.filename &&
                purpose == other.purpose &&
                expiresAt == other.expiresAt
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + `object`.hashCode()
        result = 31 * result + bytes.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + filename.hashCode()
        result = 31 * result + purpose.hashCode()
        result = 31 * result + (expiresAt?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "FileObject(id='$id', object='$`object`', bytes=$bytes, createdAt=$createdAt, " +
                "filename='$filename', purpose=$purpose, expiresAt=$expiresAt)"
}

/**
 * What an uploaded file may be used for, serialized as the raw value the API expects.
 *
 * The API currently defines a single purpose, [USER_DATA], which is what the SDK sends on
 * upload. Any other value can be wrapped directly, so a purpose the SDK declares no
 * constant for still round-trips.
 *
 * @property value Raw purpose sent in — and returned by — the Files API. Must not be blank.
 */
@Serializable
@JvmInline
public value class FilePurpose(public val value: String) {
    init {
        require(value.isNotBlank()) { "file purpose must not be blank" }
    }

    override fun toString(): String = value

    public companion object {
        /** `user_data` — the file is input for a model, such as an image for a vision model. */
        public val USER_DATA: FilePurpose = FilePurpose("user_data")
    }
}
