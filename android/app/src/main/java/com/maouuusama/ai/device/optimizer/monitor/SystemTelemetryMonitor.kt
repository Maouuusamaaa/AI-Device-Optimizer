package com.maouuusama.ai.device.optimizer.monitor

class SystemTelemetryMonitor {

    private val cpuMonitor = SystemCpuMonitor()

    fun collectSnapshot(): SystemTelemetrySnapshot {
        return when (ShizukuShell.status()) {
            ShizukuShell.Status.PERMISSION_REQUIRED -> SystemTelemetrySnapshot(
                status = SystemTelemetryStatus.PERMISSION_REQUIRED,
                provider = "shizuku",
                memory = null,
                processes = emptyList(),
                cpu = null
            )
            ShizukuShell.Status.UNAVAILABLE -> SystemTelemetrySnapshot(
                status = SystemTelemetryStatus.UNAVAILABLE,
                provider = "android-api-fallback",
                memory = null,
                processes = emptyList(),
                cpu = null
            )
            ShizukuShell.Status.AVAILABLE -> collectWithShizuku()
        }
    }

    private fun collectWithShizuku(): SystemTelemetrySnapshot = try {
        val memInfo = ShizukuShell.execute(
            "cat /proc/meminfo | grep -E '^(MemTotal|MemFree|MemAvailable|Cached|SwapTotal|SwapFree|SReclaimable|Shmem):'"
        ).getOrThrow()
        val processInfo = ShizukuShell.execute(
            "dumpsys meminfo | grep -E '^[[:space:]]+[0-9,]+K: ' | head -100"
        ).getOrThrow()
        val cpuInfo = ShizukuShell.execute("cat /proc/stat | head -1").getOrThrow()

        SystemTelemetrySnapshot(
            status = SystemTelemetryStatus.AVAILABLE,
            provider = "shizuku",
            memory = parseMemoryInfo(memInfo),
            processes = parseProcesses(processInfo),
            cpu = cpuMonitor.collect(parseCpuLine(cpuInfo))
        )
    } catch (error: Exception) {
        SystemTelemetrySnapshot(
            status = SystemTelemetryStatus.ERROR,
            provider = "shizuku",
            memory = null,
            processes = emptyList(),
            cpu = null,
            errorMessage = error.message ?: error.javaClass.simpleName
        )
    }

    internal fun parseMemoryInfo(output: String): SystemMemorySnapshot {
        val values = mutableMapOf<String, Long>()
        output.lineSequence().forEach { line ->
            val match = Regex("""^(\w+):\s+(\d+)\s+kB$""").find(line.trim())
            if (match != null) values[match.groupValues[1]] = match.groupValues[2].toLong()
        }
        fun value(key: String): Long? = values[key]
        return SystemMemorySnapshot(
            memTotalKb = value("MemTotal"),
            memFreeKb = value("MemFree"),
            memAvailableKb = value("MemAvailable"),
            cachedKb = value("Cached"),
            swapTotalKb = value("SwapTotal"),
            swapFreeKb = value("SwapFree"),
            shmemKb = value("Shmem"),
            sreclaimableKb = value("SReclaimable")
        )
    }

    internal fun parseProcesses(output: String): List<SystemProcessSnapshot> {
        val pattern = Regex("""^\s*([0-9,]+)K:\s+(.+?)\s+\(pid\s+(\d+)(?:\s*/.*)?\)\s*$""")
        return output.lineSequence()
            .mapNotNull { line ->
                val match = pattern.find(line) ?: return@mapNotNull null
                val pssKb = match.groupValues[1].replace(",", "").toLongOrNull()
                    ?: return@mapNotNull null
                val pid = match.groupValues[3].toIntOrNull() ?: return@mapNotNull null
                SystemProcessSnapshot(pid, match.groupValues[2].trim(), pssKb)
            }
            .distinctBy { it.pid }
            .sortedByDescending { it.pssKb }
            .toList()
    }

    internal fun parseCpuLine(output: String): SystemCpuCounters? {
        val line = output.lineSequence().firstOrNull { it.trimStart().startsWith("cpu ") } ?: return null
        val fields = line.trim().split(Regex("""\s+"""))
        if (fields.size < 8) return null
        return runCatching {
            SystemCpuCounters(
                userJiffies = fields[1].toLong(),
                niceJiffies = fields[2].toLong(),
                systemJiffies = fields[3].toLong(),
                idleJiffies = fields[4].toLong(),
                ioWaitJiffies = fields[5].toLong(),
                irqJiffies = fields[6].toLong(),
                softIrqJiffies = fields[7].toLong()
            )
        }.getOrNull()
    }

    data class SystemCpuCounters(
        val userJiffies: Long,
        val niceJiffies: Long,
        val systemJiffies: Long,
        val idleJiffies: Long,
        val ioWaitJiffies: Long,
        val irqJiffies: Long,
        val softIrqJiffies: Long
    )
}
