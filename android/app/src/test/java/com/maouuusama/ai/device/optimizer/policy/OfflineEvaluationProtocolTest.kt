package com.maouuusama.ai.device.optimizer.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineEvaluationProtocolTest {
    private fun metadata(complete: Boolean = true) = OfflineEvaluationMetadata(
        charging = if (complete) false else null,
        batteryTemperatureC = if (complete) 35.0 else null,
        thermalStatus = if (complete) 0 else null,
        networkTransport = if (complete) "wifi" else null,
        networkValidated = if (complete) true else null,
        interactive = if (complete) true else null,
        workload = if (complete) "idle" else null
    )

    private fun record(id: String, time: Long, action: String? = null, complete: Boolean = true, outcome: String? = null) =
        ReproducibleEvaluationRecord(
            observationId = id,
            timestampMs = time,
            conditionIds = listOf("device.normal"),
            actionIds = listOfNotNull(action),
            metadata = metadata(complete),
            outcomeLabel = outcome
        )

    @Test
    fun completeDisjointDatasetIsReadyOnlyForDescriptiveReview() {
        val records = listOf(
            record("1", 1),
            record("2", 2, "observe.memory_pressure", outcome = "no_change"),
            record("3", 3),
            record("4", 4, "observe.memory_pressure", outcome = "no_change"),
            record("5", 5)
        )
        val evaluator = ReproducibleOfflineEvaluator()
        val (split, integrity) = evaluator.validateAndSplit(records)
        val report = OfflineEvaluationProtocol().assess(split, integrity)
        assertEquals(5, report.totalObservations)
        assertEquals(4, report.trainingObservations)
        assertEquals(1, report.holdoutObservations)
        assertEquals(2, report.actionCoverage["observe.memory_pressure"])
        assertEquals(2, report.trainingOutcomeCount)
        assertEquals(0, report.holdoutOutcomeCount)
        assertEquals(OfflineEvaluationReadiness.READY_FOR_DESCRIPTIVE_REVIEW, report.readiness)
        assertFalse(report.policySelectionAllowed)
        assertFalse(report.executionAllowed)
    }

    @Test
    fun incompleteMetadataBlocksReadiness() {
        val records = listOf(record("1", 1, complete = false), record("2", 2))
        val (split, integrity) = ReproducibleOfflineEvaluator().validateAndSplit(records)
        val report = OfflineEvaluationProtocol().assess(split, integrity)
        assertEquals(1, report.incompleteMetadataCount)
        assertEquals(OfflineEvaluationReadiness.NOT_READY, report.readiness)
    }

    @Test
    fun leakageBlocksReadiness() {
        val records = listOf(record("1", 1), record("1", 1), record("2", 2))
        val (split, integrity) = ReproducibleOfflineEvaluator().validateAndSplit(records)
        val report = OfflineEvaluationProtocol().assess(split, integrity)
        assertTrue(report.leakageDetected)
        assertEquals(OfflineEvaluationReadiness.NOT_READY, report.readiness)
    }

    @Test
    fun insufficientTrainingSetBlocksReadiness() {
        val records = listOf(record("1", 1))
        val (split, integrity) = ReproducibleOfflineEvaluator(0.8).validateAndSplit(records)
        val report = OfflineEvaluationProtocol().assess(split, integrity)
        assertEquals(0, report.trainingObservations)
        assertEquals(1, report.holdoutObservations)
        assertEquals(OfflineEvaluationReadiness.NOT_READY, report.readiness)
    }
}
