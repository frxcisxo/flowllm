package com.frandev.flowllm.providers

import com.frandev.flowllm.LLMProvider
import com.frandev.flowllm.providers.model.GeminiModel
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GeminiProvider(
    apiKey: String,
    model: GeminiModel = GeminiModel.FLASH_LITE
) : LLMProvider {

    private val generativeModel = GenerativeModel(
        modelName = model.id,
        apiKey = apiKey
    )

    override fun stream(prompt: String): Flow<String> =
        generativeModel.generateContentStream(prompt)
            .map { it.text ?: "" }
}