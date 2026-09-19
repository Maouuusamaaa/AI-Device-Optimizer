package com.maouuusama.ai.device.optimizer.monitor

data class SystemCpuSnapshot(
    val userJiffies: Long,
    val niceJiffies: Long,
    val systemJiffies: Long,
    val idleJiffies: Long,
    val ioWaitJiffies: Long,
    val irqJiffies: Long,
    val softIrqJiffies: Long,
    val totalJiffies: Long,
    val utilizationPercent: Double?
)
