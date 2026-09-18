package com.maouuusama.ai.device.optimizer.policy

import com.maouuusama.ai.device.optimizer.monitor.DeviceSnapshot

data class DeviceState(
    val availableRamMb: Long,
    val totalRamMb: Long,
    val batteryPercent: Int?,
    val isCharging: Boolean,
    val isGaming: Boolean
) {
    val availableRamRatio: Double
        get() = if (totalRamMb > 0) availableRamMb.toDouble() / totalRamMb else 0.0

    companion object {
        fun fromSnapshot(snapshot: DeviceSnapshot, isGaming: Boolean = false): DeviceState =
            DeviceState(
                availableRamMb = snapshot.availableRamMb,
                totalRamMb = snapshot.totalRamMb,
                batteryPercent = snapshot.batteryPercent,
                isCharging = snapshot.isCharging,
                isGaming = isGaming
            )
    }
}
