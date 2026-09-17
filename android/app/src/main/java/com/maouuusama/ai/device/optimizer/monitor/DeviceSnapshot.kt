package com.maouuusama.ai.device.optimizer.monitor

data class DeviceSnapshot(
    val timestampMs: Long,
    val androidApi: Int,
    val manufacturer: String,
    val model: String,
    val totalRamMb: Long,
    val availableRamMb: Long,
    val batteryPercent: Int?,
    val isCharging: Boolean
)