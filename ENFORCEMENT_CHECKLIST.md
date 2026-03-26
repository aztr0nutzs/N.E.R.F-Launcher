This project must result in:
A fully reactive, high-performance, deeply customizable Android launcher with a cohesive Nerf-inspired visual system and zero architectural shortcuts.
If it feels like:
a demo → ❌ fail
a themed skin → ❌ fail
inconsistent UI → ❌ fail
It should feel like:
a real product
a system-level experience
a polished, responsive UI engine
🧱 NON-NEGOTIABLE ARCHITECTURE RULES
✅ SINGLE SOURCE OF TRUTH
AppConfig is the ONLY state authority
No duplicate state anywhere
❌ FAIL IF:
ThemeManager stores its own state
Taskbar has independent config
Settings write directly to UI
✅ CENTRALIZED DATA FLOW
Required flow:

User Action → ConfigRepository → AppConfig → UI Observers → UI Update
❌ FAIL IF:
UI is updated manually
Functions like applyTheme() exist
Direct view manipulation bypasses config
✅ FULL REACTIVITY
ALL UI must:
Observe config changes
Update instantly
Never require restart
❌ FAIL IF:
Activity recreation is used
RecyclerView fully reloads unnecessarily
UI flickers or resets
🎨 THEME SYSTEM REQUIREMENTS
✅ TRUE THEME ENGINE
Each theme MUST define:
primary color
secondary color
accent color
background style
glow intensity
✅ ZERO HARDCODED VALUES
❌ FAIL IF:
#FFxxxx exists in XML
setBackgroundColor() uses literals
Any UI element defines its own color
✅ CONSISTENT THEME APPLICATION
ALL UI must derive from:
ThemeManager → AppConfig → NerfTheme
❌ FAIL IF:
Taskbar uses different colors than app grid
HUD ignores theme
Mixed styling approaches exist
🎯 ICON SYSTEM REQUIREMENTS
✅ CENTRAL ICON RESOLUTION
ALL icons MUST go through:
IconProvider
✅ ICON FALLBACK CHAIN

Custom Pack → Selected Pack → System Icon
❌ FAIL IF:
Adapter loads icons directly
Missing icons crash or show blank
No caching exists
✅ PERFORMANCE
Icons cached efficiently
No repeated loading during scroll
❌ FAIL IF:
Jank while scrolling
Icons reload unnecessarily
⚙️ SETTINGS SYSTEM REQUIREMENTS
✅ FULLY FUNCTIONAL CONTROLS
Each setting MUST:
Persist
Apply instantly
Reflect in UI
✅ NO FAKE SETTINGS
❌ FAIL IF:
Setting exists but does nothing
UI changes but logic doesn’t
Requires restart to apply
🧨 TASKBAR SYSTEM REQUIREMENTS
✅ FULL CONFIG CONTROL
Must support:
size
icon size
transparency
background style
enable/disable
✅ FULL THEME INTEGRATION
❌ FAIL IF:
Taskbar styling is hardcoded
Doesn’t update with theme changes
⚡ PERFORMANCE REQUIREMENTS
✅ SMOOTH UI
No lag when scrolling
No dropped frames
✅ EFFICIENT DATA HANDLING
RecyclerView uses DiffUtil
No full refresh spam
✅ THREADING
App loading is async
No main-thread blocking
🧠 CODE QUALITY REQUIREMENTS
✅ CLEAN STRUCTURE
No duplicate logic
No unused classes
No dead code
✅ NO PLACEHOLDERS
❌ FAIL IF:
“TODO”
“mock”
empty functions
fake data
🚨 CRITICAL FAILURE CONDITIONS (AUTO-REJECT)
Immediately reject ANY AI output that:
Removes working features
Simplifies existing logic
Breaks UI layout
Introduces hardcoded values
Duplicates state
Adds manual UI updates
Downgrades performance
🔍 INSPECTION CHECKLIST (USE THIS EVERY TIME)
🧾 STRUCTURE
[ ] All expected files exist
[ ] No misplaced files
[ ] Clean package organization
🎨 THEME
[ ] No hardcoded colors
[ ] All UI pulls from theme
[ ] Theme updates instantly
🎯 ICONS
[ ] All icons via IconProvider
[ ] Cache implemented
[ ] No direct drawable usage
⚙️ SETTINGS
[ ] All settings persist
[ ] All settings apply instantly
[ ] No fake toggles
🧨 TASKBAR
[ ] Fully configurable
[ ] Reacts to config changes
[ ] No hardcoded layout values
⚡ PERFORMANCE
[ ] Smooth scrolling
[ ] No unnecessary reloads
[ ] Async operations in place
🧠 ARCHITECTURE
[ ] Single source of truth enforced
[ ] No duplicate state
[ ] Reactive UI everywhere
🧭 WHAT “DONE” ACTUALLY LOOKS LIKE
You’re done when:
Changing a theme updates everything instantly
Switching icon packs updates without reload
Adjusting settings feels real-time and fluid
UI looks cohesive, not patched together
Codebase has zero ambiguity in data flow