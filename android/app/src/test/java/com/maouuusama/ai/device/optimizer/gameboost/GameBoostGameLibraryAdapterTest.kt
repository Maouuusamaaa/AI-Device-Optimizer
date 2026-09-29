package com.maouuusama.ai.device.optimizer.gameboost

import com.maouuusama.ai.device.optimizer.monitor.GameModePackageEntry
import com.maouuusama.ai.device.optimizer.monitor.GameModeReadResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameBoostGameLibraryAdapterTest {
    @Test
    fun availableGameModeProviderExposesCheckedGamePackages() {
        val result = GameModeReadResult(
            available = true,
            packages = listOf(
                GameModePackageEntry(
                    packageName = "com.example.game",
                    className = null,
                    checked = true
                )
            )
        )

        val library = GameBoostGameLibraryAdapter.from(result)

        assertTrue(library.capability.isAvailable)
        assertEquals("gamemode.provider", library.capability.id)
        assertEquals(1, library.games.size)
        assertEquals("com.example.game", library.games.single().packageName)
        assertTrue(library.games.single().enabled)
    }
}