package org.oremif.deepseek.errors

import io.ktor.http.*
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
    public val headers: Headers,
    public val error: DeepSeekError?,
    message: String? = null,
    cause: Throwable? = null,
) : RuntimeException("${error?.error?.message ?: ""}\n$message", cause) {

    public class BadRequestException(
        headers: Headers, error: DeepSeekError?, message: String?
    ) : DeepSeekException(400, headers, error, message)

    public class UnauthorizedException(
        headers: Headers, error: DeepSeekError?, message: String?
    ) : DeepSeekException(401, headers, error, message)

    public class InsufficientBalanceException(
        headers: Headers, error: DeepSeekError?, message: String?
    ) : DeepSeekException(402, headers, error, message)

    public class PermissionDeniedException(
        headers: Headers, error: DeepSeekError?, message: String?
    ) : DeepSeekException(403, headers, error, message)

    public class NotFoundException(
        headers: Headers, error: DeepSeekError?, message: String?
    ) : DeepSeekException(404, headers, error, message)

    public class UnprocessableEntityException(
        headers: Headers, error: DeepSeekError?, message: String?
    ) : DeepSeekException(422, headers, error, message)

    public class RateLimitException(
        headers: Headers, error: DeepSeekError?, message: String?,
    ) : DeepSeekException(429, headers, error, message)

    public class InternalServerException(
        headers: Headers, error: DeepSeekError?, message: String?
    ) : DeepSeekException(500, headers, error, message)

    public class OverloadServerException(
        headers: Headers, error: DeepSeekError?, message: String?
    ) : DeepSeekException(500, headers, error, message)

    public class UnexpectedStatusCodeException(
        statusCode: Int, headers: Headers, error: DeepSeekError?, message: String?
    ) : DeepSeekException(statusCode, headers, error, message)

    public companion object {
        public fun from(statusCode: Int, headers: Headers, error: DeepSeekError?, message: String): DeepSeekException =
            when (statusCode) {
                400 -> BadRequestException(headers, error, message)
                401 -> UnauthorizedException(headers, error, message)
                402 -> InsufficientBalanceException(headers, error, message)
                403 -> PermissionDeniedException(headers, error, message)
                404 -> NotFoundException(headers, error, message)
                422 -> UnprocessableEntityException(headers, error, message)
                429 -> RateLimitException(headers, error, message)
                500 -> InternalServerException(headers, error, message)
                else -> UnexpectedStatusCodeException(statusCode, headers, error, message)
            }

        public fun from(statusCode: Int, headers: Headers, error: DeepSeekError?): DeepSeekException =
            when (statusCode) {
                400 -> BadRequestException(
                    headers,
                    error,
                    "Please modify your request body according to the hints in the error message.\nFor more API format details, please refer to [DeepSeek API Docs](https://api-docs.deepseek.com/)."
                )

                401 -> UnauthorizedException(
                    headers,
                    error,
                    "Please check your API key.\nIf you don't have one, please [create an API key](https://platform.deepseek.com/api_keys) first."
                )

                402 -> InsufficientBalanceException(
                    headers,
                    error,
                    "Please check your account's balance, and go to the [Top up](https://platform.deepseek.com/top_up) page to add funds."
                )

                403 -> PermissionDeniedException(
                    headers,
                    error,
                    "Please check your API key.\nIf you don't have one, please [create an API key](https://platform.deepseek.com/api_keys) first."
                )

                404 -> NotFoundException(
                    headers,
                    error,
                    "Please check the API endpoint you are using.\nFor more API format details, please refer to [DeepSeek API Docs](https://api-docs.deepseek.com/)."
                )

                422 -> UnprocessableEntityException(
                    headers,
                    error,
                    "Please modify your request parameters according to the hints in the error message.\nFor more API format details, please refer to [DeepSeek API Docs](https://api-docs.deepseek.com/)."
                )

                429 -> RateLimitException(
                    headers,
                    error,
                    "Please pace your requests reasonably.\nWe also advise users to temporarily switch to the APIs of alternative LLM service providers, like OpenAI."
                )

                500 -> InternalServerException(
                    headers,
                    error,
                    "Please retry your request after a brief wait and contact us if the issue persists."
                )

                503 -> OverloadServerException(headers, error, "Please retry your request after a brief wait.")
                else -> UnexpectedStatusCodeException(statusCode, headers, error, "Unexpected status code: $statusCode")
            }
    }
}