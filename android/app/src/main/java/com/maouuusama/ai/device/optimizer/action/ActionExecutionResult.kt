package com.maouuusama.ai.device.optimizer.action

enum class ActionExecutionStatus { DISABLED, REJECTED, NOT_IMPLEMENTED, EXECUTED }

data class ActionExecutionResult(
    val actionId: String,
    val status: ActionExecutionStatus,
    val changedDeviceState: Boolean,
    val reason: String
) {
    init {
        require(actionId.isNotBlank()) { "actionId must not be blank." }
        if (status != ActionExecutionStatus.EXECUTED) require(!changedDeviceState) { "Non-executed actions cannot change device state." }
        if (status == ActionExecutionStatus.EXECUTED) require(reason.isNotBlank()) { "Executed actions require an execution reason." }
    }
}
