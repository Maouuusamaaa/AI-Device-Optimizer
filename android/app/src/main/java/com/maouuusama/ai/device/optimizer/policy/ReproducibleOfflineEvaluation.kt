package com.maouuusama.ai.device.optimizer.policy

data class ReproducibleEvaluationRecord(
    val observationId: String,
    val timestampMs: Long,
    val conditionIds: List<String>,
    val actionIds: List<String>,
    val metadata: OfflineEvaluationMetadata,
    val outcomeLabel: String? = null
) {
    init {
        require(observationId.isNotBlank())
        require(timestampMs >= 0L)
        require(conditionIds.distinct().size == conditionIds.size)
        require(actionIds.distinct().size == actionIds.size)
    }

    val hasOutcome: Boolean
        get() = !outcomeLabel.isNullOrBlank()
}

data class OfflineEvaluationSplit(
    val training: List<ReproducibleEvaluationRecord>,
    val holdout: List<ReproducibleEvaluationRecord>
) {
    init {
        require(training.map { it.observationId }.intersect(holdout.map { it.observationId }.toSet()).isEmpty())
    }
}

data class ReproducibleOfflineEvaluationReport(
    val totalCount: Int,
    val trainingCount: Int,
    val holdoutCount: Int,
    val duplicateObservationIds: List<String>,
    val duplicateTimestamps: List<Long>,
    val metadataIncompleteCount: Int,
    val outcomeCount: Int,
    val deterministicFingerprint: String,
    val leakageDetected: Boolean,
    val evaluationReady: Boolean,
    val executionAllowed: Boolean,
    val interpretation: String
) {
    init {
        require(totalCount >= 0)
        require(trainingCount >= 0)
        require(holdoutCount >= 0)
        require(trainingCount + holdoutCount == totalCount)
        require(metadataIncompleteCount >= 0)
        require(outcomeCount >= 0)
        require(deterministicFingerprint.isNotBlank())
        require(!evaluationReady)
        require(!executionAllowed)
    }
}

class ReproducibleOfflineEvaluator(
    private val trainingFraction: Double = 0.8
) {
    init {
        require(trainingFraction in 0.5..0.95)
    }

    fun validateAndSplit(records: List<ReproducibleEvaluationRecord>): Pair<OfflineEvaluationSplit, ReproducibleOfflineEvaluationReport> {
        require(records.zipWithNext().all { (a, b) -> a.timestampMs <= b.timestampMs }) {
            "records must be chronological"
        }

        val duplicateIds = records.groupingBy { it.observationId }
            .eachCount()
            .filterValues { it > 1 }
            .keys
            .sorted()

        val duplicateTimes = records.groupingBy { it.timestampMs }
            .eachCount()
            .filterValues { it > 1 }
            .keys
            .sorted()

        val splitIndex = (records.size * trainingFraction).toInt().coerceIn(0, records.size)
        val training = records.take(splitIndex)
        val holdout = records.drop(splitIndex)
        val overlapIds = training.map { it.observationId }.toSet()
            .intersect(holdout.map { it.observationId }.toSet())

        val report = ReproducibleOfflineEvaluationReport(
            totalCount = records.size,
            trainingCount = training.size,
            holdoutCount = holdout.size,
            duplicateObservationIds = duplicateIds,
            duplicateTimestamps = duplicateTimes,
            metadataIncompleteCount = records.count { !it.metadata.complete },
            outcomeCount = records.count { it.hasOutcome },
            deterministicFingerprint = fingerprint(records),
            leakageDetected = duplicateIds.isNotEmpty() || duplicateTimes.isNotEmpty() || overlapIds.isNotEmpty(),
            evaluationReady = false,
            executionAllowed = false,
            interpretation = "descriptive_only: deterministic partition and dataset integrity are checked; no effectiveness estimate, causal inference, model training, policy selection, ranking, or action authorization is produced."
        )
        return OfflineEvaluationSplit(training, holdout) to report
    }

    private fun fingerprint(records: List<ReproducibleEvaluationRecord>): String {
        val canonical = records.joinToString("\n") { record ->
            listOf(
                record.observationId,
                record.timestampMs,
                record.conditionIds.sorted().joinToString(","),
                record.actionIds.sorted().joinToString(","),
                record.metadata.charging,
                record.metadata.batteryTemperatureC,
                record.metadata.thermalStatus,
                record.metadata.networkTransport,
                record.metadata.networkValidated,
                record.metadata.interactive,
                record.metadata.workload,
                record.outcomeLabel
            ).joinToString("|")
        }
        return canonical.fold(7L) { hash, value ->
            (hash * 31L + value.code).let { it and Long.MAX_VALUE }
        }.toString(16)
    }
}
