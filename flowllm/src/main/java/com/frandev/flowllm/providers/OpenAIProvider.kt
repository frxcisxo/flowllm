package com.frandev.flowllm.providers

import android.util.Log
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

class OpenAIProvider(
    private val apiKey: String,
    private val model: String = "gpt-4o-mini"
) : LLMProvider {

    private val json = Json { ignoreUnknownKeys = true }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    override fun stream(prompt: String): Flow<String> = flow {
        client.preparePost("https://api.openai.com/v1/chat/completions") {
            header("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("model", model)
                put("stream", true)
                putJsonArray("messages") {
                    addJsonObject {
                        put("role", "user")
                        put("content", prompt)
                    }
                }
            }.toString())
        }.execute { httpResponse ->
           Log.d("FlowLLM", "Status: ${httpResponse.status}")

            // Si el status no es 200, lanzamos un error descriptivo
            if (httpResponse.status.value != 200) {
                val errorBody = httpResponse.bodyAsText()
                throw Exception("Error ${httpResponse.status.value}: $errorBody")
            }

            val channel = httpResponse.bodyAsChannel()
            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: break

                // Log de cada línea que llega
                Log.d("FlowLLM", "Line: $line")

                if (line.isBlank()) continue
                if (!line.startsWith("data:")) continue
                val data = line.removePrefix("data:").trim()
                if (data == "[DONE]") break

                val chunk = json.parseToJsonElement(data)
                    .jsonObject["choices"]
                    ?.jsonArray?.getOrNull(0)
                    ?.jsonObject?.get("delta")
                    ?.jsonObject?.get("content")
                    ?.jsonPrimitive?.contentOrNull ?: continue

                Log.d("FlowLLM", "Chunk: $chunk")
                emit(chunk)
            }
        }
    }
}