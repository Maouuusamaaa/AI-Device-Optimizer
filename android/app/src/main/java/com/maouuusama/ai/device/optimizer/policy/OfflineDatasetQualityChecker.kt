package com.maouuusama.ai.device.optimizer.policy

/**
 * Quality checks for a chronological offline-evaluation dataset.
 *
 * These checks identify structural leakage risks and missing experimental metadata. They do not
 * infer causality, estimate effectiveness, rank actions, train a model, or authorize execution.
 */
data class OfflineDatasetQualityReport(
    val observationCount: Int,
    val duplicateTimestampCount: Int,
    val duplicateTimestampRisk: Boolean,
    val actionOverlapCount: Int,
    val actionOverlapRisk: Boolean,
    val actionlessObservationCount: Int,
    val descriptiveOnly: Boolean,
    val confounderMetadataAvailable: Boolean,
    val confounderMetadataMissing: List<String>,
    val leakageDetected: Boolean,
    val evaluationSafeForReview: Boolean,
    val interpretation: String
) {
    init {
        require(observationCount >= 0)
        require(duplicateTimestampCount >= 0)
        require(actionOverlapCount >= 0)
        require(actionlessObservationCount >= 0)
        require(confounderMetadataMissing.all { it.isNotBlank() })
        require(!evaluationSafeForReview) {
            "Dataset quality review cannot authorize evaluation or execution."
        }
    }

    fun toJson(): String = buildString {
        append("{")
        append("\"observationCount\":").append(observationCount).append(",")
        append("\"duplicateTimestampCount\":").append(duplicateTimestampCount).append(",")
        append("\"duplicateTimestampRisk\":").append(duplicateTimestampRisk).append(",")
        append("\"actionOverlapCount\":").append(actionOverlapCount).append(",")
        append("\"actionOverlapRisk\":").append(actionOverlapRisk).append(",")
        append("\"actionlessObservationCount\":").append(actionlessObservationCount).append(",")
        append("\"descriptiveOnly\":").append(descriptiveOnly).append(",")
        append("\"confounderMetadataAvailable\":").append(confounderMetadataAvailable).append(",")
        append("\"confounderMetadataMissing\":[")
        confounderMetadataMissing.forEachIndexed { index, value ->
            if (index > 0) append(",")
            append(jsonString(value))
        }
        append("],")
        append("\"leakageDetected\":").append(leakageDetected).append(",")
        append("\"evaluationSafeForReview\":false,")
        append("\"interpretation\":").append(jsonString(interpretation))
        append("}")
    }

    private fun jsonString(value: String): String =
        "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""
}

class OfflineDatasetQualityChecker {
    fun check(entries: List<DecisionHistoryEntry>): OfflineDatasetQualityReport {
        val duplicateTimestampCount = entries
            .groupingBy { it.timestampMs }
            .eachCount()
            .values
            .sumOf { count -> (count - 1).coerceAtLeast(0) }

        val actionOverlapCount = entries
            .map { entry -> entry.measurementReports.map { it.actionId }.toSet() }
            .windowed(2)
            .count { pair -> pair[0].intersect(pair[1]).isNotEmpty() }

        val actionlessObservationCount = entries.count { it.measurementReports.isEmpty() }
        val descriptiveOnly = entries.all {
            it.measurementReports.all { report ->
                report.interpretation.startsWith("descriptive_only")
            }
        }

        val missing = listOf(
            "controlled_outcome_label",
            "charging_state_at_measurement",
            "thermal_state_at_measurement",
            "network_state_at_measurement",
            "foreground_workload_at_measurement"
        )

        val leakageDetected = duplicateTimestampCount > 0
        return OfflineDatasetQualityReport(
            observationCount = entries.size,
            duplicateTimestampCount = duplicateTimestampCount,
            duplicateTimestampRisk = duplicateTimestampCount > 0,
            actionOverlapCount = actionOverlapCount,
            actionOverlapRisk = actionOverlapCount > 0,
            actionlessObservationCount = actionlessObservationCount,
            descriptiveOnly = descriptiveOnly,
            confounderMetadataAvailable = false,
            confounderMetadataMissing = missing,
            leakageDetected = leakageDetected,
            evaluationSafeForReview = false,
            interpretation = "descriptive_only: structural leakage risks and missing confounder metadata are reported for review; no causal inference, effectiveness estimate, model training, policy selection, or action authorization is produced."
        )
    }
}
