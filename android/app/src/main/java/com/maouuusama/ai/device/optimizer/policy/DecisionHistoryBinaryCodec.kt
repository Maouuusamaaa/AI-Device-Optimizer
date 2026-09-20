package com.maouuusama.ai.device.optimizer.policy

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

/**
 * Versioned, platform-independent binary codec for measurement-only decision history.
 *
 * The explicit schema version prevents future code from silently reinterpreting old records.
 */
object DecisionHistoryBinaryCodec {
    private const val MAGIC = "AI_DEVICE_OPTIMIZER_HISTORY"
    const val SCHEMA_VERSION = 1

    fun encode(entries: List<DecisionHistoryEntry>): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { output ->
            output.writeUTF(MAGIC)
            output.writeInt(SCHEMA_VERSION)
            output.writeInt(entries.size)
            entries.forEach { writeEntry(output, it) }
        }
        return bytes.toByteArray()
    }

    fun decode(bytes: ByteArray): List<DecisionHistoryEntry> {
        DataInputStream(ByteArrayInputStream(bytes)).use { input ->
            require(input.readUTF() == MAGIC) { "Invalid decision history header." }
            require(input.readInt() == SCHEMA_VERSION) { "Unsupported decision history schema version." }
            val count = input.readCount()
            return buildList(count) { repeat(count) { add(readEntry(input)) } }
        }
    }

    private fun writeEntry(output: DataOutputStream, e: DecisionHistoryEntry) {
        output.writeLong(e.timestampMs)
        writeStrings(output, e.conditionIds)
        output.writeInt(e.diagnoses.size)
        e.diagnoses.forEach {
            output.writeUTF(it.conditionId)
            output.writeUTF(it.severity.name)
            output.writeDouble(it.confidence)
            writeStrings(output, it.evidence)
            writeNullableString(output, it.proposedActionId)
        }
        output.writeInt(e.decisions.size)
        e.decisions.forEach {
            output.writeUTF(it.policyId)
            output.writeUTF(it.severity.name)
            output.writeUTF(it.reason)
            writeNullableString(output, it.proposedActionId)
            output.writeUTF(it.mode.name)
        }
        output.writeInt(e.simulations.size)
        e.simulations.forEach {
            output.writeUTF(it.actionId)
            output.writeUTF(it.status.name)
            output.writeUTF(it.message)
            writeStrings(output, it.preconditionChecks)
            output.writeUTF(it.measurementPlan)
            output.writeUTF(it.rollbackPlan)
        }
        output.writeInt(e.measurementReports.size)
        e.measurementReports.forEach { report ->
            output.writeUTF(report.actionId)
            output.writeUTF(report.status.name)
            output.writeInt(report.deltas.size)
            report.deltas.forEach {
                output.writeUTF(it.metric)
                output.writeDouble(it.before)
                output.writeDouble(it.after)
                output.writeDouble(it.absoluteDelta)
                output.writeBoolean(it.percentDelta != null)
                it.percentDelta?.let(output::writeDouble)
            }
            output.writeUTF(report.interpretation)
        }
    }

    private fun readEntry(input: DataInputStream): DecisionHistoryEntry {
        val timestamp = input.readLong()
        val conditions = readStrings(input)
        val diagnoses = List(input.readCount()) {
            Diagnosis(
                conditionId = input.readUTF(),
                severity = enumValue(input.readUTF()),
                confidence = input.readDouble(),
                evidence = readStrings(input),
                proposedActionId = readNullableString(input)
            )
        }
        val decisions = List(input.readCount()) {
            PolicyDecision(
                policyId = input.readUTF(),
                severity = enumValue(input.readUTF()),
                reason = input.readUTF(),
                proposedActionId = readNullableString(input),
                mode = enumValue(input.readUTF())
            )
        }
        val simulations = List(input.readCount()) {
            ActionSimulation(
                actionId = input.readUTF(),
                status = enumValue(input.readUTF()),
                message = input.readUTF(),
                preconditionChecks = readStrings(input),
                measurementPlan = input.readUTF(),
                rollbackPlan = input.readUTF()
            )
        }
        val reports = List(input.readCount()) {
            PostActionMeasurementReport(
                actionId = input.readUTF(),
                status = enumValue(input.readUTF()),
                deltas = List(input.readCount()) {
                    MeasurementDelta(
                        metric = input.readUTF(),
                        before = input.readDouble(),
                        after = input.readDouble(),
                        absoluteDelta = input.readDouble(),
                        percentDelta = if (input.readBoolean()) input.readDouble() else null
                    )
                },
                interpretation = input.readUTF()
            )
        }
        return DecisionHistoryEntry(timestamp, conditions, diagnoses, decisions, simulations, reports)
    }

    private fun writeStrings(output: DataOutputStream, values: List<String>) {
        output.writeInt(values.size)
        values.forEach(output::writeUTF)
    }

    private fun readStrings(input: DataInputStream): List<String> =
        List(input.readCount()) { input.readUTF() }

    private fun writeNullableString(output: DataOutputStream, value: String?) {
        output.writeBoolean(value != null)
        value?.let(output::writeUTF)
    }

    private fun readNullableString(input: DataInputStream): String? =
        if (input.readBoolean()) input.readUTF() else null

    private fun DataInputStream.readCount(): Int {
        val count = readInt()
        require(count >= 0) { "Invalid collection size." }
        require(count <= MAX_COLLECTION_SIZE) { "Collection size exceeds safety limit." }
        return count
    }

    private inline fun <reified T : Enum<T>> enumValue(name: String): T =
        enumValues<T>().firstOrNull { it.name == name }
            ?: throw IllegalArgumentException("Unknown enum value: $name")

    private const val MAX_COLLECTION_SIZE = 10_000
}
