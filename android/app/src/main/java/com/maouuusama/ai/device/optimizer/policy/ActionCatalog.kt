package com.maouuusama.ai.device.optimizer.policy

enum class ActionRisk { LOW, MEDIUM, HIGH }

data class ActionDefinition(
    val id: String,
    val description: String,
    val risk: ActionRisk,
    val requiredPermission: String,
    val reversible: Boolean,
    val expectedEffect: String,
    val rollback: String,
    val measurement: String
)

object ActionCatalog {
    private val definitions = listOf(
        ActionDefinition("observe.memory_critical", "Record a memory-critical condition without changing device state.", ActionRisk.LOW, "none", true, "No device-state change; produces an observation candidate.", "Not applicable; observation-only.", "Available RAM, swap usage, CPU utilization, and app launch timing."),
        ActionDefinition("observe.memory_pressure", "Record a memory-pressure condition without changing device state.", ActionRisk.LOW, "none", true, "No device-state change; produces an observation candidate.", "Not applicable; observation-only.", "Available RAM, swap usage, CPU utilization, and app launch timing."),
        ActionDefinition("observe.power_pressure", "Record a low-battery, not-charging condition without changing device state.", ActionRisk.LOW, "none", true, "No device-state change; produces an observation candidate.", "Not applicable; observation-only.", "Battery percentage, temperature, CPU utilization, and workload timing.")
    )
    private val byId = definitions.associateBy(ActionDefinition::id)
    fun all(): List<ActionDefinition> = definitions
    fun find(actionId: String): ActionDefinition? = byId[actionId]
}
