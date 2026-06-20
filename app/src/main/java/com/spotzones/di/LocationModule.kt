package com.spotzones.di

import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.spotzones.domain.location.ContextProvider
import com.spotzones.domain.location.GeofenceManager
import com.spotzones.domain.location.LocationProvider
import com.spotzones.location.ContextProviderImpl
import com.spotzones.location.FusedLocationProviderImpl
import com.spotzones.location.geofence.GeofenceManagerImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocationProvidersModule {
    @Provides @Singleton
    fun fusedLocationClient(@ApplicationContext context: Context): FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @Provides @Singleton
    fun geofencingClient(@ApplicationContext context: Context): GeofencingClient =
        LocationServices.getGeofencingClient(context)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class LocationBindingsModule {
    @Binds @Singleton abstract fun locationProvider(impl: FusedLocationProviderImpl): LocationProvider
    @Binds @Singleton abstract fun geofenceManager(impl: GeofenceManagerImpl): GeofenceManager
    @Binds @Singleton abstract fun contextProvider(impl: ContextProviderImpl): ContextProvider
}
