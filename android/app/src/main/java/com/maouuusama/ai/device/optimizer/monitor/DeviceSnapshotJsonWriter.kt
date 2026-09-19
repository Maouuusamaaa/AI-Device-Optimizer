package com.maouuusama.ai.device.optimizer.monitor

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object DeviceSnapshotJsonWriter {

    fun writeLatest(context: Context, snapshot: DeviceSnapshot): File {
        val root = JSONObject()
            .put("schemaVersion", 1)
            .put("timestampMs", snapshot.timestampMs)
            .put("androidApi", snapshot.androidApi)
            .put("manufacturer", snapshot.manufacturer)
            .put("model", snapshot.model)
            .put("totalRamMb", snapshot.totalRamMb)
            .put("availableRamMb", snapshot.availableRamMb)
            .put("batteryPercent", snapshot.batteryPercent ?: JSONObject.NULL)
            .put("isCharging", snapshot.isCharging)

        val processes = JSONArray()
        snapshot.processes.forEach { process ->
            processes.put(
                JSONObject()
                    .put("pid", process.pid)
                    .put("processName", process.processName)
                    .put("packageNames", JSONArray(process.packageNames))
                    .put("appLabels", JSONArray(process.appLabels))
                    .put("importance", process.importance)
                    .put("importanceLabel", process.importanceLabel)
                    .put("isForeground", process.isForeground)
                    .put("pssKb", process.pssKb)
                    .put("rssKb", process.rssKb ?: JSONObject.NULL)
                    .put("swapPssKb", process.swapPssKb)
            )
        }
        root.put("processes", processes)

        val directory = File(
            context.getExternalFilesDir(null),
            "telemetry"
        ).apply { mkdirs() }

        val file = File(directory, "latest-device-snapshot.json")
        file.writeText(root.toString(2))
        return file
    }
}
