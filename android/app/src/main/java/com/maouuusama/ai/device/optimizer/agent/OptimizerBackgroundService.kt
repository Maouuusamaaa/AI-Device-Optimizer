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
import com.maouuusama.ai.device.optimizer.policy.DecisionHistoryRecorder
import com.maouuusama.ai.device.optimizer.policy.DeviceState
import com.maouuusama.ai.device.optimizer.policy.DryRunActionEngine
import com.maouuusama.ai.device.optimizer.policy.DryRunPolicyProposal
import com.maouuusama.ai.device.optimizer.policy.DecisionLogEntry
import com.maouuusama.ai.device.optimizer.policy.DecisionLogger
import com.maouuusama.ai.device.optimizer.policy.PersistentDecisionHistoryStore
import com.maouuusama.ai.device.optimizer.policy.PolicySimulationEvaluator
import com.maouuusama.ai.device.optimizer.policy.SafetyGateResult
import com.maouuusama.ai.device.optimizer.policy.SimulatedOptimizationPlan
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class OptimizerBackgroundService : Service() {
    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private lateinit var monitor: DeviceMonitor
    private lateinit var decisionLogger: DecisionLogger
    private lateinit var historyRecorder: DecisionHistoryRecorder
    private val simulationEvaluator = PolicySimulationEvaluator()
    private val actionEngine = DryRunActionEngine()

    override fun onCreate() {
        super.onCreate()
        monitor = DeviceMonitor(this)
        decisionLogger = DecisionLogger(this)
        historyRecorder = DecisionHistoryRecorder(
            PersistentDecisionHistoryStore(this)
        )
        createNotificationChannel()
        startAsForeground()
        scheduleMonitoring()
    }

    private fun startAsForeground() {
        val notification = buildNotification("Starting device monitoring")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else 0
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun scheduleMonitoring() {
        executor.scheduleWithFixedDelay({ collectAndEvaluate() }, 0L, SAMPLE_INTERVAL_MS, TimeUnit.MILLISECONDS)
    }

    private fun collectAndEvaluate() {
        try {
            val snapshot = monitor.collectSnapshot()
            val plan = simulationEvaluator.evaluate(snapshot)
            val proposal = DryRunPolicyProposal(
                state = DeviceState.fromSnapshot(snapshot),
                decisions = plan.simulation.decisions,
                actionExecutionAllowed = plan.simulation.executionAllowed
            )
            val safetyGateResult = plan.safetyGate
            val simulations = actionEngine.simulate(proposal, safetyGateResult)

            DeviceSnapshotJsonWriter.writeLatest(this, snapshot)
            saveLatest(snapshot, proposal, safetyGateResult)
            recordHistory(snapshot, plan, simulations)

            decisionLogger.append(
                DecisionLogEntry(
                    timestampMs = snapshot.timestampMs,
                    availableRamMb = snapshot.availableRamMb,
                    totalRamMb = snapshot.totalRamMb,
                    batteryPercent = snapshot.batteryPercent,
                    isCharging = snapshot.isCharging,
                    isGaming = proposal.state.isGaming,
                    policyIds = proposal.decisions.map { it.policyId },
                    proposedActionIds = proposal.decisions.mapNotNull { it.proposedActionId },
                    policyModes = proposal.decisions.map { it.mode.name },
                    safetyGateAllowed = safetyGateResult.allowed,
                    safetyGateReasons = safetyGateResult.reasons,
                    actionExecutionAllowed = proposal.actionExecutionAllowed
                )
            )
            updateNotification(formatStatus(snapshot, proposal, safetyGateResult))
        } catch (error: Exception) {
            Log.e(TAG, "Background monitoring failed", error)
            updateNotification("Monitoring error: " + error.javaClass.simpleName)
        }
    }

    private fun recordHistory(
        snapshot: DeviceSnapshot,
        plan: SimulatedOptimizationPlan,
        simulations: List<com.maouuusama.ai.device.optimizer.policy.ActionSimulation>
    ) {
        try {
            historyRecorder.record(snapshot, plan, simulations)
        } catch (error: Exception) {
            // History persistence must never authorize or execute an action, and a persistence
            // failure must not stop read-only device monitoring.
            Log.e(TAG, "Decision history persistence failed", error)
        }
    }

    private fun saveLatest(snapshot: DeviceSnapshot, proposal: DryRunPolicyProposal, safetyGateResult: SafetyGateResult) {
        val primary = proposal.decisions.firstOrNull()
        val telemetry = snapshot.systemTelemetry
        getSharedPreferences(PREFERENCES, MODE_PRIVATE).edit()
            .putLong("last_sample_timestamp_ms", snapshot.timestampMs)
            .putLong("last_available_ram_mb", snapshot.availableRamMb)
            .putLong("last_total_ram_mb", snapshot.totalRamMb)
            .putInt("last_process_count", snapshot.processes.size)
            .putString("last_top_process", snapshot.processes.firstOrNull()?.let { process ->
                (process.appLabels.firstOrNull() ?: process.processName) + " (" + process.pssKb + " KB PSS)"
            })
            .putString("last_system_telemetry_status", telemetry?.status?.name)
            .putString("last_system_telemetry_provider", telemetry?.provider)
            .putString("last_system_telemetry_error", telemetry?.errorMessage)
            .putInt("last_system_process_count", telemetry?.processes?.size ?: 0)
            .putLong("last_system_mem_available_kb", telemetry?.memory?.memAvailableKb ?: -1L)
            .putLong("last_system_swap_used_kb", telemetry?.memory?.swapUsedKb ?: -1L)
            .putFloat("last_system_cpu_utilization", telemetry?.cpu?.utilizationPercent?.toFloat() ?: -1f)
            .putString("last_policy_id", primary?.policyId)
            .putString("last_policy_severity", primary?.severity?.name)
            .putString("last_policy_mode", primary?.mode?.name)
            .putBoolean("last_action_execution_allowed", proposal.actionExecutionAllowed)
            .putBoolean("last_has_action_proposal", proposal.hasActionProposal)
            .putString("last_proposed_action_ids", proposal.decisions.mapNotNull { it.proposedActionId }.joinToString(","))
            .putBoolean("last_safety_gate_allowed", safetyGateResult.allowed)
            .putString("last_safety_gate_reasons", safetyGateResult.reasons.joinToString(" | "))
            .apply()
    }

    private fun formatStatus(snapshot: DeviceSnapshot, proposal: DryRunPolicyProposal, safetyGateResult: SafetyGateResult): String {
        val ratio = if (snapshot.totalRamMb > 0) snapshot.availableRamMb.toDouble() / snapshot.totalRamMb * 100.0 else 0.0
        val primary = proposal.decisions.firstOrNull()
        val policyId = primary?.policyId ?: "none"
        val topProcess = snapshot.processes.firstOrNull()?.let { process ->
            val label = process.appLabels.firstOrNull() ?: process.processName
            label + " " + (process.pssKb / 1024) + "MB"
        } ?: "no-process"
        val telemetry = snapshot.systemTelemetry
        val cpu = telemetry?.cpu?.utilizationPercent?.let { String.format("%.0f%%", it) } ?: "n/a"
        val systemStatus = telemetry?.status?.name ?: "NONE"
        val proposalState = if (proposal.hasActionProposal) "proposal" else "observe"
        val gateState = if (safetyGateResult.allowed) "gate-open" else "gate-blocked"
        return ("RAM %.0f%% • CPU %s • SYS %s • %s • %s • %s • top: %s • DRY_RUN").format(ratio, cpu, systemStatus, policyId, proposalState, gateState, topProcess)
    }

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun buildNotification(text: String): Notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_menu_info_details)
        .setContentTitle("AI Device Optimizer")
        .setContentText(text)
        .setOngoing(true)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Optimizer monitoring", NotificationManager.IMPORTANCE_LOW).apply {
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
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    companion object {
        private const val TAG = "OptimizerAgent"
        private const val CHANNEL_ID = "optimizer_monitoring"
        private const val NOTIFICATION_ID = 1001
        private const val PREFERENCES = "optimizer_agent"
        const val SAMPLE_INTERVAL_MS = 10_000L
    }
}
