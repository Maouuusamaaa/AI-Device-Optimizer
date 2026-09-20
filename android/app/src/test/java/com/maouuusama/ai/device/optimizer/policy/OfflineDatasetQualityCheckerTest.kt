package com.maouuusama.ai.device.optimizer.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineDatasetQualityCheckerTest {
    private fun entry(timestamp: Long, action: String? = null): DecisionHistoryEntry {
        val report = action?.let {
            PostActionMeasurementReport(
                actionId = it,
                status = ActionSimulationStatus.SIMULATED,
                deltas = emptyList(),
                interpretation = "descriptive_only: test"
            )
        }
        return DecisionHistoryEntry(
            timestampMs = timestamp,
            conditionIds = listOf("device.normal"),
            diagnoses = emptyList(),
            decisions = emptyList(),
            simulations = emptyList(),
            measurementReports = listOfNotNull(report)
        )
    }

    @Test
    fun cleanChronologicalDataHasNoDuplicateLeakage() {
        val report = OfflineDatasetQualityChecker().check(listOf(entry(1), entry(2), entry(3)))
        assertEquals(3, report.observationCount)
        assertEquals(0, report.duplicateTimestampCount)
        assertFalse(report.leakageDetected)
        assertFalse(report.evaluationSafeForReview)
        assertFalse(report.confounderMetadataAvailable)
        assertTrue(report.confounderMetadataMissing.isNotEmpty())
    }

    @Test
    fun duplicateTimestampIsReportedAsLeakageRisk() {
        val report = OfflineDatasetQualityChecker().check(listOf(entry(1), entry(1), entry(2)))
        assertEquals(1, report.duplicateTimestampCount)
        assertTrue(report.duplicateTimestampRisk)
        assertTrue(report.leakageDetected)
    }

    @Test
    fun repeatedAdjacentActionIsReportedButNotCalledLeakage() {
        val report = OfflineDatasetQualityChecker().check(
            listOf(entry(1, "observe.memory_pressure"), entry(2, "observe.memory_pressure"))
        )
        assertEquals(1, report.actionOverlapCount)
        assertTrue(report.actionOverlapRisk)
        assertFalse(report.leakageDetected)
    }

    @Test
    fun actionlessObservationsAreCounted() {
        val report = OfflineDatasetQualityChecker().check(
            listOf(entry(1), entry(2, "observe.memory_pressure"))
        )
        assertEquals(1, report.actionlessObservationCount)
        assertTrue(report.descriptiveOnly)
    }

    @Test
    fun decisionHistoryRejectsNonDescriptiveReportsBeforeQualityCheck() {
        assertThrows(IllegalArgumentException::class.java) {
            DecisionHistoryEntry(
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
        }
    }

    @Test
    fun jsonKeepsSafetyBoundaryExplicit() {
        val json = OfflineDatasetQualityChecker().check(emptyList()).toJson()
        assertTrue(json.contains("\"evaluationSafeForReview\":false"))
        assertTrue(json.contains("\"confounderMetadataAvailable\":false"))
    }
}
