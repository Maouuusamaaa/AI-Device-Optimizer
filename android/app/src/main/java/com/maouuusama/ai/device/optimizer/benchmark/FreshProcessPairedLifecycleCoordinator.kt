package com.maouuusama.ai.device.optimizer.benchmark

import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
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
    private const val KEY_STARTED_TIMESTAMP = "started_timestamp"
    private const val KEY_BATCH_ID = "batch_id"
    private const val KEY_COMPLETED_PAIRS = "completed_pairs"
    private const val KEY_NEXT_PAIR_PENDING = "next_pair_pending"
    private const val KEY_PAIR_INDEX = "pair_index"
    private const val REQUIRED_PAIRS = 2
    private const val CONTINUE_DELAY_MS = 3_000L
    private const val CONTINUE_DEADLINE_MS = 30_000L
    private const val CONTINUE_JOB_ID = 0xA1D0
    private const val CONTINUE_FALLBACK_JOB_ID = 0xA1D1
    private const val STALE_PAIR_TIMEOUT_MS = 30 * 60 * 1_000L
    private const val MANIFEST_PREFIX = "runtime-lifecycle-pair-"
    private const val SERVICE_CLASS =
        "com.maouuusama.ai.device.optimizer.agent.OptimizerBackgroundService"
    private const val STATUS_NOTIFICATION_ID = 1002
    private const val CHANNEL_ID = "optimizer_monitoring"

    @Synchronized
    fun start(context: Context) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        recoverStalePairIfNeeded(appContext, prefs)
        if (prefs.getString(KEY_PAIR_ID, null) != null) {
            notifyStatus(appContext, "Paired lifecycle already running", "An existing pair is still in progress.")
            return
        }

        val batchId = prefs.getString(KEY_BATCH_ID, null) ?: UUID.randomUUID().toString()
        val completedPairs = prefs.getInt(KEY_COMPLETED_PAIRS, 0)
        val pairIndex = completedPairs + 1
        val pairId = UUID.randomUUID().toString()
        prefs.edit()
            .putString(KEY_BATCH_ID, batchId)
            .putInt(KEY_PAIR_INDEX, pairIndex)
            .putString(KEY_PAIR_ID, pairId)
            .putLong(KEY_STARTED_TIMESTAMP, System.currentTimeMillis())
            .putBoolean(KEY_NEXT_PAIR_PENDING, false)
            .remove(KEY_RESET_FILE)
            .remove(KEY_RESET_PID)
            .remove(KEY_RESET_START_TICKS)
            .remove(KEY_RESET_TIMESTAMP)
            .apply()

        notifyStatus(appContext, "Paired lifecycle started", "Phase 1/2: reset-enabled arm is running. Keep the device available.")

        Thread {
            try {
                notifyStatus(appContext, "Reset arm running", "Collecting baseline, inference, cleanup, reset, and post-reset evidence.")
                val result = RuntimeLifecycleMemoryBenchmark(appContext)
                    .run(resetEnabled = true)
                val file = RuntimeLifecycleMemoryBenchmarkJsonWriter.write(appContext, result)
                notifyStatus(appContext, "Reset arm complete", "Evidence saved. Preparing a fresh-process continuation.")
                prefs.edit()
                    .putString(KEY_RESET_FILE, file.name)
                    .putInt(KEY_RESET_PID, result.processMetadata.pid)
                    .putLong(KEY_RESET_START_TICKS, result.processMetadata.processStartTimeTicks ?: -1L)
                    .putLong(KEY_RESET_TIMESTAMP, System.currentTimeMillis())
                    .apply()

                if (!scheduleContinuation(appContext)) {
                    notifyStatus(appContext, "Paired lifecycle stopped", "Continuation could not be scheduled; the process was not killed.")
                    android.util.Log.e(
                        "FreshLifecyclePair",
                        "Continuation scheduling failed; process will not be killed"
                    )
                    return@Thread
                }

                notifyStatus(appContext, "Fresh-process transition", "Continuation scheduled. The current process will now end and the control arm will resume in a new process.")
                android.os.Process.killProcess(android.os.Process.myPid())
            } catch (error: Exception) {
                prefs.edit().clear().apply()
                notifyStatus(appContext, "Paired lifecycle failed", "Reset arm failed: " + error.javaClass.simpleName)
                android.util.Log.e("FreshLifecyclePair", "Reset arm failed", error)
            }
        }.start()
    }

    @Synchronized
    fun startPendingNextPair(context: Context): Boolean {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_NEXT_PAIR_PENDING, false)) return false
        prefs.edit().putBoolean(KEY_NEXT_PAIR_PENDING, false).apply()
        notifyStatus(appContext, "Fresh process detected", "Starting the next independent lifecycle pair.")
        start(appContext)
        return true
    }

    @Synchronized
    fun continueAfterFreshProcess(
        context: Context,
        onFinished: (() -> Unit)? = null
    ) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val pairId = prefs.getString(KEY_PAIR_ID, null) ?: run {
            onFinished?.invoke()
            return
        }
        val resetFile = prefs.getString(KEY_RESET_FILE, null) ?: run {
            onFinished?.invoke()
            return
        }

        notifyStatus(appContext, "Fresh process detected", "Phase 2/2: no-reset control arm is running.")

        Thread {
            try {
                notifyStatus(appContext, "No-reset control running", "Collecting the paired control evidence in the fresh process.")
                val result = RuntimeLifecycleMemoryBenchmark(appContext)
                    .run(resetEnabled = false)
                val controlFile = RuntimeLifecycleMemoryBenchmarkJsonWriter.write(appContext, result)

                val resetPid = prefs.getInt(KEY_RESET_PID, -1)
                val resetStartTicks = prefs.getLong(KEY_RESET_START_TICKS, -1L)
                val resetTimestamp = prefs.getLong(KEY_RESET_TIMESTAMP, -1L)
                val pairIndex = prefs.getInt(KEY_PAIR_INDEX, 1)
                val batchId = prefs.getString(KEY_BATCH_ID, pairId) ?: pairId
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
                    batchId = batchId,
                    pairIndex = pairIndex,
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
                val completedPairs = pairIndex
                if (processSeparated && completedPairs < REQUIRED_PAIRS) {
                    prefs.edit()
                        .remove(KEY_PAIR_ID)
                        .remove(KEY_RESET_FILE)
                        .remove(KEY_RESET_PID)
                        .remove(KEY_RESET_START_TICKS)
                        .remove(KEY_RESET_TIMESTAMP)
                        .remove(KEY_STARTED_TIMESTAMP)
                        .remove(KEY_PAIR_INDEX)
                        .putString(KEY_BATCH_ID, batchId)
                        .putInt(KEY_COMPLETED_PAIRS, completedPairs)
                        .putBoolean(KEY_NEXT_PAIR_PENDING, true)
                        .apply()
                    appContext.getSystemService(JobScheduler::class.java)?.apply {
                        cancel(CONTINUE_JOB_ID)
                        cancel(CONTINUE_FALLBACK_JOB_ID)
                    }
                    // The optimizer foreground service remains started across this
                    // intentional process boundary. START_REDELIVER_INTENT on that service
                    // causes Android to recreate the service in the new process and redeliver
                    // ACTION_START, which starts the next pair from persisted batch state.
                    if (scheduleContinuation(appContext)) {
                        notifyStatus(
                            appContext,
                            "Pair $completedPairs/$REQUIRED_PAIRS complete",
                            "Fresh-process separation verified. The next independent pair is scheduled as a system fallback."
                        )
                    } else {
                        notifyStatus(
                            appContext,
                            "Pair $completedPairs/$REQUIRED_PAIRS complete",
                            "Fresh-process separation verified. Waiting for the optimizer service process restart to begin the next pair."
                        )
                    }
                    onFinished?.invoke()
                    killProcessOnly()
                } else {
                    prefs.edit().clear().apply()
                    appContext.getSystemService(JobScheduler::class.java)?.apply {
                        cancel(CONTINUE_JOB_ID)
                        cancel(CONTINUE_FALLBACK_JOB_ID)
                    }
                    notifyStatus(
                        appContext,
                        if (processSeparated) "Paired lifecycle complete" else "Pair complete — freshness check failed",
                        if (processSeparated) "All $REQUIRED_PAIRS independent pairs are saved. PID and process-start time differ within each pair." else "Both arms saved, but fresh-process separation was not verified."
                    )
                    onFinished?.invoke()
                    stopServiceAndProcess(appContext)
                }
            } catch (error: Exception) {
                notifyStatus(appContext, "Paired lifecycle failed", "No-reset control failed: " + error.javaClass.simpleName)
                android.util.Log.e("FreshLifecyclePair", "No-reset arm failed", error)
                onFinished?.invoke()
                stopServiceAndProcess(appContext)
            }
        }.start()
    }

    fun createActionPendingIntent(context: Context, action: String): PendingIntent {
        val intent = Intent()
            .setClassName(context, SERVICE_CLASS)
            .setAction(action)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(context, action.hashCode(), intent, flags)
        } else {
            PendingIntent.getService(context, action.hashCode(), intent, flags)
        }
    }

    private fun scheduleContinuation(context: Context): Boolean {
        val scheduler = context.getSystemService(JobScheduler::class.java) ?: return false
        scheduler.cancel(CONTINUE_JOB_ID)
        scheduler.cancel(CONTINUE_FALLBACK_JOB_ID)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                // Expedited jobs are intentionally not given a minimum latency.
                // Android restricts expedited JobInfo configuration; invalid
                // combinations can throw IllegalArgumentException during build.
                val expedited = JobInfo.Builder(
                    CONTINUE_JOB_ID,
                    ComponentName(context, FreshProcessPairedLifecycleJobService::class.java)
                )
                    .setExpedited(true)
                    .build()

                if (scheduler.schedule(expedited) == JobScheduler.RESULT_SUCCESS) {
                    return true
                }
            } catch (error: IllegalArgumentException) {
                android.util.Log.w(
                    "FreshLifecyclePair",
                    "Expedited continuation JobInfo was rejected; using regular fallback",
                    error
                )
            }
        }

        return try {
            val fallback = JobInfo.Builder(
                CONTINUE_FALLBACK_JOB_ID,
                ComponentName(context, FreshProcessPairedLifecycleJobService::class.java)
            )
                .setMinimumLatency(CONTINUE_DELAY_MS)
                .setOverrideDeadline(CONTINUE_DEADLINE_MS)
                .build()
            scheduler.schedule(fallback) == JobScheduler.RESULT_SUCCESS
        } catch (error: IllegalArgumentException) {
            android.util.Log.e(
                "FreshLifecyclePair",
                "Regular continuation JobInfo was rejected",
                error
            )
            false
        }
    }

    private fun recoverStalePairIfNeeded(context: Context, prefs: android.content.SharedPreferences) {
        val pairId = prefs.getString(KEY_PAIR_ID, null) ?: return
        val startedTimestamp = prefs.getLong(KEY_STARTED_TIMESTAMP, -1L)
        val ageMs = if (startedTimestamp > 0L) System.currentTimeMillis() - startedTimestamp else Long.MAX_VALUE
        if (ageMs <= STALE_PAIR_TIMEOUT_MS) return

        context.getSystemService(JobScheduler::class.java)?.apply {
            cancel(CONTINUE_JOB_ID)
            cancel(CONTINUE_FALLBACK_JOB_ID)
        }
        prefs.edit().clear().apply()
        notifyStatus(
            context,
            "Stale paired lifecycle recovered",
            "Previous pair $pairId exceeded the ${STALE_PAIR_TIMEOUT_MS / 60_000} minute timeout and was cleared. A new run can start safely."
        )
    }

    private fun writePairManifest(
        context: Context,
        batchId: String,
        pairIndex: Int,
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
            .put("batchId", batchId)
            .put("pairIndex", pairIndex)
            .put("pairId", pairId)
            .put("requiredPairs", REQUIRED_PAIRS)
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

    fun notifyStatus(context: Context, title: String, message: String) {
        val notification = NotificationCompat.Builder(context.applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setOngoing(
                title != "Paired lifecycle complete" &&
                    title != "Pair complete — freshness check failed" &&
                    title != "Paired lifecycle failed" &&
                    title != "Paired lifecycle stopped"
            )
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(STATUS_NOTIFICATION_ID, notification)
    }

    private fun killProcessOnly() {
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    private fun stopServiceAndProcess(context: Context) {
        context.stopService(Intent().setClassName(context, SERVICE_CLASS))
        killProcessOnly()
    }
}
