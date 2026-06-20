package com.spotzones.core.crash

/**
 * Crash reporting abstraction.
 *
 * The app never depends on Firebase Crashlytics directly. Swapping in Crashlytics later is a
 * one-class change ([NoOpCrashReporter] -> a CrashlyticsCrashReporter) plus a Hilt binding,
 * with zero churn in feature code.
 */
interface CrashReporter {
    fun log(message: String)
    fun setCustomKey(key: String, value: String)
    fun recordException(throwable: Throwable)
}

/**
 * Default implementation used until a real backend is wired in. It deliberately does nothing
 * with the data beyond what Timber already surfaces, so no PII leaves the device by default.
 */
class NoOpCrashReporter : CrashReporter {
    override fun log(message: String) = Unit
    override fun setCustomKey(key: String, value: String) = Unit
    override fun recordException(throwable: Throwable) = Unit
}
