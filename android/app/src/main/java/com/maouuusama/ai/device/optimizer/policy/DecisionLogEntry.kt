package com.maouuusama.ai.device.optimizer.policy

data class DecisionLogEntry(
    val timestampMs: Long,
    val availableRamMb: Long,
    val totalRamMb: Long,
    val batteryPercent: Int?,
    val isCharging: Boolean,
    val isGaming: Boolean,
    val policyIds: List<String>,
    val proposedActionIds: List<String>,
    val policyModes: List<String>,
    val safetyGateAllowed: Boolean,
    val safetyGateReasons: List<String>,
    val actionExecutionAllowed: Boolean
)
