package com.maouuusama.ai.device.optimizer.policy

class LocalPolicyEngine(
    private val memoryPressureThresholdMb: Long = 1500L,
    private val memoryCriticalThresholdMb: Long = 1000L,
    private val lowBatteryPercent: Int = 20
) {
    fun evaluate(state: DeviceState): List<PolicyDecision> {
        val decisions = mutableListOf<PolicyDecision>()

        when {
            state.availableRamMb < memoryCriticalThresholdMb -> {
                decisions += PolicyDecision(
                    policyId = "memory.critical",
                    severity = PolicySeverity.HIGH,
                    reason = "Available RAM is below the experimental critical threshold.",
                    proposedActionId = "observe.memory_critical"
                )
            }

            state.availableRamMb <= memoryPressureThresholdMb -> {
                decisions += PolicyDecision(
                    policyId = "memory.pressure",
                    severity = PolicySeverity.ADVISORY,
                    reason = "Available RAM is at or below the experimental pressure threshold.",
                    proposedActionId = "observe.memory_pressure"
                )
            }
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