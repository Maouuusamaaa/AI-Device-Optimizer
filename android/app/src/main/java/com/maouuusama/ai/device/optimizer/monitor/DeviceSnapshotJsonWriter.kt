package com.maouuusama.ai.device.optimizer.monitor

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object DeviceSnapshotJsonWriter {

    fun writeLatest(context: Context, snapshot: DeviceSnapshot): File {
        val root = JSONObject()
            .put("schemaVersion", 3)
            .put("timestampMs", snapshot.timestampMs)
            .put("androidApi", snapshot.androidApi)
            .put("manufacturer", snapshot.manufacturer)
            .put("model", snapshot.model)
            .put("totalRamMb", snapshot.totalRamMb)
            .put("availableRamMb", snapshot.availableRamMb)
            .put("batteryPercent", snapshot.batteryPercent ?: JSONObject.NULL)
            .put("isCharging", snapshot.isCharging)
            .put("batteryTemperatureC", snapshot.batteryTemperatureC ?: JSONObject.NULL)
            .put("thermalStatus", snapshot.thermalStatus ?: JSONObject.NULL)
            .put("storageTotalBytes", snapshot.storageTotalBytes ?: JSONObject.NULL)
            .put("storageFreeBytes", snapshot.storageFreeBytes ?: JSONObject.NULL)
            .put("networkTransport", snapshot.networkTransport ?: JSONObject.NULL)
            .put("networkValidated", snapshot.networkValidated ?: JSONObject.NULL)
            .put("isInteractive", snapshot.isInteractive ?: JSONObject.NULL)
            .put("uptimeMs", snapshot.uptimeMs ?: JSONObject.NULL)

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

        snapshot.systemTelemetry?.let { telemetry ->
            val system = JSONObject()
                .put("status", telemetry.status.name)
                .put("provider", telemetry.provider)
                .put("errorMessage", telemetry.errorMessage ?: JSONObject.NULL)

            telemetry.memory?.let { memory ->
                system.put("memory", JSONObject()
                    .put("memTotalKb", memory.memTotalKb ?: JSONObject.NULL)
                    .put("memFreeKb", memory.memFreeKb ?: JSONObject.NULL)
                    .put("memAvailableKb", memory.memAvailableKb ?: JSONObject.NULL)
                    .put("cachedKb", memory.cachedKb ?: JSONObject.NULL)
                    .put("swapTotalKb", memory.swapTotalKb ?: JSONObject.NULL)
                    .put("swapFreeKb", memory.swapFreeKb ?: JSONObject.NULL)
                    .put("swapUsedKb", memory.swapUsedKb ?: JSONObject.NULL)
                    .put("shmemKb", memory.shmemKb ?: JSONObject.NULL)
                    .put("sreclaimableKb", memory.sreclaimableKb ?: JSONObject.NULL))
            }

            telemetry.cpu?.let { cpu ->
                system.put("cpu", JSONObject()
                    .put("userJiffies", cpu.userJiffies)
                    .put("niceJiffies", cpu.niceJiffies)
                    .put("systemJiffies", cpu.systemJiffies)
                    .put("idleJiffies", cpu.idleJiffies)
                    .put("ioWaitJiffies", cpu.ioWaitJiffies)
                    .put("irqJiffies", cpu.irqJiffies)
                    .put("softIrqJiffies", cpu.softIrqJiffies)
                    .put("totalJiffies", cpu.totalJiffies)
                    .put("utilizationPercent", cpu.utilizationPercent ?: JSONObject.NULL))
            }

            val systemProcesses = JSONArray()
            telemetry.processes.forEach { process ->
                systemProcesses.put(
                    JSONObject()
                        .put("pid", process.pid)
                        .put("processName", process.processName)
                        .put("pssKb", process.pssKb)
                )
            }
            system.put("processes", systemProcesses)
            root.put("systemTelemetry", system)
        }

        val directory = File(context.getExternalFilesDir(null), "telemetry")
            .apply { mkdirs() }
        val file = File(directory, "latest-device-snapshot.json")
        file.writeText(root.toString(2))
        return file
    }
}
