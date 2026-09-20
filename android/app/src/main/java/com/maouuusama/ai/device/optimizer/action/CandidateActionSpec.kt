package com.maouuusama.ai.device.optimizer.action

data class CandidateActionSpec(
    val actionId: String,
    val preconditions: List<String>,
    val rollbackPlan: String,
    val verificationPlan: List<String>,
    val killSwitchId: String,
    val executionEnabled: Boolean = false
) {
    init {
        require(actionId.isNotBlank())
        require(preconditions.isNotEmpty())
        require(rollbackPlan.isNotBlank())
        require(verificationPlan.isNotEmpty())
        require(killSwitchId.isNotBlank())
        require(!executionEnabled) {
            "Candidate actions remain disabled until a separately reviewed execution milestone."
        }
    }
}
