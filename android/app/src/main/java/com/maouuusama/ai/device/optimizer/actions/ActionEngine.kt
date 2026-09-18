package com.maouuusama.ai.device.optimizer.actions

import com.maouuusama.ai.device.optimizer.policy.PolicyDecision

class ActionEngine(
    private val allowlist: Set<ActionDefinition> = DEFAULT_ALLOWLIST
) {
    fun propose(decision: PolicyDecision): ActionProposal? {
        val actionId = decision.proposedActionId ?: return null
        val definition = allowlist.firstOrNull { it.actionId == actionId }

        if (definition == null || !definition.enabled) {
            return ActionProposal(
                actionId = actionId,
                status = ProposalStatus.BLOCKED,
                reason = "Action is not enabled in the local allowlist."
            )
        }

        return ActionProposal(
            actionId = actionId,
            status = ProposalStatus.PROPOSED,
            reason = "Dry-run only: action is approved for proposal but is not executed."
        )
    }

    companion object {
        val DEFAULT_ALLOWLIST = setOf(
            ActionDefinition(
                actionId = "observe.background_pressure",
                risk = ActionRisk.LOW
            ),
            ActionDefinition(
                actionId = "observe.power_pressure",
                risk = ActionRisk.LOW
            )
        )
    }
}