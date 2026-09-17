package org.oremif.deepseek.models.responses

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * A response produced by the Responses API.
 *
 * [output] lists the items the model produced, in order: the [ReasoningItem] of a thinking model
 * comes first, then the assistant [MessageItem], with [FunctionCallItem] and [WebSearchCallItem]
 * interleaved as the model calls tools. Because the API is stateless, feeding [output] straight
 * back into the next request's input is what continues the conversation.
 *
 * Example:
 * ```kotlin
 * val response = client.createResponse("What is the capital of France?")
 * println(response.outputText)
 * println(response.usage?.totalTokens)
 * ```
 *
 * @property id Unique identifier of the response.
 * @property object Object type discriminator; always `response`.
 * @property createdAt Unix timestamp, in seconds, of when the response was created.
 * @property status Whether the response completed, was truncated, or failed.
 * @property error Set when [status] is [ResponseStatus.FAILED], describing what went wrong.
 * @property incompleteDetails Set when [status] is [ResponseStatus.INCOMPLETE], saying why the
 *   response was cut short.
 * @property model Model that produced the response.
 * @property output Items the model produced, in generation order.
 * @property usage Token usage statistics; absent while the response is still in progress.
 * @see <a href="https://api-docs.deepseek.com/api/create-response">Create Response</a>
 */
@Serializable
public class ModelResponse(
    public val id: String,
    public val `object`: String,
    public val createdAt: Long,
    public val status: ResponseStatus,
    public val error: ResponseError? = null,
    public val incompleteDetails: IncompleteDetails? = null,
    public val model: String,
    public val output: List<ResponseItem> = emptyList(),
    public val usage: ResponseUsage? = null,
) {

    /**
     * Text of every [OutputTextPart] the model produced, concatenated in order.
     *
     * This is the plain answer, without the chain-of-thought — reasoning travels in
     * [ReasoningItem]s and is left out. Empty when the model produced no text, for instance when it
     * only called a tool.
     */
    public val outputText: String
        get() =
            output.filterIsInstance<MessageItem>().joinToString(separator = "") { message ->
                when (val content = message.content) {
                    is ResponseContent.Text -> content.text
                    is ResponseContent.Parts ->
                        content.parts.filterIsInstance<OutputTextPart>().joinToString(
                            separator = ""
                        ) {
                            it.text
                        }
                    null -> ""
                }
            }

    /** The function calls in [output], in the order the model emitted them. */
    public val functionCalls: List<FunctionCallItem>
        get() = output.filterIsInstance<FunctionCallItem>()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ModelResponse) return false
        return id == other.id &&
            `object` == other.`object` &&
            createdAt == other.createdAt &&
            status == other.status &&
            error == other.error &&
            incompleteDetails == other.incompleteDetails &&
            model == other.model &&
            output == other.output &&
            usage == other.usage
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + `object`.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + error.hashCode()
        result = 31 * result + incompleteDetails.hashCode()
        result = 31 * result + model.hashCode()
        result = 31 * result + output.hashCode()
        result = 31 * result + usage.hashCode()
        return result
    }

    override fun toString(): String =
        "ModelResponse(id='$id', object='$`object`', createdAt=$createdAt, status=$status, " +
            "error=$error, incompleteDetails=$incompleteDetails, model='$model', output=$output, usage=$usage)"
}

/**
 * State of a [ModelResponse] or of a single [ResponseItem], serialized as the raw value the API
 * sends.
 *
 * A closed enum would fail on any status the API adds later, so the value is wrapped instead:
 * `ResponseStatus("queued")` stays readable.
 *
 * @property value Raw status the API sent. Must not be blank.
 */
@Serializable
@JvmInline
public value class ResponseStatus(public val value: String) {
    init {
        require(value.isNotBlank()) { "status must not be blank" }
    }

    override fun toString(): String = value

    public companion object {
        /** `in_progress` — the response is still being generated. */
        public val IN_PROGRESS: ResponseStatus = ResponseStatus("in_progress")

        /** `completed` — the model finished on its own. */
        public val COMPLETED: ResponseStatus = ResponseStatus("completed")

        /** `incomplete` — generation stopped early; see [ModelResponse.incompleteDetails]. */
        public val INCOMPLETE: ResponseStatus = ResponseStatus("incomplete")

        /** `failed` — generation failed; see [ModelResponse.error]. Responses only. */
        public val FAILED: ResponseStatus = ResponseStatus("failed")
    }
}

/**
 * What went wrong on a [ResponseStatus.FAILED] response.
 *
 * @property code Machine-readable error code.
 * @property message Human-readable description of the failure.
 */
@Serializable
public class ResponseError(
    public val code: String? = null,
    public val message: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ResponseError) return false
        return code == other.code && message == other.message
    }

    override fun hashCode(): Int {
        var result = code?.hashCode() ?: 0
        result = 31 * result + (message?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String = "ResponseError(code=$code, message=$message)"
}

/**
 * Why a [ResponseStatus.INCOMPLETE] response stopped short.
 *
 * @property reason What truncated the response; see [IncompleteReason].
 */
@Serializable
public class IncompleteDetails(public val reason: IncompleteReason? = null) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IncompleteDetails) return false
        return reason == other.reason
    }

    override fun hashCode(): Int = reason?.hashCode() ?: 0

    override fun toString(): String = "IncompleteDetails(reason=$reason)"
}

/**
 * Reason a response was truncated, serialized as the raw value the API sends.
 *
 * @property value Raw reason the API sent. Must not be blank.
 */
@Serializable
@JvmInline
public value class IncompleteReason(public val value: String) {
    init {
        require(value.isNotBlank()) { "incomplete reason must not be blank" }
    }

    override fun toString(): String = value

    public companion object {
        /** `max_output_tokens` — generation hit the `max_output_tokens` ceiling. */
        public val MAX_OUTPUT_TOKENS: IncompleteReason = IncompleteReason("max_output_tokens")

        /** `content_filter` — generation was stopped by the content filter. */
        public val CONTENT_FILTER: IncompleteReason = IncompleteReason("content_filter")
    }
}
