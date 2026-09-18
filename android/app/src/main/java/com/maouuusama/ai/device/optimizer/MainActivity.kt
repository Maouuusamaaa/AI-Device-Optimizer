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

class MainActivity : Activity() {

    private lateinit var statusText: TextView
    private lateinit var benchmarkButton: Button
    private lateinit var workloadButton: Button
    private lateinit var agentStatusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val monitor = DeviceMonitor(this)
        val snapshot = monitor.collectSnapshot()

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
                append("Device: ").append(snapshot.manufacturer)
                    .append(" ").append(snapshot.model).append("\n")
                append("RAM total: ").append(snapshot.totalRamMb).append(" MB\n")
                append("RAM available: ").append(snapshot.availableRamMb).append(" MB\n")
                append("Battery: ").append(snapshot.batteryPercent ?: "unknown").append("%\n")
                append("Charging: ").append(snapshot.isCharging)
            }
        })

        agentStatusText = TextView(this).apply {
            textSize = 16f
            text = "\nBackground agent: starting...\nRead-only telemetry + local dry-run policy."
        }
        root.addView(agentStatusText)

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

    private fun startBackgroundAgent() {
        val start = {
            val intent = Intent(this, OptimizerBackgroundService::class.java)
            ContextCompat.startForegroundService(this, intent)
            agentStatusText.text =
                "\nBackground agent: running\nSampling every 10 seconds.\nNo system mutations; policy mode is DRY_RUN."
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATION_PERMISSION
            )
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

    private fun runBaseline() {
        benchmarkButton.isEnabled = false
        workloadButton.isEnabled = false
        statusText.text = "\nBaseline starting...\nKeep the device in its current state."

        Thread {
            try {
                val benchmark = ReadOnlyBaselineBenchmark(this)
                val samples = benchmark.run(
                    sampleCount = ReadOnlyBaselineBenchmark.DEFAULT_SAMPLE_COUNT,
                    intervalMs = ReadOnlyBaselineBenchmark.DEFAULT_INTERVAL_MS
                ) { sample ->
                    runOnUiThread {
                        statusText.text =
                            "\nBaseline running\nRAM available: " + sample.availableRamMb +
                                " MB\nBattery: " + (sample.batteryPercent ?: "unknown") + "%"
                    }
                }

                val report = BenchmarkReport.from(samples)
                val file = BenchmarkJsonWriter.write(
                    this,
                    workload = "monitor_foreground_idle",
                    samples = samples,
                    filePrefix = "baseline"
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

                val benchmark = WorkloadBenchmark(this)
                val samples = benchmark.run(
                    durationMs = WorkloadBenchmark.DEFAULT_DURATION_MS,
                    intervalMs = WorkloadBenchmark.DEFAULT_INTERVAL_MS
                ) { sample, index, elapsedMs ->
                    val elapsedSeconds = elapsedMs / 1_000L
                    val totalSeconds = WorkloadBenchmark.DEFAULT_DURATION_MS / 1_000L
                    runOnUiThread {
                        statusText.text =
                            "\nWorkload benchmark running\n" +
                                "Time: " + elapsedSeconds + "/" + totalSeconds + " s\n" +
                                "Samples: " + index + "\n" +
                                "RAM available: " + sample.availableRamMb + " MB\n" +
                                "Battery: " + (sample.batteryPercent ?: "unknown") + "%\n" +
                                "Temperature: " + (sample.temperatureC?.let { String.format("%.1f°C", it) } ?: "unknown")
                    }
                }

                val report = BenchmarkReport.from(samples)
                val file = BenchmarkJsonWriter.write(
                    this,
                    workload = "foreground_user_workload",
                    samples = samples,
                    filePrefix = "workload"
                )

                runOnUiThread {
                    statusText.text = formatReport("Workload benchmark complete", report, file.name)
                    benchmarkButton.isEnabled = true
                    workloadButton.isEnabled = true
                }
            } catch (error: Exception) {
                runOnUiThread {
                    statusText.text =
                        "\nWorkload benchmark failed: " +
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
            append("Average RAM available: ")
                .append(report.averageAvailableRamMb).append(" MB\n")
            append("RAM range: ")
                .append(report.minimumAvailableRamMb)
                .append("–")
                .append(report.maximumAvailableRamMb)
                .append(" MB\n")
            append("Average monitor collection: ")
                .append(String.format("%.2f", report.averageCollectionDurationMs))
                .append(" ms\n")
            append("Battery: ")
                .append(report.startBatteryPercent ?: "unknown")
                .append("% → ")
                .append(report.endBatteryPercent ?: "unknown")
                .append("%\n")
            append("Temperature: ")
                .append(report.startTemperatureC?.let { String.format("%.1f°C", it) } ?: "unknown")
                .append(" → ")
                .append(report.endTemperatureC?.let { String.format("%.1f°C", it) } ?: "unknown")
                .append("\n")
            append("Saved: ").append(fileName)
        }

    companion object {
        private const val REQUEST_NOTIFICATION_PERMISSION = 100
    }
}
