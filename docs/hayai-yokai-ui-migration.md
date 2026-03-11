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
| Shell/navigation | `app/src/main/res/menu/bottom_navigation.xml`, `ui/main/MainActivity.kt` | `ui/home/HomeScreen.kt` | Root tab contract remains `Library / Recents / Browse`. Legacy shell paths for `Updates`/`History` root tabs have now been removed in favor of explicit Recents sub-tab routing, and shortcut/deep-link shell entrypoints now resolve to Recents with target page selection (`All`/`History`/`Updates`) instead of parallel shell tabs. Secondary surface placement still needs a full Yokai diff and visual parity pass. | Validate side-by-side against Yokai and finish any remaining secondary-surface placement drift. |
| Library | `ui/library/LibraryController.kt`, `ui/library/compose/LibraryComposeController.kt` | `ui/library/LibraryTab.kt`, `presentation/library/*` | Hayai now has a real Yokai-style `use_staggered_grid` preference and staggered rendering in both paged and sectioned library modes, but broader category handling and visual parity still need deeper auditing against Yokai's legacy controller behavior. | Continue diffing category behavior, drag/drop behavior, and final spacing/interaction parity before treating the library migration as complete. |
| Browse | `ui/source/BrowseController.kt`, `ui/source/browse/BrowseSourceController.kt` | `ui/browse/BrowseTab.kt`, `presentation/browse/*` | Hayai browse now uses stable logical root destinations instead of hardcoded pager indices, so extension routing no longer depends on feed-tab ordering, and the root pager initializes from the saved logical destination instead of always booting on page 0 first. The Compose browse container is also flatter and closer to Yokai now: tabs live inside the main browse surface instead of above a second nested content card, source headers are reduced to lighter section labels instead of full header cards, and source rows use denser elevated list cards to read more like a single browse/feed system. Browse-root search is also now destination-owned instead of globally leaked: Extensions keeps its local search state, while the Sources destination now uses the root search affordance to submit into Hayai's existing global-search route rather than relying only on a separate toolbar action. Browse source chips/search now also preserve Popular/Latest ownership when filters resolve to defaults instead of drifting into a blank Search state, and feed saved-search/filter flows no longer dead-end when defaults are selected. Visual and interaction parity still need a deeper Yokai diff. | Re-diff and realign remaining browse interaction details and visual treatment against live Yokai screenshots. |
| Manga details | `ui/manga/MangaDetailsController.kt` | `presentation/manga/MangaScreen.kt`, `presentation/manga/components/*` | Hayai details now has a more Yokai-like unified hero header: cover/info/actions are grouped into a single top module on both phone and tablet layouts instead of separate stacked sections. The chapter section now also places start/continue reading inside the chapter header module instead of using a floating FAB, the chapter header shows current sort/filter/display state so the chapter controls are materially closer to Yokai's details flow, and that chapter block now exposes filter/sort as an explicit secondary action instead of a lone icon. The lower-half details area is now less fragmented too: the description and tags are merged into a single self-labeled details card, extras actions are self-labeled inside their own card, and page previews now render under their own internal section heading instead of relying on repeated screen-level headers. Manga toolbar parity also advanced in this pass: full-series `mark as read` / `mark as unread` actions are now available from the top overflow on both phone and tablet details flows. The overall layout is closer, but still not at full Yokai parity for chapter-area behavior and action hierarchy. | Continue rebuilding against Yokai details flow and then reinsert Hayai extras. |
| Reader | `ui/reader/ReaderActivity.kt`, reader layouts/options | `ui/reader/ReaderActivity.kt`, `presentation/reader/*` | Hayai reader settings now opens on a `General / Paged-or-Long-strip / Custom filter` tab structure closer to Yokai's settings sheet, with series-level Reading Mode and Rotation moved into the General tab. The expanded bottom bar derives button labels from Hayai's reader button model, and a new settings icon in the sheet header provides a direct route to the full reader settings screen. Reader gesture/chrome parity has also advanced: navigation overlay hints now follow LTR/RTL direction like Yokai, and page-scrub state no longer gets stuck after slider interactions. | Finish tablet-side visual diffing for reader chrome/chapter list and capture side-by-side screenshots against Yokai. |
| Recents | `ui/recents/RecentsController.kt`, `ui/recents/RecentsPresenter.kt` | `ui/more/RecentsTab.kt`, `presentation/more/RecentsScreen.kt` | Hayai Recents now includes controller-level interaction parity beyond the earlier tab/search merge: update rows in `All` and `Updates` support swipe actions (read toggle + download action), long-press selection, and grouped bulk actions (bookmark, mark read/unread, download, delete) via the shared bottom action menu. Recents command routing also now supports shell-triggered destination targeting (`History`/`Updates`) and shell-triggered download queue opening through the Recents-owned modal queue sheet. | Validate against live Yokai screenshots (phone/tablet, light/dark) and finish any remaining controller-only affordances not yet represented in Compose. |
| Settings/stats/more | `ui/setting/controllers/SettingsMainController.kt`, `ui/setting/controllers/SettingsGeneralController.kt`, `ui/more/stats/StatsController.kt` | `ui/more/MoreTab.kt`, `presentation/more/MoreScreen.kt`, `presentation/more/settings/*`, `ui/stats/StatsScreen.kt` | Yokai has no bottom-nav `More` destination. Hayai currently exposes settings/stats/downloads through a root tab, which is shell drift. Settings migration progressed: a dedicated Compose General settings surface now mirrors Yokai's `starting tab`, `back to start`, `manage notifications` (API-gated), shortcuts, updater-gated auto-update app section, locale/date behavior, and long-press navigation behavior. Remaining settings parity still needs deeper review for section ordering and long-tail option parity. | Continue re-homing settings/stats routes into Yokai's shell shape and finish settings parity diff/visual checks against the local Yokai source. |
| Shortcuts/share/snackbars | `ui/main/MainActivity.kt`, `util/manga/MangaShortcutManager.kt`, `util/system/IntentExtensions.kt` | `util/system/DynamicShortcutManager.kt`, `util/system/IntentExtensions.kt`, library removal flow | Hayai shortcut behavior is now closer to Yokai: dynamic shortcuts are rebuilt as a managed set (not only a single continue-reading entry), include recent-series shortcuts, and support source-targeted shortcut intents via explicit source action handling in `MainActivity`. This pass also added source-recency tracking (`last_used_sources`) and mixed recency ranking across series/source shortcuts, including source-only shortcut rebuild when history is empty. Shortcut refresh now responds to all shortcut-related preference changes (dynamic shortcuts, series shortcuts, source shortcuts, open chapter), and feed-source launches now respect incognito before recording source usage. Share/open behavior was tightened by removing redundant chooser wrapping from manga share and aligning reader/web share entrypoints on the shared `toShareIntent` path. Edge-case hardening now also validates shortcut extras before navigation, normalizes/guards search share queries, prevents splash hold on unknown intent actions, and makes share intents scheme-aware (`http/https` as text, URI streams for `content/file`). Snackbar parity edge cases still need deeper side-by-side verification against Yokai. | Continue auditing snackbar edge cases and finish side-by-side shortcut/share contract checks against Yokai. |

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
- Source-feed filter sheets now execute reset/save actions through `SourceFeedScreenModel` (including saved-search creation), fixing prior no-op handlers in feed context.
- Browse root extension switching now tracks the live Extensions tab index, so extension routing remains correct when feed-tab visibility/order changes at runtime.
- Browse-source filter/search resolution now falls back to Popular/Latest for default-filter/no-query cases instead of sticking to a blank Search listing, which keeps chip ownership aligned with Yokai behavior.
- Source-feed filter and saved-search entry now always route into source browse (including default-filter/default-query cases) by normalizing to Popular when needed, removing the remaining no-op path.
- Hayai Recents root and pushed Recents screen now share one tabbed Compose scaffold with `All / History / Updates`, closer to Yokai's first-class Recents destination.
- Hayai Recents now supports root search across the shared `All / History / Updates` destination instead of remaining a static summary screen.
- Hayai shared tab scaffolds now use the modern Material 3 secondary tab row APIs, and Recents preserves its last-selected sub-tab instead of always resetting to `All`.
- Hayai Recents `All` is now a real merged timeline grouped by date and interleaving history with updates, replacing the earlier stacked summary-card implementation.
- Hayai Recents now opens the download queue in a modal sheet from the Recents app bar, using a reusable `DownloadQueuePane` so the Recents destination owns queue access more like Yokai without replacing Hayai's existing queue screen/backend structure.
- Shell shortcut routes for `Updates` and `History` no longer rely on removed root tabs; they now open `Recents` with explicit sub-tab targeting.
- Shell-triggered download queue opens now drive the Recents-owned modal queue sheet directly rather than pushing a standalone queue route.
- Recents update rows now support long-press multi-selection and grouped bulk actions (bookmark/read/download/delete), and update rows in both `All` and `Updates` tabs now expose swipe actions for read/download parity behavior.
- Hayai manga details now uses a unified cover/info/actions hero section on both small and large layouts, closer to Yokai's cover-first details structure.
- Hayai manga details now places start/continue reading inside the chapter header module, replacing the old floating manga FAB and moving the chapter controls closer to Yokai's details flow.
- Hayai manga details chapter header now exposes the current chapter sort/filter/display state, closer to Yokai's chapter-state treatment instead of a generic filter-only header.
- Hayai manga details chapter header now exposes filter/sort as a real secondary action button instead of a standalone icon, making the chapter control block read more like a cohesive Yokai-style action surface on both phone and tablet layouts.
- Hayai manga details now folds description and tags into one self-labeled details card, moves extras labeling into the extras card itself, and gives page previews their own internal heading so the lower-half details flow is less fragmented and closer to Yokai's cohesive details surface.
- Manga details toolbar overflow now includes full-series `mark as read` / `mark as unread` actions on both phone and tablet layouts, closing part of the action hierarchy gap with Yokai's details controller menu.
- Download shortcuts and other shell-triggered queue opens now route through `RecentsTab`, reducing special-case navigation logic in `HomeScreen`.
- Hayai reader top bar now exposes bookmark, web view, browser, and share actions again from the visible reader chrome, and the chapter list sheet now highlights the active chapter and surfaces current-chapter context in its header.
- Hayai reader settings now opens on a `General / Paged-or-Long-strip / Custom filter` tab structure closer to Yokai's settings sheet, and the expanded bottom bar now derives button labels from Hayai's reader button model so the visible controls stay aligned with the configured reader action contract.
- Hayai reader settings General tab now includes series-level Reading Mode and Rotation controls, and the viewer-specific tabs (Paged/Long-strip) no longer have redundant internal headers, matching Yokai's settings layout.
- Hayai reader settings sheet now includes a settings icon in its header that provides a direct shortcut to the main app reader settings via a deep-link route.
- Hayai reader navigation overlay now mirrors Yokai's directional behavior: left/right tap-zone previews adapt to current reading direction (LTR/RTL) and overlay refresh behavior no longer reappears unexpectedly when navigation mode is unchanged.
- Hayai reader page-scrub interactions now clear temporary scrolling state after page selection, preventing the stuck "scrolling" state that suppressed normal menu-hide behavior after chapter/page slider gestures.
- Hayai now has a dedicated Compose `SettingsGeneralScreen` aligned to Yokai's General controller structure, including API/updater-gated availability for Manage notifications and Auto-update app options.
- General settings date-format options were narrowed to Yokai's baseline set (`default`, `MM/dd/yy`, `dd/MM/yy`, `yyyy-MM-dd`), and app-language restart helper text is now shown only where applicable on pre-Android 13 devices.
- Settings migration regressions discovered in this slice were corrected: the migration source tab now renders via `TabContent`'s function contract, advanced settings no longer reference missing string keys and now clears history through domain interactor flow, and tracking settings now use valid localized scoring-update strings.
- Recents history delete actions in both root Recents tab and pushed Recents screen are now wired to `HistoryScreenModel.removeFromHistory`, removing the prior no-op delete behavior in Compose Recents rows.
- Recents history delete in both root Recents tab and pushed Recents screen now uses the same dialog contract as History (`remove one` vs `remove all for this manga`), and routes those branches to `HistoryScreenModel.removeFromHistory` / `removeAllFromHistory` respectively.
- Dynamic shortcuts now rebuild from current preferences/history, include recent-series shortcuts in addition to continue-reading, and support source-targeted shortcut intent routing through `MainActivity`.
- Source usage tracking now stores recent source IDs with timestamps (`last_used_sources`) from browse/feed entrypoints, and dynamic shortcuts now rank mixed series/source entries by recency while still rebuilding correctly when only source shortcuts are enabled.
- Shortcut rebuild now reacts to all shortcut-related preference changes instead of only the master dynamic-shortcuts toggle.
- Feed source launches now skip source-recency tracking when incognito is active, matching the broader source-history privacy contract.
- Manga share now uses the shared intent helper directly without nested chooser wrapping, aligning the share-entry behavior with reader/webview share flow.
- Shortcut/share hardening pass: malformed shortcut extras are now ignored safely (`mangaId/sourceId > 0` required), deep-link/search queries are trimmed before routing, unknown `MainActivity` intent actions now release splash hold, and `toShareIntent` now uses scheme-aware payload construction (`text/plain` for web URLs; stream share for `content/file` URIs).

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
- settings screen side-by-side screenshot comparison against Yokai (main + general)
- final integrated-`master` screenshot sweep after A-E merge (not yet captured in this pass)

Latest compile validation:
- 2026-03-12: `.\gradlew.bat :app:compileDevDebugKotlin '-Pkotlin.incremental=false' --console=plain` (PASS)
- 2026-03-12: `JAVA_HOME='C:\Program Files\Android\Android Studio\jbr' ANDROID_HOME='C:\Users\ahmed\AppData\Local\Android\Sdk' .\gradlew.bat :app:compileDevDebugKotlin '-Pkotlin.incremental=false' --console=plain` (PASS)

Final master integration (A-E):
- 2026-03-12: integrated worktree outputs are present on `master` (`browse`, `recents`, `reader`, `shortcuts/share`, `manga details` commits in current `master` history), with no remaining merge conflicts in working tree.

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
- Recents still needs screenshot-level parity verification against Yokai for controller affordances that were ported into Compose (swipe/action-mode interactions, queue-sheet behavior, and grouped feed treatment).
- The migration must stay UI-feature-only: keep Hayai/Mihon/Tachiyomi structure and state models, and do not adopt Yokai backend/file-structure patterns that would make upstream updates harder.
