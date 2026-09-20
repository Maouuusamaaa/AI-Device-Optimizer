package com.maouuusama.ai.device.optimizer.policy

/**
 * Immutable, observation-only result of evaluating the local policy engine.
 *
 * This type intentionally contains no executable action. A proposal may describe
 * what an action engine could consider later, but this milestone never mutates
 * device state.
 */
data class DryRunPolicyProposal(
    val state: DeviceState,
    val decisions: List<PolicyDecision>,
    val actionExecutionAllowed: Boolean = false
) {
    init {
        require(decisions.all { it.mode == PolicyMode.DRY_RUN }) {
            "Dry-run proposals may only contain DRY_RUN decisions."
        }
        require(!actionExecutionAllowed) {
            "Action execution is disabled for the measurement-only milestone."
        }
    }

    val hasActionProposal: Boolean
        get() = decisions.any { it.proposedActionId != null }

    companion object {
        fun from(
            state: DeviceState,
            policyEngine: LocalPolicyEngine = LocalPolicyEngine()
        ): DryRunPolicyProposal =
            DryRunPolicyProposal(
                state = state,
                decisions = policyEngine.evaluate(state)
            )
    }
}
