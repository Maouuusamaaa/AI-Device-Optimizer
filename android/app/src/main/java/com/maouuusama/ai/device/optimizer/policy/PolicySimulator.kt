package com.maouuusama.ai.device.optimizer.policy

data class PolicySimulationResult(
    val diagnoses: List<Diagnosis>,
    val decisions: List<PolicyDecision>,
    val executionAllowed: Boolean = false
) {
    init {
        require(!executionAllowed) { "Policy simulation cannot authorize action execution." }
        require(decisions.all { it.mode == PolicyMode.DRY_RUN }) { "Policy simulation must produce DRY_RUN decisions." }
    }
    val proposedActionIds: List<String>
        get() = decisions.mapNotNull { it.proposedActionId }.distinct()
}

class PolicySimulator {
    fun simulate(diagnoses: List<Diagnosis>): PolicySimulationResult {
        require(diagnoses.isNotEmpty()) { "At least one diagnosis is required." }
        val decisions = diagnoses.map { diagnosis ->
            PolicyDecision(
                policyId = diagnosis.conditionId,
                severity = diagnosis.severity.toPolicySeverity(),
                reason = "Diagnosis " + diagnosis.conditionId + "; confidence=" + diagnosis.confidence + "; evidence=" + diagnosis.evidence.joinToString(", "),
                proposedActionId = diagnosis.proposedActionId,
                mode = PolicyMode.DRY_RUN
            )
        }
        return PolicySimulationResult(diagnoses = diagnoses, decisions = decisions)
    }
    private fun DiagnosisSeverity.toPolicySeverity(): PolicySeverity = when (this) {
        DiagnosisSeverity.INFO -> PolicySeverity.INFO
        DiagnosisSeverity.ADVISORY -> PolicySeverity.ADVISORY
        DiagnosisSeverity.HIGH -> PolicySeverity.HIGH
    }
}