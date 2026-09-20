package com.maouuusama.ai.device.optimizer.benchmark

data class MeasurementSample(
    val timestampMs: Long,
    val phase: MeasurementPhase,
    val startupMs: Double? = null,
    val availableRamMb: Double? = null,
    val cpuUtilizationPercent: Double? = null,
    val temperatureC: Double? = null
) {
    init {
        require(timestampMs >= 0) { "timestampMs must be non-negative." }
        listOfNotNull(startupMs, availableRamMb, cpuUtilizationPercent, temperatureC).forEach {
            require(it.isFinite() && it >= 0.0) { "Measurement values must be finite and non-negative." }
        }
        cpuUtilizationPercent?.let { require(it <= 100.0) { "CPU utilization must be between 0 and 100." } }
    }
}
