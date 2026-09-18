package com.maouuusama.ai.device.optimizer.actions

import com.maouuusama.ai.device.optimizer.policy.PolicyDecision
import com.maouuusama.ai.device.optimizer.policy.PolicySeverity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActionEngineTest {
    private val engine = ActionEngine()

    @Test
    fun allowlistedActionIsOnlyProposed() {
        val decision = PolicyDecision(
            "memory.low",
            PolicySeverity.HIGH,
            "test",
            "observe.background_pressure"
        )

        val proposal = engine.propose(decision)

        assertEquals(ProposalStatus.PROPOSED, proposal?.status)
        assertEquals("observe.background_pressure", proposal?.actionId)
    }

    @Test
    fun unknownActionIsBlocked() {
        val decision = PolicyDecision(
            "test",
            PolicySeverity.HIGH,
            "test",
            "kill.random.process"
        )

        val proposal = engine.propose(decision)

        assertEquals(ProposalStatus.BLOCKED, proposal?.status)
    }

    @Test
    fun decisionWithoutActionProducesNoProposal() {
        val decision = PolicyDecision(
            "device.normal",
            PolicySeverity.INFO,
            "test",
            null
        )

        assertNull(engine.propose(decision))
    }
}