package com.maouuusama.ai.device.optimizer.policy

import java.security.MessageDigest

data class AdaptiveLearningPattern(
    val manufacturer: String,
    val model: String,
    val androidApi: Int,
    val workload: String,
    val policyId: String?,
    val sampleCount: Int,
    val classifications: Set<AdaptiveLearningRecord.Classification>,
    val supportingEvidenceIds: List<String>,
    val featureAverages: Map<String, Double>,
    val eligible: Boolean
)

data class AdaptiveLearningKnowledgeState(
    val schemaVersion: Int,
    val learnerVersion: Int,
    val featureExtractorVersion: Int,
    val processedEvidenceIds: List<String>,
    val processedEvidenceCount: Int,
    val abstentionCount: Int,
    val patterns: List<AdaptiveLearningPattern>,
    val stateFingerprint: String
) {
    init {
        require(schemaVersion == AdaptiveLearningRecord.SCHEMA_VERSION)
        require(learnerVersion == AdaptiveLearningRecord.LEARNER_VERSION)
        require(featureExtractorVersion == AdaptiveLearningRecord.FEATURE_EXTRACTOR_VERSION)
        require(processedEvidenceCount == processedEvidenceIds.distinct().size)
        require(abstentionCount >= 0)
        require(patterns.all { it.sampleCount >= 0 && it.supportingEvidenceIds.distinct().size == it.sampleCount })
        require(patterns.all { it.featureAverages.values.all(Double::isFinite) })
    }

    fun recomputeFingerprint(): String = fingerprint(copy(stateFingerprint = ""))

    fun apply(records: List<AdaptiveLearningRecord>): AdaptiveLearningKnowledgeState {
        val processed = processedEvidenceIds.toMutableSet()
        val patternMap = patterns.associateBy { patternKey(it.manufacturer, it.model, it.androidApi, it.workload, it.policyId) }
            .mapValuesTo(mutableMapOf()) { (_, value) -> value.copy(
                classifications = value.classifications.toSet(),
                supportingEvidenceIds = value.supportingEvidenceIds.toList(),
                featureAverages = value.featureAverages.toMap()
            ) }
        var abstentions = abstentionCount
        var processedCount = processedEvidenceCount

        records.forEach { record ->
            if (!processed.add(record.evidenceId)) return@forEach
            processedCount++
            if (record.learningDisposition == AdaptiveLearningRecord.LearningDisposition.ABSTAIN) {
                abstentions++
                return@forEach
            }
            val key = record.patternKey
            val old = patternMap[key]
            val n = (old?.sampleCount ?: 0) + 1
            val averages = old?.featureAverages.orEmpty().toMutableMap()
            record.features.forEach { (name, value) ->
                val current = averages[name]
                averages[name] = if (current == null) value else ((current * (n - 1)) + value) / n
            }
            patternMap[key] = AdaptiveLearningPattern(
                manufacturer = record.manufacturer,
                model = record.model,
                androidApi = record.androidApi,
                workload = record.workload,
                policyId = record.policyId,
                sampleCount = n,
                classifications = (old?.classifications.orEmpty() + record.classification).toSet(),
                supportingEvidenceIds = (old?.supportingEvidenceIds.orEmpty() + record.evidenceId).distinct().sorted(),
                featureAverages = averages.toSortedMap(),
                eligible = n >= AdaptiveLearningRecord.MINIMUM_SAMPLE_COUNT &&
                    old?.classifications.orEmpty().plus(record.classification).distinct() == listOf(AdaptiveLearningRecord.Classification.NO_REGRESSION)
            )
        }
        val result = copy(
            processedEvidenceIds = processed.toList().sorted(),
            processedEvidenceCount = processedCount,
            abstentionCount = abstentions,
            patterns = patternMap.values.sortedWith(compareBy({ it.manufacturer }, { it.model }, { it.androidApi }, { it.workload }, { it.policyId ?: "" }))
        )
        return result.copy(stateFingerprint = result.fingerprint())
    }

    fun candidates(): List<AdaptiveLearningCandidate> =
        patterns.filter { it.eligible }.map { pattern ->
            AdaptiveLearningCandidate(
                policyId = pattern.policyId,
                evidenceCount = pattern.sampleCount,
                supportingEvidenceIds = pattern.supportingEvidenceIds,
                knowledgeStateFingerprint = stateFingerprint,
                provenance = mapOf(
                    "learnerVersion" to learnerVersion.toString(),
                    "featureExtractorVersion" to featureExtractorVersion.toString(),
                    "knowledgeStateFingerprint" to stateFingerprint
                )
            )
        }

    private fun fingerprint(): String = fingerprint(this)

    private fun fingerprint(state: AdaptiveLearningKnowledgeState): String {
        val canonical = buildString {
            append(state.schemaVersion).append('|')
            append(state.learnerVersion).append('|')
            append(state.featureExtractorVersion).append('|')
            append(state.processedEvidenceIds.sorted().joinToString(",")).append('|')
            append(state.processedEvidenceCount).append('|')
            append(state.abstentionCount).append('|')
            state.patterns.sortedWith(compareBy({ it.manufacturer }, { it.model }, { it.androidApi }, { it.workload }, { it.policyId ?: "" })).forEach {
                append(it.manufacturer).append('|').append(it.model).append('|').append(it.androidApi).append('|')
                append(it.workload).append('|').append(it.policyId ?: "").append('|').append(it.sampleCount).append('|')
                append(it.classifications.map(Enum<*>::name).sorted().joinToString(",")).append('|')
                append(it.supportingEvidenceIds.sorted().joinToString(",")).append('|')
                append(it.featureAverages.toSortedMap().entries.joinToString(",") { e -> "${e.key}=${e.value}" }).append('|')
                append(it.eligible).append(';')
            }
        }
        return MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    companion object {
        fun empty(): AdaptiveLearningKnowledgeState {
            val base = AdaptiveLearningKnowledgeState(
                schemaVersion = AdaptiveLearningRecord.SCHEMA_VERSION,
                learnerVersion = AdaptiveLearningRecord.LEARNER_VERSION,
                featureExtractorVersion = AdaptiveLearningRecord.FEATURE_EXTRACTOR_VERSION,
                processedEvidenceIds = emptyList(),
                processedEvidenceCount = 0,
                abstentionCount = 0,
                patterns = emptyList(),
                stateFingerprint = ""
            )
            return base.copy(stateFingerprint = base.fingerprint())
        }

        private fun patternKey(manufacturer: String, model: String, androidApi: Int, workload: String, policyId: String?): String =
            listOf(manufacturer, model, androidApi, workload, policyId ?: "").joinToString("\u001f")
    }
}

data class AdaptiveLearningCandidate(
    val policyId: String?,
    val evidenceCount: Int,
    val supportingEvidenceIds: List<String>,
    val knowledgeStateFingerprint: String,
    val provenance: Map<String, String>
)
