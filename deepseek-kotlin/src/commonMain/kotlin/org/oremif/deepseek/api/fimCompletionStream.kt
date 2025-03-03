package org.oremif.deepseek.api

import io.ktor.client.plugins.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.oremif.deepseek.client.DeepSeekClientBase
import org.oremif.deepseek.client.DeepSeekClientStream
import org.oremif.deepseek.errors.DeepSeekException
import org.oremif.deepseek.models.FIMCompletion
import org.oremif.deepseek.models.FIMCompletionParams
import org.oremif.deepseek.models.FIMCompletionRequest
import kotlin.time.Duration.Companion.minutes

public suspend fun DeepSeekClientBase.fimCompletionStream(request: FIMCompletionRequest): Flow<FIMCompletion> {
    return flow {
        try {
            client.sse(
                urlString = "beta/completions",
                request = {
                    method = HttpMethod.Post
                    accept(ContentType.Text.EventStream)
                    headers {
                        append(HttpHeaders.CacheControl, "no-cache")
                        append(HttpHeaders.Connection, "keep-alive")
                    }
                    setBody(request)
                    timeout {
                        requestTimeoutMillis = config.fimCompletionTimeout
                    }
                }
            ) {
                incoming.collect { event ->
                    event.data?.trim()?.takeIf { it != "[DONE]" }?.let { data ->
                        val fimChunk = config.jsonConfig.decodeFromString<FIMCompletion>(data)
                        emit(fimChunk)
                    }
                }
            }
        } catch (e: SSEClientException) {
            e.response?.let { response ->
                throw DeepSeekException.from(response.status.value, response.headers, null)
            }
        }
    }
}

public suspend fun DeepSeekClientStream.fim(
    params: FIMCompletionParams,
    prompt: String
): Flow<FIMCompletion> {
    val request = (if (params.stream == false) params.copy(stream = true) else params).createRequest(prompt)
    return fimCompletionStream(request)
}

public suspend fun DeepSeekClientStream.fim(prompt: String): Flow<FIMCompletion> =
    fim(FIMCompletionParams(stream = true), prompt)

public suspend fun DeepSeekClientStream.fimCompletion(
    block: FIMCompletionRequest.StreamBuilder.() -> Unit
): Flow<FIMCompletion> {
    val request = FIMCompletionRequest.StreamBuilder().apply(block).build()
    return fimCompletionStream(request)
}