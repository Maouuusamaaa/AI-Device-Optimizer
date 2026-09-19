package com.maouuusama.ai.device.optimizer.monitor

data class ProcessSnapshot(
    val pid: Int,
    val processName: String,
    val packageNames: List<String>,
    val appLabels: List<String>,
    val importance: Int,
    val importanceLabel: String,
    val isForeground: Boolean,
    val pssKb: Long,
    val rssKb: Long?,
    val swapPssKb: Long
)
