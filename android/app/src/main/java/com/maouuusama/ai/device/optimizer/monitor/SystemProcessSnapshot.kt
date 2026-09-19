package com.maouuusama.ai.device.optimizer.monitor

data class SystemProcessSnapshot(
    val pid: Int,
    val processName: String,
    val pssKb: Long
)
