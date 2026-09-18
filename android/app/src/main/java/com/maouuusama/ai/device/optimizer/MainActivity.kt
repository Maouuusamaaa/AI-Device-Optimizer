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
import com.maouuusama.ai.device.optimizer.monitor.DeviceMonitor

class MainActivity : Activity() {

    private lateinit var statusText: TextView
    private lateinit var benchmarkButton: Button
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
        statusText.text = "\nBaseline starting...\nKeep the device in its current state."

        Thread {
            try {
                val benchmark = ReadOnlyBaselineBenchmark(this)
                val samples = benchmark.run(
                    sampleCount = ReadOnlyBaselineBenchmark.DEFAULT_SAMPLE_COUNT,
                    intervalMs = ReadOnlyBaselineBenchmark.DEFAULT_INTERVAL_MS
                ) { sample ->
                    val count = samplesProgress.getAndIncrement()
                    runOnUiThread {
                        statusText.text =
                            "\nBaseline running: " + count + "/30 samples\n" +
                                "RAM available: " + sample.availableRamMb + " MB\n" +
                                "Battery: " + (sample.batteryPercent ?: "unknown") + "%"
                    }
                }

                val report = BenchmarkReport.from(samples)
                val file = BenchmarkJsonWriter.write(
                    this,
                    workload = "monitor_foreground_idle",
                    samples = samples
                )

                runOnUiThread {
                    statusText.text = buildString {
                        append("\nBaseline complete\n")
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
                        append("Saved: ").append(file.name)
                    }
                    benchmarkButton.isEnabled = true
                    samplesProgress.set(1)
                }
            } catch (error: Exception) {
                runOnUiThread {
                    statusText.text = "\nBaseline failed: " + (error.message ?: error.javaClass.simpleName)
                    benchmarkButton.isEnabled = true
                    samplesProgress.set(1)
                }
            }
        }.start()
    }

    companion object {
        private const val REQUEST_NOTIFICATION_PERMISSION = 100
    }

    private val samplesProgress = java.util.concurrent.atomic.AtomicInteger(1)
}
