package com.maouuusama.ai.device.optimizer.policy

data class AdaptiveLearningReport(
    val generatedAtMs: Long,
    val summary: AdaptiveLearningSummary
) {
    fun toJson(): String = buildString {
        append("{")
        append("\"generatedAtMs\":").append(generatedAtMs).append(",")
        append("\"observationCount\":").append(summary.observationCount).append(",")
        append("\"conditionCounts\":{")
        summary.conditionCounts.entries.forEachIndexed { index, entry ->
            if (index > 0) append(",")
            append(jsonString(entry.key)).append(":").append(entry.value)
        }
        append("},")
        append("\"actionStats\":[")
        summary.actionStats.forEachIndexed { index, stats ->
            if (index > 0) append(",")
            append("{")
            append("\"actionId\":").append(jsonString(stats.actionId)).append(",")
            append("\"observationCount\":").append(stats.observationCount).append(",")
            append("\"averageRamDeltaMb\":").append(numberOrNull(stats.averageRamDeltaMb)).append(",")
            append("\"averageBatteryDeltaPercent\":").append(numberOrNull(stats.averageBatteryDeltaPercent))
            append("}")
        }
        append("],")
        append("\"interpretation\":").append(jsonString(summary.interpretation))
        append("}")
    }

    private fun jsonString(value: String): String =
        "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""

    private fun numberOrNull(value: Double?): String =
        value?.takeIf { it.isFinite() }?.toString() ?: "null"
}
