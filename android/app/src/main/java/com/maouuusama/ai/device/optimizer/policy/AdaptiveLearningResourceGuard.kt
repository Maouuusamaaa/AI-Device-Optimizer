package com.maouuusama.ai.device.optimizer.policy

import android.app.ActivityManager
import android.content.Context
import android.os.PowerManager

data class AdaptiveLearningResourceSnapshot(
    val isLowMemory: Boolean,
    val availableMemoryBytes: Long,
    val thermalStatus: Int
)

fun interface AdaptiveLearningResourceProvider {
    fun snapshot(): AdaptiveLearningResourceSnapshot
}

class AdaptiveLearningResourceGuard(
    private val provider: AdaptiveLearningResourceProvider,
    private val minimumAvailableMemoryBytes: Long = 64L * 1024L * 1024L
) {
    init {
        require(minimumAvailableMemoryBytes >= 0L)
    }

    fun mayProcess(): Boolean {
        val snapshot = provider.snapshot()
        if (snapshot.isLowMemory) return false
        if (snapshot.availableMemoryBytes < minimumAvailableMemoryBytes) return false
        return snapshot.thermalStatus < PowerManager.THERMAL_STATUS_SEVERE
    }

    companion object {
        fun forContext(context: Context): AdaptiveLearningResourceGuard =
            AdaptiveLearningResourceGuard(
                AdaptiveLearningResourceProvider {
                    val memory = ActivityManager.MemoryInfo()
                    context.getSystemService(ActivityManager::class.java).getMemoryInfo(memory)
                    val power = context.getSystemService(PowerManager::class.java)
                    AdaptiveLearningResourceSnapshot(
                        isLowMemory = memory.lowMemory,
                        availableMemoryBytes = memory.availMem,
                        thermalStatus = if (android.os.Build.VERSION.SDK_INT >= 29) {
                            power.currentThermalStatus
                        } else {
                            PowerManager.THERMAL_STATUS_NONE
                        }
                    )
                }
            )
    }
}
