package com.maouuusama.ai.device.optimizer.policy

object AdaptiveLearningRuntimeRegistry {
    @Volatile
    var lastResult: AdaptiveLearningRunResult? = null
}
