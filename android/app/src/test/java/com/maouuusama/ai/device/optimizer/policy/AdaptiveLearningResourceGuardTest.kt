package com.maouuusama.ai.device.optimizer.policy

import android.os.PowerManager
import org.junit.Assert.*
import org.junit.Test

class AdaptiveLearningResourceGuardTest {
    @Test fun normalResourcesPermitLearning() {
        val guard = AdaptiveLearningResourceGuard(
            AdaptiveLearningResourceProvider { AdaptiveLearningResourceSnapshot(false, 256L * 1024L * 1024L, PowerManager.THERMAL_STATUS_NONE) }
        )
        assertTrue(guard.mayProcess())
    }

    @Test fun lowMemoryDefersLearning() {
        val guard = AdaptiveLearningResourceGuard(
            AdaptiveLearningResourceProvider { AdaptiveLearningResourceSnapshot(true, 256L * 1024L * 1024L, PowerManager.THERMAL_STATUS_NONE) }
        )
        assertFalse(guard.mayProcess())
    }

    @Test fun severeThermalStatusDefersLearning() {
        val guard = AdaptiveLearningResourceGuard(
            AdaptiveLearningResourceProvider { AdaptiveLearningResourceSnapshot(false, 256L * 1024L * 1024L, PowerManager.THERMAL_STATUS_SEVERE) }
        )
        assertFalse(guard.mayProcess())
    }
}
