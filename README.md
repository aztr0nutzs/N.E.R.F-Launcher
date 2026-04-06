
# N.E.R.F. Launcher Project Docs + Codex Prompt Pack

This bundle contains the complete planning and prompting package for the N.E.R.F. Launcher project, built around:
- launcher stabilization
- exact UI-preservation rules
- NERF reactor integration
- assistant integration
- Node Hunter / diagnostics integration
- Codex task execution prompts
- inspection prompts
- self-audit prompts
- amendment prompts for correction and containment

## Folder map

- `docs/`
  - Full project blueprint, integration architecture, execution order, UI-preservation rules, and inspection framework.
- `prompts/master/`
  - Master inspection, repo audit, UI-preservation, verification, and correction prompts.
- `prompts/phases/`
  - Concrete Codex prompts organized by phase and task.
- `prompts/amendments/`
  - Extra append-only amendments to tighten Codex behavior if it starts freelancing or causing regressions.
- `prompts/checklists/`
  - Review, receipt, and handoff prompts to force verification discipline.

## Intended use

Use one task prompt at a time with Codex. Review the output after each task. Do not combine multiple large prompts into one task unless a dependency absolutely forces it.

## Core rule

UI preservation is mandatory. The launcher must not be simplified, flattened, materially restyled, or "helpfully" reimagined outside the exact scope of the task being executed.

## Local build requirements

This repository does not commit a machine-specific `local.properties`.

To run Android Gradle tasks that need the SDK, the local environment must provide:
- a valid Android SDK installation
- Android SDK Platform 34
- a `local.properties` file at the repo root with `sdk.dir` pointing at that SDK, or an Android Studio setup that generates it locally
- JDK 17 or a newer JDK supported by the Android Gradle Plugin and Gradle wrapper in this repo
- executable permission on `gradlew` when building from macOS/Linux (`chmod +x ./gradlew`)

Repo-side asset wiring is intentional:
- Android assets are loaded from `app/src/assets` via `app/build.gradle`
- Android resources remain under `app/src/main/res`

## Release validation

Repo-side Gradle and source-set wiring can be inspected without an SDK, but full Android validation still requires a real local Android environment.

What is already repo-proven:
- the Gradle wrapper runs
- the project includes a single Android application module, `:app`
- launcher assets, resources, and manifest declarations live in the expected Android source sets
- shipped non-system icon packs are packaged from `app/src/assets/icon_packs`

What still requires a real Android SDK environment:
- resource compilation and merge
- manifest merge for an actual build variant
- Kotlin/Java compilation through Android tasks
- dexing, packaging, signing, and APK generation
- installation and runtime smoke testing on a device or emulator

## Validation steps after SDK setup

After creating a local `local.properties` with `sdk.dir=...` (you can copy `local.properties.example`) or opening the project in Android Studio with a configured SDK, run:

macOS/Linux:
- `./gradlew help`
- `./gradlew :app:assembleDebug`
- `./gradlew :app:installDebug`

Windows (PowerShell/cmd):
- `.\gradlew.bat help`
- `.\gradlew.bat :app:assembleDebug`
- `.\gradlew.bat :app:installDebug`

If release packaging needs to be checked as part of a release candidate, also run:
- macOS/Linux: `./gradlew :app:assembleRelease`
- Windows: `.\gradlew.bat :app:assembleRelease`

## Minimal smoke-test checklist

After `:app:installDebug` succeeds on a device or emulator:
- Launch the app and confirm the launcher opens as the Home activity without layout or navigation regressions.
- Open icon-pack settings, select `nerf` and `minimal`, and confirm covered apps render pack icons while uncovered apps still fall back to system icons.
- Change the taskbar background style, leave the screen, return, and confirm the selection persists and the visible taskbar output remains correct.
- Change the launcher theme, confirm the shell updates live, then open Taskbar Settings and confirm it follows the same launcher theme flow.
- Return to the main launcher and confirm the HUD, taskbar, app drawer, and icon labels still behave normally after those setting changes.
