package com.maouuusama.ai.device.optimizer.benchmark

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import com.maouuusama.ai.device.optimizer.agent.OptimizerBackgroundService
import com.maouuusama.ai.device.optimizer.sync.EvidenceSyncManager
import org.json.JSONObject
import java.io.File
import java.util.UUID

object FreshProcessPairedLifecycleCoordinator {
    const val ACTION_START = "com.maouuusama.ai.device.optimizer.action.START_PAIRED_LIFECYCLE"
    const val ACTION_CONTINUE = "com.maouuusama.ai.device.optimizer.action.CONTINUE_PAIRED_LIFECYCLE"

    private const val PREFERENCES = "fresh_process_paired_lifecycle"
    private const val KEY_PAIR_ID = "pair_id"
    private const val KEY_RESET_FILE = "reset_file"
    private const val KEY_RESET_PID = "reset_pid"
    private const val KEY_RESET_START_TICKS = "reset_start_ticks"
    private const val KEY_RESET_TIMESTAMP = "reset_timestamp"
    private const val CONTINUE_DELAY_MS = 3_000L
    private const val MANIFEST_PREFIX = "runtime-lifecycle-pair-"

    fun start(context: Context) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_PAIR_ID, null) != null) return

        val pairId = UUID.randomUUID().toString()
        prefs.edit()
            .putString(KEY_PAIR_ID, pairId)
            .remove(KEY_RESET_FILE)
            .remove(KEY_RESET_PID)
            .remove(KEY_RESET_START_TICKS)
            .remove(KEY_RESET_TIMESTAMP)
            .apply()

        Thread {
            try {
                val result = RuntimeLifecycleMemoryBenchmark(appContext)
                    .run(resetEnabled = true)
                val file = RuntimeLifecycleMemoryBenchmarkJsonWriter.write(appContext, result)
                prefs.edit()
                    .putString(KEY_RESET_FILE, file.name)
                    .putInt(KEY_RESET_PID, result.processMetadata.pid)
                    .putLong(KEY_RESET_START_TICKS, result.processMetadata.processStartTimeTicks ?: -1L)
                    .putLong(KEY_RESET_TIMESTAMP, System.currentTimeMillis())
                    .apply()

                scheduleContinuation(appContext)
                android.os.Process.killProcess(android.os.Process.myPid())
            } catch (error: Exception) {
                prefs.edit().clear().apply()
                android.util.Log.e("FreshLifecyclePair", "Reset arm failed", error)
            }
        }.start()
    }

    fun continueAfterFreshProcess(context: Context) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val pairId = prefs.getString(KEY_PAIR_ID, null) ?: return
        val resetFile = prefs.getString(KEY_RESET_FILE, null) ?: return

        Thread {
            try {
                val result = RuntimeLifecycleMemoryBenchmark(appContext)
                    .run(resetEnabled = false)
                val controlFile = RuntimeLifecycleMemoryBenchmarkJsonWriter.write(appContext, result)

                val resetPid = prefs.getInt(KEY_RESET_PID, -1)
                val resetStartTicks = prefs.getLong(KEY_RESET_START_TICKS, -1L)
                val resetTimestamp = prefs.getLong(KEY_RESET_TIMESTAMP, -1L)
                val controlPid = result.processMetadata.pid
                val controlStartTicks = result.processMetadata.processStartTimeTicks ?: -1L
                val processSeparated = resetPid > 0 &&
                    controlPid > 0 &&
                    resetPid != controlPid &&
                    resetStartTicks > 0L &&
                    controlStartTicks > 0L &&
                    resetStartTicks != controlStartTicks

                writePairManifest(
                    context = appContext,
                    pairId = pairId,
                    resetFile = resetFile,
                    controlFile = controlFile.name,
                    resetPid = resetPid,
                    resetStartTicks = resetStartTicks,
                    resetTimestamp = resetTimestamp,
                    controlPid = controlPid,
                    controlStartTicks = controlStartTicks,
                    processSeparated = processSeparated
                )
                prefs.edit().clear().apply()
                stopServiceAndProcess(appContext)
            } catch (error: Exception) {
                android.util.Log.e("FreshLifecyclePair", "No-reset arm failed", error)
                stopServiceAndProcess(appContext)
            }
        }.start()
    }

    fun createActionPendingIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, OptimizerBackgroundService::class.java).setAction(action)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(context, action.hashCode(), intent, flags)
        } else {
            PendingIntent.getService(context, action.hashCode(), intent, flags)
        }
    }

    private fun scheduleContinuation(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = createActionPendingIntent(context, ACTION_CONTINUE)
        alarmManager?.set(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + CONTINUE_DELAY_MS,
            pendingIntent
        )
    }

    private fun writePairManifest(
        context: Context,
        pairId: String,
        resetFile: String,
        controlFile: String,
        resetPid: Int,
        resetStartTicks: Long,
        resetTimestamp: Long,
        controlPid: Int,
        controlStartTicks: Long,
        processSeparated: Boolean
    ) {
        val dir = File(context.getExternalFilesDir(null), "benchmarks").apply { mkdirs() }
        val file = File(dir, MANIFEST_PREFIX + pairId + ".json")
        val root = JSONObject()
            .put("schemaVersion", 1)
            .put("protocol", "fresh_process_paired_runtime_lifecycle")
            .put("pairId", pairId)
            .put("requiredPairs", 2)
            .put("freshProcessVerified", processSeparated)
            .put("resetArm", JSONObject()
                .put("mode", "reset_enabled")
                .put("file", resetFile)
                .put("pid", resetPid)
                .put("processStartTimeTicks", if (resetStartTicks > 0) resetStartTicks else JSONObject.NULL)
                .put("completedTimestampMs", resetTimestamp))
            .put("noResetArm", JSONObject()
                .put("mode", "no_reset_control")
                .put("file", controlFile)
                .put("pid", controlPid)
                .put("processStartTimeTicks", if (controlStartTicks > 0) controlStartTicks else JSONObject.NULL))
            .put("processSeparatedByPidAndStartTime", processSeparated)
            .put("interpretation", "PSS transitions are observational evidence; do not diagnose a memory leak from PSS alone.")
        file.writeText(root.toString(2))
        EvidenceSyncManager.enqueue(context, file)
    }

    private fun stopServiceAndProcess(context: Context) {
        context.stopService(Intent(context, OptimizerBackgroundService::class.java))
        android.os.Process.killProcess(android.os.Process.myPid())
    }
}
