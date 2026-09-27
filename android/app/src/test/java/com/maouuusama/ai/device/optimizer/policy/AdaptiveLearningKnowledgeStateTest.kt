package com.maouuusama.ai.device.optimizer.policy

import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptiveLearningKnowledgeStateTest {
    @Test
    fun emptyStateStartsWithSchemaAndNoEvidence() {
        val state = AdaptiveLearningKnowledgeState.empty()
        assertEquals(1, state.schemaVersion)
        assertEquals(1, state.learnerVersion)
        assertEquals(0, state.processedEvidenceIds.size)
        assertEquals(0, state.processedEvidenceCount)
        assertEquals(0, state.abstentionCount)
        assertEquals(0, state.patterns.size)
        assertEquals(state.stateFingerprint, state.recomputeFingerprint())
    }
}
