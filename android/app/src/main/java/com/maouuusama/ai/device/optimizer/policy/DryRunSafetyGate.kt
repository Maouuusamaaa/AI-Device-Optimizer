package com.maouuusama.ai.device.optimizer.policy

data class SafetyGateResult(
    val allowed: Boolean,
    val reasons: List<String>
) {
    init {
        require(reasons.isNotEmpty() || allowed) {
            "A denied safety-gate result must explain why it was denied."
        }
    }
}

class DryRunSafetyGate(
    private val catalog: ActionCatalog = ActionCatalog
) {
    fun evaluate(proposal: DryRunPolicyProposal): SafetyGateResult {
        val reasons = mutableListOf<String>()
        if (proposal.actionExecutionAllowed) {
            reasons += "Action execution is disabled for the measurement-only milestone."
        }
        proposal.decisions.forEach { decision ->
            if (decision.mode != PolicyMode.DRY_RUN) {
                reasons += "Policy " + decision.policyId + " is not DRY_RUN."
            }
            decision.proposedActionId?.let { actionId ->
                val action = catalog.find(actionId)
                if (action == null) {
                    reasons += "Action " + actionId + " is not present in the action catalog."
                } else {
                    if (action.risk == ActionRisk.HIGH) reasons += "High-risk action " + actionId + " is blocked in the dry-run safety gate."
                    if (action.requiredPermission != "none") reasons += "Action " + actionId + " requires permission " + action.requiredPermission + "."
                    if (!action.reversible) reasons += "Irreversible action " + actionId + " is blocked in the dry-run safety gate."
                }
            }
        }
        return SafetyGateResult(
            allowed = false,
            reasons = (reasons + "Execution remains disabled; dry-run proposals are observation-only.").distinct()
        )
    }
}
