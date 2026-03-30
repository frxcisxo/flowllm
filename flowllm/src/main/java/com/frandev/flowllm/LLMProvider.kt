package com.frandev.flowllm

import kotlinx.coroutines.flow.Flow

interface LLMProvider {
    fun stream(prompt: String): Flow<String>
}