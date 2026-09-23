package com.maouuusama.ai.device.optimizer

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.maouuusama.ai.device.optimizer.agent.OptimizerBackgroundService
import com.maouuusama.ai.device.optimizer.benchmark.BenchmarkJsonWriter
import com.maouuusama.ai.device.optimizer.benchmark.BenchmarkReport
import com.maouuusama.ai.device.optimizer.benchmark.ReadOnlyBaselineBenchmark
import com.maouuusama.ai.device.optimizer.benchmark.WorkloadBenchmark
import com.maouuusama.ai.device.optimizer.benchmark.WorkloadRecoveryBenchmark
import com.maouuusama.ai.device.optimizer.benchmark.WorkloadRecoveryBenchmarkJsonWriter
import com.maouuusama.ai.device.optimizer.benchmark.LocalInferenceBenchmark
import com.maouuusama.ai.device.optimizer.benchmark.LocalInferenceBenchmarkJsonWriter
import com.maouuusama.ai.device.optimizer.monitor.DeviceMonitor
import com.maouuusama.ai.device.optimizer.sync.GitHubSyncConfig
import com.maouuusama.ai.device.optimizer.sync.EvidenceSyncManager
import com.maouuusama.ai.device.optimizer.sync.EvidenceSyncScheduler
import com.maouuusama.ai.device.optimizer.sync.GitHubEvidenceClient
import com.maouuusama.ai.device.optimizer.sync.GitHubTokenStore
import com.maouuusama.ai.device.optimizer.monitor.ShizukuShell
import com.maouuusama.ai.device.optimizer.monitor.SystemTelemetrySnapshot
import com.maouuusama.ai.device.optimizer.monitor.SystemTelemetryStatus
import com.maouuusama.ai.device.optimizer.policy.AdaptiveLearningSummarizer
import com.maouuusama.ai.device.optimizer.policy.PersistentDecisionHistoryStore
import com.maouuusama.ai.device.optimizer.localai.LocalLlamaRuntime
import com.maouuusama.ai.device.optimizer.localai.QwenLocalModel
import com.maouuusama.ai.device.optimizer.localai.QwenModelDownloader

class MainActivity : Activity() {
    private lateinit var statusText: TextView
    private lateinit var benchmarkButton: Button
    private lateinit var workloadButton: Button
    private lateinit var recoveryButton: Button
    private lateinit var agentStatusText: TextView
    private lateinit var processText: TextView
    private lateinit var systemText: TextView
    private lateinit var learningText: TextView
    private lateinit var localAiText: TextView
    private lateinit var localAiDownloadButton: Button
    private lateinit var localAiRunButton: Button
    private lateinit var localAiBenchmarkButton: Button
    private lateinit var evidenceSyncText: TextView
    private lateinit var evidenceSyncRepository: EditText
    private lateinit var evidenceSyncBranch: EditText
    private lateinit var evidenceSyncToken: EditText

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
            text = "\nAdaptive learning: not summarized yet"
        }
        root.addView(learningText)

        root.addView(TextView(this).apply {
            textSize = 18f
            text = "\nGitHub evidence sync"
        })
        root.addView(TextView(this).apply {
            textSize = 13f
            text = "Benchmark JSON is validated, hashed, queued locally, and uploaded serially when network access is available. The token is stored encrypted with Android Keystore."
        })
        evidenceSyncRepository = EditText(this).apply {
            hint = "GitHub repository (owner/name)"
            setText(GitHubSyncConfig(this@MainActivity).repository)
            singleLine = true
        }
        root.addView(evidenceSyncRepository)
        evidenceSyncBranch = EditText(this).apply {
            hint = "Branch"
            setText(GitHubSyncConfig(this@MainActivity).branch)
            singleLine = true
        }
        root.addView(evidenceSyncBranch)
        evidenceSyncToken = EditText(this).apply {
            hint = "Fine-grained GitHub token (leave blank to keep stored token)"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            singleLine = true
        }
        root.addView(evidenceSyncToken)

        root.addView(Button(this).apply {
            text = "Save GitHub sync settings"
            setOnClickListener { saveEvidenceSyncSettings() }
        })
        root.addView(Button(this).apply {
            text = "Test GitHub connection + enable sync"
            setOnClickListener { testEvidenceSyncConnection() }
        })
        root.addView(Button(this).apply {
            text = "Retry pending evidence uploads"
            setOnClickListener {
                EvidenceSyncScheduler.enqueue(this@MainActivity)
                refreshEvidenceSyncStatus()
            }
        })
        evidenceSyncText = TextView(this).apply {
            textSize = 14f
            text = "\nGitHub sync: checking queue..."
        }
        root.addView(evidenceSyncText)
        refreshEvidenceSyncStatus()

        localAiText = TextView(this).apply {
            textSize = 14f
            text = "\nLocal AI: runtime loading..."
        }
        root.addView(localAiText)

        localAiDownloadButton = Button(this).apply {
            text = "Download / verify Qwen3 0.6B"
            setOnClickListener { downloadLocalModel() }
        }
        root.addView(localAiDownloadButton)

        localAiRunButton = Button(this).apply {
            text = "Run local AI advisory test"
            isEnabled = false
            setOnClickListener { runLocalAiTest() }
        }
        root.addView(localAiRunButton)

        localAiBenchmarkButton = Button(this).apply {
            text = "Run Stage 9 inference benchmark (2 vs 4 threads)"
            isEnabled = false
            setOnClickListener { runLocalAiBenchmark() }
        }
        root.addView(localAiBenchmarkButton)

        refreshLocalAiStatus()

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

        recoveryButton = Button(this).apply {
            text = "Run workload → recovery memory benchmark"
            setOnClickListener { runWorkloadRecoveryBenchmark() }
        }
        root.addView(recoveryButton)

        statusText = TextView(this).apply {
            textSize = 16f
            text = "\nBaseline: not run"
        }
        root.addView(statusText)

        val scrollView = ScrollView(this).apply {
            isFillViewport = true
            addView(root)
        }
        setContentView(scrollView)
        EvidenceSyncScheduler.enqueue(this)
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
        learningText.text = "\nAdaptive learning: reading persistent history..."
        Thread {
            try {
                val history = PersistentDecisionHistoryStore(this).snapshot()
                val summary = AdaptiveLearningSummarizer().summarize(history)
                val text = buildString {
                    append("\nAdaptive learning summary\n")
                    append("Observations: ").append(summary.observationCount).append("\n")
                    append("Conditions:\n")
                    if (summary.conditionCounts.isEmpty()) {
                        append("  none\n")
                    } else {
                        summary.conditionCounts.forEach { (condition, count) ->
                            append("  ").append(condition).append(": ").append(count).append("\n")
                        }
                    }
                    append("Action observations:\n")
                    if (summary.actionStats.isEmpty()) {
                        append("  none\n")
                    } else {
                        summary.actionStats.forEach { stats ->
                            append("  ").append(stats.actionId)
                                .append(": ").append(stats.observationCount).append("\n")
                            stats.averageRamDeltaMb?.let {
                                append("    avg RAM delta: ").append(String.format("%.2f MB", it)).append("\n")
                            }
                            stats.averageBatteryDeltaPercent?.let {
                                append("    avg battery delta: ").append(String.format("%.2f%%", it)).append("\n")
                            }
                        }
                    }
                    append("Mode: descriptive-only; no learned action is authorized.")
                }
                runOnUiThread { learningText.text = text }
            } catch (error: Exception) {
                runOnUiThread {
                    learningText.text = "\nAdaptive learning failed: " +
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

    private fun refreshLocalAiStatus() {
        Thread {
            try {
                val downloader = QwenModelDownloader(this)
                val modelInstalled = downloader.isInstalled()
                val runtime = LocalLlamaRuntime()
                val version = runCatching { runtime.runtimeVersion() }.getOrElse { "load failed" }
                val status = if (modelInstalled) "model verified" else "model not installed"
                runOnUiThread {
                    localAiText.text =
                        "\nLocal AI runtime\n" +
                            "llama.cpp: " + version + "\n" +
                            "Qwen3 0.6B Q4_0: " + status + "\n" +
                            "Mode: advisory-only; no device mutation."
                    localAiRunButton.isEnabled = modelInstalled
                    localAiBenchmarkButton.isEnabled = modelInstalled
                }
            } catch (error: Exception) {
                runOnUiThread {
                    localAiText.text = "\nLocal AI unavailable: " +
                        (error.message ?: error.javaClass.simpleName)
                    localAiRunButton.isEnabled = false
                    localAiBenchmarkButton.isEnabled = false
                }
            }
        }.start()
    }

    private fun downloadLocalModel() {
        localAiDownloadButton.isEnabled = false
        localAiRunButton.isEnabled = false
        localAiText.text = "\nQwen3 download starting...\nRequired model size: 429 MiB.\nThe APK does not bundle the model."
        Thread {
            try {
                val downloader = QwenModelDownloader(this)
                check(downloader.hasEnoughStorage()) { "Insufficient free storage for the verified model" }
                downloader.download { downloaded, total ->
                    val percent = if (total > 0L) downloaded * 100L / total else 0L
                    runOnUiThread {
                        localAiText.text = "\nQwen3 download: " + percent + "%\nDownloaded: " +
                            (downloaded / 1024 / 1024) + " / " + (total / 1024 / 1024) + " MiB"
                    }
                }
                runOnUiThread {
                    localAiText.text = "\nQwen3 model verified successfully.\nSHA-256 matches the pinned manifest.\nMode: advisory-only."
                    localAiDownloadButton.isEnabled = true
                    localAiRunButton.isEnabled = true
                    localAiBenchmarkButton.isEnabled = true
                }
            } catch (error: Exception) {
                runOnUiThread {
                    localAiText.text = "\nQwen3 download/verification failed: " +
                        (error.message ?: error.javaClass.simpleName)
                    localAiDownloadButton.isEnabled = true
                    localAiRunButton.isEnabled = false
                    localAiBenchmarkButton.isEnabled = false
                }
            }
        }.start()
    }

    private fun saveEvidenceSyncSettings() {
        val config = GitHubSyncConfig(this)
        val repository = evidenceSyncRepository.text.toString().trim()
        val branch = evidenceSyncBranch.text.toString().trim()
        if (!repository.matches(Regex("^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$"))) {
            evidenceSyncText.text = "\nGitHub sync: invalid repository. Use owner/name."
            return
        }
        if (!branch.matches(Regex("^[A-Za-z0-9._/-]+$"))) {
            evidenceSyncText.text = "\nGitHub sync: invalid branch."
            return
        }
        config.repository = repository
        config.branch = branch
        val token = evidenceSyncToken.text.toString().trim()
        if (token.isNotBlank()) {
            GitHubTokenStore(this).save(token)
            evidenceSyncToken.text?.clear()
        }
        config.enabled = GitHubTokenStore(this).hasToken()
        EvidenceSyncScheduler.enqueue(this)
        refreshEvidenceSyncStatus()
    }

    private fun testEvidenceSyncConnection() {
        val config = GitHubSyncConfig(this)
        val repository = evidenceSyncRepository.text.toString().trim()
        val branch = evidenceSyncBranch.text.toString().trim()
        val enteredToken = evidenceSyncToken.text.toString().trim()
        val storedToken = GitHubTokenStore(this).read()
        val token = if (enteredToken.isNotBlank()) enteredToken else storedToken
        if (token.isNullOrBlank()) {
            evidenceSyncText.text = "\nGitHub sync: token required for the first connection test."
            return
        }
        if (!repository.matches(Regex("^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$"))) {
            evidenceSyncText.text = "\nGitHub sync: invalid repository."
            return
        }
        if (!branch.matches(Regex("^[A-Za-z0-9._/-]+$"))) {
            evidenceSyncText.text = "\nGitHub sync: invalid branch."
            return
        }

        evidenceSyncText.text = "\nGitHub sync: testing authenticated repository access..."
        Thread {
            try {
                GitHubEvidenceClient(token, repository, branch).verifyRepository()
                if (enteredToken.isNotBlank()) {
                    GitHubTokenStore(this).save(enteredToken)
                    runOnUiThread { evidenceSyncToken.text?.clear() }
                }
                config.repository = repository
                config.branch = branch
                config.enabled = true
                EvidenceSyncScheduler.enqueue(this)
                runOnUiThread {
                    evidenceSyncText.text =
                        "\nGitHub sync: CONNECTED and enabled. Pending evidence will upload automatically when network is available.\n" +
                            EvidenceSyncManager.status(this)
                }
            } catch (error: Exception) {
                runOnUiThread {
                    evidenceSyncText.text = "\nGitHub sync connection failed: " +
                        (error.message ?: error.javaClass.simpleName)
                }
            }
        }.start()
    }

    private fun refreshEvidenceSyncStatus() {
        Thread {
            val status = EvidenceSyncManager.status(this)
            runOnUiThread { evidenceSyncText.text = "\n" + status }
        }.start()
    }

    private fun runLocalAiTest() {
        localAiRunButton.isEnabled = false
        localAiBenchmarkButton.isEnabled = false
        localAiDownloadButton.isEnabled = false
        localAiText.text = "\nLocal AI inference running...\nNo action execution is permitted."
        Thread {
            val started = System.currentTimeMillis()
            try {
                val runtime = LocalLlamaRuntime()
                val snapshot = DeviceMonitor(this).collectSnapshot(includeSystemTelemetry = false)
                val prompt = QwenLocalModel.prompt(
                    "Analyze this Android device observation and identify whether more evidence is needed. " +
                        "Battery=" + (snapshot.batteryPercent ?: "unknown") + "%, " +
                        "Temperature=" + (snapshot.batteryTemperatureC ?: "unknown") + "C, " +
                        "AvailableRAM=" + snapshot.availableRamMb + "MB. " +
                        "Do not propose or execute device mutations."
                )
                val result = runtime.generate(QwenLocalModel.file(this), prompt)
                val elapsed = System.currentTimeMillis() - started
                runOnUiThread {
                    localAiText.text = "\nLocal AI advisory result\nInference wall time: " + elapsed +
                        " ms\nRaw model result:\n" + result +
                        "\n\nSafety: executionRequested=false; deviceMutationAllowed=false."
                    localAiRunButton.isEnabled = true
                    localAiBenchmarkButton.isEnabled = true
                    localAiDownloadButton.isEnabled = true
                }
            } catch (error: Exception) {
                runOnUiThread {
                    localAiText.text = "\nLocal AI inference failed: " +
                        (error.message ?: error.javaClass.simpleName)
                    localAiRunButton.isEnabled = true
                    localAiBenchmarkButton.isEnabled = true
                    localAiDownloadButton.isEnabled = true
                }
            }
        }.start()
    }

    private fun runLocalAiBenchmark() {
        localAiBenchmarkButton.isEnabled = false
        localAiRunButton.isEnabled = false
        localAiDownloadButton.isEnabled = false
        localAiText.text =
            "\nStage 9 benchmark starting...\n" +
                "Controlled configurations: 2 threads and 4 threads; 2 repetitions each.\n" +
                "Expected duration can be several minutes because every run loads the model."
        Thread {
            try {
                val benchmark = LocalInferenceBenchmark(this)
                val result = benchmark.run { threads, repetition, totalRuns ->
                    runOnUiThread {
                        localAiText.text =
                            "\nStage 9 benchmark running\n" +
                                "Run " + ((repetition - 1) * 2 + if (threads == 2) 1 else 2) +
                                "/" + totalRuns + "\n" +
                                "Threads: " + threads + "\n" +
                                "No device mutations are permitted."
                    }
                }
                val file = LocalInferenceBenchmarkJsonWriter.write(this, result)
                val sharedExportPath = runCatching {
                    LocalInferenceBenchmarkJsonWriter.exportToSharedDownloads(this, file)
                }.getOrNull()
                val summary = buildString {
                    append("\nStage 9 benchmark complete\n")
                    result.threadConfigurations.forEach { threads ->
                        val group = result.samples.filter { it.threadCount == threads }
                        append(threads).append(" threads: ")
                            .append(group.size).append(" samples\n")
                        append("  avg wall: ")
                            .append(String.format("%.1f s", group.map { it.wallTimeMs }.average() / 1000.0))
                            .append("\n")
                        append("  avg generation: ")
                            .append(String.format("%.3f tok/s", group.map { it.generationTokensPerSecond }.average()))
                            .append("\n")
                        append("  avg load: ")
                            .append(String.format("%.1f s", group.map { it.loadMs }.average() / 1000.0))
                            .append("\n")
                        append("  avg PSS delta: ")
                            .append(String.format("%.1f MB", group.map { it.processPssDeltaKb }.average() / 1024.0))
                            .append("\n")
                    }
                    append("Model output contained <think>: ")
                        .append(result.samples.count { it.modelOutputContainsThink })
                        .append("/").append(result.samples.size).append("\n")
                    append("Safety: advisoryOnly=true; executionRequested=false; deviceMutationAllowed=false\n")
                    append("App-private JSON: ").append(file.name).append("\n")
                    if (sharedExportPath != null) {
                        append("Termux-readable export: /storage/emulated/0/")
                            .append(sharedExportPath)
                    } else {
                        append("Shared Downloads export: failed; app-private JSON remains available.")
                    }
                }
                runOnUiThread {
                    localAiText.text = summary
                    localAiRunButton.isEnabled = true
                    localAiDownloadButton.isEnabled = true
                    localAiBenchmarkButton.isEnabled = true
                }
            } catch (error: Exception) {
                runOnUiThread {
                    localAiText.text =
                        "\nStage 9 benchmark failed: " +
                            (error.message ?: error.javaClass.simpleName)
                    localAiRunButton.isEnabled = true
                    localAiDownloadButton.isEnabled = true
                    localAiBenchmarkButton.isEnabled = true
                }
            }
        }.start()
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

    private fun runWorkloadRecoveryBenchmark() {
        benchmarkButton.isEnabled = false
        workloadButton.isEnabled = false
        recoveryButton.isEnabled = false
        statusText.text =
            "\nRecovery benchmark preparing...\nBaseline 60s → workload 5min → recovery 2min.\n" +
                "After workload, stop the workload and return to the optimizer app if practical.\n" +
                "No system mutations are performed."
        Thread {
            try {
                val samples = WorkloadRecoveryBenchmark(this).run(
                    onPhase = { phase ->
                        runOnUiThread {
                            statusText.text = when (phase) {
                                "baseline" -> "\nRecovery protocol: baseline 60s\nKeep the device idle."
                                "workload" -> "\nRecovery protocol: workload 5min\nSwitch to your normal app/game and use it normally."
                                else -> "\nRecovery protocol: recovery 2min\nStop the workload and return to the optimizer app if practical.\nRead-only monitoring continues."
                            }
                        }
                    },
                    onSample = { sample ->
                        runOnUiThread {
                            val pss = sample.sample.processes
                                .firstOrNull { it.packageNames.any { pkg -> pkg == packageName } }
                                ?.pssKb
                            statusText.text =
                                "\nRecovery benchmark: " + sample.phase +
                                    "\nSample: " + sample.phaseSampleIndex +
                                    "\nRAM available: " + sample.sample.availableRamMb + " MB" +
                                    "\nOptimizer PSS: " + (pss ?: -1L) + " KiB" +
                                    "\nTemperature: " +
                                    (sample.sample.temperatureC?.let { String.format("%.1f°C", it) } ?: "unknown")
                        }
                    }
                )
                val file = WorkloadRecoveryBenchmarkJsonWriter.write(this, samples)
                val shared = WorkloadRecoveryBenchmarkJsonWriter.exportToSharedDownloads(this, file)
                val byPhase = samples.groupBy { it.phase }
                val summary = buildString {
                    append("\nWorkload → recovery benchmark complete\n")
                    listOf("baseline", "workload", "recovery").forEach { phase ->
                        val group = byPhase[phase].orEmpty()
                        val pss = group.flatMap { s ->
                            s.sample.processes
                                .filter { it.packageNames.any { pkg -> pkg == packageName } }
                                .map { it.pssKb }
                        }
                        append(phase).append(": ").append(group.size).append(" samples")
                        if (pss.isNotEmpty()) {
                            append("; PSS ").append(pss.first()).append(" → ").append(pss.last()).append(" KiB")
                        }
                        append("\n")
                    }
                    append("Raw JSON: ").append(file.name).append("\n")
                    append(
                        if (shared != null) {
                            "Shared export: /storage/emulated/0/$shared"
                        } else {
                            "Shared export failed; app-private JSON remains available."
                        }
                    )
                }
                runOnUiThread {
                    statusText.text = summary
                    benchmarkButton.isEnabled = true
                    workloadButton.isEnabled = true
                    recoveryButton.isEnabled = true
                }
            } catch (error: Exception) {
                runOnUiThread {
                    statusText.text =
                        "\nRecovery benchmark failed: " +
                            (error.message ?: error.javaClass.simpleName)
                    benchmarkButton.isEnabled = true
                    workloadButton.isEnabled = true
                    recoveryButton.isEnabled = true
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
