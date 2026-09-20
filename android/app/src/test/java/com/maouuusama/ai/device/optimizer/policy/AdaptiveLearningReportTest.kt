package com.maouuusama.ai.device.optimizer.policy

import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveLearningReportTest {
    @Test
    fun reportJsonContainsDescriptiveSummary() {
        val summary = AdaptiveLearningSummary(
            observationCount = 2,
            conditionCounts = mapOf("device.normal" to 2),
            actionStats = listOf(
                ActionLearningStats("observe.memory_pressure", 1, 100.0, null)
            ),
            interpretation = "descriptive_only: test"
        )
        val json = AdaptiveLearningReport(123L, summary).toJson()
        assertTrue(json.contains("\"observationCount\":2"))
        assertTrue(json.contains("\"device.normal\":2"))
        assertTrue(json.contains("\"actionId\":\"observe.memory_pressure\""))
        assertTrue(json.contains("\"averageBatteryDeltaPercent\":null"))
        assertTrue(json.contains("descriptive_only"))
    }
}
