package com.maouuusama.ai.device.optimizer.monitor

data class DeviceSnapshot(
    val timestampMs: Long,
    val androidApi: Int,
    val manufacturer: String,
    val model: String,
    val totalRamMb: Long,
    val availableRamMb: Long,
    val batteryPercent: Int?,
    val isCharging: Boolean,
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
)
