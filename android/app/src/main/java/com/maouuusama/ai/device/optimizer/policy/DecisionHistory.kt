package com.maouuusama.ai.device.optimizer.policy

/**
 * Immutable record of one measurement-only optimization decision cycle.
 *
 * This record is intentionally descriptive. It does not authorize execution and does not
 * infer causal effectiveness from before/after measurements.
 */
data class DecisionHistoryEntry(
    val timestampMs: Long,
    val conditionIds: List<String>,
    val diagnoses: List<Diagnosis>,
    val decisions: List<PolicyDecision>,
    val simulations: List<ActionSimulation>,
    val measurementReports: List<PostActionMeasurementReport>
) {
    init {
        require(timestampMs >= 0L) { "timestampMs must be non-negative." }
        require(conditionIds.all { it.isNotBlank() }) { "condition IDs must not be blank." }
        require(diagnoses.all { it.conditionId in conditionIds }) {
            "Every diagnosis must reference a recorded condition."
        }
        require(decisions.all { it.mode == PolicyMode.DRY_RUN }) {
            "Decision history may only contain DRY_RUN policy decisions."
        }
        require(simulations.all { it.status == ActionSimulationStatus.SIMULATED || it.status == ActionSimulationStatus.BLOCKED }) {
            "Decision history contains an unknown simulation status."
        }
        require(measurementReports.all { it.interpretation.startsWith("descriptive_only") }) {
            "Measurement reports must remain descriptive-only."
        }
    }
}

/**
 * Small, deterministic history store for the measurement-only milestone.
 *
 * The store keeps records in insertion order and returns snapshots, so callers cannot mutate
 * the internal collection. Persistence can be added later without changing the record contract.
 */
class DecisionHistoryStore {
    private val entries = mutableListOf<DecisionHistoryEntry>()

    fun append(entry: DecisionHistoryEntry) {
        entries += entry
    }

    fun snapshot(): List<DecisionHistoryEntry> = entries.toList()

    fun size(): Int = entries.size
}
