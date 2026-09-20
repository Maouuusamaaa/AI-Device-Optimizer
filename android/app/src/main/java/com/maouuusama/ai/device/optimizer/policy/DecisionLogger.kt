package com.maouuusama.ai.device.optimizer.policy

import android.content.Context
import java.io.File
import java.nio.charset.StandardCharsets

class DecisionLogger(
    context: Context,
    private val maxBytes: Long = DEFAULT_MAX_BYTES
) {
    private val logFile = File(context.filesDir, LOG_FILE_NAME)

    init {
        require(maxBytes >= MIN_MAX_BYTES) {
            "Decision log maxBytes must be at least $MIN_MAX_BYTES."
        }
    }

    fun append(entry: DecisionLogEntry) {
        val line = DecisionLogFormatter.toJson(entry) + "\n"
        val bytes = line.toByteArray(StandardCharsets.UTF_8)
        if (logFile.exists() && logFile.length() + bytes.size > maxBytes) {
            logFile.delete()
        }
        logFile.appendBytes(bytes)
    }

    fun readAll(): List<DecisionLogEntry> =
        if (!logFile.exists()) emptyList()
        else logFile.readLines(StandardCharsets.UTF_8)
            .filter { it.isNotBlank() }
            .map(DecisionLogFormatter::fromJson)

    fun file(): File = logFile

    companion object {
        const val LOG_FILE_NAME = "decision-log.jsonl"
        const val DEFAULT_MAX_BYTES = 256L * 1024L
        const val MIN_MAX_BYTES = 4L * 1024L
    }
}
