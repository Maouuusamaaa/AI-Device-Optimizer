package com.maouuusama.ai.device.optimizer.policy

import com.maouuusama.ai.device.optimizer.monitor.DeviceSnapshot

/**
 * Connects the measurement-only diagnosis/simulation pipeline to decision history.
 *
 * The recorder intentionally evaluates simulated actions against the same pre/post state.
 * This produces a zero-reference descriptive baseline and never claims that an action caused
 * the observed values to change.
 */
class DecisionHistoryRecorder(
    private val historyStore: DecisionHistoryStore,
    private val measurementEvaluator: PostActionMeasurementEvaluator = PostActionMeasurementEvaluator()
) {
    fun record(
        snapshot: DeviceSnapshot,
        plan: SimulatedOptimizationPlan,
        simulations: List<ActionSimulation>
    ): DecisionHistoryEntry {
        val state = DeviceState.fromSnapshot(snapshot)
        val reports = simulations.map { simulation ->
            measurementEvaluator.evaluate(simulation, state, state)
        }

        val entry = DecisionHistoryEntry(
            timestampMs = snapshot.timestampMs,
            conditionIds = plan.diagnoses.map { it.conditionId }.distinct(),
            diagnoses = plan.diagnoses,
            decisions = plan.simulation.decisions,
            simulations = simulations,
            measurementReports = reports
        )
        historyStore.append(entry)
        return entry
    }
}
