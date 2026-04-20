package org.oremif.deepseek.testing

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import org.oremif.deepseek.client.DeepSeekClient
import org.oremif.deepseek.client.DeepSeekClientStream

internal class SseMockEngine(config: MockEngineConfig) : MockEngine(config) {
    override val supportedCapabilities: Set<HttpClientEngineCapability<out Any>> =
        super.supportedCapabilities + SSECapability

    @OptIn(InternalAPI::class)
    override suspend fun execute(data: HttpRequestData): HttpResponseData {
        val response = super.execute(data)
        val body = response.body
        if (body !is ByteReadChannel) return response
        val adapter = data.attributes.getOrNull(ResponseAdapterAttributeKey) ?: return response
        val adapted = adapter.adapt(
            data,
            response.statusCode,
            response.headers,
            body,
            data.body,
            response.callContext,
        ) ?: return response
        return HttpResponseData(
            response.statusCode,
            response.requestTime,
            response.headers,
            response.version,
            adapted,
            response.callContext,
        )
    }
}

internal fun sseMockEngine(handler: MockRequestHandler): SseMockEngine =
    SseMockEngine(MockEngineConfig().apply { requestHandlers.add(handler) })

internal fun mockEngine(handler: MockRequestHandler): MockEngine =
    MockEngine(MockEngineConfig().apply { requestHandlers.add(handler) })

@OptIn(ExperimentalSerializationApi::class)
internal val TestJson: Json = Json {
    ignoreUnknownKeys = true
    namingStrategy = JsonNamingStrategy.SnakeCase
}

internal fun testClient(
    engine: HttpClientEngine,
    token: String = "test-token",
): DeepSeekClient {
    val http = HttpClient(engine) {
        install(ContentNegotiation) { json(TestJson) }
        defaultRequest {
            url("https://api.deepseek.com")
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }
    return DeepSeekClient {
        jsonConfig(TestJson)
        httpClient(http)
    }
}

internal fun testStreamClient(
    engine: HttpClientEngine,
    token: String = "test-token",
): DeepSeekClientStream {
    val http = HttpClient(engine) {
        install(ContentNegotiation) { json(TestJson) }
        install(SSE)
        defaultRequest {
            url("https://api.deepseek.com")
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }
    return DeepSeekClientStream {
        jsonConfig(TestJson)
        httpClient(http)
    }
}
