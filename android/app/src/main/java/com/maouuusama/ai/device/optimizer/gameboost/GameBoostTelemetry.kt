package com.maouuusama.ai.device.optimizer.gameboost

import com.maouuusama.ai.device.optimizer.monitor.SystemTelemetrySnapshot
import com.maouuusama.ai.device.optimizer.monitor.SystemTelemetryStatus

data class GameBoostTelemetry(
    val systemTelemetry: GameBoostCapability,
    val cpu: GameBoostCapability,
    val memory: GameBoostCapability
)

object GameBoostTelemetryAdapter {
    fun from(snapshot: SystemTelemetrySnapshot): GameBoostTelemetry {
        val systemStatus = when (snapshot.status) {
            SystemTelemetryStatus.AVAILABLE -> CapabilityStatus.AVAILABLE
            SystemTelemetryStatus.PERMISSION_REQUIRED -> CapabilityStatus.UNAVAILABLE
            SystemTelemetryStatus.UNAVAILABLE -> CapabilityStatus.UNAVAILABLE
            SystemTelemetryStatus.ERROR -> CapabilityStatus.FAILED
        }

        val systemDetail = snapshot.errorMessage
            ?: when (snapshot.status) {
                SystemTelemetryStatus.AVAILABLE -> "System telemetry is available"
                SystemTelemetryStatus.PERMISSION_REQUIRED -> "Shizuku permission is required"
                SystemTelemetryStatus.UNAVAILABLE -> "No supported telemetry provider is available"
                SystemTelemetryStatus.ERROR -> "System telemetry provider failed"
            }

        fun componentCapability(
            id: String,
            available: Boolean
        ): GameBoostCapability {
            val status = when {
                snapshot.status == SystemTelemetryStatus.ERROR -> CapabilityStatus.FAILED
                snapshot.status != SystemTelemetryStatus.AVAILABLE -> CapabilityStatus.UNAVAILABLE
                available -> CapabilityStatus.AVAILABLE
                else -> CapabilityStatus.UNAVAILABLE
            }

            val detail = when (status) {
                CapabilityStatus.AVAILABLE -> "$id telemetry is available"
                CapabilityStatus.UNAVAILABLE -> "$id telemetry is unavailable"
                CapabilityStatus.FAILED -> "System telemetry provider failed"
                CapabilityStatus.UNKNOWN -> "$id telemetry availability is unknown"
            }

            return GameBoostCapability(
                id = id,
                status = status,
                source = snapshot.provider,
                detail = detail
            )
        }

        return GameBoostTelemetry(
            systemTelemetry = GameBoostCapability(
                id = "telemetry.system",
                status = systemStatus,
                source = snapshot.provider,
                detail = systemDetail
            ),
            cpu = componentCapability("telemetry.cpu", snapshot.cpu != null),
            memory = componentCapability("telemetry.memory", snapshot.memory != null)
        )
    }
}
