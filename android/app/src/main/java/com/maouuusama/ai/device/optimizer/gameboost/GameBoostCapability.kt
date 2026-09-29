package com.maouuusama.ai.device.optimizer.gameboost

enum class CapabilityStatus {
    AVAILABLE,
    UNAVAILABLE,
    FAILED,
    UNKNOWN
}

data class GameBoostCapability(
    val id: String,
    val status: CapabilityStatus,
    val source: String,
    val detail: String
) {
    val isAvailable: Boolean
        get() = status == CapabilityStatus.AVAILABLE
}
