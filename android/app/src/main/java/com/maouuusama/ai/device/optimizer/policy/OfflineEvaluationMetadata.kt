package com.maouuusama.ai.device.optimizer.policy

data class OfflineEvaluationMetadata(
    val charging: Boolean?,
    val batteryTemperatureC: Double?,
    val thermalStatus: Int?,
    val networkTransport: String?,
    val networkValidated: Boolean?,
    val interactive: Boolean?,
    val workload: String?
) {
    init {
        require(batteryTemperatureC == null || batteryTemperatureC.isFinite())
        require(workload == null || workload.isNotBlank())
    }

    fun missingFields(): List<String> = buildList {
        if (charging == null) add("charging")
        if (batteryTemperatureC == null) add("batteryTemperatureC")
        if (thermalStatus == null) add("thermalStatus")
        if (networkTransport == null) add("networkTransport")
        if (networkValidated == null) add("networkValidated")
        if (interactive == null) add("interactive")
        if (workload == null) add("workload")
    }

    val complete: Boolean
        get() = missingFields().isEmpty()
}

data class OfflineEvaluationRecord(
    val observationId: String,
    val timestampMs: Long,
    val conditionIds: List<String>,
    val actionIds: List<String>,
    val metadata: OfflineEvaluationMetadata,
    val outcomeLabel: String?
) {
    init {
        require(observationId.isNotBlank())
        require(timestampMs >= 0L)
        require(conditionIds.distinct().size == conditionIds.size)
        require(actionIds.distinct().size == actionIds.size)
    }

    val hasControlledOutcome: Boolean
        get() = !outcomeLabel.isNullOrBlank()
}
