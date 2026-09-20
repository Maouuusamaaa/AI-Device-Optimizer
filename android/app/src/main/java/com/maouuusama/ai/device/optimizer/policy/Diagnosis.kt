package com.maouuusama.ai.device.optimizer.policy

enum class DiagnosisSeverity { INFO, ADVISORY, HIGH }

data class Diagnosis(
    val conditionId: String,
    val severity: DiagnosisSeverity,
    val confidence: Double,
    val evidence: List<String>,
    val proposedActionId: String? = null
) {
    init {
        require(confidence in 0.0..1.0) { "confidence must be between 0 and 1" }
        require(evidence.isNotEmpty()) { "diagnosis must contain evidence" }
    }
}
