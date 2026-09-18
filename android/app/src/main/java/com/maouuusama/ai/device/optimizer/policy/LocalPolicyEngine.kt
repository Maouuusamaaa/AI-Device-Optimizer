package com.maouuusama.ai.device.optimizer.policy

class LocalPolicyEngine(
    private val lowRamRatio: Double = 0.15,
    private val lowBatteryPercent: Int = 20
) {
    fun evaluate(state: DeviceState): List<PolicyDecision> {
        val decisions = mutableListOf<PolicyDecision>()

        if (state.availableRamRatio <= lowRamRatio) {
            decisions += PolicyDecision(
                policyId = "memory.low",
                severity = PolicySeverity.HIGH,
                reason = "Available RAM is at or below the configured threshold.",
                proposedActionId = "observe.background_pressure"
            )
        }

        if (state.batteryPercent != null &&
            state.batteryPercent <= lowBatteryPercent &&
            !state.isCharging
        ) {
            decisions += PolicyDecision(
                policyId = "battery.low",
                severity = PolicySeverity.ADVISORY,
                reason = "Battery is low and the device is not charging.",
                proposedActionId = "observe.power_pressure"
            )
        }

        if (state.isGaming) {
            decisions += PolicyDecision(
                policyId = "workload.gaming",
                severity = PolicySeverity.INFO,
                reason = "Gaming is active; avoid background optimization from this condition alone."
            )
        }

        if (decisions.isEmpty()) {
            decisions += PolicyDecision(
                policyId = "device.normal",
                severity = PolicySeverity.INFO,
                reason = "No configured local threshold requires intervention."
            )
        }

        return decisions
    }
}