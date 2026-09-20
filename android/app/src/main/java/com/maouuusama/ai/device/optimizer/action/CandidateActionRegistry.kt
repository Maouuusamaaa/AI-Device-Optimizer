package com.maouuusama.ai.device.optimizer.action

object CandidateActionRegistry {
    val firstCandidate = CandidateActionSpec(
        actionId = "observe.remeasure_baseline",
        preconditions = listOf(
            "device telemetry is AVAILABLE",
            "workload is recorded",
            "measurement protocol is active"
        ),
        rollbackPlan = "No device mutation is performed; discard the observation run if controls are invalid.",
        verificationPlan = listOf(
            "preserve raw samples",
            "verify device identity and Android API",
            "compare baseline and repeat measurements",
            "record decision-log evidence"
        ),
        killSwitchId = "optimizer.execution.disabled"
    )
}
