package com.spotzones.domain.engine

import com.spotzones.domain.model.Action
import com.spotzones.domain.model.EvaluationCandidate
import com.spotzones.domain.model.EvaluationContext
import com.spotzones.domain.model.Rule
import com.spotzones.domain.model.Zone

/**
 * Result of one evaluation pass.
 *
 * @param winner the candidate whose action should drive playback, or null if nothing matched.
 * @param matches every candidate that matched, ordered by the same precedence used to pick the
 *   winner — surfaced to the UI/history for transparency ("why is this playing?").
 */
data class AutomationDecision(
    val winner: EvaluationCandidate?,
    val matches: List<EvaluationCandidate>,
) {
    val action: Action get() = winner?.action ?: Action.None
    val activeZone: Zone? get() = winner?.sourceZone
}

/**
 * Resolves which zone/rule should control playback for a given context.
 *
 * Precedence (highest first):
 *  1. Explicit priority (1..100).
 *  2. More specific geofence — a smaller radius wins, so "Bedroom" beats the enclosing "Home".
 *  3. Most recently updated, as a final deterministic tie-break.
 *
 * The engine is pure: feed it state, get a decision. All I/O (sensors, persistence, Spotify) lives
 * in the surrounding controller, which keeps this fully unit-testable.
 */
class RuleEngine(
    private val evaluator: ConditionEvaluator = ConditionEvaluator(),
) {

    fun evaluate(
        zones: List<Zone>,
        rules: List<Rule>,
        context: EvaluationContext,
    ): AutomationDecision {
        val zoneIndex: Map<String, Zone> = zones.associateBy { it.id }
        val lookup: (String) -> Zone? = zoneIndex::get

        val matches = buildCandidates(zones, rules)
            .filter { evaluator.evaluate(it.condition, context, lookup) }
            .sortedWith(candidatePrecedence())

        return AutomationDecision(winner = matches.firstOrNull(), matches = matches)
    }

    private fun buildCandidates(zones: List<Zone>, rules: List<Rule>): List<EvaluationCandidate> {
        val zoneCandidates = zones.filter { it.enabled }.map { zone ->
            EvaluationCandidate(
                id = zone.id,
                displayName = zone.name,
                priority = zone.priority,
                condition = zone.activationCondition(),
                action = Action.Play(zone.playback),
                sourceZone = zone,
            )
        }
        val ruleCandidates = rules.filter { it.enabled }.map { rule ->
            EvaluationCandidate(
                id = rule.id,
                displayName = rule.name,
                priority = rule.priority,
                condition = rule.condition,
                action = rule.action,
                sourceZone = null,
            )
        }
        return zoneCandidates + ruleCandidates
    }

    private fun candidatePrecedence(): Comparator<EvaluationCandidate> =
        compareByDescending<EvaluationCandidate> { it.priority }
            .thenBy { it.sourceZone?.radiusMeters ?: Double.MAX_VALUE } // smaller radius = more specific
            .thenByDescending { it.sourceZone?.updatedAt ?: 0L }
}
