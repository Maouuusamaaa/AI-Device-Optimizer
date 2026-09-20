package com.maouuusama.ai.device.optimizer.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
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
        assertEquals(original, DecisionHistoryBinaryCodec.decode(DecisionHistoryBinaryCodec.encode(original)))
    }

    @Test(expected = IllegalArgumentException::class) fun unsupportedSchemaIsRejected() {
        val bytes = ByteArrayOutputStream().also { stream -> DataOutputStream(stream).use { output -> output.writeUTF("AI_DEVICE_OPTIMIZER_HISTORY"); output.writeInt(999); output.writeInt(0) } }.toByteArray()
        DecisionHistoryBinaryCodec.decode(bytes)
    }

    @Test fun persistentStoreSurvivesNewStoreInstance() {
        val dir = createTempDirectory("optimizer-history-reload").toFile()
        val file = File(dir, "history.bin")
        PersistentDecisionHistoryStore(file, 2).append(entry(42L))
        val reopened = PersistentDecisionHistoryStore(file, 2)
        assertEquals(listOf(42L), reopened.snapshot().map { it.timestampMs })
        dir.deleteRecursively()
    }

    @Test fun corruptedHistoryIsRejected() {
        val dir = createTempDirectory("optimizer-history-corrupt").toFile()
        val file = File(dir, "history.bin")
        file.writeBytes(byteArrayOf(1, 2, 3, 4))
        val store = PersistentDecisionHistoryStore(file, 2)
        assertThrows(IllegalStateException::class.java) { store.snapshot() }
        dir.deleteRecursively()
    }

    @Test fun persistentStoreKeepsNewestEntriesWithinBound() {
        val dir = createTempDirectory("optimizer-history").toFile()
        val file = File(dir, "history.bin")
        val store = PersistentDecisionHistoryStore(file, 2)
        store.append(entry(1L)); store.append(entry(2L)); store.append(entry(3L))
        assertEquals(listOf(2L, 3L), store.snapshot().map { it.timestampMs })
        assertTrue(file.exists())
        dir.deleteRecursively()
    }
}