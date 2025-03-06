package org.oremif.deepseek.api

import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import org.oremif.deepseek.client.DeepSeekClient
import org.oremif.deepseek.client.DeepSeekClientBase
import org.oremif.deepseek.models.FIMCompletion
import org.oremif.deepseek.models.FIMCompletionParams
import org.oremif.deepseek.models.FIMCompletionRequest
import org.oremif.deepseek.utils.validateResponse

public suspend fun DeepSeekClientBase.fimCompletion(request: FIMCompletionRequest): FIMCompletion {
    val response = client.post("beta/completions") {
        setBody(request)
        timeout {
            requestTimeoutMillis = config.fimCompletionTimeout
        }
    }
    validateResponse(response)
    return response.body()
}

public suspend fun DeepSeekClient.fim(params: FIMCompletionParams, prompt: String): FIMCompletion {
    val request = (if (params.stream == true) params.copy(stream = false) else params).createRequest(prompt)
    return fimCompletion(request)
}

public suspend fun DeepSeekClient.fim(prompt: String): FIMCompletion =
    fim(FIMCompletionParams(), prompt)

public suspend fun DeepSeekClient.fimCompletion(block: FIMCompletionRequest.Builder.() -> Unit): FIMCompletion {
    val request = FIMCompletionRequest.Builder().apply(block).build()
    return fimCompletion(request)
}