package com.maouuusama.ai.device.optimizer.monitor

import android.app.ActivityManager
import android.content.Context
import java.io.File

class ProcessMonitor(private val context: Context) {

    fun collectProcesses(): List<ProcessSnapshot> {
        val activityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        val running = activityManager.runningAppProcesses ?: return emptyList()
        if (running.isEmpty()) return emptyList()

        val pids = running.map { it.pid }.distinct().toIntArray()
        val memoryInfoByPid = activityManager
            .getProcessMemoryInfo(pids)
            .withIndex()
            .associate { it.index to it.value }

        return running.mapIndexedNotNull { index, process ->
            val memoryInfo = memoryInfoByPid[index] ?: return@mapIndexedNotNull null
            val packages = process.pkgList?.distinct().orEmpty()
            val labels = packages.mapNotNull { packageName ->
                runCatching {
                    context.packageManager
                        .getApplicationInfo(packageName, 0)
                        .loadLabel(context.packageManager)
                        .toString()
                }.getOrNull()
            }

            ProcessSnapshot(
                pid = process.pid,
                processName = process.processName,
                packageNames = packages,
                appLabels = labels,
                importance = process.importance,
                importanceLabel = importanceLabel(process.importance),
                isForeground = process.importance ==
                    ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND,
                pssKb = memoryInfo.totalPss.toLong(),
                rssKb = readRssKb(process.pid),
                swapPssKb = memoryInfo
                    .getMemoryStat("summary.total-swap")
                    ?.toLongOrNull()
                    ?: 0L
            )
        }.sortedByDescending { it.pssKb }
    }

    private fun readRssKb(pid: Int): Long? =
        runCatching {
            File("/proc/$pid/status").useLines { lines ->
                lines.firstOrNull { it.startsWith("VmRSS:") }
                    ?.substringAfter(":")
                    ?.trim()
                    ?.substringBefore(" ")
                    ?.toLongOrNull()
            }
        }.getOrNull()

    private fun importanceLabel(importance: Int): String =
        when (importance) {
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND -> "FOREGROUND"
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE -> "FOREGROUND_SERVICE"
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE -> "VISIBLE"
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE -> "PERCEPTIBLE"
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE -> "SERVICE"
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_CANT_SAVE_STATE -> "CANT_SAVE_STATE"
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED -> "CACHED"
            else -> "OTHER"
        }
}
