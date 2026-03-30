package com.frandev.flowllm.providers

import android.util.Log
import com.frandev.flowllm.LLMProvider
import io.ktor.client.*
import io.ktor.client.call.body
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

class OllamaProvider(
    private val model: String = "deepseek-coder",
    private val baseUrl: String = "http://10.0.2.2:11434" // localhost desde emulador
) : LLMProvider {

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) { json() }
    }

    override fun stream(prompt: String): Flow<String> = flow {
        client.preparePost("$baseUrl/api/generate") {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("model", model)
                put("prompt", prompt)
                put("stream", true)
            }.toString())
        }.execute { httpResponse ->
            val channel = httpResponse.bodyAsChannel()
            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: break
                if (line.isBlank()) continue

                // Separamos por salto de línea por si vienen juntos
                line.split("\n").forEach { rawJson ->
                    if (rawJson.isBlank()) return@forEach
                    try {
                        val json = Json.parseToJsonElement(rawJson).jsonObject
                        val chunk = json["response"]
                            ?.jsonPrimitive?.contentOrNull ?: return@forEach
                        if (chunk.isNotEmpty()) emit(chunk)
                        if (json["done"]?.jsonPrimitive?.boolean == true) return@forEach
                    } catch (e: Exception) {
                        android.util.Log.e("OllamaProvider", "Parse error: $rawJson")
                    }
                }
            }
        }
    }
}