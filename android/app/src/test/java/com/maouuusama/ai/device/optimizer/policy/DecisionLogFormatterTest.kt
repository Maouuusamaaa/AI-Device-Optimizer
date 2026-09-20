package com.maouuusama.ai.device.optimizer.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DecisionLogFormatterTest {
    private fun entry(battery: Int? = 42) = DecisionLogEntry(
        timestampMs = 1234L,
        availableRamMb = 2200L,
        totalRamMb = 5634L,
        batteryPercent = battery,
        isCharging = true,
        isGaming = false,
        policyIds = listOf("device.normal"),
        proposedActionIds = emptyList(),
        policyModes = listOf("DRY_RUN"),
        safetyGateAllowed = false,
        safetyGateReasons = listOf("Execution remains disabled; dry-run proposals are observation-only."),
        actionExecutionAllowed = false
    )

    @Test
    fun roundTripPreservesDecision() {
        val original = entry()
        val restored = DecisionLogFormatter.fromJson(DecisionLogFormatter.toJson(original))
        assertEquals(original, restored)
    }

    @Test
    fun nullableBatteryIsEncodedAsNull() {
        val restored = DecisionLogFormatter.fromJson(DecisionLogFormatter.toJson(entry(null)))
        assertNull(restored.batteryPercent)
    }

    @Test
    fun executionCannotBeEnabledByFormatting() {
        val json = DecisionLogFormatter.toJson(entry())
        assertFalse(DecisionLogFormatter.fromJson(json).actionExecutionAllowed)
        assertTrue(json.contains("safetyGateAllowed"))
    }
}
