package com.maouuusama.ai.device.optimizer.policy

enum class PolicySeverity { INFO, ADVISORY, HIGH }

enum class PolicyMode { DRY_RUN }

data class PolicyDecision(
    val policyId: String,
    val severity: PolicySeverity,
    val reason: String,
    val proposedActionId: String? = null,
    val mode: PolicyMode = PolicyMode.DRY_RUN
)
