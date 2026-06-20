package com.spotzones.di

import com.spotzones.core.coroutines.ApplicationScope
import com.spotzones.core.coroutines.DefaultDispatcher
import com.spotzones.core.coroutines.IoDispatcher
import com.spotzones.core.coroutines.MainDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
object CoroutinesModule {
    @Provides @IoDispatcher fun io() = Dispatchers.IO
    @Provides @DefaultDispatcher fun defaultDispatcher() = Dispatchers.Default
    @Provides @MainDispatcher fun main() = Dispatchers.Main.immediate

    @Provides
    @Singleton
    @ApplicationScope
    fun applicationScope(@IoDispatcher dispatcher: kotlinx.coroutines.CoroutineDispatcher): CoroutineScope =
        CoroutineScope(SupervisorJob() + dispatcher)
}
