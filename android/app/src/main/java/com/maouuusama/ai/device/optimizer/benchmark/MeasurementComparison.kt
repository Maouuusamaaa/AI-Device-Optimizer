package com.maouuusama.ai.device.optimizer.benchmark

data class MetricComparison(val metric: String, val baselineMean: Double, val experimentMean: Double, val absoluteDelta: Double, val percentDelta: Double?)
data class MeasurementComparison(val baselineCount: Int, val experimentCount: Int, val metrics: List<MetricComparison>)

object ControlledMeasurementEvaluator {
    fun compare(baseline: List<MeasurementSample>, experiment: List<MeasurementSample>): MeasurementComparison {
        require(baseline.isNotEmpty()) { "At least one baseline sample is required." }
        require(experiment.isNotEmpty()) { "At least one experiment sample is required." }
        val metrics = listOf(
            "startupMs" to { s: MeasurementSample -> s.startupMs },
            "availableRamMb" to { s: MeasurementSample -> s.availableRamMb },
            "cpuUtilizationPercent" to { s: MeasurementSample -> s.cpuUtilizationPercent },
            "temperatureC" to { s: MeasurementSample -> s.temperatureC }
        )
        val comparisons = metrics.mapNotNull { (name, selector) ->
            val b = baseline.mapNotNull(selector)
            val e = experiment.mapNotNull(selector)
            if (b.isEmpty() || e.isEmpty()) return@mapNotNull null
            val bm = b.average(); val em = e.average()
            MetricComparison(name, bm, em, em - bm, if (bm == 0.0) null else ((em - bm) / bm) * 100.0)
        }
        require(comparisons.isNotEmpty()) { "At least one common metric is required." }
        return MeasurementComparison(baseline.size, experiment.size, comparisons)
    }
}
