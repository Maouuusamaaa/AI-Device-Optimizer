package com.maouuusama.ai.device.optimizer.policy

data class DeviceState(
    val availableRamMb: Long,
    val totalRamMb: Long,
    val batteryPercent: Int?,
    val isCharging: Boolean,
    val isGaming: Boolean
) {
    val availableRamRatio: Double
        get() = if (totalRamMb > 0) availableRamMb.toDouble() / totalRamMb else 0.0
}