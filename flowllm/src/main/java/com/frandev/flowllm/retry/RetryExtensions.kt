package com.frandev.flowllm.retry

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

fun <T> Flow<T>.withRetry(policy: RetryPolicy = RetryPolicy()): Flow<T> = flow {
    var currentDelay = policy.initialDelayMs
    var attempt = 0

    while (true) {
        try {
            collect { emit(it) }
            return@flow // éxito, salimos
        } catch (e: Exception) {
            attempt++
            if (attempt > policy.maxRetries) throw e

            android.util.Log.w(
                "FlowLLM",
                "Intento $attempt fallido: ${e.message}. Reintentando en ${currentDelay}ms"
            )

            delay(currentDelay)

            // backoff exponencial con límite máximo
            currentDelay = (currentDelay * policy.multiplier)
                .toLong()
                .coerceAtMost(policy.maxDelayMs)
        }
    }
}