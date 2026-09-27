package com.maouuusama.ai.device.optimizer.policy

import org.junit.Assert.*
import org.junit.Test

class AdaptiveLearningKnowledgeStateTest {
    @Test fun emptyStateStartsWithSchemaAndNoEvidence() {
        val state = AdaptiveLearningKnowledgeState.empty()
        assertEquals(1, state.schemaVersion)
        assertEquals(1, state.learnerVersion)
        assertEquals(0, state.processedEvidenceIds.size)
        assertEquals(0, state.processedEvidenceCount)
        assertEquals(0, state.abstentionCount)
        assertTrue(state.patterns.isEmpty())
        assertEquals(state.stateFingerprint, state.recomputeFingerprint())
    }

    @Test fun threeConsistentNoRegressionRecordsBecomeEligible() {
        var state = AdaptiveLearningKnowledgeState.empty()
        val records = (1..3).map { id ->
            AdaptiveLearningRecord(
                evidenceId = "e$id", manufacturer = "Test", model = "Device", androidApi = 33,
                workload = "background_monitoring", policyId = "policy.memory",
                classification = AdaptiveLearningRecord.Classification.NO_REGRESSION,
                features = mapOf("availableRamMbDelta" to 1.0),
                provenance = mapOf("analysisId" to "a$id"),
                learningDisposition = AdaptiveLearningRecord.LearningDisposition.LEARN
            )
        }
        state = state.apply(records)
        assertEquals(3, state.processedEvidenceCount)
        assertTrue(state.patterns.single().eligible)
        assertEquals(3, state.candidates().single().evidenceCount)
    }

    @Test fun duplicateEvidenceIsIdempotent() {
        val record = AdaptiveLearningRecord(
            evidenceId = "same", manufacturer = "Test", model = "Device", androidApi = 33,
            workload = "background_monitoring", policyId = null,
            classification = AdaptiveLearningRecord.Classification.NO_REGRESSION,
            features = emptyMap(), provenance = mapOf("analysisId" to "a"),
            learningDisposition = AdaptiveLearningRecord.LearningDisposition.LEARN
        )
        val state = AdaptiveLearningKnowledgeState.empty().apply(listOf(record)).apply(listOf(record))
        assertEquals(1, state.processedEvidenceCount)
        assertEquals(1, state.patterns.single().sampleCount)
    }
}
