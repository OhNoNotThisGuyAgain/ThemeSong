package com.spotzones.data.local

import androidx.room.TypeConverter
import com.spotzones.core.serialization.AppJson
import com.spotzones.domain.model.Action
import com.spotzones.domain.model.Condition
import com.spotzones.domain.model.PlaybackConfig
import com.spotzones.domain.model.Schedule
import kotlinx.serialization.encodeToString

/**
 * Room type converters that store rich value objects as JSON columns.
 *
 * Rationale: [Condition]/[Action] are deep, evolving trees; modelling them as relational tables
 * would be brittle and slow to query. Persisting them as JSON keeps the schema stable while the
 * model grows, and reuses the exact same serializer as import/export, guaranteeing round-trip
 * fidelity between the database and backup files.
 */
class Converters {
    @TypeConverter fun fromCondition(value: Condition): String = AppJson.encodeToString(value)
    @TypeConverter fun toCondition(value: String): Condition = AppJson.decodeFromString(value)

    @TypeConverter fun fromAction(value: Action): String = AppJson.encodeToString(value)
    @TypeConverter fun toAction(value: String): Action = AppJson.decodeFromString(value)

    @TypeConverter fun fromPlayback(value: PlaybackConfig): String = AppJson.encodeToString(value)
    @TypeConverter fun toPlayback(value: String): PlaybackConfig = AppJson.decodeFromString(value)

    @TypeConverter fun fromSchedule(value: Schedule): String = AppJson.encodeToString(value)
    @TypeConverter fun toSchedule(value: String): Schedule = AppJson.decodeFromString(value)
}
