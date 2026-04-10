# MASTER_INSPECTION.md

You are performing a FULL, DEEP, READ-ONLY INSPECTION of the NERD Launcher Android project.

STRICT RULES:
- DO NOT MODIFY FILES
- DO NOT WRITE CODE
- DO NOT SUGGEST PARTIAL FIXES
- DO NOT ASSUME ANYTHING IS CORRECT
- TRACE REAL EXECUTION PATHS
- VERIFY AGAINST ACTUAL UI BEHAVIOR

GOAL:
Determine whether the project meets production-level standards for:
- architecture
- reactivity
- performance
- UI precision
- assistant system fidelity
- overlay mapping accuracy

-----------------------------------

# 🧠 CORE ARCHITECTURE VALIDATION

## 1. SINGLE SOURCE OF TRUTH

VERIFY:
- AppConfig (or equivalent) exists and is authoritative
- ALL systems read from it:
  - Theme system
  - Assistant system
  - Overlay system
  - Taskbar
  - Settings

FLAG:
- Any duplicate state
- ViewModel storing persistent UI config
- UI directly reading SharedPreferences

---

## 2. DATA FLOW INTEGRITY

TRACE FULL PATH:

User Interaction →
ViewModel →
ConfigRepository →
AppConfig →
StateFlow →
Composable UI

VERIFY:
- No manual UI refresh calls
- No direct UI manipulation
- No "applyTheme()" style functions

FLAG:
- Imperative UI updates
- bypassed state flow
- inconsistent patterns

-----------------------------------

# ⚡ REACTIVE UI SYSTEM CHECK

VERIFY:

- ALL UI driven by observable state
- Assistant screen updates without recomposition issues
- Overlay system responds to state instantly

FLAG:
- Activity recreation
- full recomposition for small changes
- flickering overlays

-----------------------------------

# 🤖 ASSISTANT SYSTEM AUDIT (NEW - CRITICAL)

## VERIFY:

### 1. STRUCTURE
- Dedicated AssistantScreen
- Dedicated ViewModel
- State-driven chat system
- Input → Response → UI update loop

### 2. BACKGROUND RENDERING MODEL
- Assistant UI uses:
  - static image backplate
  - dynamic overlay system
- NO layout recreation of artwork

### 3. INTERACTION SYSTEM
- Reactor interactions implemented
- Hitboxes mapped precisely
- Chat input fully functional

FLAG:
- fake chat UI
- placeholder assistant responses
- static-only assistant visuals

-----------------------------------

# 🎯 OVERLAY / HITBOX SYSTEM AUDIT (NEW - CRITICAL)

VERIFY:

### 1. NORMALIZED COORDINATE SYSTEM
- All overlays use normalized (0–1) mapping
- Based on rendered image rect

### 2. HITBOX ACCURACY
- Each region correctly mapped:
  - chat panel
  - input field
  - send button
  - dock icons
  - reactor core + sectors
  - left action stack
  - hand node

### 3. DEVICE SCALING
- Works across aspect ratios
- No drift on different screen sizes

FLAG:
- hardcoded pixel positioning
- incorrect alignment on different devices
- overlapping hitboxes

-----------------------------------

# ⚛️ REACTOR SYSTEM AUDIT

VERIFY:

- Core tap detection works
- Sector detection uses angle-based logic
- No overlap between core and sectors
- Feedback visually matches interaction

FLAG:
- incorrect sector triggering
- dead zones triggering actions
- inconsistent detection

-----------------------------------

# 🎨 THEME SYSTEM AUDIT

VERIFY:

- Supports:
  - multiple assistant themes
  - background image switching
  - overlay color adaptation

- NO hardcoded colors

FLAG:
- hex colors in code
- theme inconsistencies
- assistant UI ignoring theme

-----------------------------------

# 🎯 ICON SYSTEM AUDIT

VERIFY:

- IconProvider used globally
- caching implemented
- no direct drawable usage

FLAG:
- icon flickering
- repeated loading
- inconsistent sources

-----------------------------------

# ⚙️ SETTINGS SYSTEM AUDIT

VERIFY:

- settings persist
- apply instantly
- affect:
  - assistant theme
  - overlays
  - behavior

FLAG:
- fake toggles
- restart-required settings
- disconnected UI

-----------------------------------

# 🧨 TASKBAR SYSTEM AUDIT

VERIFY:

- config-driven
- theme-aware
- responsive to changes

FLAG:
- hardcoded layout
- no reactivity

-----------------------------------

# ⚡ PERFORMANCE AUDIT

VERIFY:

- no main-thread blocking
- overlay rendering efficient
- image rendering optimized
- no unnecessary recomposition

FLAG:
- jank
- overdraw
- heavy layout nesting

-----------------------------------

# 🧠 CODE QUALITY AUDIT

SCAN FOR:

- duplicate logic
- unused classes
- dead code
- placeholder logic

FLAG:
- TODO
- mock data
- incomplete features

-----------------------------------

# 🚨 CRITICAL FAILURE CONDITIONS

AUTO-FAIL IF:

- overlay system not normalized
- assistant not interactive
- reactor not functional
- UI alignment inconsistent
- duplicate state exists
- hardcoded values present

-----------------------------------

# 📊 OUTPUT FORMAT

1. OVERALL SCORE (0–10)

2. CATEGORY SCORES:
- Architecture
- Reactivity
- Assistant System
- Overlay Accuracy
- Theme System
- Icon System
- Settings
- Taskbar
- Performance

3. CRITICAL FAILURES

4. STRUCTURAL WEAKNESSES

5. MISSED REQUIREMENTS

6. RISK ASSESSMENT

7. FINAL VERDICT

-----------------------------------

REMEMBER:
This is not a launcher skin.

This is a system-level interactive UI engine.

Anything less = fail.
