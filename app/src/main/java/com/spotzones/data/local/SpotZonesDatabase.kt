package com.spotzones.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.spotzones.data.local.dao.HistoryDao
import com.spotzones.data.local.dao.PlaylistPositionDao
import com.spotzones.data.local.dao.RuleDao
import com.spotzones.data.local.dao.ZoneDao
import com.spotzones.data.local.entity.PlaylistPositionEntity
import com.spotzones.data.local.entity.RuleEntity
import com.spotzones.data.local.entity.TransitionHistoryEntity
import com.spotzones.data.local.entity.ZoneEntity

@Database(
    entities = [
        ZoneEntity::class,
        RuleEntity::class,
        TransitionHistoryEntity::class,
        PlaylistPositionEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class SpotZonesDatabase : RoomDatabase() {
    abstract fun zoneDao(): ZoneDao
    abstract fun ruleDao(): RuleDao
    abstract fun historyDao(): HistoryDao
    abstract fun playlistPositionDao(): PlaylistPositionDao

    companion object {
        const val NAME = "spotzones.db"
    }
}
