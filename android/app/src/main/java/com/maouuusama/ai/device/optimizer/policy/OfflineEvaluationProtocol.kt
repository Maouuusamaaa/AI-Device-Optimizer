package com.maouuusama.ai.device.optimizer.policy

enum class OfflineEvaluationReadiness {
    NOT_READY,
    READY_FOR_DESCRIPTIVE_REVIEW
}

data class OfflineEvaluationProtocolReport(
    val totalObservations: Int,
    val trainingObservations: Int,
    val holdoutObservations: Int,
    val leakageDetected: Boolean,
    val incompleteMetadataCount: Int,
    val trainingOutcomeCount: Int,
    val holdoutOutcomeCount: Int,
    val conditionCoverage: Map<String, Int>,
    val actionCoverage: Map<String, Int>,
    val readiness: OfflineEvaluationReadiness,
    val policySelectionAllowed: Boolean,
    val executionAllowed: Boolean,
    val interpretation: String
) {
    init {
        require(totalObservations >= 0)
        require(trainingObservations >= 0)
        require(holdoutObservations >= 0)
        require(trainingObservations + holdoutObservations == totalObservations)
        require(incompleteMetadataCount >= 0)
        require(trainingOutcomeCount >= 0)
        require(holdoutOutcomeCount >= 0)
        require(!policySelectionAllowed)
        require(!executionAllowed)
    }
}

class OfflineEvaluationProtocol {
    fun assess(
        split: OfflineEvaluationSplit,
        integrity: ReproducibleOfflineEvaluationReport
    ): OfflineEvaluationProtocolReport {
        require(split.training.size == integrity.trainingCount)
        require(split.holdout.size == integrity.holdoutCount)

        val all = split.training + split.holdout
        val coverage = all.flatMap { it.conditionIds }.groupingBy { it }.eachCount().toSortedMap()
        val actions = all.flatMap { it.actionIds }.groupingBy { it }.eachCount().toSortedMap()
        val incomplete = all.count { !it.metadata.complete }

        val descriptiveReady =
            !integrity.leakageDetected &&
            incomplete == 0 &&
            split.training.isNotEmpty() &&
            split.holdout.isNotEmpty()

        return OfflineEvaluationProtocolReport(
            totalObservations = all.size,
            trainingObservations = split.training.size,
            holdoutObservations = split.holdout.size,
            leakageDetected = integrity.leakageDetected,
            incompleteMetadataCount = incomplete,
            trainingOutcomeCount = split.training.count { it.hasOutcome },
            holdoutOutcomeCount = split.holdout.count { it.hasOutcome },
            conditionCoverage = coverage,
            actionCoverage = actions,
            readiness = if (descriptiveReady) OfflineEvaluationReadiness.READY_FOR_DESCRIPTIVE_REVIEW
                else OfflineEvaluationReadiness.NOT_READY,
            policySelectionAllowed = false,
            executionAllowed = false,
            interpretation = "descriptive_only: protocol checks dataset integrity, metadata completeness, and coverage; it does not estimate action effectiveness, infer causality, rank policies, select a policy, or authorize execution."
        )
    }
}
