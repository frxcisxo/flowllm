package com.frandev.flowllm.compose

import androidx.compose.runtime.*
import com.frandev.flowllm.LLMProvider
import com.frandev.flowllm.LLMState
import com.frandev.flowllm.retry.RetryPolicy
import com.frandev.flowllm.retry.withRetry
import kotlinx.coroutines.launch

@Composable
fun rememberLLMStream(
    provider: LLMProvider,
    prompt: String,
    // RetryPolicy opcional — si no se pasa usa los defaults
    retryPolicy: RetryPolicy = RetryPolicy()
): LLMState {
    var state by remember { mutableStateOf<LLMState>(LLMState.Idle) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(prompt) {
        if (prompt.isBlank()) return@LaunchedEffect
        scope.launch {
            state = LLMState.Loading
            var accumulated = ""
            try {
                provider.stream(prompt)
                    .withRetry(retryPolicy) // ← retry automático
                    .collect { chunk ->
                        accumulated += chunk
                        state = LLMState.Streaming(accumulated)
                    }
                state = LLMState.Done(accumulated)
            } catch (e: Exception) {
                // Solo llega aquí si se agotan todos los reintentos
                state = LLMState.Error(e)
            }
        }
    }

    return state
}