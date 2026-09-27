package com.maouuusama.ai.device.optimizer.policy

import org.junit.Assert.*
import org.junit.Test

class AdaptiveLearningRecordTest {
    @Test fun insufficientHistoryEvidenceAbstainsAndHasDeterministicId() {
        val report = PostActionMeasurementReport(
            actionId = "action.memory",
            status = ActionSimulationStatus.SIMULATED,
            deltas = listOf(MeasurementDelta("availableRamMb", 100.0, 100.0, 0.0, 0.0)),
            interpretation = "descriptive_only: simulated"
        )
        val entry = DecisionHistoryEntry(
            timestampMs = 1000L,
            conditionIds = emptyList(),
            diagnoses = emptyList(),
            decisions = emptyList(),
            simulations = emptyList(),
            measurementReports = listOf(report)
        )
        val first = AdaptiveLearningRecord.fromHistory(entry, "Test", "Device", 33)
        val second = AdaptiveLearningRecord.fromHistory(entry, "Test", "Device", 33)
        assertEquals(1, first.size)
        assertEquals(first.single().evidenceId, second.single().evidenceId)
        assertEquals(AdaptiveLearningRecord.Classification.INSUFFICIENT_EVIDENCE, first.single().classification)
        assertEquals(AdaptiveLearningRecord.LearningDisposition.ABSTAIN, first.single().learningDisposition)
        assertEquals(0.0, first.single().features["availableRamMbDelta"]!!, 0.0)
    }
}
