package com.maouuusama.ai.device.optimizer.policy

class CloudPolicySimulationAdapter(
    private val validator: CloudPolicyCandidateValidator = CloudPolicyCandidateValidator(),
    private val simulator: PolicySimulator = PolicySimulator()
) {
    fun simulate(
        candidate: CloudPolicyCandidate,
        availableEvidenceIds: Set<String>
    ): CloudPolicySimulationResult {
        val validated = validator.validate(candidate)
        if (!validated.evidenceRefs.all(availableEvidenceIds::contains)) {
            return CloudPolicySimulationResult(
                status = CloudPolicySimulationStatus.INSUFFICIENT_EVIDENCE,
                evidenceId = validated.evidenceId,
                advisorVersion = validated.advisorVersion,
                policyId = validated.policyId,
                simulation = null
            )
        }

        val diagnosis = Diagnosis(
            conditionId = validated.policyId,
            severity = DiagnosisSeverity.ADVISORY,
            confidence = validated.confidence ?: 0.0,
            evidence = validated.evidenceRefs.toList(),
            proposedActionId = validated.actionType
        )
        val simulation = simulator.simulate(listOf(diagnosis))
        return CloudPolicySimulationResult(
            status = CloudPolicySimulationStatus.SIMULATED,
            evidenceId = validated.evidenceId,
            advisorVersion = validated.advisorVersion,
            policyId = validated.policyId,
            simulation = simulation
        )
    }
}
