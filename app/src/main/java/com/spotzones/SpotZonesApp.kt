package com.spotzones

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.spotzones.core.notification.NotificationChannels
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import timber.log.Timber

/**
 * Application entry point.
 *
 * Responsibilities are intentionally minimal: initialise logging, register notification
 * channels and expose a Hilt-aware [Configuration] so WorkManager can inject dependencies
 * into workers. Everything else is owned by feature-scoped components.
 */
@HiltAndroidApp
class SpotZonesApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var notificationChannels: NotificationChannels

    @Inject lateinit var crashReporter: com.spotzones.core.crash.CrashReporter

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            CrashReportingTree.install(crashReporter)
            Timber.plant(CrashReportingTree())
        }
        notificationChannels.ensureCreated()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.WARN)
            .build()
}
