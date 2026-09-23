package com.maouuusama.ai.device.optimizer.localai

import java.io.File

class LocalLlamaRuntime {
    companion object {
        init { System.loadLibrary("ai_local_runtime") }
        const val DEFAULT_CONTEXT_TOKENS = 4096
        const val DEFAULT_MAX_TOKENS = 64
        const val DEFAULT_THREADS = 4
        const val MIN_THREADS = 1
        const val MAX_THREADS = 8
    }

    private external fun nativeVersion(): String
    private external fun nativeGenerate(
        modelPath: String,
        prompt: String,
        contextTokens: Int,
        maxTokens: Int,
        threads: Int,
        keepBackendAlive: Boolean
    ): String
    private external fun nativeIsAvailable(): Boolean
    private external fun nativeResetRuntime()

    fun isAvailable(): Boolean = nativeIsAvailable()
    fun resetRuntime() = nativeResetRuntime()
    fun runtimeVersion(): String = nativeVersion()

    fun generate(
        modelFile: File,
        prompt: String,
        contextTokens: Int = DEFAULT_CONTEXT_TOKENS,
        maxTokens: Int = DEFAULT_MAX_TOKENS,
        threads: Int = DEFAULT_THREADS
    ): String = generateInternal(
        modelFile, prompt, contextTokens, maxTokens, threads, keepBackendAlive = false
    )

    /** Diagnostic-only mode: model/context are freed, backend state remains until resetRuntime(). */
    fun generateForLifecycleDiagnostic(
        modelFile: File,
        prompt: String,
        contextTokens: Int = DEFAULT_CONTEXT_TOKENS,
        maxTokens: Int = DEFAULT_MAX_TOKENS,
        threads: Int = DEFAULT_THREADS
    ): String = generateInternal(
        modelFile, prompt, contextTokens, maxTokens, threads, keepBackendAlive = true
    )

    private fun generateInternal(
        modelFile: File,
        prompt: String,
        contextTokens: Int,
        maxTokens: Int,
        threads: Int,
        keepBackendAlive: Boolean
    ): String {
        require(modelFile.isFile) { "Model file does not exist: ${modelFile.absolutePath}" }
        require(modelFile.length() > 0L) { "Model file is empty" }
        require(prompt.isNotBlank()) { "Prompt must not be blank" }
        require(contextTokens in 256..8192) { "contextTokens must be 256..8192" }
        require(maxTokens in 1..256) { "maxTokens must be 1..256" }
        require(threads in MIN_THREADS..MAX_THREADS) { "threads must be ${MIN_THREADS}..${MAX_THREADS}" }
        return nativeGenerate(
            modelFile.absolutePath, prompt, contextTokens, maxTokens, threads, keepBackendAlive
        )
    }
}
