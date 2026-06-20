package com.spotzones.data.backup

import com.spotzones.BuildConfig
import com.spotzones.core.coroutines.IoDispatcher
import com.spotzones.core.serialization.AppJsonPretty
import com.spotzones.data.security.PassphraseCrypto
import com.spotzones.domain.backup.BackupBundle
import com.spotzones.domain.backup.BackupManager
import com.spotzones.domain.backup.ImportMode
import com.spotzones.domain.backup.ImportSummary
import com.spotzones.domain.backup.ZoneExport
import com.spotzones.domain.repository.RuleRepository
import com.spotzones.domain.repository.ZoneRepository
import com.spotzones.domain.util.DomainError
import com.spotzones.domain.util.Outcome
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString

@Singleton
class BackupManagerImpl @Inject constructor(
    private val zoneRepository: ZoneRepository,
    private val ruleRepository: RuleRepository,
    @IoDispatcher private val io: CoroutineDispatcher,
) : BackupManager {

    override suspend fun export(): Outcome<String> = withContext(io) {
        Outcome.catching { AppJsonPretty.encodeToString(snapshot()) }
    }

    override suspend fun exportEncrypted(passphrase: CharArray): Outcome<String> = withContext(io) {
        Outcome.catching { PassphraseCrypto.encrypt(AppJsonPretty.encodeToString(snapshot()), passphrase) }
    }

    override suspend fun import(payload: String, mode: ImportMode): Outcome<ImportSummary> = withContext(io) {
        decodeAndApply(payload, mode)
    }

    override suspend fun importEncrypted(
        payload: String,
        passphrase: CharArray,
        mode: ImportMode,
    ): Outcome<ImportSummary> = withContext(io) {
        val decrypted = try {
            PassphraseCrypto.decrypt(payload, passphrase)
        } catch (e: Exception) {
            return@withContext Outcome.Failure(DomainError.Validation("Wrong passphrase or corrupted backup."))
        }
        decodeAndApply(decrypted, mode)
    }

    override suspend fun exportZone(zoneId: String): Outcome<String> = withContext(io) {
        val zone = zoneRepository.getZone(zoneId)
            ?: return@withContext Outcome.Failure(DomainError.NotFound("Zone not found"))
        Outcome.catching { AppJsonPretty.encodeToString(ZoneExport(zone = zone)) }
    }

    override suspend fun importZone(payload: String): Outcome<Unit> = withContext(io) {
        Outcome.catching {
            val export = AppJsonPretty.decodeFromString<ZoneExport>(payload)
            // New id so importing a shared zone never clobbers an existing one.
            val copy = export.zone.copy(id = java.util.UUID.randomUUID().toString())
            zoneRepository.upsert(copy)
            Unit
        }
    }

    private suspend fun snapshot(): BackupBundle = BackupBundle(
        exportedAt = System.currentTimeMillis(),
        appVersion = BuildConfig.VERSION_NAME,
        zones = zoneRepository.getAllZones(),
        rules = ruleRepository.getAllRules(),
    )

    private suspend fun decodeAndApply(payload: String, mode: ImportMode): Outcome<ImportSummary> = try {
        val bundle = AppJsonPretty.decodeFromString<BackupBundle>(payload)
        when (mode) {
            ImportMode.REPLACE -> {
                zoneRepository.replaceAll(bundle.zones)
                ruleRepository.replaceAll(bundle.rules)
            }
            ImportMode.MERGE -> {
                bundle.zones.forEach { zoneRepository.upsert(it) }
                bundle.rules.forEach { ruleRepository.upsert(it) }
            }
        }
        Outcome.Success(ImportSummary(bundle.zones.size, bundle.rules.size))
    } catch (e: Exception) {
        Outcome.Failure(DomainError.Validation("Couldn't read backup: ${e.message}"))
    }
}
