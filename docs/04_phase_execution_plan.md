
# Concrete Execution Plan

## Phase 0 - inspection and repo truth
Run master inspection first:
- identify current launcher flow
- identify current View vs Compose capability
- identify all build/resource blockers
- identify any existing assistant hooks
- identify actual scanner backend quality
- identify runtime background flattening points

## Phase A - stabilization and taskbar/settings/build correctness
- A1: fix missing string references
- A2: fix missing popup theme style
- A3: consolidate theme definitions
- A4: stop theme/background flattening
- A5: make taskbar dynamic
- A6: improve taskbar long-press destination
- A7: restore proper launcher icons
- A8: stabilize settings wiring
- A9: remove safe junk only

## Phase B - UI fidelity groundwork
- B1: upgrade main reactor
- B2: upgrade panel chrome
- B3: refine home-shell composition
- B4: improve app sockets and app tiles
- B5: create separate app drawer surface
- B6: upgrade quick controls into a real control bay
- B7: strengthen dashboard cluster hierarchy
- B8: refine typography
- B9: integrate NERF branding/media intentionally
- B10: refine motion and interaction polish

## Phase R - reactor/assistant/module integration
- R1: normalize uploaded reactor bundle packages and staging
- R2: integrate View reactor into home shell
- R3: add ReactorCoordinator
- R4: add assistant repository, controller, overlay, and personality layer
- R5: wire assistant wake/listen/respond states to reactor visuals
- R6: integrate Node Hunter as a launcher-owned module
- R7: align visuals with uploaded reactor art
- R8: stage Compose premium upgrade path without making it production-critical

## Phase C - extended surfaces
- C1: launcher-owned lock-style surface
- C2: widgets/system surface
- C3: remaining highest-value branded surface

## Stop conditions after every task
Do not continue to the next task until:
- build/resource verification is clean enough for the touched scope
- no obvious UI regressions remain
- task report is honest and file-based
- known risks are documented

## Repo hygiene rules
- one prompt per Codex task
- no giant combined prompts
- force self-audit after each task
- re-inspect after major layout work
