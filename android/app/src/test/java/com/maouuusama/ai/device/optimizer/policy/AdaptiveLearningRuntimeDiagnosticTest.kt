package com.maouuusama.ai.device.optimizer.policy

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveLearningRuntimeDiagnosticTest {
    @Test
    fun readReportsMissingStateWithoutCreatingIt() {
        val file = File.createTempFile("adaptive-learning-missing-", ".bin").apply { delete() }
        try {
            AdaptiveLearningRuntimeRegistry.lastResult = null
            val snapshot = AdaptiveLearningRuntimeDiagnostic.read(AdaptiveLearningKnowledgeStore(file))
            assertEquals(AdaptiveLearningKnowledgeStateStatus.NOT_INITIALIZED, snapshot.knowledgeStateStatus)
            assertEquals(0, snapshot.processedEvidenceCount)
            assertEquals(0, snapshot.abstentionCount)
            assertEquals(0, snapshot.candidateCount)
            assertTrue(snapshot.persistenceValid)
            assertTrue(!file.exists())
        } finally {
            file.delete()
        }
    }

    @Test
    fun readReportsValidPersistedStateAndFingerprint() {
        val file = File.createTempFile("adaptive-learning-valid-", ".bin")
        try {
            val store = AdaptiveLearningKnowledgeStore(file)
            val state = AdaptiveLearningKnowledgeState.empty()
            store.save(state)
            AdaptiveLearningRuntimeRegistry.lastResult = AdaptiveLearningRunResult(
                processedCount = 0,
                abstentionCount = 0,
                candidateCount = 0,
                deferred = false,
                stateFingerprint = state.stateFingerprint
            )
            val snapshot = AdaptiveLearningRuntimeDiagnostic.read(store)
            assertEquals(AdaptiveLearningKnowledgeStateStatus.READY, snapshot.knowledgeStateStatus)
            assertEquals(state.stateFingerprint, snapshot.stateFingerprint)
            assertEquals(0, snapshot.processedEvidenceCount)
            assertEquals(0, snapshot.abstentionCount)
            assertTrue(snapshot.persistenceValid)
            assertEquals(state.stateFingerprint, snapshot.lastResult?.stateFingerprint)
        } finally {
            file.delete()
        }
    }

    @Test
    fun readReportsCorruptStateWithoutReplacingTheFile() {
        val file = File.createTempFile("adaptive-learning-corrupt-", ".bin")
        val original = byteArrayOf(1, 2, 3, 4, 5)
        try {
            file.writeBytes(original)
            val snapshot = AdaptiveLearningRuntimeDiagnostic.read(AdaptiveLearningKnowledgeStore(file))
            assertEquals(AdaptiveLearningKnowledgeStateStatus.ERROR, snapshot.knowledgeStateStatus)
            assertTrue(!snapshot.persistenceValid)
            assertTrue(snapshot.persistenceError != null)
            assertTrue(original.contentEquals(file.readBytes()))
        } finally {
            file.delete()
        }
    }

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
