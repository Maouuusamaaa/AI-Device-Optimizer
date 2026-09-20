package com.maouuusama.ai.device.optimizer.policy

/**
 * Deterministic temporal holdout assessment for historical measurement data.
 *
 * This is deliberately not a model evaluator. It partitions history without shuffling and reports
 * whether a future offline experiment has enough train/holdout observations. It never ranks
 * actions, estimates causal effects, selects a policy, or authorizes execution.
 */
data class OfflineEvaluationPartition(
    val trainingCount: Int,
    val holdoutCount: Int,
    val minimumHoldoutCount: Int,
    val trainingConditions: Map<String, Int>,
    val holdoutConditions: Map<String, Int>,
    val trainingActions: Map<String, Int>,
    val holdoutActions: Map<String, Int>
) {
    val sufficientHoldout: Boolean
        get() = holdoutCount >= minimumHoldoutCount
}

data class ControlledOfflineEvaluationReport(
    val generatedAtMs: Long,
    val totalObservations: Int,
    val trainingFraction: Double,
    val partition: OfflineEvaluationPartition,
    val evaluationReady: Boolean,
    val interpretation: String
) {
    init {
        require(generatedAtMs >= 0L)
        require(totalObservations >= 0)
        require(trainingFraction in 0.5..0.95)
        require(partition.trainingCount + partition.holdoutCount == totalObservations)
        require(partition.minimumHoldoutCount > 0)
        require(!evaluationReady) {
            "This milestone must not authorize or enable learned policy evaluation."
        }
    }

    fun toJson(): String = buildString {
        append("{")
        append("\"generatedAtMs\":").append(generatedAtMs).append(",")
        append("\"totalObservations\":").append(totalObservations).append(",")
        append("\"trainingFraction\":").append(trainingFraction).append(",")
        append("\"trainingCount\":").append(partition.trainingCount).append(",")
        append("\"holdoutCount\":").append(partition.holdoutCount).append(",")
        append("\"minimumHoldoutCount\":").append(partition.minimumHoldoutCount).append(",")
        append("\"sufficientHoldout\":").append(partition.sufficientHoldout).append(",")
        append("\"evaluationReady\":false,")
        append("\"interpretation\":").append(jsonString(interpretation))
        append("}")
    }

    private fun jsonString(value: String): String =
        "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""
}

class ControlledOfflineEvaluation(
    private val trainingFraction: Double = 0.8,
    private val minimumHoldoutCount: Int = 2
) {
    init {
        require(trainingFraction in 0.5..0.95)
        require(minimumHoldoutCount > 0)
    }

    fun evaluate(
        entries: List<DecisionHistoryEntry>,
        generatedAtMs: Long
    ): ControlledOfflineEvaluationReport {
        require(generatedAtMs >= 0L)
        require(entries.zipWithNext().all { (a, b) -> a.timestampMs <= b.timestampMs }) {
            "History must be ordered chronologically for temporal holdout evaluation."
        }
        require(entries.all { entry ->
            entry.measurementReports.all { it.interpretation.startsWith("descriptive_only") }
        }) {
            "Only descriptive measurement reports can enter offline evaluation."
        }

        val splitIndex = (entries.size * trainingFraction).toInt().coerceIn(0, entries.size)
        val training = entries.take(splitIndex)
        val holdout = entries.drop(splitIndex)

        fun conditions(source: List<DecisionHistoryEntry>): Map<String, Int> =
            source.flatMap { it.conditionIds }.groupingBy { it }.eachCount().toSortedMap()

        fun actions(source: List<DecisionHistoryEntry>): Map<String, Int> =
            source.flatMap { it.measurementReports }.groupingBy { it.actionId }.eachCount().toSortedMap()

        val partition = OfflineEvaluationPartition(
            trainingCount = training.size,
            holdoutCount = holdout.size,
            minimumHoldoutCount = minimumHoldoutCount,
            trainingConditions = conditions(training),
            holdoutConditions = conditions(holdout),
            trainingActions = actions(training),
            holdoutActions = actions(holdout)
        )

        return ControlledOfflineEvaluationReport(
            generatedAtMs = generatedAtMs,
            totalObservations = entries.size,
            trainingFraction = trainingFraction,
            partition = partition,
            evaluationReady = false,
            interpretation = "descriptive_only: temporal train/holdout partitioning checks data sufficiency only; no model, effectiveness estimate, ranking, policy selection, or action authorization is produced."
        )
    }
}
