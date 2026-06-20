package com.spotzones.domain.repository

import com.spotzones.domain.model.Rule
import com.spotzones.domain.util.Outcome
import kotlinx.coroutines.flow.Flow

/** Persistence boundary for standalone [Rule]s. */
interface RuleRepository {
    fun observeRules(): Flow<List<Rule>>
    suspend fun getAllRules(): List<Rule>
    suspend fun getEnabledRules(): List<Rule>
    suspend fun upsert(rule: Rule): Outcome<Rule>
    suspend fun delete(id: String): Outcome<Unit>
    suspend fun replaceAll(rules: List<Rule>): Outcome<Unit>
}
