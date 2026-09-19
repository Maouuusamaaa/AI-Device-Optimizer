package com.maouuusama.ai.device.optimizer.policy

import com.maouuusama.ai.device.optimizer.monitor.DeviceSnapshot
import com.maouuusama.ai.device.optimizer.monitor.ProcessSnapshot

data class DeviceState(
    val availableRamMb: Long,
    val totalRamMb: Long,
    val batteryPercent: Int?,
    val isCharging: Boolean,
    val isGaming: Boolean,
    val processes: List<ProcessSnapshot> = emptyList()
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
                isGaming = isGaming,
                processes = snapshot.processes
            )
    }
}
