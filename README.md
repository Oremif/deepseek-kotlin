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
  - [Models](#models)
  - [Chat completion](#chat-completion)
  - [Streaming chat](#streaming-chat)
  - [Thinking mode](#thinking-mode)
  - [Image input](#image-input)
  - [Tool / function calling](#tool--function-calling)
  - [JSON output mode](#json-output-mode)
  - [Responses API](#responses-api)
  - [Files API](#files-api)
  - [Fill-In-the-Middle (FIM)](#fill-in-the-middle-fim)
  - [Prefix completion](#prefix-completion)
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
- **Full API coverage** — Chat (thinking and non-thinking), Responses, Files, Fill-In-the-Middle, Models, User Balance.
- **Multimodal** — send images by URL, inline base64, or the id of a file you uploaded.
- **Type-safe DSL** — build requests, conversations, and multimodal content with idiomatic Kotlin builders.
- **Typed errors** — HTTP status codes are mapped to dedicated `DeepSeekException` subclasses.
- **Automatic retries** — retries with exponential backoff and `Retry-After` support for transient failures.
- **Configurable** — customize JSON, logging, per-endpoint timeouts, base URL, or the underlying Ktor `HttpClient`.

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

- Kotlin `2.0` or later
- JVM / Android: Java 11 or later
- A DeepSeek API key — create one at [platform.deepseek.com](https://platform.deepseek.com/api_keys)

## Installation

Add the dependency to your Gradle build:

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.oremif:deepseek-kotlin:0.5.0")
}
```

<details>
<summary>Gradle (Groovy DSL)</summary>

```groovy
// build.gradle
dependencies {
    implementation 'org.oremif:deepseek-kotlin:0.5.0'
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
                implementation("org.oremif:deepseek-kotlin:0.5.0")
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
- `DeepSeekClientStream` — for SSE streaming calls; returns a `Flow` of chunks or events.

```kotlin
import org.oremif.deepseek.client.DeepSeekClient
import org.oremif.deepseek.client.DeepSeekClientStream

val client = DeepSeekClient(System.getenv("DEEPSEEK_API_KEY"))
val streamClient = DeepSeekClientStream(System.getenv("DEEPSEEK_API_KEY"))
```

> [!NOTE]
> Clients are designed to be **long-lived** — create one instance and reuse it for the lifetime of your application. Calling `close()` is usually unnecessary; prefer `closeAndJoin()` from a coroutine context if you do need to release resources eagerly.

### Models

`ChatModel` is a `value class` wrapping the raw slug, with constants for the two models the API serves:

| Constant | Slug | Notes |
|---|---|---|
| `ChatModel.DEEPSEEK_FLASH` | `deepseek-flash` | The default of every builder and every `chat` / `createResponse` overload |
| `ChatModel.DEEPSEEK_V4_PRO` | `deepseek-v4-pro` | Strongest capability, lower concurrency limit |

Both models share the same feature set: a 1M token context, up to 384K output tokens, tool calling, JSON output, image input, and thinking enabled by default.

Any other slug — an alias, or a model released after this version — can be wrapped directly:

```kotlin
import org.oremif.deepseek.models.ChatModel

val alias = ChatModel("deepseek-v4-flash")
```

> [!NOTE]
> `deepseek-chat` and `deepseek-reasoner` are retired names that the API still resolves to `deepseek-flash`. Their constants are deprecated and will be removed in 0.6.0. A response always echoes the model that actually ran, so read `ChatCompletion.model` instead of assuming the slug you sent.

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
    model = ChatModel.DEEPSEEK_FLASH
    temperature = 0.3
    maxTokens = 2048
    userId = "tenant-42"     // Optional: content-safety review, cache and scheduling isolation
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
        model = ChatModel.DEEPSEEK_V4_PRO
        temperature = 0.7
        maxTokens = 2000
    }
    messages {
        system("You are a Kotlin expert.")
        user("How do I use Flow in Kotlin?")
    }
}
```

> [!NOTE]
> `frequencyPenalty` and `presencePenalty` are deprecated: the API accepts them but ignores them. They will be removed in 0.6.0.

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
    model = ChatModel.DEEPSEEK_FLASH
    temperature = 0.3
}

streamClient.chat(params) {
    system("You are a helpful assistant.")
    user("Tell me a joke.")
}.collect { chunk ->
    print(chunk.choices.firstOrNull()?.delta?.content ?: "")
}
```

> [!NOTE]
> Token statistics ride along on the **last content chunk** — the one whose single choice carries a non-null `finishReason` — rather than in a separate trailing chunk. `StreamOptions(includeUsage = true)` is not required for that.

### Thinking mode

Every model thinks by default and returns the chain-of-thought in a separate `reasoningContent` field next to the answer. `reasoningEffort` controls how long it thinks:

```kotlin
val params = chatCompletionParams {
    model = ChatModel.DEEPSEEK_V4_PRO
    reasoningEffort = ReasoningEffort.MAX   // LOW, HIGH (the API default) or MAX
    maxTokens = 2048
}

val response = client.chat(params) {
    user("How many R's are in the word 'strawberry'?")
}

val message = response.choices.first().message
println("Reasoning:\n${message.reasoningContent}")
println("Answer:\n${message.content}")
println("Reasoning tokens: ${response.usage.completionTokensDetails?.reasoningTokens}")
```

Turn thinking off for a faster, cheaper answer from the same model:

```kotlin
val fast = chatCompletionParams {
    model = ChatModel.DEEPSEEK_FLASH
    thinking = Thinking(ThinkingType.DISABLED)
}
```

Streaming works the same way — each chunk may contain either `delta.reasoningContent` or `delta.content`:

```kotlin
streamClient.chat(chatCompletionStreamParams { model = ChatModel.DEEPSEEK_V4_PRO }) {
    user("How many R's are in the word 'strawberry'?")
}.collect { chunk ->
    val delta = chunk.choices.first().delta
    delta.reasoningContent?.let { print(it) }
    delta.content?.let { print(it) }
}
```

### Image input

A user message carries either plain text or a list of content parts. Both models read images, so no separate vision model is needed:

```kotlin
val response = client.chat(chatCompletionParams { model = ChatModel.DEEPSEEK_FLASH }) {
    user {
        text("What is in this image?")
        image("https://example.com/cat.jpg", ImageDetail.LOW)   // LOW downscales to 512x512
    }
}
```

The DSL offers three ways to attach an image:

| Builder call | Wire form | Use for |
|---|---|---|
| `image(url, detail)` | `image_url` part | An `http(s)` link, or a `data:image/jpeg;base64,...` URL |
| `imageFile(fileId)` | `file` part | A file uploaded through the [Files API](#files-api) |
| `imageData(base64, filename)` | `file` part | An image inlined as a base64 data URL |

Content parts can also be built explicitly with `TextPart`, `ImageUrlPart` and `FilePart`, and `UserMessage(parts)` takes the resulting list. The API rejects images in system and assistant messages, which is why parts exist only on user messages.

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
        strict = true,   // Beta: forces the arguments to match the schema exactly
    ),
)
```

The model's reply goes straight back into the conversation, followed by one `ToolMessage` per call:

```kotlin
val params = chatCompletionParams {
    model = ChatModel.DEEPSEEK_FLASH
    tools = listOf(getWeather)
}

val messages = mutableListOf<ChatMessage>(UserMessage("What's the weather in Paris right now?"))

val answer = client.chat(params, messages).choices.first().message
messages.add(answer)

for (call in answer.toolCalls.orEmpty()) {
    val city = call.function.argumentsAsJsonOrNull()?.get("city")?.jsonPrimitive?.content
    println("Calling ${call.function.name}(city=$city)")
    messages.add(ToolMessage("""{"temperature_c": 21, "condition": "sunny"}""", call.id))
}

println(client.chat(params, messages).choices.first().message.content)
```

> [!NOTE]
> `FunctionResponse.arguments` is a **`String`**, because the model generates JSON that is not guaranteed to parse. Use `argumentsAsJsonOrNull()` to decode it defensively, or parse it yourself.

### JSON output mode

Force the model to return valid JSON with `ResponseFormat.jsonObject`:

```kotlin
val params = chatCompletionParams {
    model = ChatModel.DEEPSEEK_FLASH
    responseFormat = ResponseFormat.jsonObject
}

val response = client.chat(params) {
    system("You output strict JSON. Schema: {\"city\": string, \"country\": string}.")
    user("Paris")
}
// response.choices.first().message.content -> {"city":"Paris","country":"France"}
```

> [!TIP]
> Schema-constrained output (`json_schema`) is only available on the [Responses API](#responses-api), through `TextConfig.jsonSchema(name, schema)`.

### Responses API

`POST /responses` is the OpenAI-compatible endpoint DeepSeek also serves. It is **stateless**: the server keeps nothing between calls, so each turn sends the whole history in `input`, and continuing a conversation is appending to a list you own.

```kotlin
val params = responseParams {
    model = ChatModel.DEEPSEEK_V4_PRO
    instructions = "You are a concise assistant."   // Becomes the leading system message
    reasoning = ReasoningConfig(ReasoningEffort.LOW)
    maxOutputTokens = 1024                          // Counts reasoning tokens too
}

val history = mutableListOf<ResponseItem>(MessageItem(ResponseRole.USER, "What is 6 * 7?"))

val first = client.createResponse(params, history)
println(first.outputText)                           // Text of every message item, concatenated
println("${first.status}: ${first.usage?.totalTokens} tokens")

history += first.output                             // The model's own items go back in unchanged
history += MessageItem(ResponseRole.USER, "And what is that times two?")
println(client.createResponse(params, history).outputText)
```

`createResponse` mirrors `chat`: it takes a string, a `List<ResponseItem>`, a DSL block, or a full request builder.

```kotlin
val response = client.createResponse {
    params {
        model = ChatModel.DEEPSEEK_FLASH
        maxOutputTokens = 2000
    }
    input {
        user {
            text("What is in this image?")
            image("https://example.com/cat.jpg")
        }
    }
}
```

`ModelResponse.output` is a list of items — `ReasoningItem`, `MessageItem`, `FunctionCallItem`, `WebSearchCallItem` — in the order the model produced them. Items the SDK does not recognize arrive as `UnknownItem` carrying the raw JSON instead of failing the call.

**Server-side web search** is available here and nowhere else:

```kotlin
import org.oremif.deepseek.models.responses.WebSearchTool

val params = responseParams {
    model = ChatModel.DEEPSEEK_V4_PRO
    tools = listOf(WebSearchTool.webSearch)
}
println(client.createResponse(params, "What is the latest Kotlin release?").outputText)
```

**Schema-constrained output**:

```kotlin
import org.oremif.deepseek.models.responses.TextConfig

val params = responseParams {
    text = TextConfig.jsonSchema(
        name = "city",
        schema = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") { putJsonObject("country") { put("type", "string") } }
        },
    )
}
```

**Streaming** the Responses API emits semantic events rather than chunks; the terminal event carries the finished response, usage included:

```kotlin
import org.oremif.deepseek.models.responses.OutputTextDeltaEvent
import org.oremif.deepseek.models.responses.ReasoningTextDeltaEvent
import org.oremif.deepseek.models.responses.ResponseSnapshotEvent
import org.oremif.deepseek.models.responses.responseStreamParams

streamClient.createResponse(
    responseStreamParams { model = ChatModel.DEEPSEEK_FLASH },
    "Write a haiku about Kotlin coroutines",
).collect { event ->
    when (event) {
        is ReasoningTextDeltaEvent -> print(event.delta)
        is OutputTextDeltaEvent -> print(event.delta)
        is ResponseSnapshotEvent ->
            if (event.isTerminal) println("\n${event.response.status}")
        else -> Unit
    }
}
```

There are twenty event types in total — lifecycle, output items, content parts, text and reasoning deltas, tool-call input deltas, and web-search progress. An event the SDK cannot map (an unknown type, or a known one whose payload does not fit) arrives as `UnknownResponseStreamEvent`, so one unexpected event never breaks the flow.

### Files API

Upload an image once and reference it by id from as many messages as you like:

```kotlin
import org.oremif.deepseek.api.deleteFile
import org.oremif.deepseek.api.listFiles
import org.oremif.deepseek.api.retrieveFile
import org.oremif.deepseek.api.uploadFile

// The format is detected from the bytes; `filename` is only a label. Max 64 MiB.
// `expiresAfterSeconds` (3600..2592000) makes the file temporary; without it, it is kept.
val uploaded = client.uploadFile(imageBytes, "cat.jpg", expiresAfterSeconds = 3600)

val response = client.chat(chatCompletionParams { model = ChatModel.DEEPSEEK_FLASH }) {
    user {
        text("What is in this image?")
        imageFile(uploaded.id)
    }
}

var page = client.listFiles(limit = 100)
while (true) {
    page.data.forEach { println("${it.id} ${it.filename}") }
    if (!page.hasMore) break
    page = client.listFiles(after = page.lastId, limit = 100)
}

client.retrieveFile(uploaded.id)
client.deleteFile(uploaded.id)
```

Uploads use their own `uploadTimeout` (5 minutes by default) rather than the standard request timeout.

### Fill-In-the-Middle (FIM)

FIM is ideal for code completion — you provide a prefix (and optionally a suffix) and the model fills in the middle.

> [!IMPORTANT]
> The FIM endpoint lives under the beta path (`/beta/completions`). Behavior and availability may change; see the [DeepSeek API docs](https://api-docs.deepseek.com/) for the current status.

```kotlin
import org.oremif.deepseek.api.fim

val completion = client.fim("fun main() {")
println(completion.choices.first().text)
```

With parameters, including a `suffix`:

```kotlin
import org.oremif.deepseek.models.fimCompletionParams

val params = fimCompletionParams {
    model = ChatModel.DEEPSEEK_V4_PRO   // The default; deepseek-flash is served too
    temperature = 0.2
    maxTokens = 200
    suffix = "\n}"
}

val completion = client.fim(params, "fun calculateArea(radius: Double): Double {")
println(completion.choices.first().text)
```

Streaming FIM:

```kotlin
streamClient.fim("fun main() {").collect { chunk ->
    print(chunk.choices.firstOrNull()?.text ?: "")
}
```

> [!NOTE]
> `echo` cannot be combined with `suffix` or `logprobs` — the API answers 400.

### Prefix completion

Make the model continue a partial assistant message by ending the conversation with an `AssistantMessage` whose `prefix` is `true`. The SDK routes such a request to the beta path automatically:

```kotlin
import org.oremif.deepseek.models.AssistantMessage

val response = client.chat(
    listOf(
        UserMessage("Write a Kotlin function that sums a list of ints."),
        AssistantMessage("fun sum(numbers: List<Int>): Int {", prefix = true),
    )
)
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
val client = DeepSeekClient(System.getenv("DEEPSEEK_API_KEY")) {
    // Override the API base URL (e.g. for a proxy or staging environment).
    baseUrl("https://api.deepseek.com")

    // Customize JSON (layered on top of the defaults: snake_case + ignoreUnknownKeys = true).
    jsonConfig {
        prettyPrint = false
        ignoreUnknownKeys = true
    }

    // Per-endpoint timeouts, as milliseconds or Duration.
    chatCompletionTimeout(2.minutes)   // default 300_000
    fimCompletionTimeout(60_000)       // default 60_000
    uploadTimeout(5.minutes)           // default 300_000
    responseTimeout(5.minutes)         // default 300_000

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
> The default HTTP client installs `Auth` (bearer), `ContentNegotiation` (JSON), `HttpRequestRetry` (3 retries, honors `Retry-After`), and `HttpTimeout` (request 60s, connect 10s, socket 300s). The per-endpoint timeouts above override the request timeout for their own calls. Logging is **opt-in**; nothing is logged unless `logging { }` is called.

> [!NOTE]
> `level` and `logger` are Ktor's own types. The logging plugin comes in as a transitive runtime
> dependency, but naming those types in your code needs it at compile time too — add
> `implementation("io.ktor:ktor-client-logging:<ktor-version>")`. A bare `logging { }`, with or
> without `sanitizeHeader { }`, needs nothing extra.

> [!WARNING]
> `httpClient(client: HttpClient)` **replaces** the underlying Ktor client entirely — you become responsible for installing `Auth`, `ContentNegotiation`, retries, timeouts, logging, and (for streaming) `SSE`. Use the `httpClient { ... }` block form to layer on top of the defaults instead.

> [!TIP]
> DeepSeek also serves an Anthropic-compatible endpoint at `https://api.deepseek.com/anthropic`. It is out of this SDK's scope — use an Anthropic client for it.

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

An error body that is not JSON — an HTML page from a proxy, for instance — still produces a `DeepSeekException`, just with `error == null`.

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
- [`thinking.kt`](example/src/main/kotlin/thinking.kt) — thinking mode and reasoning effort
- [`thinkingStreaming.kt`](example/src/main/kotlin/thinkingStreaming.kt) — streaming chain-of-thought
- [`toolCalling.kt`](example/src/main/kotlin/toolCalling.kt) — a full tool-calling round trip
- [`vision.kt`](example/src/main/kotlin/vision.kt) — image input
- [`files.kt`](example/src/main/kotlin/files.kt) — Files API, from upload to deletion
- [`responses.kt`](example/src/main/kotlin/responses.kt) — Responses API, multi-turn
- [`responsesStreaming.kt`](example/src/main/kotlin/responsesStreaming.kt) — streaming Responses events
- [`webSearch.kt`](example/src/main/kotlin/webSearch.kt) — server-side web search
- [`fim.kt`](example/src/main/kotlin/fim.kt) — Fill-In-the-Middle
- [`fimStreaming.kt`](example/src/main/kotlin/fimStreaming.kt) — streaming FIM
- [`models.kt`](example/src/main/kotlin/models.kt) — list available models
- [`userBalance.kt`](example/src/main/kotlin/userBalance.kt) — account balance

## Contributing

Contributions are welcome! Please read the [Oremif contribution guide](https://github.com/Oremif/.github/blob/master/CONTRIBUTING.md) before opening an issue or a pull request.

A few project-specific notes:

- Run the JVM test suite with `./gradlew :deepseek-kotlin:jvmTest`.
- Code is formatted with [ktfmt](https://github.com/Kotlin/ktfmt) in `kotlinlang` style. Run `./gradlew ktfmtFormat ktfmtFormatScripts` before pushing; CI enforces it with `ktfmtCheck`.
- Any change to the public API must be accompanied by a regenerated ABI dump: `./gradlew :deepseek-kotlin:updateKotlinAbi`.
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
