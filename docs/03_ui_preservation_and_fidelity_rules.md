
# UI Preservation Rules and Visual Fidelity Mandate

## Non-negotiable rule

The current launcher UI must be preserved outside the exact scope of each task. No task may:
- flatten the UI
- simplify the shell into generic Material cards
- replace custom backgrounds with plain fills
- convert live modules into decorative images
- resize or reflow unrelated sections
- remove existing features to make implementation easier

## What must be preserved

### Existing functional behavior
- app loading
- app launching
- taskbar behavior
- settings persistence
- current launcher flow
- any already-working quick controls
- any already-working system module bindings

### Existing visual intent
- black base
- premium HUD framing
- neon accent hierarchy
- centered command-shell composition
- custom module identity
- reactor-led visual direction

## What counts as a regression

### Visual regressions
- panel backgrounds replaced with flat colors
- loss of custom drawables
- weaker spacing hierarchy
- generic typography replacing tuned styles
- loss of reactor prominence
- app tiles reverting to generic cards
- side modules collapsing into bland rectangles
- random image clutter obscuring controls

### Functional regressions
- broken app launch
- broken taskbar
- broken settings navigation
- broken state updates
- dead quick controls
- launcher crashes or missing resource refs
- broken view bindings
- dead long-presses
- bad manifest wiring

## Rules for themed/background logic

### Forbidden
- blindly calling `setBackgroundColor()` on root HUD containers that are supposed to keep custom drawables
- overwriting styled panel assets with flat theme fills

### Required
- tint overlays
- targeted accent updates
- child element accent logic
- drawable tinting where appropriate
- preserve custom panel assets

## Reactor-specific preservation rules

1. One primary home reactor only
2. No side-by-side dual-reactor demo layout on the home screen
3. Reactor remains interactive, not decorative
4. Assistant visuals must sync with reactor states
5. Smaller reactor modules must remain secondary to the main hero core

## Assistant-specific preservation rules

1. Assistant must remain launcher-owned
2. Assistant should not forcibly become a fullscreen chat app
3. Assistant quick actions must map to real launcher actions
4. Personality layer must not be confused with actual intelligence
5. Overlay must remain readable and not block unrelated navigation by default

## Node Hunter preservation rules

1. Launchable module only
2. Not always-present home clutter
3. Clean back navigation to home
4. Same NERF shell language on its screen
5. No pretending prototype scanner code is already production-grade

## Change control rule

If a task requires touching extra files outside the expected scope, the implementation report must explicitly list:
- why the extra file was necessary
- what was changed
- whether the change affects UI or behavior outside the task area

## Verification rule

Every task must end with:
- exact files changed
- exact commands run
- exact verification results
- exact remaining risks
- explicit mention of any unverified areas
