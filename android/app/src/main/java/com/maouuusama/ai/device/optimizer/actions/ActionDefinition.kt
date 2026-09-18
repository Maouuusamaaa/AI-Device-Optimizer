package com.maouuusama.ai.device.optimizer.actions

enum class ActionRisk { LOW, MEDIUM, HIGH }

data class ActionDefinition(
    val actionId: String,
    val risk: ActionRisk,
    val enabled: Boolean = true
)