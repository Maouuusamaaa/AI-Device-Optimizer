package com.maouuusama.ai.device.optimizer.policy

/**
 * Converts measured DeviceState values into explicit, evidence-backed diagnoses.
 *
 * Measurement-only boundary: this class never executes an action. It only classifies
 * observed conditions and attaches confidence/evidence for downstream policy evaluation.
 */
class DiagnosisEngine(
    private val memoryPressureThresholdMb: Long = 1500L,
    private val memoryCriticalThresholdMb: Long = 1000L,
    private val lowBatteryPercent: Int = 20
) {
    fun diagnose(state: DeviceState): List<Diagnosis> {
        val diagnoses = mutableListOf<Diagnosis>()

        when {
            state.availableRamMb < memoryCriticalThresholdMb -> diagnoses += Diagnosis(
                conditionId = "memory.critical",
                severity = DiagnosisSeverity.HIGH,
                confidence = confidenceFor(state.totalRamMb > 0),
                evidence = listOf(
                    "availableRamMb=${state.availableRamMb}",
                    "criticalThresholdMb=${memoryCriticalThresholdMb}",
                    "totalRamMb=${state.totalRamMb}"
                ),
                proposedActionId = "observe.memory_critical"
            )
            state.availableRamMb <= memoryPressureThresholdMb -> diagnoses += Diagnosis(
                conditionId = "memory.pressure",
                severity = DiagnosisSeverity.ADVISORY,
                confidence = confidenceFor(state.totalRamMb > 0),
                evidence = listOf(
                    "availableRamMb=${state.availableRamMb}",
                    "pressureThresholdMb=${memoryPressureThresholdMb}",
                    "totalRamMb=${state.totalRamMb}"
                ),
                proposedActionId = "observe.memory_pressure"
            )
        }

        if (state.batteryPercent != null &&
            state.batteryPercent <= lowBatteryPercent &&
            !state.isCharging
        ) {
            diagnoses += Diagnosis(
                conditionId = "battery.low",
                severity = DiagnosisSeverity.ADVISORY,
                confidence = confidenceFor(true),
                evidence = listOf(
                    "batteryPercent=${state.batteryPercent}",
                    "lowBatteryPercent=${lowBatteryPercent}",
                    "isCharging=${state.isCharging}"
                ),
                proposedActionId = "observe.power_pressure"
            )
        }

        if (diagnoses.isEmpty()) {
            diagnoses += Diagnosis(
                conditionId = "device.normal",
                severity = DiagnosisSeverity.INFO,
                confidence = confidenceFor(state.totalRamMb > 0),
                evidence = listOf(
                    "availableRamMb=${state.availableRamMb}",
                    "batteryPercent=${state.batteryPercent}",
                    "isCharging=${state.isCharging}"
                )
            )
        }

        return diagnoses
    }

    private fun confidenceFor(primaryEvidencePresent: Boolean): Double =
        if (primaryEvidencePresent) 0.95 else 0.50
}
