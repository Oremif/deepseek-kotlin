package org.oremif.deepseek.errors

import kotlinx.serialization.Serializable

/**
 * JSON body returned by the DeepSeek API when a request fails.
 *
 * The SDK deserialises this payload and wraps it inside a [DeepSeekException] together with
 * the HTTP status code and headers. Use [DeepSeekException.error] to access it from a
 * caught exception.
 *
 * @property error The nested error object describing what went wrong
 */
@Serializable
public class DeepSeekError(
    public val error: Error
) {
    /**
     * Details about a DeepSeek API error.
     *
     * All fields are nullable because the API does not guarantee their presence on every
     * failure mode.
     *
     * @property message Human-readable description of what went wrong
     * @property type High-level category of the error (e.g. `invalid_request_error`)
     * @property param Name of the request parameter that caused the error, when applicable
     * @property code Machine-readable error code, when applicable
     */
    @Serializable
    public class Error(
        public val message: String? = null,
        public val type: String? = null,
        public val param: String? = null,
        public val code: String? = null,
    ) {
        override fun toString(): String = "Error(message=$message, type=$type, param=$param, code=$code)"
    }

    override fun toString(): String = "DeepSeekError(error=$error)"
}

/**
 * Base class for all exceptions thrown by DeepSeek SDK endpoint calls.
 *
 * Thrown by every endpoint extension function (chat, FIM, models, userBalance, and their
 * streaming counterparts) when the API returns a non-2xx status. Use [statusCode] and the
 * subclass type to distinguish failure modes, [error] to inspect the structured error
 * payload, and [headers] for rate-limit or retry hints.
 *
 * Example:
 * ```kotlin
 * try {
 *     client.chat("hello")
 * } catch (e: DeepSeekException.RateLimitException) {
 *     val retryAfter = e.headers["Retry-After"]
 *     // back off and retry
 * } catch (e: DeepSeekException) {
 *     println("DeepSeek call failed with ${e.statusCode}: ${e.message}")
 * }
 * ```
 *
 * @property statusCode HTTP status code returned by the API
 * @property headers Response headers snapshot (case-insensitive lookups)
 * @property error Parsed error payload, or `null` if the body could not be decoded
 */
public sealed class DeepSeekException(
    public val statusCode: Int,
    public val headers: DeepSeekHeaders,
    public val error: DeepSeekError?,
    message: String? = null,
    cause: Throwable? = null,
) : RuntimeException(listOfNotNull(error?.error?.message, message).joinToString("\n"), cause) {

    /**
     * Thrown for HTTP 400 — the request body or parameters are malformed.
     *
     * Inspect [error] for details on which field is invalid and adjust the request
     * accordingly before retrying.
     */
    public class BadRequestException(
        headers: DeepSeekHeaders, error: DeepSeekError?, message: String?
    ) : DeepSeekException(400, headers, error, message)

    /**
     * Thrown for HTTP 401 — the API key is missing, invalid, or revoked.
     *
     * Verify the token passed to [org.oremif.deepseek.client.DeepSeekClient] or create a new
     * key at [https://platform.deepseek.com/api_keys](https://platform.deepseek.com/api_keys).
     */
    public class UnauthorizedException(
        headers: DeepSeekHeaders, error: DeepSeekError?, message: String?
    ) : DeepSeekException(401, headers, error, message)

    /**
     * Thrown for HTTP 402 — the account has insufficient balance to complete the call.
     *
     * Top up the account at [https://platform.deepseek.com/top_up](https://platform.deepseek.com/top_up)
     * or check the current balance with
     * [org.oremif.deepseek.api.userBalance].
     */
    public class InsufficientBalanceException(
        headers: DeepSeekHeaders, error: DeepSeekError?, message: String?
    ) : DeepSeekException(402, headers, error, message)

    /**
     * Thrown for HTTP 403 — the API key is valid but lacks permission for the resource.
     */
    public class PermissionDeniedException(
        headers: DeepSeekHeaders, error: DeepSeekError?, message: String?
    ) : DeepSeekException(403, headers, error, message)

    /**
     * Thrown for HTTP 404 — the endpoint or model referenced by the request does not exist.
     */
    public class NotFoundException(
        headers: DeepSeekHeaders, error: DeepSeekError?, message: String?
    ) : DeepSeekException(404, headers, error, message)

    /**
     * Thrown for HTTP 422 — the request is syntactically valid but semantically invalid
     * (for example, a parameter is outside its allowed range).
     */
    public class UnprocessableEntityException(
        headers: DeepSeekHeaders, error: DeepSeekError?, message: String?
    ) : DeepSeekException(422, headers, error, message)

    /**
     * Thrown for HTTP 429 — the client exceeded the allowed request rate.
     *
     * Consult the `Retry-After` response header (available via [headers]) before retrying.
     */
    public class RateLimitException(
        headers: DeepSeekHeaders, error: DeepSeekError?, message: String?,
    ) : DeepSeekException(429, headers, error, message)

    /**
     * Thrown for HTTP 500 — the DeepSeek service failed to process the request.
     *
     * Retry after a brief wait; if the problem persists, contact DeepSeek support.
     */
    public class InternalServerException(
        headers: DeepSeekHeaders, error: DeepSeekError?, message: String?
    ) : DeepSeekException(500, headers, error, message)

    /**
     * Thrown for HTTP 503 — the DeepSeek service is temporarily overloaded.
     *
     * Retry after a brief wait. The Ktor retry plugin already retries this status
     * automatically up to the default retry limit.
     */
    public class OverloadServerException(
        headers: DeepSeekHeaders, error: DeepSeekError?, message: String?
    ) : DeepSeekException(503, headers, error, message)

    /**
     * Thrown for any non-2xx status not covered by the other subclasses.
     */
    public class UnexpectedStatusCodeException(
        statusCode: Int, headers: DeepSeekHeaders, error: DeepSeekError?, message: String?
    ) : DeepSeekException(statusCode, headers, error, message)

    public companion object {
        /**
         * Creates the matching [DeepSeekException] subtype for [statusCode].
         *
         * Example:
         * ```kotlin
         * throw DeepSeekException.from(response.status.value, headers, error, "custom message")
         * ```
         *
         * @param statusCode HTTP status code returned by the API
         * @param headers Response headers snapshot
         * @param error Parsed error payload, or `null` if the body could not be decoded
         * @param message Human-readable message attached to the exception
         * @return A concrete [DeepSeekException] matching [statusCode]
         */
        public fun from(
            statusCode: Int, headers: DeepSeekHeaders, error: DeepSeekError?, message: String
        ): DeepSeekException =
            create(statusCode, headers, error, message)

        /**
         * Creates the matching [DeepSeekException] subtype for [statusCode], using a built-in
         * default message that hints at the likely cause and the remediation step.
         *
         * @param statusCode HTTP status code returned by the API
         * @param headers Response headers snapshot
         * @param error Parsed error payload, or `null` if the body could not be decoded
         * @return A concrete [DeepSeekException] matching [statusCode]
         */
        public fun from(statusCode: Int, headers: DeepSeekHeaders, error: DeepSeekError?): DeepSeekException =
            create(statusCode, headers, error, defaultMessageFor(statusCode))

        private fun create(
            statusCode: Int, headers: DeepSeekHeaders, error: DeepSeekError?, message: String
        ): DeepSeekException = when (statusCode) {
            400 -> BadRequestException(headers, error, message)
            401 -> UnauthorizedException(headers, error, message)
            402 -> InsufficientBalanceException(headers, error, message)
            403 -> PermissionDeniedException(headers, error, message)
            404 -> NotFoundException(headers, error, message)
            422 -> UnprocessableEntityException(headers, error, message)
            429 -> RateLimitException(headers, error, message)
            500 -> InternalServerException(headers, error, message)
            503 -> OverloadServerException(headers, error, message)
            else -> UnexpectedStatusCodeException(statusCode, headers, error, message)
        }

        private fun defaultMessageFor(statusCode: Int): String = when (statusCode) {
            400 -> "Please modify your request body according to the hints in the error message.\nFor more API format details, please refer to [DeepSeek API Docs](https://api-docs.deepseek.com/)."
            401 -> "Please check your API key.\nIf you don't have one, please [create an API key](https://platform.deepseek.com/api_keys) first."
            402 -> "Please check your account's balance, and go to the [Top up](https://platform.deepseek.com/top_up) page to add funds."
            403 -> "Please check your API key.\nIf you don't have one, please [create an API key](https://platform.deepseek.com/api_keys) first."
            404 -> "Please check the API endpoint you are using.\nFor more API format details, please refer to [DeepSeek API Docs](https://api-docs.deepseek.com/)."
            422 -> "Please modify your request parameters according to the hints in the error message.\nFor more API format details, please refer to [DeepSeek API Docs](https://api-docs.deepseek.com/)."
            429 -> "Please pace your requests reasonably.\nWe also advise users to temporarily switch to the APIs of alternative LLM service providers, like OpenAI."
            500 -> "Please retry your request after a brief wait and contact us if the issue persists."
            503 -> "Please retry your request after a brief wait."
            else -> "Unexpected status code: $statusCode"
        }
    }
}
