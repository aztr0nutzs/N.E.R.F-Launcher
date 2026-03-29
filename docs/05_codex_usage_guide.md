
# Codex Usage Guide for This Project

## One-task rule
Run one prompt per Codex task. Do not bundle multiple large implementation goals into one request.

## Preferred task order
1. Master inspection
2. Phase A stabilization tasks
3. Phase B fidelity tasks
4. Reactor/assistant/module integration tasks
5. Phase C extended surfaces

## Required pattern
For each task:
- use the universal header
- use one phase prompt
- optionally append one amendment
- require the universal footer
- review the receipt
- run the post-task self-audit prompt before moving on

## Recommended pairing examples

### If Codex starts rewriting too much
Append:
- `prompts/amendments/01_strict_change_control_amendment.md`

### If Codex starts flattening the UI
Append:
- `prompts/amendments/02_ui_preservation_amendment.md`
- `prompts/amendments/07_if_background_flattening_detected_amendment.md`

### If build tooling is unavailable
Append:
- `prompts/amendments/05_if_build_tools_missing_amendment.md`

### If you suspect dead or fake logic
Append:
- `prompts/amendments/08_if_fake_logic_detected_amendment.md`

## Golden review sequence after each task
1. read the receipt
2. run the post-task self-audit prompt
3. compare touched files against scope
4. only then continue
