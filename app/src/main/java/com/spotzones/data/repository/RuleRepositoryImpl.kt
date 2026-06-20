package com.spotzones.data.repository

import com.spotzones.core.coroutines.IoDispatcher
import com.spotzones.data.local.dao.RuleDao
import com.spotzones.data.local.entity.toDomain
import com.spotzones.data.local.entity.toEntity
import com.spotzones.domain.model.Rule
import com.spotzones.domain.repository.RuleRepository
import com.spotzones.domain.util.Outcome
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class RuleRepositoryImpl @Inject constructor(
    private val ruleDao: RuleDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : RuleRepository {

    override fun observeRules(): Flow<List<Rule>> =
        ruleDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getAllRules(): List<Rule> =
        withContext(io) { ruleDao.getAll().map { it.toDomain() } }

    override suspend fun getEnabledRules(): List<Rule> =
        withContext(io) { ruleDao.getEnabled().map { it.toDomain() } }

    override suspend fun upsert(rule: Rule): Outcome<Rule> = withContext(io) {
        Outcome.catching {
            val normalized = rule.copy(updatedAt = System.currentTimeMillis())
            ruleDao.upsert(normalized.toEntity())
            normalized
        }
    }

    override suspend fun delete(id: String): Outcome<Unit> = withContext(io) {
        Outcome.catching { ruleDao.delete(id) }
    }

    override suspend fun replaceAll(rules: List<Rule>): Outcome<Unit> = withContext(io) {
        Outcome.catching { ruleDao.replaceAll(rules.map { it.toEntity() }) }
    }
}
