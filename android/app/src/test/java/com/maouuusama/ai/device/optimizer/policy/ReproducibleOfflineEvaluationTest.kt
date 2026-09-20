package com.maouuusama.ai.device.optimizer.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReproducibleOfflineEvaluationTest {
    private fun metadata() = OfflineEvaluationMetadata(
        charging = false,
        batteryTemperatureC = 35.0,
        thermalStatus = 0,
        networkTransport = "wifi",
        networkValidated = true,
        interactive = true,
        workload = "idle"
    )

    private fun record(id: String, time: Long) = ReproducibleEvaluationRecord(
        observationId = id,
        timestampMs = time,
        conditionIds = listOf("device.normal"),
        actionIds = emptyList(),
        metadata = metadata()
    )

    @Test
    fun splitIsChronologicalAndDeterministic() {
        val records = (1L..10L).map { record("obs-$it", it) }
        val evaluator = ReproducibleOfflineEvaluator()
        val first = evaluator.validateAndSplit(records)
        val second = evaluator.validateAndSplit(records)
        assertEquals(8, first.first.training.size)
        assertEquals(2, first.first.holdout.size)
        assertEquals(first.second.deterministicFingerprint, second.second.deterministicFingerprint)
        assertEquals(first.first.holdout.map { it.observationId }, second.first.holdout.map { it.observationId })
        assertFalse(first.second.leakageDetected)
        assertFalse(first.second.evaluationReady)
        assertFalse(first.second.executionAllowed)
    }

    @Test(expected = IllegalArgumentException::class)
    fun unorderedRecordsAreRejected() {
        ReproducibleOfflineEvaluator().validateAndSplit(listOf(record("a", 2L), record("b", 1L)))
    }

    @Test
    fun duplicateIdsAndTimestampsAreReported() {
        val records = listOf(record("a", 1L), record("a", 1L), record("b", 2L))
        val result = ReproducibleOfflineEvaluator().validateAndSplit(records)
        assertTrue(result.second.leakageDetected)
        assertEquals(listOf("a"), result.second.duplicateObservationIds)
        assertEquals(listOf(1L), result.second.duplicateTimestamps)
    }

    @Test
    fun incompleteMetadataIsCountedWithoutInference() {
        val incomplete = ReproducibleEvaluationRecord(
            observationId = "a",
            timestampMs = 1L,
            conditionIds = listOf("device.normal"),
            actionIds = emptyList(),
            metadata = OfflineEvaluationMetadata(null, null, null, null, null, null, null)
        )
        val result = ReproducibleOfflineEvaluator().validateAndSplit(listOf(incomplete))
        assertEquals(1, result.second.metadataIncompleteCount)
        assertEquals(0, result.second.outcomeCount)
        assertFalse(result.second.evaluationReady)
    }

    @Test
    fun outcomeLabelsAreCountedButNotInterpreted() {
        val withOutcome = ReproducibleEvaluationRecord(
            observationId = "a",
            timestampMs = 1L,
            conditionIds = listOf("device.normal"),
            actionIds = listOf("observe.memory_pressure"),
            metadata = metadata(),
            outcomeLabel = "no_change"
        )
        val result = ReproducibleOfflineEvaluator().validateAndSplit(listOf(withOutcome))
        assertEquals(1, result.second.outcomeCount)
        assertFalse(result.second.evaluationReady)
    }
}
