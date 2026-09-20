package com.maouuusama.ai.device.optimizer.policy

import org.json.JSONArray
import org.json.JSONObject

object DecisionHistoryJsonCodec {
    const val SCHEMA_VERSION = 1

    fun encode(entries: List<DecisionHistoryEntry>): String = JSONObject()
        .put("schemaVersion", SCHEMA_VERSION)
        .put("entries", JSONArray().apply { entries.forEach { put(encodeEntry(it)) } })
        .toString(2)

    fun decode(json: String): List<DecisionHistoryEntry> {
        val root = JSONObject(json)
        require(root.optInt("schemaVersion", -1) == SCHEMA_VERSION) { "Unsupported decision history schema version." }
        val array = root.optJSONArray("entries") ?: throw IllegalArgumentException("Decision history entries are missing.")
        return buildList(array.length()) { for (index in 0 until array.length()) add(decodeEntry(array.getJSONObject(index))) }
    }

    private fun encodeEntry(e: DecisionHistoryEntry) = JSONObject()
        .put("timestampMs", e.timestampMs).put("conditionIds", JSONArray(e.conditionIds))
        .put("diagnoses", JSONArray().apply { e.diagnoses.forEach { put(JSONObject().put("conditionId", it.conditionId).put("severity", it.severity.name).put("confidence", it.confidence).put("evidence", JSONArray(it.evidence)).put("proposedActionId", it.proposedActionId ?: JSONObject.NULL)) } })
        .put("decisions", JSONArray().apply { e.decisions.forEach { put(JSONObject().put("policyId", it.policyId).put("severity", it.severity.name).put("reason", it.reason).put("proposedActionId", it.proposedActionId ?: JSONObject.NULL).put("mode", it.mode.name)) } })
        .put("simulations", JSONArray().apply { e.simulations.forEach { put(JSONObject().put("actionId", it.actionId).put("status", it.status.name).put("message", it.message).put("preconditionChecks", JSONArray(it.preconditionChecks)).put("measurementPlan", it.measurementPlan).put("rollbackPlan", it.rollbackPlan)) } })
        .put("measurementReports", JSONArray().apply { e.measurementReports.forEach { r -> put(JSONObject().put("actionId", r.actionId).put("status", r.status.name).put("deltas", JSONArray().apply { r.deltas.forEach { d -> put(JSONObject().put("metric", d.metric).put("before", d.before).put("after", d.after).put("absoluteDelta", d.absoluteDelta).put("percentDelta", d.percentDelta ?: JSONObject.NULL)) } }).put("interpretation", r.interpretation)) } })

    private fun decodeEntry(v: JSONObject) = DecisionHistoryEntry(
        v.getLong("timestampMs"), v.getJSONArray("conditionIds").strings(),
        v.getJSONArray("diagnoses").objects().map { Diagnosis(it.getString("conditionId"), enumValue(it.getString("severity")), it.getDouble("confidence"), it.getJSONArray("evidence").strings(), it.optionalString("proposedActionId")) },
        v.getJSONArray("decisions").objects().map { PolicyDecision(it.getString("policyId"), enumValue(it.getString("severity")), it.getString("reason"), it.optionalString("proposedActionId"), enumValue(it.getString("mode"))) },
        v.getJSONArray("simulations").objects().map { ActionSimulation(it.getString("actionId"), enumValue(it.getString("status")), it.getString("message"), it.getJSONArray("preconditionChecks").strings(), it.getString("measurementPlan"), it.getString("rollbackPlan")) },
        v.getJSONArray("measurementReports").objects().map { r -> PostActionMeasurementReport(r.getString("actionId"), enumValue(r.getString("status")), r.getJSONArray("deltas").objects().map { d -> MeasurementDelta(d.getString("metric"), d.getDouble("before"), d.getDouble("after"), d.getDouble("absoluteDelta"), d.optionalDouble("percentDelta")) }, r.getString("interpretation")) }
    )

    private inline fun <reified T : Enum<T>> enumValue(name: String): T = enumValues<T>().firstOrNull { it.name == name } ?: throw IllegalArgumentException("Unknown enum value: $name")
    private fun JSONArray.strings() = buildList<String>(length()) { for (i in 0 until length()) add(getString(i)) }
    private fun JSONArray.objects() = buildList<JSONObject>(length()) { for (i in 0 until length()) add(getJSONObject(i)) }
    private fun JSONObject.optionalString(key: String): String? = if (isNull(key)) null else getString(key)
    private fun JSONObject.optionalDouble(key: String): Double? = if (isNull(key)) null else getDouble(key)
}