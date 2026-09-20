package com.maouuusama.ai.device.optimizer.action

import com.maouuusama.ai.device.optimizer.policy.DeviceState
import com.maouuusama.ai.device.optimizer.policy.DryRunPolicyProposal
import com.maouuusama.ai.device.optimizer.policy.LocalPolicyEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ActionExecutorTest {
    @Test fun proposedActionIsNeverExecutedInDryRun() {
        val state = DeviceState(availableRamMb = 900, totalRamMb = 5634, batteryPercent = 50, isCharging = true, isGaming = false)
        val results = ActionExecutor().execute(DryRunPolicyProposal.from(state, LocalPolicyEngine()))
        assertEquals(1, results.size)
        assertEquals("observe.memory_critical", results.single().actionId)
        assertEquals(ActionExecutionStatus.DISABLED, results.single().status)
        assertFalse(results.single().changedDeviceState)
    }

    @Test fun normalStateProducesNoExecutionResult() {
        val state = DeviceState(availableRamMb = 2500, totalRamMb = 5634, batteryPercent = 50, isCharging = true, isGaming = false)
        assertEquals(emptyList<ActionExecutionResult>(), ActionExecutor().execute(DryRunPolicyProposal.from(state)))
    }
}
