# Implementation Plan: Hayai UI Migration

## Phase 1: Shell & Core Navigation Parity
- [ ] Task: Audit and clean up legacy SY-style navigation preferences.
    - [ ] Identify and remove SY-style nav customization preferences in `SettingsAppearanceScreen.kt`.
    - [ ] Ensure `SHOW_RECENTLY_UPDATED` and `SHOW_RECENTLY_READ` route exclusively to `Recents`.
    - [ ] Verify 3-destination contract in `HomeScreen` is robust and handles deep links correctly.
- [ ] Task: Conductor - User Manual Verification 'Phase 1: Shell & Core Navigation Parity' (Protocol in workflow.md)

## Phase 2: Library Surface Audit & Realignment
- [ ] Task: Write tests for staggered grid preference and state persistence.
- [ ] Task: Deep-diff category handling logic against `LibraryController.kt` from Yokai.
    - [ ] Align category tab spacing and interaction behavior.
    - [ ] Realign drag-and-drop interaction feedback.
- [ ] Task: Verify Hayai-only features (Dynamic Categories) are correctly embedded in the new structure.
- [ ] Task: Conductor - User Manual Verification 'Phase 2: Library Surface Audit & Realignment' (Protocol in workflow.md)

## Phase 3: Browse Surface Parity
- [ ] Task: Write tests for source-owned search state and routing.
- [ ] Task: Refactor Source headers and item treatment to match Yokai's dense elevated cards.
- [ ] Task: Realign Browse-root search to use the root search affordance for global-search routing.
- [ ] Task: Conductor - User Manual Verification 'Phase 3: Browse Surface Parity' (Protocol in workflow.md)

## Phase 4: Manga Details & Reader Chrome
- [ ] Task: Write tests for the unified hero header and chapter action hierarchy.
- [ ] Task: Rebuild the chapter header module to match Yokai's sort/filter/display state treatment.
- [ ] Task: Re-diff reader chrome (top bar actions) and settings sheet tab order.
- [ ] Task: Realign in-reader chapter sheet to highlight active chapter and show context in header.
- [ ] Task: Conductor - User Manual Verification 'Phase 4: Manga Details & Reader Chrome' (Protocol in workflow.md)
