package com.maouuusama.ai.device.optimizer.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class DecisionHistoryPersistenceTest {
    private fun entry(timestamp: Long): DecisionHistoryEntry {
        val diagnosis = Diagnosis("memory.pressure", DiagnosisSeverity.ADVISORY, 0.95, listOf("availableRamMb=1200"), "observe.memory_pressure")
        val decision = PolicyDecision("memory.pressure", PolicySeverity.ADVISORY, "test", "observe.memory_pressure")
        val simulation = ActionSimulation("observe.memory_pressure", ActionSimulationStatus.SIMULATED, "Observation-only simulation", listOf("catalog membership: PASS"), "capture memory", "none")
        val report = PostActionMeasurementReport("observe.memory_pressure", ActionSimulationStatus.SIMULATED, listOf(MeasurementDelta("availableRamMb", 1200.0, 1300.0, 100.0, 8.333333333)), "descriptive_only: test")
        return DecisionHistoryEntry(timestamp, listOf("memory.pressure"), listOf(diagnosis), listOf(decision), listOf(simulation), listOf(report))
    }

    @Test fun codecRoundTripsNestedHistory() {
        val original = listOf(entry(100L))
        assertEquals(original, DecisionHistoryJsonCodec.decode(DecisionHistoryJsonCodec.encode(original)))
    }

    @Test(expected = IllegalArgumentException::class) fun unsupportedSchemaIsRejected() {
        DecisionHistoryJsonCodec.decode("{\"schemaVersion\":999,\"entries\":[]}"))
    }

    @Test fun persistentStoreKeepsNewestEntriesWithinBound() {
        val dir = createTempDirectory("optimizer-history").toFile()
        val file = File(dir, "history.json")
        val store = PersistentDecisionHistoryStore(file, 2)
        store.append(entry(1L)); store.append(entry(2L)); store.append(entry(3L))
        assertEquals(listOf(2L, 3L), store.snapshot().map { it.timestampMs })
        assertTrue(file.exists())
        dir.deleteRecursively()
    }
}