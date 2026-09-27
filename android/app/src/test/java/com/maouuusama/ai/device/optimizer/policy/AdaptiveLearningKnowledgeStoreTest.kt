package com.maouuusama.ai.device.optimizer.policy

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files

class AdaptiveLearningKnowledgeStoreTest {
    @Test fun missingStateInitializesEmpty() {
        val dir = Files.createTempDirectory("adaptive-state").toFile()
        val state = AdaptiveLearningKnowledgeStore(File(dir, "state.bin")).load()
        assertEquals(AdaptiveLearningKnowledgeState.empty(), state)
    }

    @Test fun validStateSurvivesReload() {
        val dir = Files.createTempDirectory("adaptive-state").toFile()
        val file = File(dir, "state.bin")
        val store = AdaptiveLearningKnowledgeStore(file)
        val record = AdaptiveLearningRecord(
            evidenceId = "e1", manufacturer = "Test", model = "Device", androidApi = 33,
            workload = "background_monitoring", policyId = null,
            classification = AdaptiveLearningRecord.Classification.INSUFFICIENT_EVIDENCE,
            features = emptyMap(), provenance = mapOf("analysisId" to "a"),
            learningDisposition = AdaptiveLearningRecord.LearningDisposition.ABSTAIN
        )
        val expected = AdaptiveLearningKnowledgeState.empty().apply(listOf(record))
        store.save(expected)
        assertEquals(expected, store.load())
    }

    @Test(expected = IllegalStateException::class)
    fun corruptStateFailsClosed() {
        val dir = Files.createTempDirectory("adaptive-state").toFile()
        val file = File(dir, "state.bin")
        file.writeText("corrupt")
        AdaptiveLearningKnowledgeStore(file).load()
    }
}
