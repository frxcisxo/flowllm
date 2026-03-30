package com.frandev.flowllm.retry

data class RetryPolicy(
    val maxRetries: Int = 3,
    val initialDelayMs: Long = 1000,
    val multiplier: Double = 2.0,
    val maxDelayMs: Long = 10000
)