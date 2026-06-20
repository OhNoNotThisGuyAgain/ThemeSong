package com.spotzones.domain.backup

import com.spotzones.domain.model.Rule
import com.spotzones.domain.model.Zone
import com.spotzones.domain.util.Outcome
import kotlinx.serialization.Serializable

/**
 * Versioned, serializable snapshot of user-authored configuration. Playback state and history are
 * intentionally excluded — backups describe *intent* (zones/rules), not transient runtime data.
 */
@Serializable
data class BackupBundle(
    val schemaVersion: Int = CURRENT_SCHEMA,
    val exportedAt: Long,
    val appVersion: String,
    val zones: List<Zone>,
    val rules: List<Rule>,
) {
    companion object {
        const val CURRENT_SCHEMA = 1
    }
}

/** A single exportable/importable zone, used for sharing one zone at a time. */
@Serializable
data class ZoneExport(val schemaVersion: Int = BackupBundle.CURRENT_SCHEMA, val zone: Zone)

enum class ImportMode { MERGE, REPLACE }

/**
 * Backup/restore boundary. Plain-JSON and AES-encrypted variants are supported so users can choose
 * shareable vs. protected backups (the "encrypted backups" requirement). Cloud sync is abstracted
 * behind the same interface and can be added later without touching callers.
 */
interface BackupManager {
    suspend fun export(): Outcome<String>
    suspend fun exportEncrypted(passphrase: CharArray): Outcome<String>
    suspend fun import(payload: String, mode: ImportMode): Outcome<ImportSummary>
    suspend fun importEncrypted(payload: String, passphrase: CharArray, mode: ImportMode): Outcome<ImportSummary>
    suspend fun exportZone(zoneId: String): Outcome<String>
    suspend fun importZone(payload: String): Outcome<Unit>
}

data class ImportSummary(val zonesImported: Int, val rulesImported: Int)
