package com.maouuusama.ai.device.optimizer.policy

import com.maouuusama.ai.device.optimizer.monitor.DeviceSnapshot
import com.maouuusama.ai.device.optimizer.monitor.ProcessSnapshot
import com.maouuusama.ai.device.optimizer.monitor.SystemTelemetrySnapshot

data class DeviceState(
    val availableRamMb: Long,
    val totalRamMb: Long,
    val batteryPercent: Int?,
    val isCharging: Boolean,
    val isGaming: Boolean,
    val batteryTemperatureC: Double? = null,
    val thermalStatus: Int? = null,
    val storageTotalBytes: Long? = null,
    val storageFreeBytes: Long? = null,
    val networkTransport: String? = null,
    val networkValidated: Boolean? = null,
    val isInteractive: Boolean? = null,
    val uptimeMs: Long? = null,
    val processes: List<ProcessSnapshot> = emptyList(),
    val systemTelemetry: SystemTelemetrySnapshot? = null
) {
    val availableRamRatio: Double
        get() = if (totalRamMb > 0) availableRamMb.toDouble() / totalRamMb else 0.0

    val storageFreeRatio: Double?
        get() = if (storageTotalBytes != null && storageFreeBytes != null && storageTotalBytes > 0) {
            storageFreeBytes.toDouble() / storageTotalBytes
        } else {
            null
        }

    companion object {
        fun fromSnapshot(snapshot: DeviceSnapshot, isGaming: Boolean = false): DeviceState =
            DeviceState(
                availableRamMb = snapshot.availableRamMb,
                totalRamMb = snapshot.totalRamMb,
                batteryPercent = snapshot.batteryPercent,
                isCharging = snapshot.isCharging,
                isGaming = isGaming,
                batteryTemperatureC = snapshot.batteryTemperatureC,
                thermalStatus = snapshot.thermalStatus,
                storageTotalBytes = snapshot.storageTotalBytes,
                storageFreeBytes = snapshot.storageFreeBytes,
                networkTransport = snapshot.networkTransport,
                networkValidated = snapshot.networkValidated,
                isInteractive = snapshot.isInteractive,
                uptimeMs = snapshot.uptimeMs,
                processes = snapshot.processes,
                systemTelemetry = snapshot.systemTelemetry
            )
    }
}
