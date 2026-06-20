package com.spotzones.domain.repository

import com.spotzones.domain.model.AppSettings
import com.spotzones.domain.model.ManualOverride
import kotlinx.coroutines.flow.Flow

/** App-wide preferences and the current manual override, persisted via DataStore. */
interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun current(): AppSettings
    suspend fun update(transform: (AppSettings) -> AppSettings)

    val override: Flow<ManualOverride?>
    suspend fun currentOverride(): ManualOverride?
    suspend fun setOverride(override: ManualOverride?)
}
