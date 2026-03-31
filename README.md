
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

Repo-side asset wiring is intentional:
- Android assets are loaded from `app/src/assets` via `app/build.gradle`
- Android resources remain under `app/src/main/res`
