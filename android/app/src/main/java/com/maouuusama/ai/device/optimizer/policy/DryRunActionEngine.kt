package com.maouuusama.ai.device.optimizer.policy

enum class ActionSimulationStatus { SIMULATED, BLOCKED }

data class ActionSimulation(
    val actionId: String,
    val status: ActionSimulationStatus,
    val message: String,
    val preconditionChecks: List<String>,
    val measurementPlan: String,
    val rollbackPlan: String
)

class DryRunActionEngine(private val catalog: ActionCatalog = ActionCatalog) {
    fun simulate(proposal: DryRunPolicyProposal, gate: SafetyGateResult = DryRunSafetyGate(catalog).evaluate(proposal)): List<ActionSimulation> {
        require(!gate.allowed) { "Dry-run action engine must never receive an execution-authorizing gate." }
        return proposal.decisions.mapNotNull { decision ->
            val actionId = decision.proposedActionId ?: return@mapNotNull null
            val action = catalog.find(actionId)
            if (action == null) return@mapNotNull ActionSimulation(actionId, ActionSimulationStatus.BLOCKED, "Action is not present in the allowlist.", listOf("catalog membership: FAIL"), "No measurement plan available.", "No rollback plan available.")
            ActionSimulation(
                action.id, ActionSimulationStatus.SIMULATED,
                "Observation-only simulation; no device mutation was attempted.",
                listOf(
                    "catalog membership: PASS",
                    "risk LOW: " + if (action.risk == ActionRisk.LOW) "PASS" else "FAIL",
                    "permission none: " + if (action.requiredPermission == "none") "PASS" else "FAIL",
                    "reversible: " + if (action.reversible) "PASS" else "FAIL"
                ),
                action.measurement, action.rollback
            )
        }
    }
}
