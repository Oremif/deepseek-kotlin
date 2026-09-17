package org.oremif.deepseek.api

import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import org.oremif.deepseek.client.DeepSeekClient
import org.oremif.deepseek.client.DeepSeekClientBase
import org.oremif.deepseek.errors.DeepSeekException
import org.oremif.deepseek.models.responses.ModelResponse
import org.oremif.deepseek.models.responses.ResponseItem
import org.oremif.deepseek.models.responses.ResponseParams
import org.oremif.deepseek.models.responses.ResponseRequest
import org.oremif.deepseek.utils.validateResponse

/** Path of the Responses API, which is served from the standard base URL. */
internal const val RESPONSES_PATH: String = "responses"

/**
 * Sends a request to the Responses API.
 *
 * This is the low-level entry point; the [createResponse] overloads that take parameters and an
 * input are usually more convenient.
 *
 * Example:
 * ```kotlin
 * val request = responseParams { model = ChatModel.DEEPSEEK_V4_PRO }
 *     .createRequest("Explain the Kotlin type system")
 * val response = client.createResponse(request)
 * ```
 *
 * @param request The fully configured request object containing all parameters for the API call
 * @return The [ModelResponse] the API produced
 * @throws DeepSeekException if the API returns a non-2xx status
 */
public suspend fun DeepSeekClientBase.createResponse(request: ResponseRequest): ModelResponse {
    val response =
        client.post(RESPONSES_PATH) {
            setBody(request)
            timeout { requestTimeoutMillis = config.responseTimeout }
        }
    validateResponse(response, config.jsonConfig)
    return response.body()
}

/**
 * Sends a conversation to the Responses API with custom parameters.
 *
 * The API keeps no state between calls, so [input] carries the whole history — appending the
 * previous response's [ModelResponse.output] to it is what continues the conversation.
 *
 * Example:
 * ```kotlin
 * val params = responseParams { instructions = "You are a helpful assistant." }
 * val history = mutableListOf<ResponseItem>(MessageItem(ResponseRole.USER, "What is 6 * 7?"))
 *
 * val answer = client.createResponse(params, history)
 * history += answer.output
 * history += MessageItem(ResponseRole.USER, "And 6 * 8?")
 * val next = client.createResponse(params, history)
 * ```
 *
 * @param params Configuration parameters that control the model's behavior
 * @param input The conversation so far, in the order the model should see it
 * @return The [ModelResponse] the API produced
 * @throws DeepSeekException if the API returns a non-2xx status
 */
public suspend fun DeepSeekClient.createResponse(
    params: ResponseParams,
    input: List<ResponseItem>,
): ModelResponse = createResponse(params.unary().createRequest(input))

/**
 * Sends a single user message to the Responses API with custom parameters.
 *
 * Example:
 * ```kotlin
 * val params = responseParams {
 *     model = ChatModel.DEEPSEEK_V4_PRO
 *     reasoning = ReasoningConfig(ReasoningEffort.MAX)
 * }
 * val response = client.createResponse(params, "Prove that sqrt(2) is irrational")
 * ```
 *
 * @param params Configuration parameters that control the model's behavior
 * @param input Text of the user's message
 * @return The [ModelResponse] the API produced
 * @throws DeepSeekException if the API returns a non-2xx status
 */
public suspend fun DeepSeekClient.createResponse(
    params: ResponseParams,
    input: String,
): ModelResponse = createResponse(params.unary().createRequest(input))

/**
 * Sends a single user message to the Responses API with default parameters.
 *
 * Example:
 * ```kotlin
 * println(client.createResponse("What is the capital of France?").outputText)
 * ```
 *
 * @param input Text of the user's message
 * @return The [ModelResponse] the API produced
 * @throws DeepSeekException if the API returns a non-2xx status
 */
public suspend fun DeepSeekClient.createResponse(input: String): ModelResponse =
    createResponse(ResponseParams(), input)

/**
 * Sends a conversation to the Responses API with default parameters.
 *
 * @param input The conversation so far, in the order the model should see it
 * @return The [ModelResponse] the API produced
 * @throws DeepSeekException if the API returns a non-2xx status
 */
public suspend fun DeepSeekClient.createResponse(input: List<ResponseItem>): ModelResponse =
    createResponse(ResponseParams(), input)

/**
 * Sends a conversation built by a DSL, with custom parameters.
 *
 * Example:
 * ```kotlin
 * val response = client.createResponse(params) {
 *     user("What is in this image?")
 *     items(previous.output)
 *     functionCallOutput(callId, """{"temp_c": 21}""")
 * }
 * ```
 *
 * @param params Configuration parameters that control the model's behavior
 * @param block A builder block for constructing the conversation
 * @return The [ModelResponse] the API produced
 * @throws DeepSeekException if the API returns a non-2xx status
 */
public suspend fun DeepSeekClient.createResponse(
    params: ResponseParams,
    block: ResponseRequest.InputBuilder.() -> Unit,
): ModelResponse = createResponse(params, ResponseRequest.InputBuilder().apply(block).build())

/**
 * Sends a fully customizable Responses API request.
 *
 * Example:
 * ```kotlin
 * val response = client.createResponse {
 *     params {
 *         model = ChatModel.DEEPSEEK_FLASH
 *         instructions = "You are a helpful assistant."
 *         maxOutputTokens = 2000
 *     }
 *     input {
 *         user {
 *             text("What is in this image?")
 *             image("https://example.com/cat.jpg", ImageDetail.LOW)
 *         }
 *     }
 * }
 * ```
 *
 * @param block A builder block for constructing the complete request
 * @return The [ModelResponse] the API produced
 * @throws DeepSeekException if the API returns a non-2xx status
 */
public suspend fun DeepSeekClient.createResponse(
    block: ResponseRequest.Builder.() -> Unit
): ModelResponse = createResponse(ResponseRequest.Builder().apply(block).build())

/** Drops a `stream = true` left over from streaming parameters reused for a unary call. */
internal fun ResponseParams.unary(): ResponseParams =
    if (stream == true) copy(stream = false) else this

/** Turns parameters into streaming ones, so a plain [ResponseParams] can drive a streamed call. */
internal fun ResponseParams.streaming(): ResponseParams =
    if (stream == true) this else copy(stream = true)
