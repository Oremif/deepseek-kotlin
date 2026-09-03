package org.oremif.deepseek.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One page of the files stored by the Files API.
 *
 * Pages are walked with the [lastId] cursor:
 * ```kotlin
 * var page = client.listFiles()
 * while (page.hasMore) {
 *     page.data.forEach { println(it.filename) }
 *     page = client.listFiles(after = page.lastId)
 * }
 * ```
 *
 * @property object Object type discriminator; always `list`.
 * @property data Files on this page, ordered by creation time per the requested [SortOrder].
 * @property hasMore `true` when further files follow this page.
 * @property firstId Identifier of the first file on the page; `null` for an empty page.
 * @property lastId Identifier of the last file on the page; pass it as `after` to fetch the
 * next page. `null` for an empty page.
 * @see <a href="https://api-docs.deepseek.com/api/list-files">List Files</a>
 */
@Serializable
public class FileList(
    public val `object`: String,
    public val data: List<FileObject>,
    public val hasMore: Boolean,
    public val firstId: String? = null,
    public val lastId: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FileList) return false
        return `object` == other.`object` &&
                data == other.data &&
                hasMore == other.hasMore &&
                firstId == other.firstId &&
                lastId == other.lastId
    }

    override fun hashCode(): Int {
        var result = `object`.hashCode()
        result = 31 * result + data.hashCode()
        result = 31 * result + hasMore.hashCode()
        result = 31 * result + (firstId?.hashCode() ?: 0)
        result = 31 * result + (lastId?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "FileList(object='$`object`', data=$data, hasMore=$hasMore, firstId=$firstId, lastId=$lastId)"
}

/**
 * Direction a listing endpoint sorts by.
 *
 * @property value Raw value sent as a query parameter.
 */
@Serializable
public enum class SortOrder(public val value: String) {
    /** Oldest first — what a listing endpoint falls back to when no order is requested. */
    @SerialName("asc")
    ASC("asc"),

    /** Newest first. */
    @SerialName("desc")
    DESC("desc"),
}
