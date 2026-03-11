# Hayai <-> Yokai Parity Ledger

This file is the source-of-truth migration ledger for rebuilding Hayai against a local Yokai checkout.

## Reference baseline

- Yokai local path: `C:\Users\ahmed\Documents\G2\yokai`
- Yokai commit: `2a600e3e1d9bbb296a3be2a5740713177de4d175`
- Hayai workspace: `C:\Users\ahmed\Documents\G2\hayai`

Rules for this migration:
- Yokai is the UX and feature reference, not a loose inspiration.
- Hayai-only features stay, but they must be embedded into Yokai's structure.
- Any prior migration work not backed by a Yokai diff is provisional.
- Deviations must be listed explicitly under `Intentional divergences`.

## Parity matrix

| Surface | Yokai source of truth | Hayai current implementation | Current delta | Action |
| --- | --- | --- | --- | --- |
| Shell/navigation | `app/src/main/res/menu/bottom_navigation.xml`, `ui/main/MainActivity.kt` | `ui/home/HomeScreen.kt` | Root tab contract is now reduced to `Library / Recents / Browse`, and legacy download shortcuts are rerouted through Recents. Shell-triggered queue opening is now owned by `RecentsTab` instead of being pushed directly from `HomeScreen`. Secondary surface placement still needs a full Yokai diff and visual parity pass. | Finish re-homing leftover `More` functionality, delete stale shell paths, and validate side-by-side against Yokai. |
| Library | `ui/library/LibraryController.kt`, `ui/library/compose/LibraryComposeController.kt` | `ui/library/LibraryTab.kt`, `presentation/library/*` | Hayai now has a real Yokai-style `use_staggered_grid` preference and staggered rendering in both paged and sectioned library modes, but broader category handling and visual parity still need deeper auditing against Yokai's legacy controller behavior. | Continue diffing category behavior, drag/drop behavior, and final spacing/interaction parity before treating the library migration as complete. |
| Browse | `ui/source/BrowseController.kt`, `ui/source/browse/BrowseSourceController.kt` | `ui/browse/BrowseTab.kt`, `presentation/browse/*` | Hayai browse now uses stable logical root destinations instead of hardcoded pager indices, so extension routing no longer depends on feed-tab ordering, and the root pager initializes from the saved logical destination instead of always booting on page 0 first. The Compose browse container is also flatter and closer to Yokai now: tabs live inside the main browse surface instead of above a second nested content card, source headers are reduced to lighter section labels instead of full header cards, and source rows use denser elevated list cards to read more like a single browse/feed system. Browse-root search is also now destination-owned instead of globally leaked: Extensions keeps its local search state, while the Sources destination now uses the root search affordance to submit into Hayai's existing global-search route rather than relying only on a separate toolbar action. Visual and interaction parity still need a deeper Yokai diff. | Re-diff and realign search, tabs, chips, and item treatment. |
| Manga details | `ui/manga/MangaDetailsController.kt` | `presentation/manga/MangaScreen.kt`, `presentation/manga/components/*` | Hayai details now has a more Yokai-like unified hero header: cover/info/actions are grouped into a single top module on both phone and tablet layouts instead of separate stacked sections. The chapter section now also places start/continue reading inside the chapter header module instead of using a floating FAB, the chapter header shows current sort/filter/display state so the chapter controls are materially closer to Yokai's details flow, and that chapter block now exposes filter/sort as an explicit secondary action instead of a lone icon. The lower-half details area is now less fragmented too: the description and tags are merged into a single self-labeled details card, extras actions are self-labeled inside their own card, and page previews now render under their own internal section heading instead of relying on repeated screen-level headers. The overall layout is closer, but still not at full Yokai parity for chapter-area behavior and action hierarchy. | Continue rebuilding against Yokai details flow and then reinsert Hayai extras. |
| Reader | `ui/reader/ReaderActivity.kt`, reader layouts/options | `ui/reader/ReaderActivity.kt`, `presentation/reader/*` | Hayai has provisional reader chrome/settings changes, but they were not diffed against Yokai's visible reader UX. The reader shell is closer now: top-bar chapter actions are back in the reader chrome instead of being silently dropped, current-chapter bookmarking is exposed again from the top bar, the in-reader chapter sheet now highlights the active chapter and surfaces chapter-count/current-chapter context in its header, and the reader settings sheet now follows a more Yokai-like `General / Paged-or-Long-strip / Custom filter` tab order instead of leading with reading mode. Expanded reader controls also now label buttons from the actual reader button contract instead of a mix of generic strings, which keeps crop/layout/reader-mode labels aligned with the configured action set. The broader reader/settings/tablet behavior still needs a deeper Yokai diff. | Re-diff bars, chapter list, settings, gestures, and tablet behavior. |
| Recents | `ui/recents/RecentsController.kt`, `ui/recents/RecentsPresenter.kt` | `ui/more/RecentsTab.kt`, `presentation/more/RecentsScreen.kt` | Hayai Recents is now a first-class 3-tab Compose destination (`All / History / Updates`) with shared routing for both the root tab and pushed Recents screen, root-level search across history and updates, saved sub-tab selection state, and a merged `All` timeline that interleaves history and updates under date headers instead of showing two summary cards. Recents now also owns download-queue access more like Yokai: the queue opens in a modal sheet from the Recents app bar, backed by a reusable `DownloadQueuePane` that still preserves Hayai's existing download queue screen and screen model. Deeper controller-level action parity still differs from Yokai. | Continue reworking Recents behavior toward Yokai's controller model and validate against live reference screenshots. |
| Settings/stats/more | `ui/setting/controllers/SettingsMainController.kt`, `ui/more/stats/StatsController.kt` | `ui/more/MoreTab.kt`, `presentation/more/MoreScreen.kt`, `presentation/more/settings/*`, `ui/stats/StatsScreen.kt` | Yokai has no bottom-nav `More` destination. Hayai currently exposes settings/stats/downloads through a root tab, which is shell drift. | Remove `More` from primary-nav parity target and re-home these routes. |
| Shortcuts/share/snackbars | `ui/main/MainActivity.kt`, `util/manga/MangaShortcutManager.kt` | `util/system/DynamicShortcutManager.kt`, `util/system/IntentExtensions.kt`, library removal flow | Hayai now has partial implementations, but they have not yet been checked against Yokai's contract. | Audit each behavior against Yokai before finalizing. |

## Yokai features to import

These are required targets unless explicitly moved to `Intentional divergences`:

- 3-destination primary nav: Library, Recents, Browse
- Yokai recents behavior and routing
- Yokai manga details structure and cover-driven theming
- Yokai reader chrome and chapter list behavior
- floating search behavior where Yokai uses it
- staggered and layout switching behavior that Yokai exposes
- dynamic shortcuts behavior
- snackbar undo flows
- modern share/open flows
- theme offerings and Material You additions that exist in Yokai and fit Hayai's stack

## Hayai-only features to preserve

- dynamic categories beyond Yokai's base grouping
- recommendations
- merged manga
- notes
- metadata viewer
- info edit
- page previews
- tracking filters
- lewd filter
- source migration
- EH/EXH-specific behavior
- delegated/custom source behavior

## Provisional migration work

These areas were changed before Yokai was cloned locally and must be treated as provisional:

- Compose shell/navigation redesign
- sectioned library work
- browse restyling
- manga details expressive regrouping
- reader chrome/settings restyling
- recents Compose screen
- settings visual regrouping
- shortcut/snackbar/share changes

Each item above must be reclassified as:
- matches Yokai
- Hayai-only addition to keep
- divergence to keep
- divergence to rework/remove

## First confirmed shell differences

Confirmed from local Yokai diff:

- Yokai primary nav menu is `Library / Recents / Browse`.
- Hayai still exposes SY-style nav customization preferences for hiding Updates/History buttons, which do not belong to Yokai's primary shell model.
- Yokai routes `SHOW_RECENTLY_UPDATED` and `SHOW_RECENTLY_READ` into `Recents`, not into separate root tabs.

Current shell corrections already landed:

- `HomeScreen` root tabs now follow the 3-destination contract instead of keeping `More` in primary navigation.
- legacy download shortcut routing now targets `Recents` and opens the download queue as a secondary surface.
- Recents root surfaces now expose settings, stats, and download queue actions directly so the `More` tab is no longer required for those entrypoints.
- The dead Compose `MoreTab` / `MoreScreen` root path has been removed, and its remaining secondary destinations are now reachable from `Recents` and `Settings`.
- Hayai library settings and renderers now support Yokai's staggered library grid via the same `use_staggered_grid` preference key, including the single-list sectioned mode already present in Hayai.
- Hayai browse root no longer hardcodes the Extensions tab pager index, which prevents routing drift when the feed tab is hidden or reordered.
- Hayai browse now restores directly into the saved logical root destination instead of entering on the first tab and then scrolling to the intended one.
- Hayai browse now uses a flatter tabbed container and lighter source section headers, reducing nested-card noise and making source lists feel closer to Yokai's content-first browse surface.
- Hayai browse tabs now own their own search state/submit behavior: Extensions keeps extension-local filtering, while Sources uses the root browse search affordance to launch global search directly, which is closer to Yokai's browse-search ownership than the old shared search plumbing.
- Hayai Recents root and pushed Recents screen now share one tabbed Compose scaffold with `All / History / Updates`, closer to Yokai's first-class Recents destination.
- Hayai Recents now supports root search across the shared `All / History / Updates` destination instead of remaining a static summary screen.
- Hayai shared tab scaffolds now use the modern Material 3 secondary tab row APIs, and Recents preserves its last-selected sub-tab instead of always resetting to `All`.
- Hayai Recents `All` is now a real merged timeline grouped by date and interleaving history with updates, replacing the earlier stacked summary-card implementation.
- Hayai Recents now opens the download queue in a modal sheet from the Recents app bar, using a reusable `DownloadQueuePane` so the Recents destination owns queue access more like Yokai without replacing Hayai's existing queue screen/backend structure.
- Hayai manga details now uses a unified cover/info/actions hero section on both small and large layouts, closer to Yokai's cover-first details structure.
- Hayai manga details now places start/continue reading inside the chapter header module, replacing the old floating manga FAB and moving the chapter controls closer to Yokai's details flow.
- Hayai manga details chapter header now exposes the current chapter sort/filter/display state, closer to Yokai's chapter-state treatment instead of a generic filter-only header.
- Hayai manga details chapter header now exposes filter/sort as a real secondary action button instead of a standalone icon, making the chapter control block read more like a cohesive Yokai-style action surface on both phone and tablet layouts.
- Hayai manga details now folds description and tags into one self-labeled details card, moves extras labeling into the extras card itself, and gives page previews their own internal heading so the lower-half details flow is less fragmented and closer to Yokai's cohesive details surface.
- Download shortcuts and other shell-triggered queue opens now route through `RecentsTab`, reducing special-case navigation logic in `HomeScreen`.
- Hayai reader top bar now exposes bookmark, web view, browser, and share actions again from the visible reader chrome, and the chapter list sheet now highlights the active chapter and surfaces current-chapter context in its header.
- Hayai reader settings now opens on a `General / Paged-or-Long-strip / Custom filter` tab structure closer to Yokai's settings sheet, and the expanded bottom bar now derives button labels from Hayai's reader button model so the visible controls stay aligned with the configured reader action contract.

## Validation artifacts captured so far

Hayai emulator artifacts:
- `build/hayai-shell-2.png`
- `build/browse-2.png`
- `build/recents.png`
- `build/more-2.png`
- `build/current.xml`
- `build/more-2.xml`

Open validation items:
- Yokai screenshots for the same surfaces
- tablet comparison
- dark and amoled comparison
- reader and manga-details side-by-side comparison

## Intentional divergences

None confirmed yet.

Anything not matching Yokai must remain out of this section until justified.

## Current blockers

- Prior migration work has to be audited before it can be trusted as parity work.
- Several imported/provisional preferences need a keep/remove decision after the Yokai diff:
  - expanded app bars
  - floating search bars
  - cover-themed manga details
  - dynamic shortcuts
  - combined two-page reader mode
- Recents still needs Yokai-parity behavior work for download sheet behavior and the remaining controller-level actions around the merged feed.
- The migration must stay UI-feature-only: keep Hayai/Mihon/Tachiyomi structure and state models, and do not adopt Yokai backend/file-structure patterns that would make upstream updates harder.
