# DeepSeek Kotlin SDK

[![Maven Central](https://img.shields.io/maven-central/v/org.oremif/deepseek-kotlin)](https://central.sonatype.com/artifact/org.oremif/deepseek-kotlin)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/kotlin-multiplatform-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![CI](https://github.com/Oremif/deepseek-kotlin/actions/workflows/ci.yml/badge.svg)](https://github.com/Oremif/deepseek-kotlin/actions/workflows/ci.yml)

A Kotlin Multiplatform SDK for the [DeepSeek REST API](https://api-docs.deepseek.com/). Idiomatic, coroutine-first, and built on top of [Ktor](https://ktor.io/).

## Table of Contents

- [Features](#features)
- [Supported Platforms](#supported-platforms)
- [Requirements](#requirements)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Usage](#usage)
  - [Creating a client](#creating-a-client)
  - [Chat completion](#chat-completion)
  - [Streaming chat](#streaming-chat)
  - [Reasoning model](#reasoning-model)
  - [Fill-In-the-Middle (FIM)](#fill-in-the-middle-fim)
  - [JSON output mode](#json-output-mode)
  - [Tool / function calling](#tool--function-calling)
  - [List models](#list-models)
  - [Account balance](#account-balance)
- [Client Configuration](#client-configuration)
- [Error Handling](#error-handling)
- [Documentation](#documentation)
- [Examples](#examples)
- [Contributing](#contributing)
- [Code of Conduct](#code-of-conduct)
- [License](#license)

## Features

- **Kotlin Multiplatform** — one SDK for JVM, Android, iOS, macOS, Linux, Windows, and WebAssembly.
- **Coroutines-first** — every endpoint is `suspend`; streaming endpoints return `Flow`.
- **Full API coverage** — Chat, Reasoner, Fill-In-the-Middle, Models, User Balance.
- **Type-safe DSL** — build requests and conversations with idiomatic Kotlin builders.
- **Typed errors** — HTTP status codes are mapped to dedicated `DeepSeekException` subclasses.
- **Automatic retries** — retries with exponential backoff and `Retry-After` support for transient failures.
- **Configurable** — customize JSON, logging, timeouts, base URL, or the underlying Ktor `HttpClient`.

## Supported Platforms

| Target   | Artifacts                                                        | HTTP engine |
|----------|------------------------------------------------------------------|-------------|
| JVM      | Java 11+                                                         | OkHttp      |
| Android  | `minSdk` 24, `compileSdk` 34                                     | OkHttp      |
| Apple    | `iosX64`, `iosArm64`, `iosSimulatorArm64`, `macosArm64`          | Darwin      |
| Linux    | `linuxX64`, `linuxArm64`                                         | CIO         |
| Windows  | `mingwX64`                                                       | CIO         |
| Wasm     | `wasmJs` (Node.js and browsers)                                  | JS          |

## Requirements

- Kotlin `2.0` or later (tested on `2.3.20`)
- JVM / Android: Java 11 or later
- A DeepSeek API key — create one at [platform.deepseek.com](https://platform.deepseek.com/api_keys)

## Installation

Add the dependency to your Gradle build:

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.oremif:deepseek-kotlin:0.4.0")
}
```

<details>
<summary>Gradle (Groovy DSL)</summary>

```groovy
// build.gradle
dependencies {
    implementation 'org.oremif:deepseek-kotlin:0.4.0'
}
```

</details>

<details>
<summary>Kotlin Multiplatform</summary>

```kotlin
// build.gradle.kts
kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation("org.oremif:deepseek-kotlin:0.4.0")
            }
        }
    }
}
```

</details>

> [!TIP]
> Expose your API key via the `DEEPSEEK_API_KEY` environment variable rather than hard-coding it — all snippets in this README follow that convention.

## Quick Start

```kotlin
import kotlinx.coroutines.runBlocking
import org.oremif.deepseek.api.chat
import org.oremif.deepseek.client.DeepSeekClient

fun main() = runBlocking {
    val client = DeepSeekClient(System.getenv("DEEPSEEK_API_KEY"))

    val response = client.chat("Say hello in one sentence.")
    println(response.choices.first().message.content)
}
```

## Usage

### Creating a client

There are two client types:

- `DeepSeekClient` — for unary (request/response) calls.
- `DeepSeekClientStream` — for SSE streaming calls; returns a `Flow` of chunks.

```kotlin
import org.oremif.deepseek.client.DeepSeekClient
import org.oremif.deepseek.client.DeepSeekClientStream

val client = DeepSeekClient(System.getenv("DEEPSEEK_API_KEY"))
val streamClient = DeepSeekClientStream(System.getenv("DEEPSEEK_API_KEY"))
```

> [!NOTE]
> Clients are designed to be **long-lived** — create one instance and reuse it for the lifetime of your application. Calling `close()` is usually unnecessary; prefer `closeAndJoin()` from a coroutine context if you do need to release resources eagerly.

### Chat completion

The simplest form accepts a single user message:

```kotlin
val response = client.chat("Tell me a joke about Kotlin.")
println(response.choices.first().message.content)
```

Pass a list of messages for multi-turn conversations:

```kotlin
import org.oremif.deepseek.models.SystemMessage
import org.oremif.deepseek.models.UserMessage

val response = client.chat(
    listOf(
        SystemMessage("You are a helpful assistant."),
        UserMessage("Explain Kotlin coroutines in one paragraph."),
    )
)
```

Use the message-builder DSL for a more idiomatic conversation:

```kotlin
val response = client.chat {
    system("You are a helpful assistant.")
    user("Explain Kotlin coroutines in one paragraph.")
}
```

Tune model behavior with `chatCompletionParams`:

```kotlin
import org.oremif.deepseek.models.ChatModel
import org.oremif.deepseek.models.chatCompletionParams

val params = chatCompletionParams {
    model = ChatModel.DEEPSEEK_CHAT
    temperature = 0.3
    maxTokens = 2048
}

val response = client.chat(params) {
    system("You are a helpful assistant.")
    user("Tell me a joke.")
}
```

For full control, use the `chatCompletion` builder:

```kotlin
val response = client.chatCompletion {
    params {
        model = ChatModel.DEEPSEEK_CHAT
        temperature = 0.7
        maxTokens = 2000
        frequencyPenalty = 0.5
    }
    messages {
        system("You are a Kotlin expert.")
        user("How do I use Flow in Kotlin?")
    }
}
```

### Streaming chat

Streaming endpoints are exposed on `DeepSeekClientStream` and return a `Flow` of chunks — each chunk can be processed as soon as it arrives:

```kotlin
import kotlinx.coroutines.flow.Flow
import org.oremif.deepseek.api.chat
import org.oremif.deepseek.client.DeepSeekClientStream
import org.oremif.deepseek.models.ChatCompletionChunk

val streamClient = DeepSeekClientStream(System.getenv("DEEPSEEK_API_KEY"))

val chunks: Flow<ChatCompletionChunk> = streamClient.chat {
    system("You are a helpful assistant.")
    user("Write a short poem about Kotlin.")
}

chunks.collect { chunk ->
    print(chunk.choices.firstOrNull()?.delta?.content ?: "")
}
```

Streaming follows the same three shapes as the unary API (string, `List<ChatMessage>`, DSL) and also has a full-builder form:

```kotlin
import org.oremif.deepseek.models.chatCompletionStreamParams

val params = chatCompletionStreamParams {
    model = ChatModel.DEEPSEEK_CHAT
    temperature = 0.3
}

streamClient.chat(params) {
    system("You are a helpful assistant.")
    user("Tell me a joke.")
}.collect { chunk ->
    print(chunk.choices.firstOrNull()?.delta?.content ?: "")
}
```

### Reasoning model

`ChatModel.DEEPSEEK_REASONER` returns both the final answer and the chain-of-thought in a separate `reasoningContent` field:

```kotlin
val client = DeepSeekClient(System.getenv("DEEPSEEK_API_KEY"))

val params = chatCompletionParams {
    model = ChatModel.DEEPSEEK_REASONER
    maxTokens = 2048
}

val response = client.chat(params) {
    user("How many R's are in the word 'strawberry'?")
}

val message = response.choices.first().message
println("Reasoning:\n${message.reasoningContent}")
println("Answer:\n${message.content}")
```

Streaming works the same way — each chunk may contain either `delta.reasoningContent` or `delta.content`:

```kotlin
streamClient.chat(chatCompletionStreamParams { model = ChatModel.DEEPSEEK_REASONER }) {
    user("How many R's are in the word 'strawberry'?")
}.collect { chunk ->
    val delta = chunk.choices.first().delta
    delta.reasoningContent?.let { print(it) }
    delta.content?.let { print(it) }
}
```

### Fill-In-the-Middle (FIM)

FIM is ideal for code completion — you provide a prefix (and optionally a suffix) and the model fills in the middle.

> [!IMPORTANT]
> The FIM endpoint lives under the beta path (`/beta/completions`). Behavior and availability may change; see the [DeepSeek API docs](https://api-docs.deepseek.com/) for the current status.

```kotlin
import org.oremif.deepseek.api.fim
import org.oremif.deepseek.client.DeepSeekClient

val client = DeepSeekClient(System.getenv("DEEPSEEK_API_KEY"))

val completion = client.fim("fun main() {")
println(completion.choices.first().text)
```

With parameters, including a `suffix`:

```kotlin
import org.oremif.deepseek.models.fimCompletionParams

val params = fimCompletionParams {
    temperature = 0.2
    maxTokens = 200
    suffix = "\n}"
}

val completion = client.fim(params, "fun calculateArea(radius: Double): Double {")
println(completion.choices.first().text)
```

Streaming FIM:

```kotlin
import org.oremif.deepseek.client.DeepSeekClientStream

val streamClient = DeepSeekClientStream(System.getenv("DEEPSEEK_API_KEY"))

streamClient.fim("fun main() {").collect { chunk ->
    print(chunk.choices.firstOrNull()?.text ?: "")
}
```

### JSON output mode

Force the model to return valid JSON with `ResponseFormat.jsonObject`:

```kotlin
import org.oremif.deepseek.models.ResponseFormat

val params = chatCompletionParams {
    model = ChatModel.DEEPSEEK_CHAT
    responseFormat = ResponseFormat.jsonObject
}

val response = client.chat(params) {
    system("You output strict JSON. Schema: {\"city\": string, \"country\": string}.")
    user("Paris")
}
// response.choices.first().message.content -> {"city":"Paris","country":"France"}
```

### Tool / function calling

Describe a tool with JSON Schema parameters and let the model pick when to call it:

```kotlin
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.oremif.deepseek.models.FunctionRequest
import org.oremif.deepseek.models.Tool
import org.oremif.deepseek.models.ToolCallType

val getWeather = Tool(
    type = ToolCallType.FUNCTION,
    function = FunctionRequest(
        name = "get_weather",
        description = "Get the current weather for a city.",
        parameters = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("city") {
                    put("type", "string")
                    put("description", "City name, e.g. Paris")
                }
            }
            putJsonArray("required") { add("city") }
        },
    ),
)

val response = client.chat(
    chatCompletionParams {
        model = ChatModel.DEEPSEEK_CHAT
        tools = listOf(getWeather)
    }
) {
    user("What's the weather in Paris right now?")
}

val toolCalls = response.choices.first().message.toolCalls.orEmpty()
for (call in toolCalls) {
    println("Calling ${call.function.name} with ${call.function.arguments}")
}
```

### List models

```kotlin
import org.oremif.deepseek.api.models

client.models().data.forEach { model ->
    println("${model.id} (owner: ${model.ownedBy})")
}
```

### Account balance

```kotlin
import org.oremif.deepseek.api.userBalance

val balance = client.userBalance()
balance.balanceInfos.forEach { info ->
    println("${info.currency}: ${info.totalBalance} (granted=${info.grantedBalance}, topped-up=${info.toppedUpBalance})")
}
```

## Client Configuration

The client builder exposes a handful of layered configuration hooks. They compose in any order — the underlying `HttpClient` is assembled lazily in `build()`.

```kotlin
import io.ktor.client.plugins.logging.LogLevel
import org.oremif.deepseek.client.DeepSeekClient

val client = DeepSeekClient(System.getenv("DEEPSEEK_API_KEY")) {
    // Override the API base URL (e.g. for a proxy or staging environment).
    baseUrl("https://api.deepseek.com")

    // Customize JSON (layered on top of the defaults: snake_case + ignoreUnknownKeys = true).
    jsonConfig {
        prettyPrint = false
        ignoreUnknownKeys = true
    }

    // Per-endpoint timeouts, in milliseconds.
    chatCompletionTimeout(45_000)
    fimCompletionTimeout(60_000)

    // Opt-in logging. Authorization is always redacted; add more predicates as needed.
    logging {
        level = LogLevel.HEADERS
        sanitizeHeader { header -> header == "Cookie" }
    }

    // Layer additional Ktor plugins on top of the defaults.
    httpClient {
        // install(...) { ... }
    }
}
```

> [!NOTE]
> The default HTTP client installs `Auth` (bearer), `ContentNegotiation` (JSON), `HttpRequestRetry` (3 retries, honors `Retry-After`), and `HttpTimeout` (request 60s, connect 10s, socket 300s). Logging is **opt-in**; nothing is logged unless `logging { }` is called.

> [!WARNING]
> `httpClient(client: HttpClient)` **replaces** the underlying Ktor client entirely — you become responsible for installing `Auth`, `ContentNegotiation`, retries, timeouts, logging, and (for streaming) `SSE`. Use the `httpClient { ... }` block form to layer on top of the defaults instead.

## Error Handling

Non-2xx responses are thrown as typed subclasses of `DeepSeekException`:

| Status | Exception                          |
|--------|------------------------------------|
| 400    | `BadRequestException`              |
| 401    | `UnauthorizedException`            |
| 402    | `InsufficientBalanceException`     |
| 403    | `PermissionDeniedException`        |
| 404    | `NotFoundException`                |
| 422    | `UnprocessableEntityException`     |
| 429    | `RateLimitException`               |
| 500    | `InternalServerException`          |
| 503    | `OverloadServerException`          |
| other  | `UnexpectedStatusCodeException`    |

Each exception carries the HTTP `statusCode`, response `headers`, and the parsed `DeepSeekError` (when available):

```kotlin
import org.oremif.deepseek.errors.DeepSeekException

try {
    client.chat("Hello")
} catch (e: DeepSeekException.RateLimitException) {
    println("Rate limited. Retry-After: ${e.headers["Retry-After"]}")
} catch (e: DeepSeekException.UnauthorizedException) {
    println("Invalid API key: ${e.error?.error?.message}")
} catch (e: DeepSeekException) {
    println("DeepSeek error ${e.statusCode}: ${e.message}")
}
```

> [!TIP]
> The client already retries transient failures (timeouts, 429, 5xx) up to 3 times with exponential backoff and honors the `Retry-After` header. You typically only need to catch `DeepSeekException` around permanent failures.

## Documentation

- **API reference (Dokka):** [oremif.github.io/deepseek-kotlin](https://oremif.github.io/deepseek-kotlin/)
- **DeepSeek REST API:** [api-docs.deepseek.com](https://api-docs.deepseek.com/)

## Examples

Full, runnable samples live in the [`example/`](example) directory — it is a standalone Gradle project consuming the published artifact. Each file has its own `main()` function; run them from your IDE, or compile the project with:

```bash
export DEEPSEEK_API_KEY=sk-...
./gradlew -p example build
```

Highlights:

- [`chat.kt`](example/src/main/kotlin/chat.kt) — basic chat completion
- [`chatStreaming.kt`](example/src/main/kotlin/chatStreaming.kt) — streaming chat
- [`reasoning.kt`](example/src/main/kotlin/reasoning.kt) — reasoning model
- [`reasoningStreaming.kt`](example/src/main/kotlin/reasoningStreaming.kt) — streaming reasoning
- [`fim.kt`](example/src/main/kotlin/fim.kt) — Fill-In-the-Middle
- [`fimStreaming.kt`](example/src/main/kotlin/fimStreaming.kt) — streaming FIM
- [`models.kt`](example/src/main/kotlin/models.kt) — list available models
- [`userBalance.kt`](example/src/main/kotlin/userBalance.kt) — account balance

## Contributing

Contributions are welcome! Please read the [Oremif contribution guide](https://github.com/Oremif/.github/blob/master/CONTRIBUTING.md) before opening an issue or a pull request.

A few project-specific notes:

- Run the JVM test suite with `./gradlew :deepseek-kotlin:jvmTest`.
- Any change to the public API must be accompanied by a regenerated ABI dump: `./gradlew :deepseek-kotlin:apiDump`.
- See [`AGENTS.md`](AGENTS.md) for an overview of the project structure, build commands, and conventions.

## Code of Conduct

This project follows the [Oremif Code of Conduct](https://github.com/Oremif/.github/blob/master/CODE_OF_CONDUCT.md). By participating, you are expected to uphold it.

## License

Licensed under the [Apache License, Version 2.0](LICENSE).

```
Copyright 2025 Oremif

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0
```
