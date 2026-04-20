package org.oremif.deepseek.errors

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
public class DeepSeekError(
    public val error: Error
) {
    @Serializable
    public class Error(
        public val message: String? = null,
        public val type: String? = null,
        public val param: JsonObject? = null,
        public val code: String? = null,
    ) {
        override fun toString(): String = "Error(message=$message, type=$type, param=$param, code=$code)"
    }

    override fun toString(): String = "DeepSeekError(error=$error)"
}

public sealed class DeepSeekException(
    public val statusCode: Int,
    public val headers: DeepSeekHeaders,
    public val error: DeepSeekError?,
    message: String? = null,
    cause: Throwable? = null,
) : RuntimeException(listOfNotNull(error?.error?.message, message).joinToString("\n"), cause) {

    public class BadRequestException(
        headers: DeepSeekHeaders, error: DeepSeekError?, message: String?
    ) : DeepSeekException(400, headers, error, message)

    public class UnauthorizedException(
        headers: DeepSeekHeaders, error: DeepSeekError?, message: String?
    ) : DeepSeekException(401, headers, error, message)

    public class InsufficientBalanceException(
        headers: DeepSeekHeaders, error: DeepSeekError?, message: String?
    ) : DeepSeekException(402, headers, error, message)

    public class PermissionDeniedException(
        headers: DeepSeekHeaders, error: DeepSeekError?, message: String?
    ) : DeepSeekException(403, headers, error, message)

    public class NotFoundException(
        headers: DeepSeekHeaders, error: DeepSeekError?, message: String?
    ) : DeepSeekException(404, headers, error, message)

    public class UnprocessableEntityException(
        headers: DeepSeekHeaders, error: DeepSeekError?, message: String?
    ) : DeepSeekException(422, headers, error, message)

    public class RateLimitException(
        headers: DeepSeekHeaders, error: DeepSeekError?, message: String?,
    ) : DeepSeekException(429, headers, error, message)

    public class InternalServerException(
        headers: DeepSeekHeaders, error: DeepSeekError?, message: String?
    ) : DeepSeekException(500, headers, error, message)

    public class OverloadServerException(
        headers: DeepSeekHeaders, error: DeepSeekError?, message: String?
    ) : DeepSeekException(503, headers, error, message)

    public class UnexpectedStatusCodeException(
        statusCode: Int, headers: DeepSeekHeaders, error: DeepSeekError?, message: String?
    ) : DeepSeekException(statusCode, headers, error, message)

    public companion object {
        public fun from(
            statusCode: Int, headers: DeepSeekHeaders, error: DeepSeekError?, message: String
        ): DeepSeekException =
            create(statusCode, headers, error, message)

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
