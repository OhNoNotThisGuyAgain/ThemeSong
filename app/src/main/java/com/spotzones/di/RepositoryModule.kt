package com.spotzones.di

import com.spotzones.data.backup.BackupManagerImpl
import com.spotzones.data.remote.SpotifyCatalogImpl
import com.spotzones.data.remote.WebApiSpotifyController
import com.spotzones.data.remote.auth.SpotifyAuthImpl
import com.spotzones.data.repository.HistoryRepositoryImpl
import com.spotzones.data.repository.PlaybackPositionRepositoryImpl
import com.spotzones.data.repository.RuleRepositoryImpl
import com.spotzones.data.repository.ZoneRepositoryImpl
import com.spotzones.data.settings.SettingsRepositoryImpl
import com.spotzones.domain.backup.BackupManager
import com.spotzones.domain.repository.HistoryRepository
import com.spotzones.domain.repository.PlaybackPositionRepository
import com.spotzones.domain.repository.RuleRepository
import com.spotzones.domain.repository.SettingsRepository
import com.spotzones.domain.repository.ZoneRepository
import com.spotzones.domain.spotify.SpotifyAuth
import com.spotzones.domain.spotify.SpotifyCatalog
import com.spotzones.domain.spotify.SpotifyController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds domain ports to their concrete data-layer implementations. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton abstract fun zoneRepository(impl: ZoneRepositoryImpl): ZoneRepository
    @Binds @Singleton abstract fun ruleRepository(impl: RuleRepositoryImpl): RuleRepository
    @Binds @Singleton abstract fun historyRepository(impl: HistoryRepositoryImpl): HistoryRepository
    @Binds @Singleton abstract fun settingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
    @Binds @Singleton abstract fun playbackPositionRepository(impl: PlaybackPositionRepositoryImpl): PlaybackPositionRepository
    @Binds @Singleton abstract fun backupManager(impl: BackupManagerImpl): BackupManager

    @Binds @Singleton abstract fun spotifyAuth(impl: SpotifyAuthImpl): SpotifyAuth
    @Binds @Singleton abstract fun spotifyCatalog(impl: SpotifyCatalogImpl): SpotifyCatalog

    /**
     * The active [SpotifyController]. Swap this single binding to an App Remote-backed
     * implementation to use the SDK instead of the Web API (see README).
     */
    @Binds @Singleton abstract fun spotifyController(impl: WebApiSpotifyController): SpotifyController
}
