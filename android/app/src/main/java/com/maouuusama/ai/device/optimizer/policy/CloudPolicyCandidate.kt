package com.maouuusama.ai.device.optimizer.policy

data class CloudPolicyCandidate(
    val schemaVersion: Int,
    val advisorVersion: String,
    val evidenceId: String,
    val policyId: String,
    val actionType: String,
    val parameters: Map<String, Any?>,
    val reason: String,
    val expectedEffect: String,
    val confidence: Double?,
    val evidenceRefs: List<String>,
    val executionRequested: Boolean = false,
    val deviceMutationAllowed: Boolean = false
)

enum class CloudPolicySimulationStatus {
    SIMULATED,
    INSUFFICIENT_EVIDENCE
}

data class CloudPolicySimulationResult(
    val status: CloudPolicySimulationStatus,
    val evidenceId: String,
    val advisorVersion: String,
    val policyId: String,
    val simulation: PolicySimulationResult?,
    val executionAllowed: Boolean = false
) {
    init {
        require(!executionAllowed) { "Cloud policy simulation cannot authorize execution." }
        require(
            status == CloudPolicySimulationStatus.INSUFFICIENT_EVIDENCE || simulation != null
        ) { "A simulated result requires simulation output." }
    }
}
