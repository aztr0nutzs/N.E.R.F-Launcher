
# Master Project Blueprint for N.E.R.F. Launcher

## Project goal

Transform the launcher into a stable, premium, NERF-branded command-center experience with:
- one coherent launcher entry flow
- a central reactor-led home shell
- launcher-owned assistant overlay
- reactor-launched tools and diagnostics
- maintainable component architecture
- zero accidental UI regressions

## Core development principles

1. Preserve existing working behavior unless the task explicitly changes it.
2. Preserve current UI outside the exact task scope.
3. Do not replace live UI with static posters or decorative image dumps.
4. Do not merge competing prototype systems directly into production without choosing one.
5. Every implementation task must end with concrete verification.
6. Every architecture choice must support future fidelity upgrades.

## Current strategic decisions

### Production launcher shell
- Keep `MainActivity` as the launcher home entry.

### First production reactor integration
- Use the View/XML reactor path first.

### Assistant
- Use the assistant bundle as the voice/personality layer.
- Build a launcher-owned assistant overlay around it.
- Treat real intelligence as a separate backend bridge.

### Node Hunter
- Integrate as a reactor-launched module, not permanent home clutter.

### Compose reactor files
- Preserve as a Phase 2 premium upgrade path.
- Do not make Compose and View reactors co-equal production systems at the same time.

## Main home-shell target

The launcher home shell should have:
- one dominant central reactor
- a top status strip
- a right-side assistant lane
- a left-side controls/settings lane
- a bottom tools/dock lane
- surrounding framed system modules

## Reactor behavior map

### Center core
- tap: pulse and expand state summary
- double tap: wake assistant
- long press: open full Reactor Hub or advanced controls

### Top sector
- system / diagnostics / network
- tap: preview system state
- long press: launch Node Hunter or diagnostics

### Right sector
- assistant
- tap: open assistant overlay
- long press: open assistant settings / voice controls

### Bottom sector
- tools / arsenal / modules
- tap: open tools tray
- long press: launch default special module

### Left sector
- settings / themes / controls
- tap: launcher settings
- long press: advanced customization

## Visual role assignment for uploaded reactor assets

### `reactor_skin_primary`
- primary default home core visual target

### `reactor_skin_assistant`
- assistant orb / AI core / right-side assistant visuals

### `reactor_skin_system_card`
- wide status card and diagnostics banner treatment

### `reactor_skin_secondary`
- smaller quick-control or side reactor module

### `reactor_skin_audio`
- special audio-reactive or overdrive state only

### `reactor_home_reference`
- composition and spacing reference for the home shell

### `reactor_detail_reference`
- expanded detail panel / Reactor Hub / assistant detail card reference

## Mandatory implementation order

1. Normalize packages and stage files
2. Stabilize build/resource/runtime issues
3. Integrate one primary reactor into the home shell
4. Add reactor coordination logic
5. Add assistant personality and overlay
6. Add Node Hunter as a module
7. Refine visual fidelity toward uploaded reactor art
8. Revisit Compose premium upgrade only after stability is proven
