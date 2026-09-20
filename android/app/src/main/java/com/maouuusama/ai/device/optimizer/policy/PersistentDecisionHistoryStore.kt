package com.maouuusama.ai.device.optimizer.policy

import android.content.Context
import java.io.File

class PersistentDecisionHistoryStore(context: Context, private val maxEntries: Int = DEFAULT_MAX_ENTRIES) {
    init { require(maxEntries > 0) { "maxEntries must be positive." } }
    private val file = File(context.filesDir, HISTORY_FILE_NAME)

    fun append(entry: DecisionHistoryEntry) {
        val entries = loadMutable().apply { add(entry) }
        val bounded = if (entries.size > maxEntries) entries.takeLast(maxEntries) else entries
        writeAtomically(bounded)
    }

    fun snapshot(): List<DecisionHistoryEntry> = loadMutable().toList()

    fun clear() {
        if (file.exists() && !file.delete()) throw IllegalStateException("Unable to delete decision history.")
    }

    private fun loadMutable(): MutableList<DecisionHistoryEntry> = if (!file.exists()) mutableListOf() else try {
        DecisionHistoryJsonCodec.decode(file.readText()).toMutableList()
    } catch (error: Exception) {
        throw IllegalStateException("Decision history is unreadable.", error)
    }

    private fun writeAtomically(entries: List<DecisionHistoryEntry>) {
        val temporary = File(file.parentFile, "$HISTORY_FILE_NAME.tmp")
        temporary.writeText(DecisionHistoryJsonCodec.encode(entries))
        if (file.exists() && !file.delete()) { temporary.delete(); throw IllegalStateException("Unable to replace decision history.") }
        if (!temporary.renameTo(file)) { temporary.delete(); throw IllegalStateException("Unable to commit decision history.") }
    }

    companion object {
        const val HISTORY_FILE_NAME = "decision-history.json"
        const val DEFAULT_MAX_ENTRIES = 1000
    }
}