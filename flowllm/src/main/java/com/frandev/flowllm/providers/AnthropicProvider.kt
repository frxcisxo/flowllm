package com.frandev.flowllm.providers

import com.frandev.flowllm.LLMProvider
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.*

class AnthropicProvider(
    private val apiKey: String,
    private val model: String = "claude-sonnet-4-20250514"
) : LLMProvider {

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) { json() }
    }

    override fun stream(prompt: String): Flow<String> = flow {
        client.preparePost("https://api.anthropic.com/v1/messages") {
            header("x-api-key", apiKey)
            header("anthropic-version", "2023-06-01")
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("model", model)
                put("max_tokens", 1024)
                put("stream", true)
                putJsonArray("messages") {
                    addJsonObject {
                        put("role", "user")
                        put("content", prompt)
                    }
                }
            }.toString())
        }.execute { httpResponse ->
            val channel = httpResponse.bodyAsChannel()
            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: break
                if (!line.startsWith("data:")) continue
                val data = line.removePrefix("data:").trim()
                val json = Json.parseToJsonElement(data).jsonObject
                if (json["type"]?.jsonPrimitive?.content != "content_block_delta") continue
                val chunk = json["delta"]
                    ?.jsonObject?.get("text")
                    ?.jsonPrimitive?.contentOrNull ?: continue
                emit(chunk)
            }
        }
    }
}