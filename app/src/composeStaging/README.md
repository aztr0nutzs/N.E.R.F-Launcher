# Compose Reactor Staging

This subtree preserves the future premium Compose reactor without making the current launcher depend on Compose.

Current state:
- Production home reactor remains the View implementation at `app/src/main/java/com/nerf/launcher/ui/reactor/ReactorModuleView.kt`.
- This staging subtree is not part of the active Android source sets.
- Compose is not enabled in `app/build.gradle`, so these files are stored for later migration only.

Package layout:
- Premium path: `com.nerf.launcher.ui.reactor.compose.premium`
- Prototype holdover: `com.nerf.launcher.ui.reactor.compose.prototype`

Future migration requirements:
1. Decide whether the premium reactor replaces the View home reactor or ships behind a dedicated build flavor/feature flag.
2. Enable Compose in `app/build.gradle` with `buildFeatures { compose = true }`.
3. Add the required Compose BOM/runtime/ui/material dependencies and set the matching Kotlin compiler extension version for the repo's Kotlin/AGP level.
4. Move or register the `composeStaging` source set intentionally instead of relying on it as dormant storage.
5. Bridge launcher-owned callbacks from `ReactorCoordinator` and `AssistantController` into the Compose reactor so sector taps, assistant state, and sync previews remain launcher-owned.
6. Replace or remove prototype-only demo APIs before wiring any Compose screen into `MainActivity`.
