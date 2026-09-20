package com.maouuusama.ai.device.optimizer.policy

enum class SafetyBlockReason {
    EXECUTION_DISABLED, NON_DRY_RUN_POLICY, UNKNOWN_ACTION, PERMISSION_REQUIRED,
    HIGH_RISK_ACTION, IRREVERSIBLE_ACTION, MISSING_MEASUREMENT, ACTION_NOT_ALLOWLISTED
}

data class SafetyGateResult(
    val allowed: Boolean,
    val reasons: List<String>,
    val blockReasons: List<SafetyBlockReason>
) {
    init {
        require(reasons.isNotEmpty() || allowed) { "A denied safety-gate result must explain why it was denied." }
        require(blockReasons.isNotEmpty() || allowed) { "A denied safety-gate result must contain structured block reasons." }
    }
}

class DryRunSafetyGate(private val catalog: ActionCatalog = ActionCatalog) {
    fun evaluate(proposal: DryRunPolicyProposal): SafetyGateResult {
        val reasons = mutableListOf<String>()
        val blocks = linkedSetOf<SafetyBlockReason>()

        if (proposal.actionExecutionAllowed) {
            blocks += SafetyBlockReason.EXECUTION_DISABLED
            reasons += "Action execution is disabled for the measurement-only milestone."
        }

        proposal.decisions.forEach { decision ->
            if (decision.mode != PolicyMode.DRY_RUN) {
                blocks += SafetyBlockReason.NON_DRY_RUN_POLICY
                reasons += "Policy " + decision.policyId + " is not DRY_RUN."
            }
            decision.proposedActionId?.let { actionId ->
                val action = catalog.find(actionId)
                if (action == null) {
                    blocks += SafetyBlockReason.UNKNOWN_ACTION
                    blocks += SafetyBlockReason.ACTION_NOT_ALLOWLISTED
                    reasons += "Action " + actionId + " is not present in the action catalog."
                } else {
                    if (action.risk != ActionRisk.LOW) {
                        blocks += SafetyBlockReason.HIGH_RISK_ACTION
                        reasons += "Action " + actionId + " is not LOW risk."
                    }
                    if (action.requiredPermission != "none") {
                        blocks += SafetyBlockReason.PERMISSION_REQUIRED
                        reasons += "Action " + actionId + " requires permission " + action.requiredPermission + "."
                    }
                    if (!action.reversible) {
                        blocks += SafetyBlockReason.IRREVERSIBLE_ACTION
                        reasons += "Action " + actionId + " is irreversible."
                    }
                    if (action.measurement.isBlank()) {
                        blocks += SafetyBlockReason.MISSING_MEASUREMENT
                        reasons += "Action " + actionId + " has no measurement definition."
                    }
                }
            }
        }

        blocks += SafetyBlockReason.EXECUTION_DISABLED
        reasons += "Execution remains disabled; safety-gate evaluation is observation-only."
        return SafetyGateResult(false, reasons.distinct(), blocks.toList())
    }
}
