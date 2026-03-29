
# File-by-File Reactor, Assistant, and Node Hunter Integration Blueprint

## New package structure

### Reactor
`app/src/main/java/com/nerf/launcher/ui/reactor/`

### Assistant
`app/src/main/java/com/nerf/launcher/util/assistant/`

### Node Hunter
`app/src/main/java/com/nerf/launcher/ui/nodehunter/`

### Network utilities
`app/src/main/java/com/nerf/launcher/util/network/`

## Source-to-target mapping

### View reactor
Source: `ReactorModuleView.kt`
Target: `app/src/main/java/com/nerf/launcher/ui/reactor/ReactorModuleView.kt`
Role: primary integrated home reactor widget

### Reactor demo activity
Source: `ReactorActivity.kt`
Action: do not keep as a production screen
Extract useful logic into:
- `ReactorCoordinator.kt`
- `MainActivity.kt`
- optional future `ReactorHub` screen

### Reactor staging layout
Source: `activity_reactor_diagnostics.xml`
Action: keep staged off-home as a non-exported diagnostics screen

### Assistant personality layer
Source: `ReactorAssistant.kt`
Target: `app/src/main/java/com/nerf/launcher/util/assistant/ReactorAssistant.kt`
Role: voice line + TTS orchestration layer, not real intelligence backend

### Assistant response bank
Source: `reactor_ai_responses.json`
Target: `app/src/main/res/raw/reactor_ai_responses.json`

### Node Hunter scanner
Source: `LocalNetworkScanner.kt`
Target: `app/src/main/java/com/nerf/launcher/util/network/LocalNetworkScanner.kt`
Role: temporary prototype scanner unless replaced by stronger launcher backend

### Node Hunter view
Source: `NodeHunterGameView.kt`
Target: `app/src/main/java/com/nerf/launcher/ui/nodehunter/NodeHunterGameView.kt`

### Node Hunter activity
Source: uploaded Node Hunter activity prototype
Target: `app/src/main/java/com/nerf/launcher/ui/nodehunter/NodeHunterModuleActivity.kt`

### Node Hunter layout
Source: `activity_node_hunter.xml`
Target: `app/src/main/res/layout/activity_node_hunter.xml`

### Compose reactor staging
Targets:
- `app/src/composeStaging/java/com/nerf/launcher/ui/reactor/compose/premium/ReactorCore.kt`
- `.../premium/ReactorDraw.kt`
- `.../premium/ReactorDrawing.kt`
- `.../premium/ReactorInteractions.kt`
- `.../premium/ReactorState.kt`
- `.../premium/AudioReactive.kt`
- `.../premium/ReactorDemoScreen.kt`
- `.../prototype/LegacyReactorScreen.kt`
Role: future premium upgrade path, not first production merge

Migration note:
- keep Compose staging isolated until the repo explicitly enables Compose in `app/build.gradle`
- keep `ReactorModuleView` as the only production home reactor until a deliberate premium switch plan exists
- when migrating, bridge `ReactorCoordinator` and assistant state into the Compose reactor instead of introducing a second independent home system

## New classes to create

### Reactor orchestration
- `app/src/main/java/com/nerf/launcher/ui/reactor/ReactorCoordinator.kt`

Responsibilities:
- register reactor callbacks
- map sectors to launcher actions
- update reactor visual states
- coordinate assistant wake/listen/respond visuals
- coordinate tools tray and module launches
- keep MainActivity from becoming a garbage heap

### Assistant
- `app/src/main/java/com/nerf/launcher/util/assistant/AssistantController.kt`
- `app/src/main/java/com/nerf/launcher/util/assistant/AssistantOverlayController.kt`
- `app/src/main/java/com/nerf/launcher/util/assistant/AssistantState.kt`
- `app/src/main/java/com/nerf/launcher/util/assistant/AssistantBackendBridge.kt`
- optional future `AiResponseRepository.kt`

### Node Hunter coordination
- `app/src/main/java/com/nerf/launcher/ui/nodehunter/NodeHunterCoordinator.kt`
- optional `NodeModel.kt`

## Layouts to create

### Home reactor include
`app/src/main/res/layout/layout_reactor_home_module.xml`

Must contain:
- `ReactorModuleView`
- status text
- optional caption/chip strip

### Assistant overlay
`app/src/main/res/layout/layout_assistant_overlay.xml`

Must contain:
- assistant orb or mini-reactor
- transcript/messages area
- current state text
- quick actions row
- voice toggle
- mute toggle
- close/minimize action

### Tools tray
`app/src/main/res/layout/layout_reactor_tools_tray.xml`

Must contain:
- Node Hunter tile
- diagnostics tile
- assistant tile
- settings tile
- optional future tools slots

### Optional full Reactor Hub
`app/src/main/res/layout/activity_reactor_hub.xml`

## MainActivity integration

### Files to modify
- `app/src/main/java/com/nerf/launcher/ui/MainActivity.kt`
- `app/src/main/res/layout/activity_main.xml`

### IDs to add or preserve
- `homeReactorView`
- `assistantEntryContainer`
- `assistantOverlayContainer`
- `reactorStatusText`
- `reactorToolsTray`
- `reactorSystemCard`
- `assistantPreviewCard`

## Resource additions

### Raw
- `res/raw/reactor_ai_responses.json`

### Drawable-nodpi
- `reactor_home_reference.png`
- `reactor_detail_reference.png`
- `reactor_skin_audio.png`
- `reactor_skin_assistant.png`
- `reactor_skin_system_card.png`
- `reactor_skin_primary.png`
- `reactor_skin_secondary.png`

### Strings
Add strings for:
- sector labels
- assistant states
- tool titles
- diagnostics titles
- Node Hunter title and actions
- status lines

### Colors
Add or map:
- reactor cyan
- reactor orange
- assistant pink/purple
- tools lime/green
- warning amber
- neutral blue-white

### Dimens
Add:
- reactor hero size
- assistant orb size
- tools tray spacing
- overlay margins
- status strip sizing

## Manifest updates

### Add
- `NodeHunterModuleActivity`

### Do not add
- prototype `ReactorActivity` as a production launcher screen

## Production rules

1. Do not keep the side-by-side dual-reactor demo layout as the actual launcher home.
2. Do not wire `ReactorAssistant` as if it were the real backend.
3. Do not trust `LocalNetworkScanner.kt` as a final production scanning engine without review or replacement.
4. Do not promote both View and Compose reactors to co-equal production systems simultaneously.
