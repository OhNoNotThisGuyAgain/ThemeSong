package com.spotzones.di

import com.spotzones.core.crash.CrashReporter
import com.spotzones.core.crash.NoOpCrashReporter
import com.spotzones.domain.ai.ContextRecommender
import com.spotzones.domain.ai.HeuristicPlaylistPredictor
import com.spotzones.domain.ai.NextZonePredictor
import com.spotzones.domain.ai.NoOpContextRecommender
import com.spotzones.domain.ai.NoOpNextZonePredictor
import com.spotzones.domain.ai.NoOpScheduleLearner
import com.spotzones.domain.ai.PlaylistPredictor
import com.spotzones.domain.ai.ScheduleLearner
import com.spotzones.domain.engine.ConditionEvaluator
import com.spotzones.domain.engine.RuleEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides framework-free domain singletons and the AI/crash abstractions.
 *
 * The AI bindings resolve to deterministic no-op/heuristic implementations today; shipping real
 * models later is a binding swap, with no change to call sites — exactly the extensibility the
 * "prepare interfaces for future AI features" requirement asks for.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton fun conditionEvaluator(): ConditionEvaluator = ConditionEvaluator()

    @Provides @Singleton fun ruleEngine(evaluator: ConditionEvaluator): RuleEngine = RuleEngine(evaluator)

    @Provides @Singleton fun crashReporter(): CrashReporter = NoOpCrashReporter()

    @Provides @Singleton fun playlistPredictor(): PlaylistPredictor = HeuristicPlaylistPredictor()
    @Provides @Singleton fun nextZonePredictor(): NextZonePredictor = NoOpNextZonePredictor()
    @Provides @Singleton fun scheduleLearner(): ScheduleLearner = NoOpScheduleLearner()
    @Provides @Singleton fun contextRecommender(): ContextRecommender = NoOpContextRecommender()
}
