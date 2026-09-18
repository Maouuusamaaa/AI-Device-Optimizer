package com.maouuusama.ai.device.optimizer.policy

enum class PolicySeverity { INFO, ADVISORY, HIGH }

data class PolicyDecision(
    val policyId: String,
    val severity: PolicySeverity,
    val reason: String,
    val proposedActionId: String? = null
)