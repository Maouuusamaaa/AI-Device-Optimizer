package com.maouuusama.ai.device.optimizer.action

import com.maouuusama.ai.device.optimizer.policy.ActionCatalog
import com.maouuusama.ai.device.optimizer.policy.ActionDefinition

enum class AdaptiveOptimizerReadinessStatus {
    READY_FOR_REVIEW,
    BLOCKED
}

data class AdaptiveOptimizerReadiness(
    val actionId: String,
    val status: AdaptiveOptimizerReadinessStatus,
    val reasons: List<String>,
    val executionAllowed: Boolean
) {
    init {
        require(actionId.isNotBlank())
        require(reasons.isNotEmpty())
        require(!executionAllowed) {
            "Adaptive optimizer readiness cannot authorize execution."
        }
    }
}

/**
 * Checks whether a candidate action satisfies the structural prerequisites for a future
 * separately-reviewed execution milestone.
 *
 * This evaluator never selects, enables, ranks, or executes an action.
 */
class AdaptiveOptimizerReadinessEvaluator(
    private val catalog: ActionCatalogProvider = ActionCatalogProvider.Default()
) {
    fun evaluate(spec: CandidateActionSpec): AdaptiveOptimizerReadiness {
        val action = catalog.find(spec.actionId)
            ?: return blocked(spec.actionId, "Action is not present in the allowlist catalog.")

        val reasons = mutableListOf<String>()

        if (action.risk != com.maouuusama.ai.device.optimizer.policy.ActionRisk.LOW) {
            reasons += "Action risk must be LOW."
        }
        if (action.requiredPermission != "none") {
            reasons += "Action requires an additional permission."
        }
        if (!action.reversible) {
            reasons += "Action must have a reversible path."
        }
        if (action.measurement.isBlank()) {
            reasons += "Action must define a measurement."
        }
        if (action.rollback.isBlank()) {
            reasons += "Action must define rollback semantics."
        }
        if (spec.preconditions.isEmpty()) {
            reasons += "Candidate must declare preconditions."
        }
        if (spec.verificationPlan.isEmpty()) {
            reasons += "Candidate must declare post-action verification."
        }
        if (spec.killSwitchId.isBlank()) {
            reasons += "Candidate must declare a kill switch."
        }

        return if (reasons.isEmpty()) {
            AdaptiveOptimizerReadiness(
                actionId = spec.actionId,
                status = AdaptiveOptimizerReadinessStatus.READY_FOR_REVIEW,
                reasons = listOf(
                    "Structural prerequisites are present.",
                    "Execution remains disabled and requires a separate reviewed milestone."
                ),
                executionAllowed = false
            )
        } else {
            blocked(spec.actionId, reasons.joinToString(" "))
        }
    }

    private fun blocked(actionId: String, reason: String): AdaptiveOptimizerReadiness =
        AdaptiveOptimizerReadiness(
            actionId = actionId,
            status = AdaptiveOptimizerReadinessStatus.BLOCKED,
            reasons = listOf(reason, "Execution remains disabled."),
            executionAllowed = false
        )
}

interface ActionCatalogProvider {
    fun find(actionId: String): ActionDefinition?

    class Default : ActionCatalogProvider {
        override fun find(actionId: String): ActionDefinition? = ActionCatalog.find(actionId)
    }
}
