package com.maouuusama.ai.device.optimizer.gameboost

import com.maouuusama.ai.device.optimizer.monitor.GameModeReadResult

data class GameBoostGame(
    val packageName: String,
    val className: String?,
    val enabled: Boolean
)

data class GameBoostGameLibrary(
    val capability: GameBoostCapability,
    val games: List<GameBoostGame>
)

object GameBoostGameLibraryAdapter {
    fun from(result: GameModeReadResult): GameBoostGameLibrary {
        val capability = GameBoostCapability(
            id = "gamemode.provider",
            status = if (result.available) {
                CapabilityStatus.AVAILABLE
            } else {
                CapabilityStatus.UNAVAILABLE
            },
            source = "SmartPanel Game Mode provider",
            detail = result.error ?: "SmartPanel Game Mode provider is available"
        )

        return GameBoostGameLibrary(
            capability = capability,
            games = result.packages.map { entry ->
                GameBoostGame(
                    packageName = entry.packageName,
                    className = entry.className,
                    enabled = entry.checked
                )
            }
        )
    }
}
