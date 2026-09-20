package com.maouuusama.ai.device.optimizer.policy

data class ActionLearningStats(
    val actionId: String,
    val observationCount: Int,
    val averageRamDeltaMb: Double?,
    val averageBatteryDeltaPercent: Double?
)

data class AdaptiveLearningSummary(
    val observationCount: Int,
    val conditionCounts: Map<String, Int>,
    val actionStats: List<ActionLearningStats>,
    val interpretation: String
)

/**
 * Aggregates historical observations without turning them into autonomous optimization rules.
 *
 * No action is selected, enabled, scored, or executed here. The output is an empirical summary
 * that can be reviewed before any future policy-learning milestone.
 */
class AdaptiveLearningSummarizer {
    fun summarize(entries: List<DecisionHistoryEntry>): AdaptiveLearningSummary {
        require(entries.all { it.measurementReports.all { report -> report.interpretation.startsWith("descriptive_only") } }) {
            "Only descriptive measurement reports can be summarized."
        }

        val conditionCounts = entries
            .flatMap { it.conditionIds }
            .groupingBy { it }
            .eachCount()
            .toSortedMap()

        val reportsByAction = entries
            .flatMap { it.measurementReports }
            .groupBy { it.actionId }

        val actionStats = reportsByAction
            .toSortedMap()
            .map { (actionId, reports) ->
                ActionLearningStats(
                    actionId = actionId,
                    observationCount = reports.size,
                    averageRamDeltaMb = averageMetric(reports, "availableRamMb"),
                    averageBatteryDeltaPercent = averageMetric(reports, "batteryPercent")
                )
            }

        return AdaptiveLearningSummary(
            observationCount = entries.size,
            conditionCounts = conditionCounts,
            actionStats = actionStats,
            interpretation = "descriptive_only: historical observations are summarized for review; no learned policy, ranking, causal claim, or action authorization is produced."
        )
    }

    private fun averageMetric(
        reports: List<PostActionMeasurementReport>,
        metric: String
    ): Double? {
        val values = reports.flatMap { report ->
            report.deltas.filter { it.metric == metric }.map { it.absoluteDelta }
        }
        return if (values.isEmpty()) null else values.average()
    }
}
