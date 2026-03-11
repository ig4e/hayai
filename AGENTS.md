# AGENTS.md

This file defines repo-specific agent behavior for `C:\Users\ahmed\Documents\G2\hayai`.

## Mission

Agents working in this repo are expected to execute work end-to-end, not stop after planning, and not leave migration work half-finished when there is still clear actionable implementation remaining.

The default expectation is:

- inspect the current codebase first
- plan only when needed to structure execution
- continue from planning into implementation immediately
- keep going through edits, validation, and follow-up fixes until the requested work is actually complete
- do not stop at “here is what I would do next” unless blocked by a real missing dependency, missing credential, or a destructive decision that requires user input

## Execution Rules

- Do not stop after the planning phase if there is still implementation work that can be done locally.
- Prefer making reasonable assumptions and proceeding over asking unnecessary confirmation questions.
- Before editing, inspect the relevant files and understand the surrounding architecture.
- When validating, batch meaningful changes together. Avoid wasteful build-after-every-small-edit loops.
- After each substantial slice, run the smallest high-signal validation that can catch integration issues.
- If a larger milestone lands, follow with a stronger validation pass.
- Never claim parity with another project unless that project has been cloned or otherwise verified locally.
- Never describe UI as matching a reference app unless the actual screens, flows, and entrypoints were diffed against that reference.
- Do not hardcode user-facing strings. Add new strings to resources.
- Preserve existing user changes unless explicitly asked to replace them.
- Keep comments minimal and only where they explain non-obvious behavior.

## Skills Available In This Environment

These are the skills currently available to the agent and may be used when the task matches them.

- `404-page-generator`: create, optimize, or audit 404 pages
- `android-expert`: comprehensive Android development guidance covering Compose, coroutines, architecture, Hilt, Navigation, testing, performance, and Material 3
- `android-kotlin-compose`: production Android app and feature implementation with Kotlin, Compose, MVVM, Hilt, Room, and modular architecture
- `compose-navigation`: Navigation Compose setup, deep links, argument passing, and screen structure
- `find-skills`: discover relevant installable or available skills
- `kotlin-android`: modern Android and Jetpack guidance
- `kotlin-concurrency-expert`: coroutine, lifecycle, cancellation, and thread-safety review/remediation
- `kotlin-testing`: Kotlin test authoring and remediation with JUnit, MockK, Kotest, and coroutine testing
- `nextjs-react-typescript`: TypeScript, Node.js, Next.js, React, Radix, Shadcn, Tailwind
- `premium-frontend-design`: high-end frontend design and motion work
- `ui-ux-design-patterns`: interface design and UX improvement patterns
- `skill-creator`: creating or updating skills
- `skill-installer`: installing Codex skills
- `slides`: create and edit presentation decks
- `spreadsheets`: create and edit spreadsheet workbooks

## Migration Rules

These rules apply to large migrations in this repo, including the current Hayai to Yokai UI parity migration.

### Source Of Truth

- When migrating toward another app or fork, clone the reference project locally first.
- Record the exact reference path and commit hash in project documentation.
- Treat the local reference checkout as the only valid parity source, not memory or rough resemblance.
- If the migration target changes, update the documented source of truth before continuing implementation.

### Diff First, Then Implement

- Build a surface-by-surface parity matrix before calling work complete.
- For each subsystem, identify:
  - reference entrypoints
  - Hayai entrypoints
  - behavior differences
  - missing imported features
  - Hayai-only features that must survive
- Do not invent “inspired by” behavior when exact reference behavior can be inspected directly.

### Preserve Hayai While Importing

- Hayai-only features must remain reachable after each migration slice.
- Imported UI must absorb Hayai features cleanly instead of forcing legacy fallback screens to remain forever.
- If a Hayai-specific feature requires a divergence from the reference app, document that divergence explicitly.

### Replace, Do Not Accumulate

- Avoid permanent duplicate screens or parallel UI stacks.
- Once a screen reaches parity and preserves required Hayai features, remove or retire the replaced legacy path.
- Do not leave dead or stale navigation routes around after a migrated destination is live.

### Validation Standard

- Validate each major migration slice with at least a compile pass.
- Validate each completed subsystem with stronger checks when possible:
  - app build
  - targeted tests
  - emulator/device smoke pass
  - screenshot comparison against the local reference app
- For UI parity work, capture real screenshots and compare them to the reference app instead of relying on code inspection alone.
- If a device shows behavior that contradicts the source edits, treat that as an active blocker and resolve it before claiming completion.

### Documentation

- Keep `docs/hayai-yokai-ui-migration.md` updated as the canonical parity ledger for the current UI migration.
- Record:
  - reference commit
  - completed surfaces
  - imported features
  - preserved Hayai-only features
  - intentional divergences
  - removed provisional work
  - validation status
  - remaining blockers
- Do not mark a feature as complete in documentation until it has been implemented and validated.

## Current Migration Context

The active migration in this repo is:

- Hayai UI and UX migration toward local Yokai parity

Working rules for this migration:

- Yokai must be cloned locally before parity claims are made.
- Yokai UX and features are the baseline target.
- Hayai-only features must be preserved and integrated into the Yokai-shaped UI.
- Previous provisional migration work may be reworked or deleted if it does not match the verified Yokai diff.
- The migration is not complete until shell, library, browse, manga details, reader, recents, stats, settings, dialogs, sheets, shortcuts, snackbars, and share flows are all reviewed against the local Yokai reference.

## String And Resource Rules

- All new user-visible text must go through the existing string resource system.
- Do not add hardcoded strings in Compose, Views, dialogs, snackbars, menus, or tests unless the file already uses fixed debug-only text.

## Build And Validation Guidance

- Prefer Java 17 / Android Studio JBR for local Gradle validation in this repo.
- Use smaller compile/test checks during active iteration, then run larger validation after major slices land.
- If emulator validation is part of the task, use `adb logcat -b crash` as part of the final crash check.

## Completion Rule

A task is complete only when:

- code changes are implemented
- high-signal validation has been run
- follow-up fixes from that validation are applied
- required documentation is updated

Do not stop at planning if these steps are still pending and can be performed locally.
