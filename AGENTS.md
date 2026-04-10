# AGENTS.md

This project uses multiple AI agents (Codex, Claude, Gemini) for development.

ALL agents must follow these rules strictly.

-----------------------------------

# 🧠 SYSTEM IDENTITY

This is NOT:
- a basic Android launcher
- a themed UI
- a generic chat interface

This IS:
- an image-backed UI engine
- a precision overlay system
- a reactor-driven assistant interface
- a state-driven Compose application

-----------------------------------

# 🧱 CORE ARCHITECTURE RULES

## SINGLE SOURCE OF TRUTH

AppConfig is the ONLY authority.

❌ NEVER:
- duplicate state
- store config in ViewModels
- read preferences directly in UI

---

## DATA FLOW

User Action → ViewModel → Repository → AppConfig → StateFlow → UI

❌ NEVER:
- manually update UI
- call "applyTheme()"
- mutate views directly

---

## UI MODEL

The assistant screen is:

BACKGROUND (STATIC IMAGE)
+
OVERLAY LAYERS (COMPOSE)

❌ NEVER:
- redraw the UI layout from scratch
- recreate panels with Compose shapes
- move UI elements away from the artwork

-----------------------------------

# 🎯 OVERLAY SYSTEM RULES

## ALL UI IS POSITIONED VIA NORMALIZED COORDINATES

- Range: 0.0 → 1.0
- Based on image rect, NOT screen

❌ NEVER:
- hardcode pixel positions
- assume full-screen image scaling

---

## REQUIRED OVERLAY REGIONS

ALL must exist and be mapped:

- chat panel
- transcript region
- input field
- send button
- mic button
- emoji button
- left control stack
- top modules
- reactor core
- reactor sectors
- hand node
- bottom dock
- dock buttons

---

## HITBOX REQUIREMENTS

- No overlap unless intentional
- Must align visually
- Must scale across devices

❌ FAIL IF:
- tap area != visual element
- drift occurs across screen sizes

-----------------------------------

# ⚛️ REACTOR SYSTEM RULES

- Must use polar coordinate detection
- Must separate:
  - core
  - segmented ring

## Sector mapping is REQUIRED

❌ NEVER:
- use rectangular detection
- approximate angles
- allow overlapping zones

-----------------------------------

# 🤖 ASSISTANT SYSTEM RULES

- Must be state-driven
- Must support:
  - chat input
  - transcript rendering
  - assistant state (idle, listening, thinking, speaking)
- Must NOT be static

❌ FAIL IF:
- UI is decorative only
- no real interaction exists

-----------------------------------

# 🎨 THEME SYSTEM RULES

- Multiple assistant themes supported
- Background image changes per theme
- Overlay colors adapt

❌ NEVER:
- hardcode colors
- mismatch UI across themes

-----------------------------------

# ⚡ PERFORMANCE RULES

- No heavy recomposition
- No main-thread blocking
- Efficient overlay rendering

-----------------------------------

# 🚨 CRITICAL FAILURE CONDITIONS

Immediately reject any change that:

- breaks overlay alignment
- replaces image-backed UI with Compose layout
- introduces hardcoded values
- duplicates state
- removes interaction
- simplifies architecture

-----------------------------------

# 🧭 HOW AGENTS SHOULD WORK

## CODEX
- precise implementation
- file-level changes
- strict adherence to rules

## CLAUDE
- architecture
- mapping
- system reasoning

## GEMINI
- image asset generation ONLY

❌ Gemini must NEVER be used for logic or UI structure.

-----------------------------------

# 🧠 FINAL RULE

Preserve:
- structure
- mapping
- system integrity

Enhance:
- precision
- performance
- polish

NEVER:
- redesign
- simplify
- reinterpret the UI
