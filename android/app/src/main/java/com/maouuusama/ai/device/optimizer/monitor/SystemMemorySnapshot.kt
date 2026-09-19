package com.maouuusama.ai.device.optimizer.monitor

data class SystemMemorySnapshot(
    val memTotalKb: Long?,
    val memFreeKb: Long?,
    val memAvailableKb: Long?,
    val cachedKb: Long?,
    val swapTotalKb: Long?,
    val swapFreeKb: Long?,
    val shmemKb: Long?,
    val sreclaimableKb: Long?
) {
    val swapUsedKb: Long?
        get() = if (swapTotalKb != null && swapFreeKb != null) {
            (swapTotalKb - swapFreeKb).coerceAtLeast(0L)
        } else null
}
