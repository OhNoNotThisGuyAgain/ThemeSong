package com.spotzones.di

import android.content.Context
import androidx.room.Room
import com.spotzones.data.local.SpotZonesDatabase
import com.spotzones.data.local.dao.HistoryDao
import com.spotzones.data.local.dao.PlaylistPositionDao
import com.spotzones.data.local.dao.RuleDao
import com.spotzones.data.local.dao.ZoneDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): SpotZonesDatabase =
        Room.databaseBuilder(context, SpotZonesDatabase::class.java, SpotZonesDatabase.NAME)
            // No destructive fallback: real migrations will be added per schema bump to protect user data.
            .build()

    @Provides fun zoneDao(db: SpotZonesDatabase): ZoneDao = db.zoneDao()
    @Provides fun ruleDao(db: SpotZonesDatabase): RuleDao = db.ruleDao()
    @Provides fun historyDao(db: SpotZonesDatabase): HistoryDao = db.historyDao()
    @Provides fun playlistPositionDao(db: SpotZonesDatabase): PlaylistPositionDao = db.playlistPositionDao()
}
