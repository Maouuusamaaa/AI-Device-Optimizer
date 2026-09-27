package com.maouuusama.ai.device.optimizer.policy

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files

class AdaptiveLearningRuntimeTest {
    @Test fun runtimeProcessesHistoryAndIsIdempotent() {
        val dir = Files.createTempDirectory("adaptive-runtime").toFile()
        val history = PersistentDecisionHistoryStore(File(dir, "history.bin"), 100)
        val report = PostActionMeasurementReport(
            "action.memory", ActionSimulationStatus.SIMULATED,
            listOf(MeasurementDelta("availableRamMb", 100.0, 100.0, 0.0, 0.0)),
            "descriptive_only: simulated"
        )
        history.append(DecisionHistoryEntry(1L, emptyList(), emptyList(), emptyList(), emptyList(), listOf(report)))
        val runtime = AdaptiveLearningRuntime(
            history, AdaptiveLearningKnowledgeStore(File(dir, "state.bin")),
            AdaptiveLearningResourceGuard { AdaptiveLearningResourceSnapshot(false, 256L * 1024L * 1024L, 0) },
            "Test", "Device", 33
        )
        val first = runtime.processAvailable()
        val second = runtime.processAvailable()
        assertEquals(1, first.processedCount)
        assertEquals(1, first.abstentionCount)
        assertEquals(0, first.candidateCount)
        assertEquals(0, second.processedCount)
        assertEquals(first.stateFingerprint, second.stateFingerprint)
    }

    @Test fun runtimeDefersWithoutChangingState() {
        val dir = Files.createTempDirectory("adaptive-runtime").toFile()
        val history = PersistentDecisionHistoryStore(File(dir, "history.bin"), 100)
        val runtime = AdaptiveLearningRuntime(
            history, AdaptiveLearningKnowledgeStore(File(dir, "state.bin")),
            AdaptiveLearningResourceGuard { AdaptiveLearningResourceSnapshot(true, 256L * 1024L * 1024L, 0) },
            "Test", "Device", 33
        )
        val result = runtime.processAvailable()
        assertTrue(result.deferred)
        assertEquals(0, result.processedCount)
    }
}
