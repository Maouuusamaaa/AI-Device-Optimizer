package com.maouuusama.ai.device.optimizer.policy

import com.maouuusama.ai.device.optimizer.monitor.DeviceSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DecisionHistoryRecorderTest {
    private fun snapshot(availableRamMb: Long = 900L) = DeviceSnapshot(
        timestampMs = 1234L,
        androidApi = 33,
        manufacturer = "TEST",
        model = "TEST_DEVICE",
        totalRamMb = 5634L,
        availableRamMb = availableRamMb,
        batteryPercent = 50,
        isCharging = false,
        batteryTemperatureC = 40.0,
        thermalStatus = 0,
        storageTotalBytes = 10_000L,
        storageFreeBytes = 5_000L,
        networkTransport = "WIFI",
        networkValidated = true,
        isInteractive = true,
        uptimeMs = 1_000L,
        processes = emptyList(),
        systemTelemetry = null
    )

    @Test
    fun recordsSimulationAndDescriptiveMeasurement() {
        val store = DecisionHistoryStore()
        val plan = PolicySimulationEvaluator().evaluate(snapshot())
        val proposal = DryRunPolicyProposal(
            DeviceState.fromSnapshot(snapshot()),
            plan.simulation.decisions,
            plan.simulation.executionAllowed
        )
        val simulations = DryRunActionEngine().simulate(proposal, plan.safetyGate)

        val entry = DecisionHistoryRecorder(store).record(snapshot(), plan, simulations)

        assertEquals(1, store.size())
        assertEquals(entry, store.snapshot().single())
        assertEquals(plan.diagnoses, entry.diagnoses)
        assertEquals(plan.simulation.decisions, entry.decisions)
        assertEquals(simulations, entry.simulations)
        assertTrue(entry.measurementReports.all {
            it.interpretation.startsWith("descriptive_only")
        })
        assertTrue(entry.measurementReports.all { report ->
            report.deltas.all { delta -> delta.absoluteDelta == 0.0 }
        })
        assertFalse(entry.decisions.any { it.mode != PolicyMode.DRY_RUN })
    }

    @Test
    fun recordsNormalObservationWithoutInventingActionResults() {
        val normal = snapshot(availableRamMb = 3000L)
        val store = DecisionHistoryStore()
        val plan = PolicySimulationEvaluator().evaluate(normal)
        val proposal = DryRunPolicyProposal(
            DeviceState.fromSnapshot(normal),
            plan.simulation.decisions,
            plan.simulation.executionAllowed
        )
        val simulations = DryRunActionEngine().simulate(proposal, plan.safetyGate)

        val entry = DecisionHistoryRecorder(store).record(normal, plan, simulations)

        assertEquals(listOf("device.normal"), entry.conditionIds)
        assertTrue(entry.simulations.isEmpty())
        assertTrue(entry.measurementReports.isEmpty())
    }
}
