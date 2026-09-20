package com.maouuusama.ai.device.optimizer.policy

import com.maouuusama.ai.device.optimizer.monitor.DeviceSnapshot

object OfflineEvaluationMetadataFactory {
    fun fromSnapshot(snapshot: DeviceSnapshot, workload: String? = null): OfflineEvaluationMetadata =
        OfflineEvaluationMetadata(
            charging = snapshot.isCharging,
            batteryTemperatureC = snapshot.batteryTemperatureC,
            thermalStatus = snapshot.thermalStatus,
            networkTransport = snapshot.networkTransport,
            networkValidated = snapshot.networkValidated,
            interactive = snapshot.isInteractive,
            workload = workload
        )
}
