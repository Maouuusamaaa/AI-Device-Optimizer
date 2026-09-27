package com.maouuusama.ai.device.optimizer.policy

data class AdaptiveLearningRunResult(
    val processedCount: Int,
    val abstentionCount: Int,
    val candidateCount: Int,
    val deferred: Boolean,
    val stateFingerprint: String
)

class AdaptiveLearningRuntime(
    private val historyStore: PersistentDecisionHistoryStore,
    private val knowledgeStore: AdaptiveLearningKnowledgeStore,
    private val resourceGuard: AdaptiveLearningResourceGuard,
    private val manufacturer: String,
    private val model: String,
    private val androidApi: Int,
    private val workload: String = "background_monitoring",
    private val maxRecordsPerPass: Int = 8
) {
    init {
        require(manufacturer.isNotBlank())
        require(model.isNotBlank())
        require(androidApi > 0)
        require(maxRecordsPerPass in 1..8)
    }

    var lastResult: AdaptiveLearningRunResult = AdaptiveLearningRunResult(
        processedCount = 0,
        abstentionCount = 0,
        candidateCount = 0,
        deferred = false,
        stateFingerprint = AdaptiveLearningKnowledgeState.empty().stateFingerprint
    )
        private set

    fun processAvailable(): AdaptiveLearningRunResult {
        val current = knowledgeStore.load()
        if (!resourceGuard.mayProcess()) {
            return AdaptiveLearningRunResult(0, 0, current.candidates().size, true, current.stateFingerprint)
                .also { lastResult = it }
        }

        val records = historyStore.snapshot()
            .flatMap { AdaptiveLearningRecord.fromHistory(it, manufacturer, model, androidApi, workload) }
            .filterNot { current.processedEvidenceIds.contains(it.evidenceId) }
            .take(maxRecordsPerPass)

        if (records.isEmpty()) {
            return AdaptiveLearningRunResult(0, 0, current.candidates().size, false, current.stateFingerprint)
                .also { lastResult = it }
        }

        val next = current.apply(records)
        knowledgeStore.save(next)
        val result = AdaptiveLearningRunResult(
            processedCount = records.size,
            abstentionCount = records.count { it.learningDisposition == AdaptiveLearningRecord.LearningDisposition.ABSTAIN },
            candidateCount = next.candidates().size,
            deferred = false,
            stateFingerprint = next.stateFingerprint
        )
        lastResult = result
        return result
    }
}
