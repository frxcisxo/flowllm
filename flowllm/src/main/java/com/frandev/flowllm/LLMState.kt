package com.frandev.flowllm

sealed class LLMState {
    data object Idle : LLMState()
    data object Loading : LLMState()
    data class Streaming(val text: String) : LLMState()
    data class Done(val text: String) : LLMState()
    data class Error(val exception: Throwable) : LLMState()
}