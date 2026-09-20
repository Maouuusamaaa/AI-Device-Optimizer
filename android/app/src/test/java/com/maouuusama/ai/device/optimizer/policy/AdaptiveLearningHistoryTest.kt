package com.maouuusama.ai.device.optimizer.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveLearningHistoryTest {
    private fun diagnosis(condition: String, action: String? = null) =
        Diagnosis(
            conditionId = condition,
            severity = DiagnosisSeverity.ADVISORY,
            confidence = 0.95,
            evidence = listOf("test evidence"),
            proposedActionId = action
        )

    private fun decision(action: String? = null) =
        PolicyDecision(
            policyId = "test.policy",
            severity = PolicySeverity.ADVISORY,
            reason = "test",
            proposedActionId = action,
            mode = PolicyMode.DRY_RUN
        )

    private fun simulation(action: String) =
        ActionSimulation(
            actionId = action,
            status = ActionSimulationStatus.SIMULATED,
            message = "simulation",
            preconditionChecks = listOf("catalog membership: PASS"),
            measurementPlan = "test",
            rollbackPlan = "none"
        )

    private fun report(action: String, ramDelta: Double, batteryDelta: Double) =
        PostActionMeasurementReport(
            actionId = action,
            status = ActionSimulationStatus.SIMULATED,
            deltas = listOf(
                MeasurementDelta("availableRamMb", 1000.0, 1000.0 + ramDelta, ramDelta, null),
                MeasurementDelta("batteryPercent", 50.0, 50.0 + batteryDelta, batteryDelta, null)
            ),
            interpretation = "descriptive_only: test"
        )

    private fun entry(
        timestamp: Long,
        condition: String,
        action: String,
        ramDelta: Double,
        batteryDelta: Double
    ) = DecisionHistoryEntry(
        timestampMs = timestamp,
        conditionIds = listOf(condition),
        diagnoses = listOf(diagnosis(condition, action)),
        decisions = listOf(decision(action)),
        simulations = listOf(simulation(action)),
        measurementReports = listOf(report(action, ramDelta, batteryDelta))
    )

    @Test
    fun storePreservesInsertionOrderAndReturnsSnapshot() {
        val store = DecisionHistoryStore()
        store.append(entry(10L, "memory.pressure", "observe.memory_pressure", 100.0, -1.0))
        store.append(entry(20L, "memory.critical", "observe.memory_critical", -50.0, -2.0))

        assertEquals(2, store.size())
        assertEquals(10L, store.snapshot().first().timestampMs)
        assertEquals(20L, store.snapshot().last().timestampMs)
    }

    @Test
    fun summarizerAggregatesDescriptiveObservations() {
        val entries = listOf(
            entry(10L, "memory.pressure", "observe.memory_pressure", 100.0, -1.0),
            entry(20L, "memory.pressure", "observe.memory_pressure", 300.0, -3.0),
            entry(30L, "memory.critical", "observe.memory_critical", -50.0, -2.0)
        )

        val summary = AdaptiveLearningSummarizer().summarize(entries)

        assertEquals(3, summary.observationCount)
        assertEquals(2, summary.conditionCounts["memory.pressure"])
        assertEquals(1, summary.conditionCounts["memory.critical"])

        val pressure = summary.actionStats.first { it.actionId == "observe.memory_pressure" }
        assertEquals(2, pressure.observationCount)
        assertEquals(200.0, pressure.averageRamDeltaMb!!, 0.0)
        assertEquals(-2.0, pressure.averageBatteryDeltaPercent!!, 0.0)
        assertTrue(summary.interpretation.startsWith("descriptive_only"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun nonDescriptiveMeasurementIsRejected() {
        DecisionHistoryEntry(
            timestampMs = 1L,
            conditionIds = listOf("memory.pressure"),
            diagnoses = listOf(diagnosis("memory.pressure")),
            decisions = listOf(decision("observe.memory_pressure")),
            simulations = listOf(simulation("observe.memory_pressure")),
            measurementReports = listOf(
                PostActionMeasurementReport(
                    actionId = "observe.memory_pressure",
                    status = ActionSimulationStatus.SIMULATED,
                    deltas = emptyList(),
                    interpretation = "causal_success"
                )
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankConditionIsRejected() {
        entry(1L, "", "observe.memory_pressure", 0.0, 0.0)
    }
}
