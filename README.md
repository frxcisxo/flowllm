# FlowLLM 🤖

> Kotlin + Compose extensions for real-time LLM streaming on Android

[![](https://jitpack.io/v/frandev/flowllm.svg)](https://jitpack.io/#frandev/flowllm)
![API](https://img.shields.io/badge/API-24%2B-brightgreen)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2-purple)
![License](https://img.shields.io/badge/license-MIT-blue)

---

## The problem

Integrating LLM streaming in Android is verbose, repetitive, and inconsistent across providers.
Every project ends up with the same boilerplate: managing coroutines, accumulating chunks, handling errors, retrying on failure.

**FlowLLM solves this with a single composable.**

---

## Usage

```kotlin
val provider = remember { GeminiProvider(apiKey = "YOUR_API_KEY") }
val state = rememberLLMStream(provider, prompt)

when (state) {
    is LLMState.Idle      -> Text("Write a prompt to start")
    is LLMState.Loading   -> CircularProgressIndicator()
    is LLMState.Streaming -> Text(state.text) // updates in real-time
    is LLMState.Done      -> Text("✅ ${state.text}")
    is LLMState.Error     -> Text("❌ ${state.exception.message}")
}
```

---

## Supported Providers

| Provider | Class | Notes |
|---|---|---|
| **Gemini** | `GeminiProvider` | Uses official Google SDK |
| **OpenAI** | `OpenAIProvider` | GPT-4o, GPT-4o-mini, etc. |
| **Anthropic** | `AnthropicProvider` | Claude Sonnet, Haiku, etc. |
| **Ollama** | `OllamaProvider` | Local models, no API key needed |

---

## Installation

Add JitPack to your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
}
```

Add the dependency:

```kotlin
dependencies {
    implementation("com.github.frandev:flowllm:1.0.0")
}
```

---

## Providers

### Gemini

```kotlin
// Default model: gemini-2.0-flash
val provider = GeminiProvider(apiKey = "YOUR_API_KEY")

// Choose a specific model
val provider = GeminiProvider(
    apiKey = "YOUR_API_KEY",
    model = GeminiModel.PRO_2_5
)
```

Available models:
- `GeminiModel.FLASH` — gemini-2.0-flash (default)
- `GeminiModel.FLASH_LITE` — gemini-2.0-flash-lite
- `GeminiModel.PRO_1_5` — gemini-1.5-pro
- `GeminiModel.PRO_2_5` — gemini-2.5-pro-exp-03-25

### OpenAI

```kotlin
val provider = OpenAIProvider(
    apiKey = "YOUR_API_KEY",
    model = "gpt-4o-mini" // default
)
```

### Anthropic

```kotlin
val provider = AnthropicProvider(
    apiKey = "YOUR_API_KEY",
    model = "claude-sonnet-4-20250514" // default
)
```

### Ollama (local)

```kotlin
// Runs on your local machine, no API key needed
val provider = OllamaProvider(
    model = "deepseek-coder", // any model you have pulled
    baseUrl = "http://10.0.2.2:11434" // default (emulator → localhost)
)
```

> For Ollama, add `android:usesCleartextTraffic="true"` to your `AndroidManifest.xml`

---

## Retry with exponential backoff

```kotlin
val state = rememberLLMStream(
    provider = provider,
    prompt = submitted,
    retryPolicy = RetryPolicy(
        maxRetries = 3,
        initialDelayMs = 1000,
        multiplier = 2.0,
        maxDelayMs = 10000
    )
)
```

---

## Bring your own provider

Implement the `LLMProvider` interface to support any LLM:

```kotlin
class MyCustomProvider : LLMProvider {
    override fun stream(prompt: String): Flow<String> = flow {
        // emit chunks here
    }
}
```

---

## LLMState

```kotlin
sealed class LLMState {
    data object Idle : LLMState()
    data object Loading : LLMState()
    data class Streaming(val text: String) : LLMState()
    data class Done(val text: String) : LLMState()
    data class Error(val exception: Throwable) : LLMState()
}
```

---

## Requirements

- Android API 24+
- Kotlin 2.2+
- Jetpack Compose

---

## License

```
MIT License — feel free to use, modify and distribute.
```

---

Made with ❤️ by [frandev](https://github.com/frandev)
