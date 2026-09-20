package com.maouuusama.ai.device.optimizer.policy

import android.content.Context
import java.io.File

class AdaptiveLearningReportWriter(private val context: Context) {
    fun write(report: AdaptiveLearningReport): File {
        val target = File(context.filesDir, FILE_NAME)
        val temporary = File(context.filesDir, "$FILE_NAME.tmp")
        temporary.writeText(report.toJson(), Charsets.UTF_8)
        if (target.exists() && !target.delete()) {
            temporary.delete()
            throw IllegalStateException("Unable to replace adaptive learning report.")
        }
        if (!temporary.renameTo(target)) {
            temporary.delete()
            throw IllegalStateException("Unable to commit adaptive learning report.")
        }
        return target
    }

    companion object {
        const val FILE_NAME = "adaptive-learning-summary.json"
    }
}
