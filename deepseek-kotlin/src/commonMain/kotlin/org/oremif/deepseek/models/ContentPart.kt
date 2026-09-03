@file:OptIn(ExperimentalSerializationApi::class)

package org.oremif.deepseek.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

/**
 * One element of a [UserMessage]'s multimodal content array.
 *
 * A user message carries either plain text or a list of these parts. The concrete subtypes
 * — [TextPart], [ImageUrlPart], [FilePart] — are distinguished on the wire by the `type`
 * discriminator.
 *
 * Images are only interpreted by [ChatModel.DEEPSEEK_V4_FLASH_VISION_EXP]; other models
 * replace them with a placeholder text. The API rejects image parts in system and assistant
 * messages, which is why parts are only available on [UserMessage].
 *
 * Prefer the `user { ... }` DSL over instantiating parts directly:
 * ```kotlin
 * client.chat {
 *     user {
 *         text("What is in this image?")
 *         image("https://example.com/cat.jpg", ImageDetail.LOW)
 *     }
 * }
 * ```
 *
 * @see <a href="https://api-docs.deepseek.com/guides/vision">DeepSeek vision guide</a>
 */
@Serializable
@JsonClassDiscriminator("type")
public sealed interface ContentPart

/**
 * Text fragment of a multimodal user message.
 *
 * @property text The text itself.
 */
@Serializable
@SerialName("text")
public class TextPart(public val text: String) : ContentPart {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TextPart) return false
        return text == other.text
    }

    override fun hashCode(): Int = text.hashCode()

    override fun toString(): String = "TextPart(text='$text')"
}

/**
 * Image passed to the model by URL, either remote or inline.
 *
 * @property imageUrl The image location and how much detail to preserve.
 */
@Serializable
@SerialName("image_url")
public class ImageUrlPart(public val imageUrl: ImageUrl) : ContentPart {

    /**
     * Shorthand for `ImageUrlPart(ImageUrl(url, detail))`.
     *
     * @param url `http(s)` link to the image, or a `data:` URL carrying it inline
     * @param detail How much of the image's resolution the model sees; see [ImageDetail]
     */
    public constructor(url: String, detail: ImageDetail? = null) : this(ImageUrl(url, detail))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ImageUrlPart) return false
        return imageUrl == other.imageUrl
    }

    override fun hashCode(): Int = imageUrl.hashCode()

    override fun toString(): String = "ImageUrlPart(imageUrl=$imageUrl)"
}

/**
 * Location of an image referenced by an [ImageUrlPart].
 *
 * @property url Either an `http(s)` link the API fetches, or a base64 data URL that inlines
 * the image (`data:image/jpeg;base64,...`). JPEG, PNG, GIF and WebP are supported.
 * @property detail How much of the image's resolution reaches the model; defaults to the
 * API's own choice when left unset.
 */
@Serializable
public class ImageUrl(
    public val url: String,
    public val detail: ImageDetail? = null,
) {
    init {
        require(url.isNotBlank()) { "image url must not be blank" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ImageUrl) return false
        return url == other.url && detail == other.detail
    }

    override fun hashCode(): Int {
        var result = url.hashCode()
        result = 31 * result + (detail?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String = "ImageUrl(url='$url', detail=$detail)"
}

/**
 * Values accepted by [ImageUrl.detail].
 */
@Serializable
public enum class ImageDetail {
    /** Downscales the image to 512×512 before the model sees it: faster and cheaper. */
    @SerialName("low")
    LOW,

    /** Keeps the image at its original resolution. */
    @SerialName("high")
    HIGH,

    /** Keeps the image at its original resolution. */
    @SerialName("original")
    ORIGINAL,

    /** Lets the API pick the detail level. */
    @SerialName("auto")
    AUTO,
}

/**
 * Image passed to the model as a file — either one already uploaded to the Files API, or one
 * inlined as base64.
 *
 * Exactly one of [fileId] and [fileData] must be set.
 *
 * @property fileId Identifier of a file uploaded to the Files API, of the form
 * `file-api-...`. Mutually exclusive with [fileData].
 * @property fileData Base64 data URL carrying the image inline
 * (`data:image/jpeg;base64,...`). Mutually exclusive with [fileId].
 * @property filename Optional name for the inlined image; only valid alongside [fileData].
 */
@Serializable
@SerialName("file")
public class FilePart(
    public val fileId: String? = null,
    public val fileData: String? = null,
    public val filename: String? = null,
) : ContentPart {
    init {
        require((fileId == null) != (fileData == null)) {
            "a file part must carry exactly one of fileId and fileData"
        }
        require(filename == null || fileData != null) {
            "filename is only valid together with fileData"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FilePart) return false
        return fileId == other.fileId && fileData == other.fileData && filename == other.filename
    }

    override fun hashCode(): Int {
        var result = fileId?.hashCode() ?: 0
        result = 31 * result + (fileData?.hashCode() ?: 0)
        result = 31 * result + (filename?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "FilePart(fileId=$fileId, fileData=$fileData, filename=$filename)"
}
