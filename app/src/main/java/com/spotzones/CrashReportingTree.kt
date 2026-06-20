package com.spotzones

import android.util.Log
import timber.log.Timber

/**
 * Release [Timber.Tree] that forwards warnings and errors towards the crash-reporting backend.
 *
 * To keep [SpotZonesApp] free of a DI lookup at the moment of planting, the bridge is installed
 * lazily via [install]. Until then it degrades to standard logcat at WARN+.
 */
class CrashReportingTree : Timber.Tree() {

    override fun isLoggable(tag: String?, priority: Int): Boolean = priority >= Log.WARN

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (!isLoggable(tag, priority)) return
        reporter?.log(message)
        if (priority >= Log.ERROR && t != null) {
            reporter?.recordException(t)
        }
    }

    companion object {
        @Volatile
        private var reporter: com.spotzones.core.crash.CrashReporter? = null

        /** Wires the crash backend once Hilt has constructed it. */
        fun install(crashReporter: com.spotzones.core.crash.CrashReporter) {
            reporter = crashReporter
        }
    }
}
