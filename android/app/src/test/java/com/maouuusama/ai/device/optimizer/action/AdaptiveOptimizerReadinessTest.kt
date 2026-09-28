package com.maouuusama.ai.device.optimizer.action

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveOptimizerReadinessTest {
    private fun validSpec(actionId: String = "observe.memory_pressure") =
        CandidateActionSpec(
            actionId = actionId,
            preconditions = listOf("validated telemetry available"),
            rollbackPlan = "No mutation; discard observation.",
            verificationPlan = listOf("verify evidence persisted"),
            killSwitchId = "optimizer.execution.disabled"
        )

    @Test
    fun structurallyValidCandidateIsReadyForReviewButNeverExecution() {
        val result = AdaptiveOptimizerReadinessEvaluator().evaluate(validSpec())

        assertEquals(AdaptiveOptimizerReadinessStatus.READY_FOR_REVIEW, result.status)
        assertFalse(result.executionAllowed)
        assertTrue(result.reasons.any { it.contains("Execution remains disabled") })
    }

    @Test
    fun unknownActionIsBlocked() {
        val result = AdaptiveOptimizerReadinessEvaluator().evaluate(
            validSpec("unknown.action")
        )

        assertEquals(AdaptiveOptimizerReadinessStatus.BLOCKED, result.status)
        assertFalse(result.executionAllowed)
    }

    @Test
    fun permissionedActionIsBlocked() {
        val catalog = object : ActionCatalogProvider {
            override fun find(actionId: String) =
                com.maouuusama.ai.device.optimizer.policy.ActionDefinition(
                    id = actionId,
                    description = "test",
                    risk = com.maouuusama.ai.device.optimizer.policy.ActionRisk.LOW,
                    requiredPermission = "android.permission.TEST",
                    reversible = true,
                    expectedEffect = "test",
                    rollback = "test rollback",
                    measurement = "test measurement"
                )
        }

        val result = AdaptiveOptimizerReadinessEvaluator(catalog).evaluate(validSpec())

        assertEquals(AdaptiveOptimizerReadinessStatus.BLOCKED, result.status)
        assertFalse(result.executionAllowed)
        assertTrue(result.reasons.any { it.contains("additional permission") })
    }

    @Test
    fun candidateExecutionFlagCannotBeEnabled() {
        val error = runCatching {
            CandidateActionSpec(
                actionId = "observe.memory_pressure",
                preconditions = listOf("validated telemetry available"),
                rollbackPlan = "No mutation.",
                verificationPlan = listOf("verify"),
                killSwitchId = "optimizer.execution.disabled",
                executionEnabled = true
            )
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
    }
}
