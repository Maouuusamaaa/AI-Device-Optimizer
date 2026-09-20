package com.maouuusama.ai.device.optimizer.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineEvaluationMetadataTest {
    private fun metadata(workload: String? = "idle") = OfflineEvaluationMetadata(
        charging = false,
        batteryTemperatureC = 35.0,
        thermalStatus = 0,
        networkTransport = "wifi",
        networkValidated = true,
        interactive = true,
        workload = workload
    )

    @Test
    fun completeMetadataHasNoMissingFields() {
        val value = metadata()
        assertTrue(value.complete)
        assertTrue(value.missingFields().isEmpty())
    }

    @Test
    fun missingMetadataIsExplicit() {
        val value = OfflineEvaluationMetadata(
            charging = null,
            batteryTemperatureC = null,
            thermalStatus = null,
            networkTransport = null,
            networkValidated = null,
            interactive = null,
            workload = null
        )
        assertFalse(value.complete)
        assertEquals(7, value.missingFields().size)
    }

    @Test
    fun recordRequiresStableIdentity() {
        val record = OfflineEvaluationRecord(
            observationId = "obs-1",
            timestampMs = 10L,
            conditionIds = listOf("device.normal"),
            actionIds = emptyList(),
            metadata = metadata(),
            outcomeLabel = null
        )
        assertFalse(record.hasControlledOutcome)
    }

    @Test
    fun controlledOutcomeIsExplicitWhenPresent() {
        val record = OfflineEvaluationRecord(
            observationId = "obs-2",
            timestampMs = 20L,
            conditionIds = listOf("device.normal"),
            actionIds = listOf("observe.memory_pressure"),
            metadata = metadata(),
            outcomeLabel = "no_change"
        )
        assertTrue(record.hasControlledOutcome)
    }
}
