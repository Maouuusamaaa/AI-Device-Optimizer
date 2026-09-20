package com.maouuusama.ai.device.optimizer.policy

import org.json.JSONArray
import org.json.JSONObject

object DecisionLogFormatter {
    fun toJson(entry: DecisionLogEntry): String =
        JSONObject().apply {
            put("timestampMs", entry.timestampMs)
            put("availableRamMb", entry.availableRamMb)
            put("totalRamMb", entry.totalRamMb)
            if (entry.batteryPercent == null) put("batteryPercent", JSONObject.NULL)
            else put("batteryPercent", entry.batteryPercent)
            put("isCharging", entry.isCharging)
            put("isGaming", entry.isGaming)
            put("policyIds", JSONArray(entry.policyIds))
            put("proposedActionIds", JSONArray(entry.proposedActionIds))
            put("policyModes", JSONArray(entry.policyModes))
            put("safetyGateAllowed", entry.safetyGateAllowed)
            put("safetyGateReasons", JSONArray(entry.safetyGateReasons))
            put("actionExecutionAllowed", entry.actionExecutionAllowed)
        }.toString()

    fun fromJson(json: String): DecisionLogEntry {
        val objectValue = JSONObject(json)
        return DecisionLogEntry(
            timestampMs = objectValue.getLong("timestampMs"),
            availableRamMb = objectValue.getLong("availableRamMb"),
            totalRamMb = objectValue.getLong("totalRamMb"),
            batteryPercent = if (objectValue.isNull("batteryPercent")) null else objectValue.getInt("batteryPercent"),
            isCharging = objectValue.getBoolean("isCharging"),
            isGaming = objectValue.getBoolean("isGaming"),
            policyIds = objectValue.getJSONArray("policyIds").toStringList(),
            proposedActionIds = objectValue.getJSONArray("proposedActionIds").toStringList(),
            policyModes = objectValue.getJSONArray("policyModes").toStringList(),
            safetyGateAllowed = objectValue.getBoolean("safetyGateAllowed"),
            safetyGateReasons = objectValue.getJSONArray("safetyGateReasons").toStringList(),
            actionExecutionAllowed = objectValue.getBoolean("actionExecutionAllowed")
        )
    }

    private fun JSONArray.toStringList(): List<String> =
        List(length()) { index -> getString(index) }
}
