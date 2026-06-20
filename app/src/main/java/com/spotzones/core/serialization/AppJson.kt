package com.spotzones.core.serialization

import kotlinx.serialization.json.Json

/**
 * Single configured [Json] instance shared by Room converters, DataStore and import/export.
 *
 * - `classDiscriminator` + the sealed `@SerialName`s on [com.spotzones.domain.model.Condition]/Action
 *   give stable, human-readable polymorphic JSON that survives refactors.
 * - `ignoreUnknownKeys` keeps older backups loadable after the schema grows.
 * - `encodeDefaults = false` keeps persisted/exported JSON compact.
 */
val AppJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
    classDiscriminator = "type"
    prettyPrint = false
    isLenient = true
}

/** Pretty variant used only for user-facing exports. */
val AppJsonPretty: Json = Json(from = AppJson) {
    prettyPrint = true
    encodeDefaults = true
}
