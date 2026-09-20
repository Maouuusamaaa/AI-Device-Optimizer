package com.maouuusama.ai.device.optimizer.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ControlledOfflineEvaluationTest {
    private fun entry(timestamp: Long, condition: String): DecisionHistoryEntry =
        DecisionHistoryEntry(
            timestampMs = timestamp,
            conditionIds = listOf(condition),
            diagnoses = emptyList(),
            decisions = emptyList(),
            simulations = emptyList(),
            measurementReports = emptyList()
        )

    @Test
    fun temporalSplitIsDeterministicAndNonAuthorizing() {
        val entries = (1L..10L).map { entry(it, if (it <= 8) "device.normal" else "memory.pressure") }

        val report = ControlledOfflineEvaluation().evaluate(entries, generatedAtMs = 20L)

        assertEquals(10, report.totalObservations)
        assertEquals(8, report.partition.trainingCount)
        assertEquals(2, report.partition.holdoutCount)
        assertTrue(report.partition.sufficientHoldout)
        assertFalse(report.evaluationReady)
        assertEquals(8, report.partition.trainingConditions["device.normal"])
        assertEquals(2, report.partition.holdoutConditions["memory.pressure"])
        assertTrue(report.interpretation.startsWith("descriptive_only"))
    }

    @Test
    fun smallHistoryIsNotReadyForHoldoutExperiment() {
        val entries = (1L..3L).map { entry(it, "device.normal") }

        val report = ControlledOfflineEvaluation(minimumHoldoutCount = 2)
            .evaluate(entries, generatedAtMs = 4L)

        assertEquals(2, report.partition.trainingCount)
        assertEquals(1, report.partition.holdoutCount)
        assertFalse(report.partition.sufficientHoldout)
        assertFalse(report.evaluationReady)
    }

    @Test(expected = IllegalArgumentException::class)
    fun unorderedHistoryIsRejected() {
        ControlledOfflineEvaluation().evaluate(
            listOf(entry(2L, "device.normal"), entry(1L, "device.normal")),
            generatedAtMs = 3L
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun nonDescriptiveMeasurementIsRejected() {
        val invalid = DecisionHistoryEntry(
            timestampMs = 1L,
            conditionIds = listOf("memory.pressure"),
            diagnoses = emptyList(),
            decisions = emptyList(),
            simulations = emptyList(),
            measurementReports = listOf(
                PostActionMeasurementReport(
                    actionId = "observe.memory_pressure",
                    status = ActionSimulationStatus.BLOCKED,
                    deltas = emptyList(),
                    interpretation = "causal_result: invalid"
                )
            )
        )
        ControlledOfflineEvaluation().evaluate(listOf(invalid), generatedAtMs = 2L)
    }

    @Test
    fun jsonKeepsEvaluationDisabled() {
        val json = ControlledOfflineEvaluation().evaluate(emptyList(), 1L).toJson()
        assertTrue(json.contains("\"evaluationReady\":false"))
    }
}
