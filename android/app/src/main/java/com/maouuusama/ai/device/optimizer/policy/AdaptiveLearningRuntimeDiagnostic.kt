package com.maouuusama.ai.device.optimizer.policy

import android.content.Context

enum class AdaptiveLearningKnowledgeStateStatus {
    READY,
    NOT_INITIALIZED,
    ERROR
}

data class AdaptiveLearningRuntimeDiagnosticSnapshot(
    val knowledgeStateStatus: AdaptiveLearningKnowledgeStateStatus,
    val processedEvidenceCount: Int,
    val abstentionCount: Int,
    val candidateCount: Int,
    val stateFingerprint: String?,
    val persistenceValid: Boolean,
    val persistenceError: String?,
    val lastResult: AdaptiveLearningRunResult?
)

object AdaptiveLearningRuntimeDiagnostic {
    fun read(context: Context): AdaptiveLearningRuntimeDiagnosticSnapshot {
        val store = AdaptiveLearningKnowledgeStore(context)
        if (!store.exists()) {
            return AdaptiveLearningRuntimeDiagnosticSnapshot(
                knowledgeStateStatus = AdaptiveLearningKnowledgeStateStatus.NOT_INITIALIZED,
                processedEvidenceCount = 0,
                abstentionCount = 0,
                candidateCount = 0,
                stateFingerprint = null,
                persistenceValid = true,
                persistenceError = null,
                lastResult = AdaptiveLearningRuntimeRegistry.lastResult
            )
        }

        return try {
            val state = store.load()
            AdaptiveLearningRuntimeDiagnosticSnapshot(
                knowledgeStateStatus = AdaptiveLearningKnowledgeStateStatus.READY,
                processedEvidenceCount = state.processedEvidenceCount,
                abstentionCount = state.abstentionCount,
                candidateCount = state.candidates().size,
                stateFingerprint = state.stateFingerprint,
                persistenceValid = true,
                persistenceError = null,
                lastResult = AdaptiveLearningRuntimeRegistry.lastResult
            )
        } catch (error: Exception) {
            AdaptiveLearningRuntimeDiagnosticSnapshot(
                knowledgeStateStatus = AdaptiveLearningKnowledgeStateStatus.ERROR,
                processedEvidenceCount = 0,
                abstentionCount = 0,
                candidateCount = 0,
                stateFingerprint = null,
                persistenceValid = false,
                persistenceError = error.message ?: error.javaClass.simpleName,
                lastResult = AdaptiveLearningRuntimeRegistry.lastResult
            )
        }
    }

    fun render(snapshot: AdaptiveLearningRuntimeDiagnosticSnapshot): String = buildString {
        append("\nAdaptive learning runtime diagnostic\n")
        append("Knowledge state: ").append(snapshot.knowledgeStateStatus.name).append("\n")
        append("Processed evidence: ").append(snapshot.processedEvidenceCount).append("\n")
        append("Abstentions: ").append(snapshot.abstentionCount).append("\n")
        append("Advisory candidates: ").append(snapshot.candidateCount).append("\n")
        append("State fingerprint: ").append(snapshot.stateFingerprint ?: "not available").append("\n")
        append("Persistence validation: ")
            .append(if (snapshot.persistenceValid) "OK" else "ERROR")
            .append("\n")
        snapshot.persistenceError?.let {
            append("Persistence error: ").append(it).append("\n")
        }
        val result = snapshot.lastResult
        if (result == null) {
            append("Last pass: not observed in this process\n")
        } else {
            append("Last pass: processed=").append(result.processedCount)
                .append(", abstained=").append(result.abstentionCount)
                .append(", deferred=").append(result.deferred)
                .append(", candidates=").append(result.candidateCount)
                .append("\n")
        }
        append("Mode: read-only diagnostic; no learned action is authorized.")
    }
}
