# ENFORCEMENT_CHECKLIST.md

This project MUST result in:

A fully reactive, high-performance, assistant-driven launcher with:
- precision overlay mapping
- real interaction systems
- cohesive NERD visual identity
- zero architectural shortcuts

-----------------------------------

# 🧱 NON-NEGOTIABLE RULES

## SINGLE SOURCE OF TRUTH

AppConfig = ONLY authority

❌ FAIL IF:
- duplicate state exists
- UI reads preferences directly
- ViewModel stores config independently

---

## CENTRALIZED DATA FLOW

User → ViewModel → Config → State → UI

❌ FAIL IF:
- manual UI updates
- applyTheme() patterns
- direct view manipulation

---

## FULL REACTIVITY

UI must:
- update instantly
- never require restart

❌ FAIL IF:
- activity recreation
- full recomposition abuse
- flickering UI

-----------------------------------

# 🤖 ASSISTANT SYSTEM REQUIREMENTS

## MUST HAVE:

- dedicated AssistantScreen
- real chat system
- state-driven messages
- working input field
- working send + mic

❌ FAIL IF:
- fake chat
- static UI
- no interaction loop

---

## VISUAL SYSTEM

- background image = fixed backplate
- UI = overlays only

❌ FAIL IF:
- recreating UI with Compose shapes
- misaligned overlays

-----------------------------------

# 🎯 OVERLAY SYSTEM (CRITICAL)

## MUST:

- use normalized coordinates
- map to image rect
- support all devices

## REQUIRED REGIONS:

- chat panel
- input field
- send button
- mic
- emoji
- reactor
- left controls
- dock
- hand node

❌ FAIL IF:
- pixel-based positioning
- drift across devices
- overlapping hitboxes

-----------------------------------

# ⚛️ REACTOR SYSTEM

## MUST:

- support:
  - core tap
  - sector detection
- angle-based logic
- correct boundaries

❌ FAIL IF:
- wrong sector triggers
- dead zones trigger actions
- core overlaps sectors

-----------------------------------

# 🎨 THEME SYSTEM

## MUST:

- support multiple assistant themes
- no hardcoded colors
- consistent application

❌ FAIL IF:
- hex values exist
- inconsistent UI colors
- assistant ignores theme

-----------------------------------

# 🎯 ICON SYSTEM

## MUST:

- use IconProvider
- cache icons
- fallback chain works

❌ FAIL IF:
- direct drawable usage
- no caching
- missing fallback

-----------------------------------

# ⚙️ SETTINGS SYSTEM

## MUST:

- persist
- apply instantly
- affect real behavior

❌ FAIL IF:
- fake toggles
- restart required
- UI only changes visually

-----------------------------------

# 🧨 TASKBAR SYSTEM

## MUST:

- be config-driven
- react to theme
- support customization

❌ FAIL IF:
- hardcoded values
- static layout

-----------------------------------

# ⚡ PERFORMANCE

## MUST:

- smooth UI
- async loading
- minimal recomposition

❌ FAIL IF:
- jank
- blocking calls
- heavy redraws

-----------------------------------

# 🧠 CODE QUALITY

## MUST:

- no dead code
- no duplication
- no placeholders

❌ FAIL IF:
- TODO
- mock logic
- unused classes

-----------------------------------

# 🔍 INSPECTION CHECKLIST

## STRUCTURE
[ ] correct file organization  
[ ] no misplaced files  

## ASSISTANT
[ ] chat functional  
[ ] input works  
[ ] overlays aligned  

## OVERLAY
[ ] normalized mapping  
[ ] correct hitboxes  
[ ] no drift  

## REACTOR
[ ] core works  
[ ] sectors accurate  

## THEME
[ ] no hardcoded colors  
[ ] consistent application  

## ICONS
[ ] provider used  
[ ] cache active  

## SETTINGS
[ ] persist  
[ ] apply instantly  

## TASKBAR
[ ] configurable  
[ ] reactive  

## PERFORMANCE
[ ] smooth  
[ ] async  

## ARCHITECTURE
[ ] single source of truth  
[ ] no duplicate state  

-----------------------------------

# 🧭 WHAT “DONE” ACTUALLY LOOKS LIKE

You’re done when:

- assistant feels alive, not static  
- reactor responds precisely to touch  
- overlays align perfectly on all devices  
- chat system feels real and responsive  
- theme switching affects everything instantly  
- UI looks engineered, not assembled  

If it feels like:
- a demo → FAIL  
- a wallpaper with buttons → FAIL  
- inconsistent UI → FAIL  

It must feel like:
a system
a tool
a product
