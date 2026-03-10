# Mihon Main Forward-Port Audit

Last updated: 2026-03-11

Upstream reference:
- Mihon `main` head reviewed manually against GitHub ref `f6b2684323569ef0eb23e143cc5d65d7cc1aae3c`
- Release milestones reviewed: `v0.19.0` through `v0.19.4`
- Porting rule: move features by hand onto Hayai, never merge or treat Mihon as drop-in

## Outcome

The Mihon-main backlog for this revamp is exhausted.

There are no known missing Mihon-main feature ports left in the areas audited for this pass. Anything still carrying Mihon- or Tachiyomi-branded names is either:
- an intentional compatibility surface
- an existing Hayai/SY/EXH divergence
- or a deliberate non-goal for this revamp, such as a full namespace rewrite

## Hand-ported in Hayai

### Recovery baseline
- Hayai app id and branding normalization
- updater cleanup and startup behavior consolidation
- onboarding recovery
- metadata screen recovery
- Compose `EditMangaDialog` recovery
- Outfit typography restored and wired into the active theme
- launcher, splash, and icon branding cleanup
- migration, library, and category fuzzy-search recovery

### Platform and theme
- Compose/runtime dependency uplift:
  - Compose BOM `2026.02.00`
  - `activity-compose` `1.12.4`
  - Paging `3.4.1`
  - WorkManager `2.11.1`
  - `moko` `0.26.0`
  - markdown `0.39.2`
  - `jsoup` `1.22.1`
  - aligned test library bumps
- Material Expressive foundation:
  - `MaterialExpressiveTheme`
  - `materialKolor`
  - expressive Monet-compatible scheme generation
- theme normalization:
  - shared theme fallback path
  - selectable theme registry cleanup

### Shared UI and screen layer
- expressive wrapper upgrades landed in Hayai-owned components, not upstream stock replacements:
  - app bar and search field shell
  - tab container surface treatment
  - navigation bar and rail items with label support
  - settings text fields on `CustomTextField`
  - action buttons, section cards, category chips, slider wrapper
- high-traffic screen polish:
  - updates toolbar now reflects active filter state visually
  - toolbar and surface layers use expressive styling while preserving Hayai presentation

### Search, library, and migration
- `src:` and `src:local` library search support
- bookmarked chapter download option:
  - manga screen download menu
  - library bulk download path
- library tracker integration:
  - tracked-library filter avoids rebuilding tracker maps per item
  - duplicate detection also matches shared tracker bindings
- migration/search correctness and parity already present locally were audited and retained:
  - same-query smart search behavior
  - selected-source handling
  - category migration flag handling
  - migration init-order crash fix
  - manual-source-search progress update

### Updates, downloader, and reader
- updates filters:
  - unread
  - downloaded
  - started
  - bookmarked
  - hide excluded scanlators
- runtime/data fixes:
  - transaction entry serializes top-level SQLDelight transactions
  - download cache invalidation cancels and restarts cleanly at startup and storage/source changes
  - storage and cache size calculations in settings run off the main thread
  - performance indexes from Mihon main added with dedicated migrations
- reader/downloader fixes:
  - reader share flow no longer wraps chooser intents twice
  - pre-1970 chapter upload dates render correctly

### Trackers, extensions, and installer flow
- MAL search now uses field-expanded single-query responses instead of N+1 detail fetches
- MAL search results now include author and artist data
- Suwayomi progress sync uses epsilon-safe chapter comparison
- Shizuku installer no longer crashes when binder teardown races install/update flow
- extension search uses a shared query predicate instead of rebuilding nested lambdas

### Compose/runtime cleanup ports
- manga description markdown annotator is remembered across recomposition
- manga and updates bottom action menus use stateful reset-job tracking instead of stale remembered locals

## Audited and already present locally

These were reviewed against Mihon changes and intentionally not re-ported:
- duplicate-library warning flow on add-to-library
- restored metadata flow
- restored onboarding redesign
- restored Compose manga editor
- current fuzzy search pipeline and category dialog search
- migration dialog wrong-entry fix
- Bangumi novel filtering
- Kitsu `library_id` handling
- tracker icon update
- extension install or update pending-state API failure guard
- extension repo URL auto-formatting
- Suwayomi remove-downloads-after-reading support
- OAuth login activity cleanup
- read-duration persistence on chapter change
- download directory creation failure handling
- chapter URL based download folder naming and legacy lookup compatibility
- non-ASCII filename handling option
- webtoon manual-scroll tap suppression
- reader inset padding fix for fullscreen and cutout interactions
- WebView multi-window UX tab handling
- reader open-in-browser support
- tracker private-entry support and login UX improvements already present in tracker settings and dialogs

## Deliberately preserved as non-goals

- full source/package namespace rename away from `eu.kanade.tachiyomi`
- removal of legacy deeplink and auth compatibility aliases
- removal of ComicInfo compatibility fields such as `SourceMihon` and `PaddingTachiyomiSY`
- replacing Hayai’s heavily styled components with upstream default Material widgets
- converting the repo into Mihon’s exact module or build layout

## Verification

Run after each major slice:
- `./gradlew :app:compileFdroidDebugKotlin`

Targeted regression coverage added or retained for the forward-port:
- `./gradlew :app:testFdroidDebugUnitTest --tests eu.kanade.tachiyomi.ui.library.LibraryItemTest`

Manual QA checkpoints still recommended before release:
- theme consistency across Hayai themes and Monet
- updates filters and selection actions
- `src:` and `src:local` library search
- bookmarked chapter downloads from manga and library flows
- onboarding and settings navigation
- metadata screen access
- updater path

## Notes

- Keep this file as the audit ledger for the Mihon-main manual port, not as an open backlog.
- If a future Mihon-main review finds a real missing feature, add it as a new dated audit entry instead of reopening this whole revamp list.
