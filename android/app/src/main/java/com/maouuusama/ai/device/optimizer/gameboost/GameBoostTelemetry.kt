package com.maouuusama.ai.device.optimizer.gameboost

import com.maouuusama.ai.device.optimizer.monitor.SystemTelemetrySnapshot
import com.maouuusama.ai.device.optimizer.monitor.SystemTelemetryStatus

data class GameBoostTelemetry(
    val systemTelemetry: GameBoostCapability
) {
    companion object {
        fun from(snapshot: SystemTelemetrySnapshot): GameBoostTelemetry {
            val status = when (snapshot.status) {
                SystemTelemetryStatus.AVAILABLE -> CapabilityStatus.AVAILABLE
                SystemTelemetryStatus.PERMISSION_REQUIRED -> CapabilityStatus.UNAVAILABLE
                SystemTelemetryStatus.UNAVAILABLE -> CapabilityStatus.UNAVAILABLE
                SystemTelemetryStatus.ERROR -> CapabilityStatus.FAILED
            }

            val detail = snapshot.errorMessage
                ?: when (snapshot.status) {
                    SystemTelemetryStatus.AVAILABLE -> "System telemetry is available"
                    SystemTelemetryStatus.PERMISSION_REQUIRED -> "Shizuku permission is required"
                    SystemTelemetryStatus.UNAVAILABLE -> "No supported telemetry provider is available"
                    SystemTelemetryStatus.ERROR -> "System telemetry provider failed"
                }

            return GameBoostTelemetry(
                systemTelemetry = GameBoostCapability(
                    id = "telemetry.system",
                    status = status,
                    source = snapshot.provider,
                    detail = detail
                )
            )
        }
    }
}
