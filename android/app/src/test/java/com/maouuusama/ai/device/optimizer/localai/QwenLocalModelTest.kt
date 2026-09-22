package com.maouuusama.ai.device.optimizer.localai

import org.junit.Assert.assertTrue
import org.junit.Test

class QwenLocalModelTest {
    @Test
    fun promptUsesHardNonThinkingGenerationPrefix() {
        val prompt = QwenLocalModel.prompt("Telemetry: battery=50%")

        assertTrue(prompt.contains("<|im_start|>system\n"))
        assertTrue(prompt.contains("<|im_start|>user\nTelemetry: battery=50%\n<|im_end|>"))
        assertTrue(prompt.endsWith("<|im_start|>assistant\n<think>\n\n</think>\n\n"))
    }

    @Test
    fun promptDoesNotNeedSoftNoThinkInstruction() {
        val prompt = QwenLocalModel.prompt("test")

        assertTrue(!prompt.contains("/no_think"))
    }
}
