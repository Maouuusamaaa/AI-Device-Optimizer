package com.maouuusama.ai.device.optimizer.localai

import android.content.Context
import java.io.File

object QwenLocalModel {
    const val MODEL_ID = "qwen3-0.6b-q4_0"
    const val FILE_NAME = "Qwen3-0.6B-Q4_0.gguf"
    const val EXPECTED_SIZE_BYTES = 428970080L
    const val EXPECTED_SHA256 =
        "da2572f16c06133561ce56accaa822216f2391ef4d37fba427801cd6736417d4"

    const val DOWNLOAD_URL =
        "https://huggingface.co/ggml-org/Qwen3-0.6B-GGUF/resolve/main/Qwen3-0.6B-Q4_0.gguf?download=true"

    fun file(context: Context): File = File(File(context.filesDir, "models"), FILE_NAME)
    fun partialFile(context: Context): File = File(File(context.filesDir, "models"), "$FILE_NAME.part")

    fun prompt(userText: String): String =
        "<|im_start|>system\n" +
            "You are a local advisory component of AI Device Optimizer. " +
            "You must never request device mutation. Return concise analysis only.\n" +
            "<|im_end|>\n" +
            "<|im_start|>user\n" +
            userText.trim() +
            " /no_think" +
            "\n<|im_end|>\n<|im_start|>assistant\n"
}
