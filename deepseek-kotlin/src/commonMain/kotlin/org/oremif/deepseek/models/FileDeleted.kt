package org.oremif.deepseek.models

import kotlinx.serialization.Serializable

/**
 * Outcome of removing a file from the Files API.
 *
 * @property id Identifier of the file the request targeted.
 * @property object Object type discriminator; always `file`.
 * @property deleted `true` when the file was removed.
 * @see <a href="https://api-docs.deepseek.com/api/delete-file">Delete File</a>
 */
@Serializable
public class FileDeleted(
    public val id: String,
    public val `object`: String,
    public val deleted: Boolean,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FileDeleted) return false
        return id == other.id && `object` == other.`object` && deleted == other.deleted
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + `object`.hashCode()
        result = 31 * result + deleted.hashCode()
        return result
    }

    override fun toString(): String =
        "FileDeleted(id='$id', object='$`object`', deleted=$deleted)"
}
