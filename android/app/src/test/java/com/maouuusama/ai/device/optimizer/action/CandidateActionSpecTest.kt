package com.maouuusama.ai.device.optimizer.action

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Test

class CandidateActionSpecTest {
    @Test
    fun firstCandidateIsExplicitlyDisabled() {
        val candidate = CandidateActionRegistry.firstCandidate
        assertEquals("observe.remeasure_baseline", candidate.actionId)
        assertEquals("optimizer.execution.disabled", candidate.killSwitchId)
        assertFalse(candidate.executionEnabled)
    }

    @Test(expected = IllegalArgumentException::class)
    fun executionCannotBeEnabledThroughCandidateSpec() {
        CandidateActionSpec(
            actionId = "test.action",
            preconditions = listOf("telemetry available"),
            rollbackPlan = "No mutation.",
            verificationPlan = listOf("verify"),
            killSwitchId = "optimizer.execution.disabled",
            executionEnabled = true
        )
    }
}
