package com.maouuusama.ai.device.optimizer.actions

enum class ProposalStatus { PROPOSED, BLOCKED }

data class ActionProposal(
    val actionId: String,
    val status: ProposalStatus,
    val reason: String
)