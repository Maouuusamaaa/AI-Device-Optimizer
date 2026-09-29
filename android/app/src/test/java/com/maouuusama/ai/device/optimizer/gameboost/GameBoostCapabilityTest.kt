package com.maouuusama.ai.device.optimizer.gameboost

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class GameBoostCapabilityTest {
    @Test
    fun unavailableCapabilityCannotBeTreatedAsAvailable() {
        val capability = GameBoostCapability(
            id = "telemetry.fps",
            status = CapabilityStatus.UNAVAILABLE,
            source = "SurfaceFlinger",
            detail = "No supported source detected"
        )

        assertEquals(CapabilityStatus.UNAVAILABLE, capability.status)
        assertFalse(capability.isAvailable)
    }
}
