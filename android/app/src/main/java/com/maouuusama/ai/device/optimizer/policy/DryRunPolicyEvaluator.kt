package com.maouuusama.ai.device.optimizer.policy

import com.maouuusama.ai.device.optimizer.monitor.DeviceSnapshot

/**
 * Connects a monitor snapshot to the local policy engine without executing actions.
 *
 * This is the measurement-only pipeline boundary:
 * DeviceSnapshot -> DeviceState -> LocalPolicyEngine -> DryRunPolicyProposal.
 */
class DryRunPolicyEvaluator(
    private val policyEngine: LocalPolicyEngine = LocalPolicyEngine()
) {
    fun evaluate(snapshot: DeviceSnapshot, isGaming: Boolean = false): DryRunPolicyProposal {
        val state = DeviceState.fromSnapshot(snapshot, isGaming = isGaming)
        return DryRunPolicyProposal.from(state, policyEngine)
    }
}
