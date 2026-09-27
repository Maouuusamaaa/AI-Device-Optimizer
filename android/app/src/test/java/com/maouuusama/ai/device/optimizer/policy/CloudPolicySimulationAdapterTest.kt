package com.maouuusama.ai.device.optimizer.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudPolicySimulationAdapterTest {
    private fun candidate(
        actionType: String = "observe.memory_pressure",
        evidenceRefs: List<String> = listOf("evidence-001"),
        executionRequested: Boolean = false,
        deviceMutationAllowed: Boolean = false
    ) = CloudPolicyCandidate(
        schemaVersion = 1,
        advisorVersion = "cloud-advisor-v1",
        evidenceId = "evidence-001",
        policyId = "policy-001",
        actionType = actionType,
        parameters = emptyMap(),
        reason = "Observe before intervention.",
        expectedEffect = "No device-state change.",
        confidence = 0.5,
        evidenceRefs = evidenceRefs,
        executionRequested = executionRequested,
        deviceMutationAllowed = deviceMutationAllowed
    )

    @Test
    fun allowlistedCandidateBecomesDryRunOnly() {
        val result = CloudPolicySimulationAdapter().simulate(candidate(), setOf("evidence-001"))
        assertEquals(CloudPolicySimulationStatus.SIMULATED, result.status)
        assertEquals("policy-001", result.policyId)
        assertEquals("evidence-001", result.evidenceId)
        assertEquals(PolicyMode.DRY_RUN, result.simulation!!.decisions.single().mode)
        assertFalse(result.simulation.executionAllowed)
    }

    @Test
    fun unknownActionIsRejectedBeforeSimulation() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            CloudPolicySimulationAdapter().simulate(
                candidate(actionType = "unknown.action"),
                setOf("evidence-001")
            )
        }
        assertTrue(error.message!!.contains("not present in the action catalog"))
    }

    @Test
    fun malformedCandidateIsRejected() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            CloudPolicySimulationAdapter().simulate(
                candidate(actionType = " "),
                setOf("evidence-001")
            )
        }
        assertTrue(error.message!!.contains("actionType"))
    }

    @Test
    fun cloudExecutionFlagsCannotEnableExecution() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            CloudPolicySimulationAdapter().simulate(
                candidate(executionRequested = true),
                setOf("evidence-001")
            )
        }
        assertTrue(error.message!!.contains("execution"))
    }

    @Test
    fun cloudMutationFlagCannotEnableExecution() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            CloudPolicySimulationAdapter().simulate(
                candidate(deviceMutationAllowed = true),
                setOf("evidence-001")
            )
        }
        assertTrue(error.message!!.contains("device mutation"))
    }

    @Test
    fun simulationIsDeterministic() {
        val adapter = CloudPolicySimulationAdapter()
        val first = adapter.simulate(candidate(), setOf("evidence-001"))
        val second = adapter.simulate(candidate(), setOf("evidence-001"))
        assertEquals(first, second)
    }

    @Test
    fun insufficientEvidenceDoesNotAssumeBenefit() {
        val result = CloudPolicySimulationAdapter().simulate(candidate(), emptySet())
        assertEquals(CloudPolicySimulationStatus.INSUFFICIENT_EVIDENCE, result.status)
        assertEquals(null, result.simulation)
        assertFalse(result.executionAllowed)
    }

    @Test
    fun missingReferencedEvidenceIsInsufficient() {
        val result = CloudPolicySimulationAdapter().simulate(candidate(), setOf("different-evidence"))
        assertEquals(CloudPolicySimulationStatus.INSUFFICIENT_EVIDENCE, result.status)
        assertFalse(result.executionAllowed)
    }

    @Test
    fun candidateEvidenceIsNotMutated() {
        val refs = mutableListOf("evidence-001")
        val candidate = candidate(evidenceRefs = refs)
        val original = refs.toList()
        CloudPolicySimulationAdapter().simulate(candidate, setOf("evidence-001"))
        assertEquals(original, refs)
    }

    @Test
    fun safetyGateRemainsAuthoritativeAfterSimulation() {
        val result = CloudPolicySimulationAdapter().simulate(candidate(), setOf("evidence-001"))
        val simulation = result.simulation!!
        val state = DeviceState(
            availableRamMb = 1800,
            totalRamMb = 8000,
            batteryPercent = 50,
            isCharging = false,
            isGaming = false
        )
        val proposal = DryRunPolicyProposal(
            state = state,
            decisions = simulation.decisions,
            actionExecutionAllowed = simulation.executionAllowed
        )
        val gate = DryRunSafetyGate().evaluate(proposal)
        assertFalse(gate.allowed)
        assertTrue(gate.blockReasons.contains(SafetyBlockReason.EXECUTION_DISABLED))
    }
}
