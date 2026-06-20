# Contributing to PlayZones

Thanks for your interest! This project aims for a premium, production-quality bar. Please keep the following in mind.

## Principles
- **Clean Architecture boundaries are sacred.** `domain` must stay free of Android imports. Add new capabilities as a **port** in `domain` + an **adapter** elsewhere, bound in a Hilt module.
- **No placeholders, TODOs, or fake implementations** in merged code. If something is intentionally deferred (e.g. AI), express it as an interface with a deterministic default.
- **Fail with `Outcome`, not exceptions**, across layer boundaries. The app must degrade gracefully.
- **Keep the rule engine pure.** No IO in `domain/engine`.

## Code style
- Kotlin official style (`kotlin.code.style=official`). 4-space indent, trailing commas.
- Prefer **self-documenting code**; comment the *why*, not the *what*.
- One public type per file where reasonable; small, single-responsibility classes — no god classes.
- Compose: hoist state, avoid unnecessary recomposition, pass lambdas not ViewModels into leaf composables.

## Tests
- Any change to `domain/engine`, `automation`, mappers, or serialization **must** come with unit tests.
- Run `./gradlew testDebugUnitTest` before opening a PR.
- Pure logic → JVM tests; Android-dependent helpers → Robolectric; flows → Turbine.

## Commits & branches
- Branch from the default branch: `feature/…`, `fix/…`, `docs/…`.
- Conventional, imperative commit subjects: `feat: add Wi-Fi trigger`, `fix: debounce search`.
- Keep PRs focused and reviewable.

## PR checklist
- [ ] Layer boundaries respected (no Android in `domain`)
- [ ] New ports have Hilt bindings and (where logic-bearing) tests
- [ ] `./gradlew testDebugUnitTest` passes
- [ ] No secrets committed; `secrets.properties` stays git-ignored
- [ ] User-facing strings in `strings.xml`, content descriptions on actionable icons
- [ ] Updated docs if behaviour or setup changed

## Adding a new trigger (worked example)
1. Add a `Condition` subtype in `domain/model/Condition.kt` with a stable `@SerialName`.
2. Handle it in `ConditionEvaluator` (return `false` when its signal is missing).
3. Surface the signal in `EvaluationContext` + populate it in `ContextProviderImpl`.
4. Expose it in the Zone editor UI.
5. Add `ConditionEvaluatorTest` cases.
