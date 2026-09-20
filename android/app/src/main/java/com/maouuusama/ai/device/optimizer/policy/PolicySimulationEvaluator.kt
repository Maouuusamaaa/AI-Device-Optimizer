package com.maouuusama.ai.device.optimizer.policy

import com.maouuusama.ai.device.optimizer.monitor.DeviceSnapshot

data class SimulatedOptimizationPlan(
    val diagnoses: List<Diagnosis>,
    val simulation: PolicySimulationResult,
    val safetyGate: SafetyGateResult
)

class PolicySimulationEvaluator(
    private val diagnosisEngine: DiagnosisEngine = DiagnosisEngine(),
    private val policySimulator: PolicySimulator = PolicySimulator(),
    private val safetyGate: DryRunSafetyGate = DryRunSafetyGate()
) {
    fun evaluate(snapshot: DeviceSnapshot, isGaming: Boolean = false): SimulatedOptimizationPlan {
        val state = DeviceState.fromSnapshot(snapshot, isGaming)
        val diagnoses = diagnosisEngine.diagnose(state)
        val simulation = policySimulator.simulate(diagnoses)
        val proposal = DryRunPolicyProposal(state, simulation.decisions, simulation.executionAllowed)
        val gate = safetyGate.evaluate(proposal)
        return SimulatedOptimizationPlan(diagnoses, simulation, gate)
    }
}