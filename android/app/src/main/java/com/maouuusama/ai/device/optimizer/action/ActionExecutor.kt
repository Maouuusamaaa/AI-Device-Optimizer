package com.maouuusama.ai.device.optimizer.action

import com.maouuusama.ai.device.optimizer.policy.DryRunPolicyProposal
import com.maouuusama.ai.device.optimizer.policy.DryRunSafetyGate

class ActionExecutor(private val safetyGate: DryRunSafetyGate = DryRunSafetyGate()) {
    fun execute(proposal: DryRunPolicyProposal): List<ActionExecutionResult> {
        val gate = safetyGate.evaluate(proposal)
        return proposal.decisions.mapNotNull { decision ->
            decision.proposedActionId?.let { actionId ->
                ActionExecutionResult(
                    actionId = actionId,
                    status = if (gate.allowed) ActionExecutionStatus.NOT_IMPLEMENTED else ActionExecutionStatus.DISABLED,
                    changedDeviceState = false,
                    reason = gate.reasons.joinToString("; ")
                )
            }
        }
    }
}
