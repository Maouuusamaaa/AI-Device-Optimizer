package com.maouuusama.ai.device.optimizer.agent

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.maouuusama.ai.device.optimizer.monitor.DeviceMonitor
import com.maouuusama.ai.device.optimizer.monitor.DeviceSnapshot
import com.maouuusama.ai.device.optimizer.monitor.DeviceSnapshotJsonWriter
import com.maouuusama.ai.device.optimizer.policy.DeviceState
import com.maouuusama.ai.device.optimizer.policy.LocalPolicyEngine
import com.maouuusama.ai.device.optimizer.policy.PolicyDecision
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class OptimizerBackgroundService : Service() {

    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private lateinit var monitor: DeviceMonitor
    private val policyEngine = LocalPolicyEngine()

    override fun onCreate() {
        super.onCreate()
        monitor = DeviceMonitor(this)
        createNotificationChannel()
        startAsForeground()
        scheduleMonitoring()
    }

    private fun startAsForeground() {
        val notification = buildNotification("Starting device monitoring")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun scheduleMonitoring() {
        executor.scheduleWithFixedDelay(
            { collectAndEvaluate() },
            0L,
            SAMPLE_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        )
    }

    private fun collectAndEvaluate() {
        try {
            val snapshot = monitor.collectSnapshot()
            val state = DeviceState.fromSnapshot(snapshot)
            val decisions = policyEngine.evaluate(state)
            DeviceSnapshotJsonWriter.writeLatest(this, snapshot)
            saveLatest(snapshot, decisions)
            updateNotification(formatStatus(snapshot, decisions))
        } catch (error: Exception) {
            Log.e(TAG, "Background monitoring failed", error)
            updateNotification("Monitoring error: ${error.javaClass.simpleName}")
        }
    }

    private fun saveLatest(snapshot: DeviceSnapshot, decisions: List<PolicyDecision>) {
        val primary = decisions.firstOrNull()
        getSharedPreferences(PREFERENCES, MODE_PRIVATE)
            .edit()
            .putLong("last_sample_timestamp_ms", snapshot.timestampMs)
            .putLong("last_available_ram_mb", snapshot.availableRamMb)
            .putLong("last_total_ram_mb", snapshot.totalRamMb)
            .putInt("last_process_count", snapshot.processes.size)
            .putString(
                "last_top_process",
                snapshot.processes.firstOrNull()?.let { process ->
                    (process.appLabels.firstOrNull() ?: process.processName) +
                        " (" + process.pssKb + " KB PSS)"
                }
            )
            .putString("last_policy_id", primary?.policyId)
            .putString("last_policy_severity", primary?.severity?.name)
            .putString("last_policy_mode", primary?.mode?.name)
            .apply()
    }

    private fun formatStatus(
        snapshot: DeviceSnapshot,
        decisions: List<PolicyDecision>
    ): String {
        val ratio = if (snapshot.totalRamMb > 0) {
            snapshot.availableRamMb.toDouble() / snapshot.totalRamMb * 100.0
        } else {
            0.0
        }
        val primary = decisions.firstOrNull()
        val policyId = primary?.policyId ?: "none"
        val topProcess = snapshot.processes.firstOrNull()?.let { process ->
            val label = process.appLabels.firstOrNull() ?: process.processName
            label + " " + (process.pssKb / 1024) + "MB"
        } ?: "no-process"
        return "RAM %.0f%% • $policyId • top: $topProcess • dry-run".format(ratio)
    }
    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun buildNotification(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("AI Device Optimizer")
            .setContentText(text)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Optimizer monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows the current local monitoring status."
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        START_STICKY

    companion object {
        private const val TAG = "OptimizerAgent"
        private const val CHANNEL_ID = "optimizer_monitoring"
        private const val NOTIFICATION_ID = 1001
        private const val PREFERENCES = "optimizer_agent"
        const val SAMPLE_INTERVAL_MS = 10_000L
    }
}
