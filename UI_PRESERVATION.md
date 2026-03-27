Absolute UI Preservation Mandate

You are working on an existing Android project with an already-established UI. Your task is strictly limited to architecture, stability, threading, data flow, compile fixes, and internal logic corrections. You must preserve the current UI exactly as-is.

Non-negotiable UI protection rules
Do not change the UI in any way whatsoever
Do not modify layout structure
Do not modify XML layout hierarchy
Do not modify Compose layout structure
Do not change padding, margins, spacing, alignment, sizing, or constraints
Do not change fonts, font sizes, font weights, letter spacing, or text casing
Do not change colors, backgrounds, gradients, drawables, icons, icon sizes, icon placement, or transparency
Do not change animations, transitions, motion timing, alpha behavior, or visibility behavior
Do not change button style, card style, list item style, HUD styling, launcher grid styling, wallpaper behavior, taskbar styling, widget styling, or settings row styling
Do not change any string text shown to the user
Do not rename tabs, labels, sections, buttons, headers, titles, or preference labels
Do not reorder UI elements
Do not remove any existing UI element
Do not add any new visible UI element
Do not replace any existing resource unless it is required to fix a compile error and the replacement is visually identical in practice
Do not re-theme, modernize, clean up, simplify, restyle, or “improve” anything visual
Do not touch any file in res/layout, res/drawable, res/color, res/font, res/values, theme files, Compose UI files, adapter item layouts, or UI-facing assets unless a requested fix absolutely requires it
If a requested internal fix can be completed without touching UI files, you must not touch them
If a requested internal fix does require touching a UI-adjacent file, only change the minimum exact line(s) required for correctness and preserve visual output exactly
Functional interpretation rule

If you encounter a tempting opportunity to:

refactor UI code,
simplify a screen,
improve readability of layout code,
fix “minor” visual inconsistencies,
optimize rendering by changing visual behavior,
rename resources for cleanliness,
consolidate themes,
restyle controls,
replace drawables,
alter adapter presentation logic,

you must not do it.

This task is not a UI task.
This task is not a design task.
This task is not a cleanup pass on visuals.
This task is not permission to “help” by changing the look or feel.

Required preservation standard

From the user’s perspective, the app must look pixel-identical before and after the change. The only acceptable differences are:

crashes fixed
threading fixed
duplicated observers removed
invalid loops stopped
state flow corrected
compile/runtime failures resolved

No other visible difference is allowed.

File modification restrictions

Before editing any file, classify it as one of the following:

Internal logic only
UI-adjacent but non-visual
Directly visual

Apply these rules:

Prefer changing internal logic only files
Avoid UI-adjacent but non-visual files unless necessary
Do not edit directly visual files unless there is no other technically correct option
If a directly visual file must be edited, change only the exact minimum required line(s), and explicitly justify why no non-visual file could solve it
Mandatory output requirements

In the final response, include a dedicated section titled:

UI Preservation Verification

In that section, explicitly list:

every UI file that was not changed
every UI-adjacent file that was changed
why each changed file did not alter visual output
confirmation that no layout, styling, text, color, spacing, animation, or visible behavior was changed
Hard fail conditions

Your solution is invalid if it does any of the following:

changes visual output
modifies layout hierarchy unnecessarily
updates strings or resources unnecessarily
changes adapter presentation behavior instead of only fixing data flow
introduces UI regressions while fixing internal architecture
performs “bonus” cleanup outside the exact requested scope

If there is any conflict between fixing the architecture and preserving the current UI, choose the approach that preserves the UI and solve the issue internally.
