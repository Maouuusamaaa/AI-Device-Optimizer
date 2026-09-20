package com.maouuusama.ai.device.optimizer.localai

import java.io.File

class LocalLlamaRuntime {
    companion object {
        init { System.loadLibrary("ai_local_runtime") }
        const val DEFAULT_CONTEXT_TOKENS = 4096
        const val DEFAULT_MAX_TOKENS = 64
    }

    private external fun nativeVersion(): String
    private external fun nativeGenerate(modelPath: String, prompt: String, contextTokens: Int, maxTokens: Int): String
    private external fun nativeIsAvailable(): Boolean

    fun isAvailable(): Boolean = nativeIsAvailable()
    fun runtimeVersion(): String = nativeVersion()

    fun generate(
        modelFile: File,
        prompt: String,
        contextTokens: Int = DEFAULT_CONTEXT_TOKENS,
        maxTokens: Int = DEFAULT_MAX_TOKENS
    ): String {
        require(modelFile.isFile) { "Model file does not exist: ${modelFile.absolutePath}" }
        require(modelFile.length() > 0L) { "Model file is empty" }
        require(prompt.isNotBlank()) { "Prompt must not be blank" }
        require(contextTokens in 256..8192) { "contextTokens must be 256..8192" }
        require(maxTokens in 1..256) { "maxTokens must be 1..256" }
        return nativeGenerate(modelFile.absolutePath, prompt, contextTokens, maxTokens)
    }
}
