package com.maouuusama.ai.device.optimizer.policy

data class MeasurementDelta(
    val metric: String,
    val before: Double,
    val after: Double,
    val absoluteDelta: Double,
    val percentDelta: Double?
)

data class PostActionMeasurementReport(
    val actionId: String,
    val status: ActionSimulationStatus,
    val deltas: List<MeasurementDelta>,
    val interpretation: String
)

class PostActionMeasurementEvaluator {
    fun evaluate(
        simulation: ActionSimulation,
        before: DeviceState,
        after: DeviceState
    ): PostActionMeasurementReport {
        require(simulation.actionId.isNotBlank()) { "Action id must not be blank." }
        require(before.totalRamMb >= 0L && after.totalRamMb >= 0L) { "RAM values must be non-negative." }

        val deltas = listOf(
            delta("availableRamMb", before.availableRamMb.toDouble(), after.availableRamMb.toDouble()),
            deltaNullable("batteryPercent", before.batteryPercent?.toDouble(), after.batteryPercent?.toDouble()),
            deltaNullable("batteryTemperatureC", before.batteryTemperatureC, after.batteryTemperatureC),
            deltaNullable("storageFreeBytes", before.storageFreeBytes?.toDouble(), after.storageFreeBytes?.toDouble())
        ).filterNotNull()

        return PostActionMeasurementReport(
            actionId = simulation.actionId,
            status = simulation.status,
            deltas = deltas,
            interpretation = "descriptive_only: this action was simulated and no device mutation was performed; deltas cannot be attributed to the action."
        )
    }

    private fun delta(metric: String, before: Double, after: Double): MeasurementDelta =
        MeasurementDelta(metric, before, after, after - before, percentDelta(before, after))

    private fun deltaNullable(metric: String, before: Double?, after: Double?): MeasurementDelta? =
        if (before != null && after != null) delta(metric, before, after) else null

    private fun percentDelta(before: Double, after: Double): Double? =
        if (before == 0.0) null else ((after - before) / before) * 100.0
}
