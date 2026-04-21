# AGENTS.md

This file provides guidance to coding agents (Claude Code, Codex, and other AI assistants) working in this repository.

## Project Overview

Kotlin Multiplatform SDK for the DeepSeek REST API.

- Published coordinates: `org.oremif:deepseek-kotlin`
- Module version in source: `0.4.0`
- Root Gradle build includes only `:deepseek-kotlin`
- `example/` is a separate standalone Gradle project with its own wrapper and `settings.gradle.kts`

Supported targets: JVM, Android, Apple (`iosX64`, `iosArm64`, `iosSimulatorArm64`, `macosArm64`), Linux (`linuxX64`, `linuxArm64`), MinGW (`mingwX64`), WebAssembly (`wasmJs`).

## Project Structure

- `deepseek-kotlin/` — main published library module (Kotlin sources under `src/commonMain/kotlin/org/oremif/deepseek/`)
  - `api/` — endpoint free functions: `chat.kt`, `chatStream.kt`, `fimCompletion.kt`, `fimCompletionStream.kt`, `userBalance.kt`, `models.kt`
  - `client/` — `DeepSeekClient`, `DeepSeekClientStream`, `DeepSeekClientConfig`, `LoggingConfig`
  - `models/` — request/response data classes, enums, message types
  - `errors/` — `DeepSeekError`, `DeepSeekHeaders`
  - `utils/` — `retry.kt` (retry policy), `validate.kt`
  - `src/commonTest/kotlin/` — shared tests; mock infrastructure in `org/oremif/deepseek/testing/MockClients.kt`
- `deepseek-kotlin/api/` — locked ABI snapshots for `apiCheck` (JVM + klib)
- `example/` — standalone JVM sample consuming the published artifact
- `gradle/libs.versions.toml` — centralized version catalog
- `.github/workflows/` — CI (`ci.yml`) and Dokka-to-GitHub-Pages (`docs.yml`)

## Core Architecture

1. **Client layer** — `DeepSeekClientBase` holds a shared `HttpClient` + `DeepSeekClientConfig`. `DeepSeekClient` handles unary calls; `DeepSeekClientStream` layers SSE on top for streaming (returns `Flow`). Both use a builder that assembles the Ktor client lazily in `build()`, so `jsonConfig` / `httpClient { }` blocks compose regardless of ordering. Clients are designed to be long-lived — `close()` is rarely needed; use `closeAndJoin()` from a coroutine for graceful shutdown.
2. **Default Ktor plugins** — `Auth` (bearer), `ContentNegotiation` (JSON, `SnakeCase` naming, `ignoreUnknownKeys=true`), `defaultRequest` pinning base URL `https://api.deepseek.com`, `HttpRequestRetry` (3 retries, honors `Retry-After`, retries on timeouts and statuses classified by `utils/retry.kt`), `HttpTimeout` (request 60s, connect 10s, socket 300s), optional `Logging`.
3. **Logging is opt-in.** Nothing is logged unless `logging { }` is called. `Authorization` is always redacted; callers add further `sanitizeHeader { }` predicates as needed.
4. **Per-platform HTTP engines** — JVM/Android: OkHttp; Apple (iOS + macOS): Darwin; Linux/MinGW: CIO; wasmJs: JS. Engine deps are declared in `deepseek-kotlin/build.gradle.kts` source sets.
5. **Endpoint files are free functions** on `DeepSeekClient` / `DeepSeekClientStream` (not methods) — look them up in `api/*.kt` rather than on the client class.
6. **`explicitApi()` is enabled** — every public declaration must have an explicit visibility and return type.

## Common Commands

### Build

```bash
./gradlew build                      # all targets
./gradlew :deepseek-kotlin:build     # library only
./gradlew -p example build           # standalone example project
```

### Test

```bash
./gradlew :deepseek-kotlin:jvmTest              # JVM (fastest; what CI runs)
./gradlew :deepseek-kotlin:test                 # all configured targets
./gradlew :deepseek-kotlin:wasmJsNodeTest       # WasmJS via Node
./gradlew :deepseek-kotlin:wasmJsBrowserTest    # WasmJS in headless browser

# Run a single test class / method (Gradle test filter):
./gradlew :deepseek-kotlin:jvmTest --tests "org.oremif.deepseek.api.ChatCompletionApiTests"
./gradlew :deepseek-kotlin:jvmTest --tests "*ChatCompletionApiTests.someMethodName"
```

Tests use `kotlin.test` + Kotest assertions (`io.kotest.matchers.*`).

### Binary compatibility (ABI) validation

The `binary-compatibility-validator` plugin locks the public API for JVM and klib targets. Intentional API changes must be accompanied by regenerated dumps under `deepseek-kotlin/api/`.

```bash
./gradlew :deepseek-kotlin:apiCheck    # fails if public API drifts from the locked dumps
./gradlew :deepseek-kotlin:apiDump     # regenerate dumps after an intentional API change
```

### Documentation

```bash
./gradlew :deepseek-kotlin:dokkaGenerate   # HTML into deepseek-kotlin/build/dokka/html
```

Published to GitHub Pages by `.github/workflows/docs.yml` on each release (and on manual dispatch).

### Publishing

```bash
./gradlew :deepseek-kotlin:publishToMavenLocal
./gradlew :deepseek-kotlin:publishToMavenCentral    # requires credentials; CI uses --no-configuration-cache
```

## Key Versions (see `gradle/libs.versions.toml`)

- Kotlin **2.3.20**, AGP **9.1.0**, Dokka **2.2.0**
- Ktor **3.4.2**, Kotlinx Serialization **1.11.0**, Coroutines **1.10.2**
- Kotest **6.1.11**, binary-compatibility-validator **0.18.1**, vanniktech maven-publish **0.36.0**

## Configuration Notes

- Minimum JVM target: Java 11 (CI builds with JDK 21)
- Android: `minSdk 24`, `compileSdk 34`, namespace `org.oremif.deepseek`
- `kotlin.mpp.enableCInteropCommonization=true`
- Dokka Gradle plugin runs in V2 mode (`org.jetbrains.dokka.experimental.gradle.pluginMode=V2Enabled`)
- `apiValidation { klib { enabled = true } }` — klib ABI is validated alongside JVM

## Development Notes

- **Do not** use `:example:*` task paths from the repository root; `example/` is not in the root build. Invoke its tasks with `-p example` (e.g. `./gradlew -p example test`).
- The example project depends on the **published** artifact `org.oremif:deepseek-kotlin:0.4.0`, not the local `:deepseek-kotlin` module. To exercise local SDK changes from the example, first run `publishToMavenLocal` or update the example's dependency.
- The example project uses Kotlin `2.3.20` and `jvmToolchain(17)`.
- Examples and sample code expect `DEEPSEEK_API_KEY` in the environment.

## CI (`.github/workflows/ci.yml`)

- `build` job: Ubuntu, JDK 21, runs `./gradlew jvmTest --continue --parallel`, uploads JUnit XML, renders a test report.
- `api-check` job: macOS, JDK 21, runs `./gradlew :deepseek-kotlin:apiCheck --parallel` (macOS needed so klib targets resolve).
- Both jobs trigger on PRs to `master` and pushes to `master`. Gradle cache is read-only on non-master refs.

## Maven Publishing

Uses `com.vanniktech.maven.publish` with automatic Maven Central Portal release and signing. The KMP plugin emits platform-specific artifacts automatically.

## Usage

See `README.md` and the `example/` module for end-to-end snippets (simple chat, params builders, streaming via `Flow`, FIM completion, custom client config).
