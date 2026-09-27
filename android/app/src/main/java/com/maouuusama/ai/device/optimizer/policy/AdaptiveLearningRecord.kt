package com.maouuusama.ai.device.optimizer.policy

import java.security.MessageDigest

data class AdaptiveLearningRecord(
    val schemaVersion: Int = SCHEMA_VERSION,
    val featureExtractorVersion: Int = FEATURE_EXTRACTOR_VERSION,
    val learnerVersion: Int = LEARNER_VERSION,
    val evidenceId: String,
    val manufacturer: String,
    val model: String,
    val androidApi: Int,
    val workload: String,
    val policyId: String?,
    val classification: Classification,
    val features: Map<String, Double>,
    val provenance: Map<String, String>,
    val learningDisposition: LearningDisposition
) {
    init {
        require(schemaVersion == SCHEMA_VERSION)
        require(featureExtractorVersion == FEATURE_EXTRACTOR_VERSION)
        require(learnerVersion == LEARNER_VERSION)
        require(evidenceId.isNotBlank())
        require(manufacturer.isNotBlank())
        require(model.isNotBlank())
        require(androidApi > 0)
        require(workload.isNotBlank())
        require(classification != Classification.INVALID_EVIDENCE)
        require(provenance.isNotEmpty())
        require(features.values.all { it.isFinite() })
        if (classification == Classification.INSUFFICIENT_EVIDENCE) {
            require(learningDisposition == LearningDisposition.ABSTAIN)
        } else {
            require(learningDisposition == LearningDisposition.LEARN)
        }
    }

    enum class Classification { NO_REGRESSION, REGRESSION, MIXED, INSUFFICIENT_EVIDENCE, INVALID_EVIDENCE }
    enum class LearningDisposition { LEARN, ABSTAIN }

    val patternKey: String
        get() = listOf(manufacturer, model, androidApi, workload, policyId ?: "").joinToString("\u001f")

    val recordFingerprint: String
        get() = sha256(
            listOf(
                schemaVersion, featureExtractorVersion, learnerVersion, evidenceId,
                manufacturer, model, androidApi, workload, policyId ?: "",
                classification.name, learningDisposition.name,
                features.toSortedMap().entries.joinToString(",") { "${it.key}=${it.value}" },
                provenance.toSortedMap().entries.joinToString(",") { "${it.key}=${it.value}" }
            ).joinToString("|")
        )

    companion object {
        const val SCHEMA_VERSION = 1
        const val FEATURE_EXTRACTOR_VERSION = 1
        const val LEARNER_VERSION = 1
        const val MINIMUM_SAMPLE_COUNT = 3

        fun fromHistory(
            entry: DecisionHistoryEntry,
            manufacturer: String,
            model: String,
            androidApi: Int,
            workload: String = "background_monitoring"
        ): List<AdaptiveLearningRecord> {
            require(manufacturer.isNotBlank() && model.isNotBlank() && androidApi > 0)
            return entry.measurementReports.map { report ->
                val policyId = entry.decisions.firstOrNull { it.proposedActionId == report.actionId }?.policyId
                val features = report.deltas.mapNotNull { delta ->
                    when (delta.metric) {
                        "availableRamMb" -> "availableRamMbDelta" to delta.absoluteDelta
                        "batteryPercent" -> "batteryPercentDelta" to delta.absoluteDelta
                        "batteryTemperatureC" -> "batteryTemperatureCDelta" to delta.absoluteDelta
                        "storageFreeBytes" -> "storageFreeBytesDelta" to delta.absoluteDelta
                        else -> null
                    }
                }.toMap()
                AdaptiveLearningRecord(
                    evidenceId = sha256("${entry.timestampMs}|${report.actionId}|${report.status.name}|${policyId ?: ""}"),
                    manufacturer = manufacturer,
                    model = model,
                    androidApi = androidApi,
                    workload = workload,
                    policyId = policyId,
                    classification = Classification.INSUFFICIENT_EVIDENCE,
                    features = features,
                    provenance = mapOf(
                        "source" to "decision_history",
                        "timestampMs" to entry.timestampMs.toString(),
                        "actionId" to report.actionId,
                        "analysisId" to "${entry.timestampMs}:${report.actionId}"
                    ),
                    learningDisposition = LearningDisposition.ABSTAIN
                )
            }
        }

        fun sha256(value: String): String =
            MessageDigest.getInstance("SHA-256")
                .digest(value.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
    }
}
