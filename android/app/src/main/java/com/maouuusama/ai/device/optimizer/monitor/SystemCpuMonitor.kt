package com.maouuusama.ai.device.optimizer.monitor

class SystemCpuMonitor {
    private var previous: SystemTelemetryMonitor.SystemCpuCounters? = null

    @Synchronized
    fun collect(current: SystemTelemetryMonitor.SystemCpuCounters?): SystemCpuSnapshot? {
        current ?: return null
        val old = previous
        previous = current

        val total = current.totalJiffies()
        val utilization = old?.let {
            val totalDelta = (total - it.totalJiffies()).coerceAtLeast(0L)
            val idleDelta = (current.idleJiffies - it.idleJiffies).coerceAtLeast(0L) +
                (current.ioWaitJiffies - it.ioWaitJiffies).coerceAtLeast(0L)
            if (totalDelta > 0L) {
                ((totalDelta - idleDelta).coerceAtLeast(0L).toDouble() / totalDelta) * 100.0
            } else null
        }

        return SystemCpuSnapshot(
            userJiffies = current.userJiffies,
            niceJiffies = current.niceJiffies,
            systemJiffies = current.systemJiffies,
            idleJiffies = current.idleJiffies,
            ioWaitJiffies = current.ioWaitJiffies,
            irqJiffies = current.irqJiffies,
            softIrqJiffies = current.softIrqJiffies,
            totalJiffies = total,
            utilizationPercent = utilization
        )
    }

    private fun SystemTelemetryMonitor.SystemCpuCounters.totalJiffies(): Long =
        userJiffies + niceJiffies + systemJiffies + idleJiffies + ioWaitJiffies +
            irqJiffies + softIrqJiffies
}
