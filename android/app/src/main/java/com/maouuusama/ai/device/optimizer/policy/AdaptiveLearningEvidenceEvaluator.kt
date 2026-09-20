package com.maouuusama.ai.device.optimizer.policy

/**
 * Measurement-only quality assessment for historical adaptive-learning observations.
 *
 * This evaluator answers whether stored descriptive evidence is structurally sufficient for a
 * future offline learning experiment. It never selects, ranks, enables, or authorizes an action.
 */
data class AdaptiveLearningEvidence(
    val actionId: String,
    val observationCount: Int,
    val completeRamDeltaCount: Int,
    val completeBatteryDeltaCount: Int,
    val descriptiveOnly: Boolean,
    val sufficientSampleSize: Boolean
)

data class AdaptiveLearningEvidenceReport(
    val generatedAtMs: Long,
    val totalObservations: Int,
    val minimumSamplesPerAction: Int,
    val evidence: List<AdaptiveLearningEvidence>,
    val futureOfflineEvaluationReady: Boolean,
    val interpretation: String
) {
    init {
        require(generatedAtMs >= 0L)
        require(totalObservations >= 0)
        require(minimumSamplesPerAction > 0)
        require(evidence.all { it.observationCount >= 0 })
        require(evidence.all { it.completeRamDeltaCount in 0..it.observationCount })
        require(evidence.all { it.completeBatteryDeltaCount in 0..it.observationCount })
        require(evidence.all { it.descriptiveOnly })
        require(!futureOfflineEvaluationReady) {
            "This milestone must not authorize or enable learned policy evaluation."
        }
    }

    fun toJson(): String = buildString {
        append("{")
        append("\"generatedAtMs\":").append(generatedAtMs).append(",")
        append("\"totalObservations\":").append(totalObservations).append(",")
        append("\"minimumSamplesPerAction\":").append(minimumSamplesPerAction).append(",")
        append("\"futureOfflineEvaluationReady\":false,")
        append("\"evidence\":[")
        evidence.forEachIndexed { index, item ->
            if (index > 0) append(",")
            append("{")
            append("\"actionId\":").append(jsonString(item.actionId)).append(",")
            append("\"observationCount\":").append(item.observationCount).append(",")
            append("\"completeRamDeltaCount\":").append(item.completeRamDeltaCount).append(",")
            append("\"completeBatteryDeltaCount\":").append(item.completeBatteryDeltaCount).append(",")
            append("\"descriptiveOnly\":").append(item.descriptiveOnly).append(",")
            append("\"sufficientSampleSize\":").append(item.sufficientSampleSize)
            append("}")
        }
        append("],")
        append("\"interpretation\":").append(jsonString(interpretation))
        append("}")
    }

    private fun jsonString(value: String): String =
        "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""
}

class AdaptiveLearningEvidenceEvaluator(
    private val minimumSamplesPerAction: Int = 10
) {
    init {
        require(minimumSamplesPerAction > 0)
    }

    fun evaluate(
        entries: List<DecisionHistoryEntry>,
        generatedAtMs: Long
    ): AdaptiveLearningEvidenceReport {
        require(generatedAtMs >= 0L)
        require(entries.all { entry ->
            entry.measurementReports.all { it.interpretation.startsWith("descriptive_only") }
        }) {
            "Only descriptive measurement reports can enter evidence evaluation."
        }

        val reportsByAction = entries
            .flatMap { it.measurementReports }
            .groupBy { it.actionId }
            .toSortedMap()

        val evidence = reportsByAction.map { (actionId, reports) ->
            val ramCount = reports.count { report ->
                report.deltas.any { it.metric == "availableRamMb" && it.absoluteDelta.isFinite() }
            }
            val batteryCount = reports.count { report ->
                report.deltas.any { it.metric == "batteryPercent" && it.absoluteDelta.isFinite() }
            }
            AdaptiveLearningEvidence(
                actionId = actionId,
                observationCount = reports.size,
                completeRamDeltaCount = ramCount,
                completeBatteryDeltaCount = batteryCount,
                descriptiveOnly = true,
                sufficientSampleSize = reports.size >= minimumSamplesPerAction
            )
        }

        return AdaptiveLearningEvidenceReport(
            generatedAtMs = generatedAtMs,
            totalObservations = entries.size,
            minimumSamplesPerAction = minimumSamplesPerAction,
            evidence = evidence,
            futureOfflineEvaluationReady = false,
            interpretation = "descriptive_only: this report assesses data completeness and sample counts; it does not estimate action effectiveness, rank actions, select policies, or authorize execution."
        )
    }
}
