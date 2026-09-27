package com.maouuusama.ai.device.optimizer.policy

import android.content.Context
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class AdaptiveLearningKnowledgeStore private constructor(
    private val stateFile: File,
    marker: Unit
) {
    constructor(context: Context) : this(File(context.filesDir, FILE_NAME), Unit)
    constructor(file: File) : this(file, Unit)

    fun exists(): Boolean = stateFile.exists()

    fun load(): AdaptiveLearningKnowledgeState {
        if (!stateFile.exists()) return AdaptiveLearningKnowledgeState.empty()
        try {
            DataInputStream(stateFile.inputStream().buffered()).use { input ->
                require(input.readUTF() == MAGIC) { "Unsupported knowledge state format." }
                val state = readState(input)
                require(state.stateFingerprint == state.recomputeFingerprint()) { "Knowledge state fingerprint mismatch." }
                return state
            }
        } catch (error: Exception) {
            throw IllegalStateException("Adaptive learning knowledge state is unreadable.", error)
        }
    }

    fun save(state: AdaptiveLearningKnowledgeState) {
        require(state.stateFingerprint == state.recomputeFingerprint()) { "Cannot persist invalid knowledge state." }
        stateFile.parentFile?.mkdirs()
        val temporary = File(stateFile.parentFile, "${stateFile.name}.tmp")
        try {
            DataOutputStream(temporary.outputStream().buffered()).use { output ->
                output.writeUTF(MAGIC)
                writeState(output, state)
                output.flush()
            }
            try {
                Files.move(
                    temporary.toPath(),
                    stateFile.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(
                    temporary.toPath(),
                    stateFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        } catch (error: Exception) {
            temporary.delete()
            throw IllegalStateException("Unable to persist adaptive learning knowledge state.", error)
        }
    }

    private fun writeState(output: DataOutputStream, state: AdaptiveLearningKnowledgeState) {
        output.writeInt(state.schemaVersion)
        output.writeInt(state.learnerVersion)
        output.writeInt(state.featureExtractorVersion)
        output.writeInt(state.processedEvidenceIds.size)
        state.processedEvidenceIds.sorted().forEach(output::writeUTF)
        output.writeInt(state.processedEvidenceCount)
        output.writeInt(state.abstentionCount)
        output.writeInt(state.patterns.size)
        state.patterns.forEach { pattern ->
            output.writeUTF(pattern.manufacturer)
            output.writeUTF(pattern.model)
            output.writeInt(pattern.androidApi)
            output.writeUTF(pattern.workload)
            output.writeBoolean(pattern.policyId != null)
            if (pattern.policyId != null) output.writeUTF(pattern.policyId)
            output.writeInt(pattern.sampleCount)
            output.writeInt(pattern.classifications.size)
            pattern.classifications.map { it.name }.sorted().forEach(output::writeUTF)
            output.writeInt(pattern.supportingEvidenceIds.size)
            pattern.supportingEvidenceIds.sorted().forEach(output::writeUTF)
            output.writeInt(pattern.featureAverages.size)
            pattern.featureAverages.toSortedMap().forEach { (name, value) ->
                output.writeUTF(name)
                output.writeDouble(value)
            }
            output.writeBoolean(pattern.eligible)
        }
        output.writeUTF(state.stateFingerprint)
    }

    private fun readState(input: DataInputStream): AdaptiveLearningKnowledgeState {
        val schema = input.readInt()
        val learner = input.readInt()
        val extractor = input.readInt()
        val evidenceCount = input.readInt()
        val evidenceIds = List(evidenceCount) { input.readUTF() }
        val processedCount = input.readInt()
        val abstentions = input.readInt()
        val patternCount = input.readInt()
        val patterns = List(patternCount) {
            val manufacturer = input.readUTF()
            val model = input.readUTF()
            val api = input.readInt()
            val workload = input.readUTF()
            val policyId = if (input.readBoolean()) input.readUTF() else null
            val sampleCount = input.readInt()
            val classCount = input.readInt()
            val classifications = List(classCount) {
                AdaptiveLearningRecord.Classification.valueOf(input.readUTF())
            }.toSet()
            val supportingCount = input.readInt()
            val supporting = List(supportingCount) { input.readUTF() }
            val featureCount = input.readInt()
            val features = buildMap {
                repeat(featureCount) { put(input.readUTF(), input.readDouble()) }
            }
            val eligible = input.readBoolean()
            AdaptiveLearningPattern(manufacturer, model, api, workload, policyId, sampleCount, classifications, supporting, features, eligible)
        }
        return AdaptiveLearningKnowledgeState(
            schema, learner, extractor, evidenceIds, processedCount, abstentions, patterns, input.readUTF()
        )
    }

    companion object {
        const val FILE_NAME = "adaptive-learning-knowledge.bin"
        private const val MAGIC = "ADO_ADAPTIVE_LEARNING_KNOWLEDGE_V1"
    }
}
