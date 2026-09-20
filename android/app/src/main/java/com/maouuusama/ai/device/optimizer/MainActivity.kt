package com.maouuusama.ai.device.optimizer

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.maouuusama.ai.device.optimizer.agent.OptimizerBackgroundService
import com.maouuusama.ai.device.optimizer.benchmark.BenchmarkJsonWriter
import com.maouuusama.ai.device.optimizer.benchmark.BenchmarkReport
import com.maouuusama.ai.device.optimizer.benchmark.ReadOnlyBaselineBenchmark
import com.maouuusama.ai.device.optimizer.benchmark.WorkloadBenchmark
import com.maouuusama.ai.device.optimizer.monitor.DeviceMonitor
import com.maouuusama.ai.device.optimizer.monitor.ShizukuShell
import com.maouuusama.ai.device.optimizer.monitor.SystemTelemetrySnapshot
import com.maouuusama.ai.device.optimizer.monitor.SystemTelemetryStatus
import com.maouuusama.ai.device.optimizer.policy.AdaptiveLearningSummarizer
import com.maouuusama.ai.device.optimizer.policy.PersistentDecisionHistoryStore

class MainActivity : Activity() {
    private lateinit var statusText: TextView
    private lateinit var benchmarkButton: Button
    private lateinit var workloadButton: Button
    private lateinit var agentStatusText: TextView
    private lateinit var processText: TextView
    private lateinit var systemText: TextView
    private lateinit var learningText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val snapshot = DeviceMonitor(this).collectSnapshot(includeSystemTelemetry = false)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
        }

        root.addView(TextView(this).apply {
            textSize = 20f
            text = "AI Device Optimizer"
        })
        root.addView(TextView(this).apply {
            textSize = 17f
            text = buildString {
                append("Monitor prototype\n\n")
                append("Android API: ").append(snapshot.androidApi).append("\n")
                append("Device: ").append(snapshot.manufacturer).append(" ").append(snapshot.model).append("\n")
                append("RAM total: ").append(snapshot.totalRamMb).append(" MB\n")
                append("RAM available: ").append(snapshot.availableRamMb).append(" MB\n")
                append("Battery: ").append(snapshot.batteryPercent ?: "unknown").append("%\n")
                append("Charging: ").append(snapshot.isCharging).append("\n")
                append("Battery temperature: ").append(snapshot.batteryTemperatureC ?: "unknown").append(" °C\n")
                append("Thermal status: ").append(snapshot.thermalStatus ?: "unknown").append("\n")
                append("Storage free: ").append(formatStorage(snapshot.storageFreeBytes)).append(" / ").append(formatStorage(snapshot.storageTotalBytes)).append("\n")
                append("Network: ").append(snapshot.networkTransport ?: "offline").append("\n")
                append("Network validated: ").append(snapshot.networkValidated ?: "unknown").append("\n")
                append("Interactive: ").append(snapshot.isInteractive ?: "unknown")
            }
        })

        agentStatusText = TextView(this).apply {
            textSize = 16f
            text = "\nBackground agent: starting...\nRead-only telemetry + local dry-run policy."
        }
        root.addView(agentStatusText)

        root.addView(Button(this).apply {
            text = "Refresh detailed process telemetry"
            setOnClickListener { refreshProcessTelemetry() }
        })
        processText = TextView(this).apply {
            textSize = 14f
            text = "\nProcesses: not sampled yet"
        }
        root.addView(processText)

        root.addView(Button(this).apply {
            text = "Refresh system telemetry (Shizuku)"
            setOnClickListener { refreshSystemTelemetry() }
        })
        systemText = TextView(this).apply {
            textSize = 14f
            text = formatSystemTelemetry(snapshot.systemTelemetry)
        }
        root.addView(systemText)

        root.addView(Button(this).apply {
            text = "Refresh adaptive learning summary"
            setOnClickListener { refreshLearningSummary() }
        })
        learningText = TextView(this).apply {
            textSize = 14f
            text = "\\nAdaptive learning: not summarized yet"
        }
        root.addView(learningText)

        benchmarkButton = Button(this).apply {
            text = "Run 60s read-only baseline"
            setOnClickListener { runBaseline() }
        }
        root.addView(benchmarkButton)

        workloadButton = Button(this).apply {
            text = "Run 5min workload benchmark"
            setOnClickListener { runWorkloadBenchmark() }
        }
        root.addView(workloadButton)

        statusText = TextView(this).apply {
            textSize = 16f
            text = "\nBaseline: not run"
        }
        root.addView(statusText)

        setContentView(root)
        startBackgroundAgent()
    }

    private fun formatStorage(bytes: Long?): String {
        if (bytes == null || bytes < 0L) return "unknown"
        return String.format("%.2f GB", bytes / 1024.0 / 1024.0 / 1024.0)
    }

    private fun startBackgroundAgent() {
        val start = {
            ContextCompat.startForegroundService(
                this,
                Intent(this, OptimizerBackgroundService::class.java)
            )
            agentStatusText.text =
                "\nBackground agent: running\nSampling every 10 seconds.\nNo system mutations; policy mode is DRY_RUN."
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATION_PERMISSION)
        }
        start()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            agentStatusText.text =
                if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                    "\nBackground agent: running\nMonitoring notification enabled."
                } else {
                    "\nBackground agent: running\nNotification permission was not granted; monitoring remains read-only."
                }
        }
    }

    private fun refreshProcessTelemetry() {
        processText.text = "\nReading running processes..."
        Thread {
            try {
                val snapshot = DeviceMonitor(this).collectSnapshot()
                val text = buildString {
                    append("\nDetailed process telemetry\n")
                    append("RAM available: ").append(snapshot.availableRamMb)
                        .append("/").append(snapshot.totalRamMb).append(" MB\n")
                    append("Processes reported by Android: ").append(snapshot.processes.size).append("\n\n")
                    snapshot.processes.take(15).forEachIndexed { index, process ->
                        val label = process.appLabels.firstOrNull() ?: process.processName
                        append(index + 1).append(". ").append(label).append("\n")
                        append("   Process: ").append(process.processName).append("\n")
                        append("   PID: ").append(process.pid).append(" • ").append(process.importanceLabel).append("\n")
                        append("   PSS: ").append(process.pssKb / 1024)
                            .append(" MB • RSS: ")
                            .append(process.rssKb?.let { it / 1024 } ?: "unavailable")
                            .append(" MB • Swap PSS: ").append(process.swapPssKb / 1024).append(" MB\n")
                        append("   Package: ").append(process.packageNames.joinToString(", ")).append("\n\n")
                    }
                }
                runOnUiThread { processText.text = text }
            } catch (error: Exception) {
                runOnUiThread {
                    processText.text = "\nProcess telemetry failed: " +
                        (error.message ?: error.javaClass.simpleName)
                }
            }
        }.start()
    }

    private fun refreshLearningSummary() {
        learningText.text = "\\nAdaptive learning: reading persistent history..."
        Thread {
            try {
                val history = PersistentDecisionHistoryStore(this).snapshot()
                val summary = AdaptiveLearningSummarizer().summarize(history)
                val text = buildString {
                    append("\\nAdaptive learning summary\\n")
                    append("Observations: ").append(summary.observationCount).append("\\n")
                    append("Conditions:\\n")
                    if (summary.conditionCounts.isEmpty()) {
                        append("  none\\n")
                    } else {
                        summary.conditionCounts.forEach { (condition, count) ->
                            append("  ").append(condition).append(": ").append(count).append("\\n")
                        }
                    }
                    append("Action observations:\\n")
                    if (summary.actionStats.isEmpty()) {
                        append("  none\\n")
                    } else {
                        summary.actionStats.forEach { stats ->
                            append("  ").append(stats.actionId)
                                .append(": ").append(stats.observationCount).append("\\n")
                            stats.averageRamDeltaMb?.let {
                                append("    avg RAM delta: ").append(String.format("%.2f MB", it)).append("\\n")
                            }
                            stats.averageBatteryDeltaPercent?.let {
                                append("    avg battery delta: ").append(String.format("%.2f%%", it)).append("\\n")
                            }
                        }
                    }
                    append("Mode: descriptive-only; no learned action is authorized.")
                }
                runOnUiThread { learningText.text = text }
            } catch (error: Exception) {
                runOnUiThread {
                    learningText.text = "\\nAdaptive learning failed: " +
                        (error.message ?: error.javaClass.simpleName)
                }
            }
        }.start()
    }

    private fun refreshSystemTelemetry() {
        when (ShizukuShell.status()) {
            ShizukuShell.Status.PERMISSION_REQUIRED -> {
                systemText.text = "\nSystem telemetry: Shizuku permission required. Requesting permission..."
                ShizukuShell.requestPermission(REQUEST_SHIZUKU_PERMISSION)
            }
            ShizukuShell.Status.UNAVAILABLE -> {
                systemText.text =
                    "\nSystem telemetry: Shizuku unavailable. Android API telemetry remains active."
            }
            ShizukuShell.Status.AVAILABLE -> {
                systemText.text = "\nSystem telemetry: collecting..."
                Thread {
                    val telemetry = DeviceMonitor(this).collectSnapshot().systemTelemetry
                    runOnUiThread { systemText.text = formatSystemTelemetry(telemetry) }
                }.start()
            }
        }
    }

    private fun formatSystemTelemetry(telemetry: SystemTelemetrySnapshot?): String = buildString {
        append("\nSystem telemetry\n")
        if (telemetry == null) {
            append("Status: unavailable")
            return@buildString
        }
        append("Status: ").append(telemetry.status.name).append("\n")
        append("Provider: ").append(telemetry.provider).append("\n")
        telemetry.errorMessage?.let { append("Error: ").append(it).append("\n") }
        telemetry.memory?.let { memory ->
            append("MemAvailable: ").append(memory.memAvailableKb?.div(1024) ?: "unknown").append(" MB\n")
            append("Swap used: ").append(memory.swapUsedKb?.div(1024) ?: "unknown").append(" MB\n")
            append("Cached: ").append(memory.cachedKb?.div(1024) ?: "unknown").append(" MB\n")
        }
        telemetry.cpu?.utilizationPercent?.let {
            append("CPU utilization: ").append(String.format("%.1f%%", it)).append("\n")
        } ?: append("CPU utilization: warming up\n")
        append("System processes: ").append(telemetry.processes.size).append("\n")
        telemetry.processes.take(10).forEachIndexed { index, process ->
            append(index + 1).append(". ").append(process.processName)
                .append(" • PID ").append(process.pid)
                .append(" • PSS ").append(process.pssKb / 1024).append(" MB\n")
        }
        if (telemetry.status == SystemTelemetryStatus.PERMISSION_REQUIRED) {
            append("Tap refresh to request Shizuku permission.")
        }
    }

    private fun runBaseline() {
        benchmarkButton.isEnabled = false
        workloadButton.isEnabled = false
        statusText.text = "\nBaseline starting...\nKeep the device in its current state."
        Thread {
            try {
                val samples = ReadOnlyBaselineBenchmark(this).run(
                    sampleCount = ReadOnlyBaselineBenchmark.DEFAULT_SAMPLE_COUNT,
                    intervalMs = ReadOnlyBaselineBenchmark.DEFAULT_INTERVAL_MS
                ) { sample ->
                    runOnUiThread {
                        statusText.text = "\nBaseline running\nRAM available: " +
                            sample.availableRamMb + " MB\nBattery: " +
                            (sample.batteryPercent ?: "unknown") + "%"
                    }
                }
                val report = BenchmarkReport.from(samples)
                val file = BenchmarkJsonWriter.write(
                    this, workload = "monitor_foreground_idle", samples = samples, filePrefix = "baseline"
                )
                runOnUiThread {
                    statusText.text = formatReport("Baseline complete", report, file.name)
                    benchmarkButton.isEnabled = true
                    workloadButton.isEnabled = true
                }
            } catch (error: Exception) {
                runOnUiThread {
                    statusText.text = "\nBaseline failed: " + (error.message ?: error.javaClass.simpleName)
                    benchmarkButton.isEnabled = true
                    workloadButton.isEnabled = true
                }
            }
        }.start()
    }

    private fun runWorkloadBenchmark() {
        benchmarkButton.isEnabled = false
        workloadButton.isEnabled = false
        statusText.text =
            "\nWorkload benchmark preparing...\nYou have 3 seconds to switch to your game/app.\n" +
                "Run it normally for 5 minutes.\nNo system mutations; policy remains DRY_RUN."
        Thread {
            try {
                Thread.sleep(3_000L)
                val samples = WorkloadBenchmark(this).run(
                    durationMs = WorkloadBenchmark.DEFAULT_DURATION_MS,
                    intervalMs = WorkloadBenchmark.DEFAULT_INTERVAL_MS
                ) { sample, index, elapsedMs ->
                    runOnUiThread {
                        statusText.text = "\nWorkload benchmark running\nTime: " +
                            (elapsedMs / 1_000L) + "/" +
                            (WorkloadBenchmark.DEFAULT_DURATION_MS / 1_000L) + " s\nSamples: " +
                            index + "\nRAM available: " + sample.availableRamMb + " MB\nBattery: " +
                            (sample.batteryPercent ?: "unknown") + "%\nTemperature: " +
                            (sample.temperatureC?.let { String.format("%.1f°C", it) } ?: "unknown")
                    }
                }
                val report = BenchmarkReport.from(samples)
                val file = BenchmarkJsonWriter.write(
                    this, workload = "foreground_user_workload", samples = samples, filePrefix = "workload"
                )
                runOnUiThread {
                    statusText.text = formatReport("Workload benchmark complete", report, file.name)
                    benchmarkButton.isEnabled = true
                    workloadButton.isEnabled = true
                }
            } catch (error: Exception) {
                runOnUiThread {
                    statusText.text = "\nWorkload benchmark failed: " +
                        (error.message ?: error.javaClass.simpleName)
                    benchmarkButton.isEnabled = true
                    workloadButton.isEnabled = true
                }
            }
        }.start()
    }

    private fun formatReport(title: String, report: BenchmarkReport, fileName: String): String =
        buildString {
            append("\n").append(title).append("\n")
            append("Samples: ").append(report.sampleCount).append("\n")
            append("Duration: ").append(report.durationMs / 1000).append(" s\n")
            append("Average RAM available: ").append(report.averageAvailableRamMb).append(" MB\n")
            append("RAM range: ").append(report.minimumAvailableRamMb).append("–")
                .append(report.maximumAvailableRamMb).append(" MB\n")
            append("Average monitor collection: ")
                .append(String.format("%.2f", report.averageCollectionDurationMs)).append(" ms\n")
            append("Battery: ").append(report.startBatteryPercent ?: "unknown").append("% → ")
                .append(report.endBatteryPercent ?: "unknown").append("%\n")
            append("Temperature: ")
                .append(report.startTemperatureC?.let { String.format("%.1f°C", it) } ?: "unknown")
                .append(" → ")
                .append(report.endTemperatureC?.let { String.format("%.1f°C", it) } ?: "unknown")
                .append("\n")
            append("Saved: ").append(fileName)
        }

    companion object {
        private const val REQUEST_NOTIFICATION_PERMISSION = 100
        private const val REQUEST_SHIZUKU_PERMISSION = 101
    }
}
