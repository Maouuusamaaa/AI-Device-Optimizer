package com.maouuusama.ai.device.optimizer.policy

class CloudPolicyCandidateValidator(
    private val catalog: ActionCatalog = ActionCatalog
) {
    fun validate(candidate: CloudPolicyCandidate): CloudPolicyCandidate {
        require(candidate.schemaVersion == 1) { "schemaVersion must be 1" }
        require(candidate.advisorVersion.isNotBlank()) { "advisorVersion must be non-empty" }
        require(candidate.evidenceId.isNotBlank()) { "evidenceId must be non-empty" }
        require(candidate.policyId.isNotBlank()) { "policyId must be non-empty" }
        require(candidate.actionType.isNotBlank()) { "actionType must be non-empty" }
        require(candidate.reason.isNotBlank()) { "reason must be non-empty" }
        require(candidate.expectedEffect.isNotBlank()) { "expectedEffect must be non-empty" }
        require(candidate.evidenceRefs.isNotEmpty()) { "evidenceRefs must be non-empty" }
        require(candidate.evidenceRefs.all { it.isNotBlank() }) { "evidenceRefs must contain non-empty values" }
        candidate.confidence?.let {
            require(it.isFinite() && it in 0.0..1.0) { "confidence must be finite and between 0 and 1" }
        }
        require(!candidate.executionRequested) { "Cloud policy cannot request execution." }
        require(!candidate.deviceMutationAllowed) { "Cloud policy cannot allow device mutation." }
        require(catalog.find(candidate.actionType) != null) {
            "Action undefined is not present in the action catalog."
        }
        return candidate.copy(
            parameters = candidate.parameters.toMap(),
            evidenceRefs = candidate.evidenceRefs.toList()
        )
    }
}
