package com.maouuusama.ai.device.optimizer.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveLearningEvidenceEvaluatorTest {
    @Test
    fun emptyHistoryProducesNonAuthorizingReport() {
        val report = AdaptiveLearningEvidenceEvaluator(minimumSamplesPerAction = 2)
            .evaluate(emptyList(), generatedAtMs = 100L)

        assertEquals(0, report.totalObservations)
        assertTrue(report.evidence.isEmpty())
        assertFalse(report.futureOfflineEvaluationReady)
        assertTrue(report.interpretation.startsWith("descriptive_only"))
    }

    @Test
    fun actionEvidenceTracksCompletenessAndSampleCount() {
        val simulation = ActionSimulation(
            actionId = "observe.memory_pressure",
            status = ActionSimulationStatus.SIMULATED,
            message = "observation-only",
            preconditionChecks = listOf("catalog membership: PASS"),
            measurementPlan = "RAM",
            rollbackPlan = "none"
        )
        val measurement = PostActionMeasurementReport(
            actionId = simulation.actionId,
            status = simulation.status,
            deltas = listOf(
                MeasurementDelta("availableRamMb", 1000.0, 1100.0, 100.0, 10.0),
                MeasurementDelta("batteryPercent", 50.0, 50.0, 0.0, 0.0)
            ),
            interpretation = "descriptive_only: test"
        )
        val entry = DecisionHistoryEntry(
            timestampMs = 1L,
            conditionIds = listOf("memory.pressure"),
            diagnoses = listOf(
                Diagnosis(
                    conditionId = "memory.pressure",
                    severity = DiagnosisSeverity.ADVISORY,
                    confidence = 0.95,
                    evidence = listOf("available RAM is low"),
                    proposedActionId = simulation.actionId
                )
            ),
            decisions = listOf(
                PolicyDecision(
                    policyId = "memory.pressure",
                    severity = PolicySeverity.ADVISORY,
                    reason = "test",
                    proposedActionId = simulation.actionId
                )
            ),
            simulations = listOf(simulation),
            measurementReports = listOf(measurement)
        )

        val result = AdaptiveLearningEvidenceEvaluator(minimumSamplesPerAction = 2)
            .evaluate(listOf(entry), generatedAtMs = 2L)

        val evidence = result.evidence.single()
        assertEquals(1, evidence.observationCount)
        assertEquals(1, evidence.completeRamDeltaCount)
        assertEquals(1, evidence.completeBatteryDeltaCount)
        assertFalse(evidence.sufficientSampleSize)
        assertFalse(result.futureOfflineEvaluationReady)
    }

    @Test(expected = IllegalArgumentException::class)
    fun nonDescriptiveReportIsRejected() {
        val entry = DecisionHistoryEntry(
            timestampMs = 1L,
            conditionIds = listOf("device.normal"),
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
        AdaptiveLearningEvidenceEvaluator().evaluate(listOf(entry), generatedAtMs = 2L)
    }

    @Test
    fun jsonSerializationKeepsSafetyBoundaryExplicit() {
        val report = AdaptiveLearningEvidenceEvaluator().evaluate(emptyList(), generatedAtMs = 123L)
        val json = report.toJson()
        assertTrue(json.contains("\"futureOfflineEvaluationReady\":false"))
        assertTrue(json.contains("descriptive_only"))
    }
}
