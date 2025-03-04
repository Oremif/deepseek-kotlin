# DeepSeek Kotlin API Library

The DeepSeek Multiplatform Kotlin SDK provides convenient access to the DeepSeek REST API from applications written in
Kotlin.

The REST API documentation can be found on [api-docs.deepseek.com](https://api-docs.deepseek.com/).

## Installation

### Gradle

```kotlin
implementation("org.oremif:deepseek-kotlin:$deepseek_kotlin_version")
```

## Requirements

This library requires Java 11 or later.

## Usage

see the [`example`](example) directory for complete and runnable examples.

```kotlin
import org.oremif.deepseek.api.chat
import org.oremif.deepseek.client.DeepSeekClient
import org.oremif.deepseek.models.ChatModel

val deepseekApiKey = System.getenv("DEEPSEEK_API_KEY")
val client = DeepSeekClient(deepseekApiKey)


val chatCompletion = client.chat("Say this is a test")
```

## Client Configuration

Configure the client:

```kotlin
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.sse.SSE
import kotlinx.serialization.json.Json
import org.oremif.deepseek.client.DeepSeekClient

val deepseekApiKey = System.getenv("DEEPSEEK_API_KEY")
val client = DeepSeekClient(deepseekApiKey) {
    jsonConfig {
        Json { ignoreUnknownKeys = true }
    }
    httpClient {
        install(SSE)
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.HEADERS
            sanitizeHeader { header -> header == "Authorization" }
        }
    }

    chatCompletionTimeout(10_000)
    fimCompletionTimeout(10_000)
}
```

## Request and responses

To send a request to the DeepSeek,
you can build an instance of some Params class and pass it to the corresponding client method or use methods directly.
When the response is received, it will be deserialized into an instance of a Kotlin class.

```kotlin
import org.oremif.deepseek.api.chat
import org.oremif.deepseek.client.DeepSeekClient
import org.oremif.deepseek.models.ChatCompletion
import org.oremif.deepseek.models.ChatModel
import org.oremif.deepseek.models.chatCompletionParams

val deepseekApiKey = System.getenv("DEEPSEEK_API_KEY")
val client = DeepSeekClient(deepseekApiKey)

val params = chatCompletionParams {
    model = ChatModel.DEEPSEEK_CHAT
    temperature = 0.3
}

val response: ChatCompletion = client.chat(params) {
    system("You are a helpful assistant")
    user("Tell me some joke")
}
```

or

```kotlin
import org.oremif.deepseek.api.chat
import org.oremif.deepseek.client.DeepSeekClient
import org.oremif.deepseek.models.ChatCompletion
import org.oremif.deepseek.models.ChatModel
import org.oremif.deepseek.models.SystemMessage
import org.oremif.deepseek.models.UserMessage
import org.oremif.deepseek.models.chatCompletionParams

val deepseekApiKey = System.getenv("DEEPSEEK_API_KEY")
val client = DeepSeekClient(deepseekApiKey)
    
val params = chatCompletionParams { 
        model = ChatModel.DEEPSEEK_CHAT
        temperature = 0.3
}
    
val listOfMessages = listOf(
        SystemMessage("You are a helpful assistant"),
        UserMessage("Tell me some joke"),
)
    
val response: ChatCompletion = client.chat(params, listOfMessages)
```

### Streaming

The SDK defines methods that return response "chunk" streams,
where each chunk can be individually processed as soon as it arrives instead of waiting on the full response.
Streaming methods generally correspond to [SSE](https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events)
responses.

```kotlin
import kotlinx.coroutines.flow.Flow
import org.oremif.deepseek.api.chat
import org.oremif.deepseek.client.DeepSeekClientStream
import org.oremif.deepseek.models.ChatCompletionChunk
import org.oremif.deepseek.models.ChatModel
import org.oremif.deepseek.models.chatCompletionStreamParams

val deepseekApiKey = System.getenv("DEEPSEEK_API_KEY")
val client = DeepSeekClientStream(deepseekApiKey)

val response: Flow<ChatCompletionChunk> = client.chatCompletion {
    params {
        model = ChatModel.DEEPSEEK_CHAT
        temperature = 0.3
    }
    messages {
        system("You are a helpful assistant")
        user("Tell me some joke")
    }
}
```

or

```kotlin
import kotlinx.coroutines.flow.Flow
import org.oremif.deepseek.api.chat
import org.oremif.deepseek.client.DeepSeekClientStream
import org.oremif.deepseek.models.ChatCompletionChunk
import org.oremif.deepseek.models.ChatModel
import org.oremif.deepseek.models.chatCompletionStreamParams

val deepseekApiKey = System.getenv("DEEPSEEK_API_KEY")
val client = DeepSeekClientStream(deepseekApiKey)

val params = chatCompletionStreamParams {
    model = ChatModel.DEEPSEEK_CHAT
    temperature = 0.3
}

val response: Flow<ChatCompletionChunk> = client.chat(params) {
    system("You are a helpful assistant")
    user("Tell me some joke")
}
```
