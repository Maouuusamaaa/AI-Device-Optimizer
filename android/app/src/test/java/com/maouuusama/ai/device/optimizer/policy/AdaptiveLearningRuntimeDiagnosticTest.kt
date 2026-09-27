package com.maouuusama.ai.device.optimizer.policy

import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveLearningRuntimeDiagnosticTest {
    @Test
    fun renderShowsPersistedStateAndLastPass() {
        val result = AdaptiveLearningRunResult(8, 8, 0, false, "abc123")
        val snapshot = AdaptiveLearningRuntimeDiagnosticSnapshot(
            knowledgeStateStatus = AdaptiveLearningKnowledgeStateStatus.READY,
            processedEvidenceCount = 16,
            abstentionCount = 16,
            candidateCount = 0,
            stateFingerprint = "abc123",
            persistenceValid = true,
            persistenceError = null,
            lastResult = result
        )
        val rendered = AdaptiveLearningRuntimeDiagnostic.render(snapshot)
        assertTrue(rendered.contains("Knowledge state: READY"))
        assertTrue(rendered.contains("Processed evidence: 16"))
        assertTrue(rendered.contains("Abstentions: 16"))
        assertTrue(rendered.contains("Advisory candidates: 0"))
        assertTrue(rendered.contains("Persistence validation: OK"))
        assertTrue(rendered.contains("Last pass: processed=8, abstained=8, deferred=false, candidates=0"))
        assertTrue(rendered.contains("Mode: read-only diagnostic; no learned action is authorized."))
    }

    @Test
    fun renderDistinguishesUninitializedStateAndMissingRuntimeResult() {
        val snapshot = AdaptiveLearningRuntimeDiagnosticSnapshot(
            knowledgeStateStatus = AdaptiveLearningKnowledgeStateStatus.NOT_INITIALIZED,
            processedEvidenceCount = 0,
            abstentionCount = 0,
            candidateCount = 0,
            stateFingerprint = null,
            persistenceValid = true,
            persistenceError = null,
            lastResult = null
        )
        val rendered = AdaptiveLearningRuntimeDiagnostic.render(snapshot)
        assertTrue(rendered.contains("Knowledge state: NOT_INITIALIZED"))
        assertTrue(rendered.contains("State fingerprint: not available"))
        assertTrue(rendered.contains("Last pass: not observed in this process"))
    }

    @Test
    fun renderShowsPersistenceErrorWithoutAuthorizingAnything() {
        val snapshot = AdaptiveLearningRuntimeDiagnosticSnapshot(
            knowledgeStateStatus = AdaptiveLearningKnowledgeStateStatus.ERROR,
            processedEvidenceCount = 0,
            abstentionCount = 0,
            candidateCount = 0,
            stateFingerprint = null,
            persistenceValid = false,
            persistenceError = "corrupt state",
            lastResult = null
        )
        val rendered = AdaptiveLearningRuntimeDiagnostic.render(snapshot)
        assertTrue(rendered.contains("Knowledge state: ERROR"))
        assertTrue(rendered.contains("Persistence validation: ERROR"))
        assertTrue(rendered.contains("Persistence error: corrupt state"))
        assertTrue(rendered.contains("read-only diagnostic"))
    }
}
