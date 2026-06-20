package com.spotzones.core.coroutines

import javax.inject.Qualifier

/** Distinguishes injected [kotlinx.coroutines.CoroutineDispatcher]s so tests can swap in fakes. */
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class IoDispatcher
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class DefaultDispatcher
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class MainDispatcher

/** Application-scoped [kotlinx.coroutines.CoroutineScope] for fire-and-forget work that outlives a screen. */
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class ApplicationScope
