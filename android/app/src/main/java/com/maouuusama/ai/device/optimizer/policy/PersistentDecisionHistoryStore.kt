package com.maouuusama.ai.device.optimizer.policy

import android.content.Context
import java.io.File

class PersistentDecisionHistoryStore private constructor(
    private val historyFile: File,
    private val maxEntries: Int,
    marker: Unit
) : DecisionHistorySink {
    constructor(context: Context, maxEntries: Int = DEFAULT_MAX_ENTRIES) :
        this(File(context.filesDir, HISTORY_FILE_NAME), maxEntries, Unit)

    constructor(file: File, maxEntries: Int) :
        this(file, maxEntries, Unit)

    init {
        require(maxEntries > 0) { "maxEntries must be positive." }
    }

    fun append(entry: DecisionHistoryEntry) {
        val entries = loadMutable().apply { add(entry) }
        val bounded = if (entries.size > maxEntries) entries.takeLast(maxEntries) else entries
        writeAtomically(bounded)
    }

    fun snapshot(): List<DecisionHistoryEntry> = loadMutable().toList()

    fun clear() {
        if (historyFile.exists() && !historyFile.delete()) {
            throw IllegalStateException("Unable to delete decision history.")
        }
    }

    private fun loadMutable(): MutableList<DecisionHistoryEntry> =
        if (!historyFile.exists()) {
            mutableListOf()
        } else {
            try {
                DecisionHistoryBinaryCodec.decode(historyFile.readBytes()).toMutableList()
            } catch (error: Exception) {
                throw IllegalStateException("Decision history is unreadable.", error)
            }
        }

    private fun writeAtomically(entries: List<DecisionHistoryEntry>) {
        val temporary = File(historyFile.parentFile, "$HISTORY_FILE_NAME.tmp")
        temporary.writeBytes(DecisionHistoryBinaryCodec.encode(entries))
        if (historyFile.exists() && !historyFile.delete()) {
            temporary.delete()
            throw IllegalStateException("Unable to replace decision history.")
        }
        if (!temporary.renameTo(historyFile)) {
            temporary.delete()
            throw IllegalStateException("Unable to commit decision history.")
        }
    }

    companion object {
        const val HISTORY_FILE_NAME = "decision-history.bin"
        const val DEFAULT_MAX_ENTRIES = 1000
    }
}
