package com.maouuusama.ai.device.optimizer.policy

import org.junit.Assert.*
import org.junit.Test

class PostActionMeasurementEvaluatorTest {
    private fun state(ram: Long, battery: Int = 50, temp: Double = 40.0, storage: Long = 5000L) =
        DeviceState(ram, 5634L, battery, true, false, temp, null, 10000L, storage)

    @Test fun computesDescriptiveDeltas() {
        val simulation = ActionSimulation(
            "observe.memory_pressure", ActionSimulationStatus.SIMULATED, "simulation",
            listOf("catalog membership: PASS"), "memory", "none"
        )
        val report = PostActionMeasurementEvaluator().evaluate(simulation, state(1000), state(1200, 49, 41.0, 5500))
        assertEquals(4, report.deltas.size)
        assertEquals(200.0, report.deltas.first { it.metric == "availableRamMb" }.absoluteDelta, 0.0)
        assertEquals(20.0, report.deltas.first { it.metric == "availableRamMb" }.percentDelta!!, 0.0)
        assertTrue(report.interpretation.startsWith("descriptive_only"))
    }

    @Test fun missingOptionalMetricsAreNotInvented() {
        val simulation = ActionSimulation("observe.memory_pressure", ActionSimulationStatus.BLOCKED, "blocked", emptyList(), "", "")
        val report = PostActionMeasurementEvaluator().evaluate(
            simulation,
            state(1000, 50, 40.0, 5000),
            DeviceState(900, 5634L, null, true, false)
        )
        assertEquals(1, report.deltas.size)
        assertEquals("availableRamMb", report.deltas.single().metric)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidRamIsRejected() {
        val simulation = ActionSimulation("observe.memory_pressure", ActionSimulationStatus.SIMULATED, "simulation", emptyList(), "m", "r")
        PostActionMeasurementEvaluator().evaluate(simulation, state(-1), state(1000))
    }
}
