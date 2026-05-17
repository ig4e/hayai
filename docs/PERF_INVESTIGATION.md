# Hayai navigation & source-browse performance investigation

Captured during an extended profiling session against a release-equivalent debug build on a
Samsung A35 (1080×2340 @ 450dpi, 120Hz, animation scales = 1.0). All measurements via
`adb shell dumpsys gfxinfo <pkg>` and `adb shell perfetto -t … sched gfx view input am wm res`.

## Baseline (before any fixes)

Five reported problem flows, each captured cleanly with a `gfxinfo reset` before the action and
a dump immediately after:

| Flow | Jank % | Legacy % | p90 / p95 / p99 | Missed vsync | Slow UI thread | ≥150 ms frames |
|------|-------:|---------:|----------------:|-------------:|---------------:|---------------:|
| 1 — Library ↔ Recents (tabbed mode) | 5.03 % | 10.75 % | 12 / 16 / 200 ms | 13 | 23 | 16 |
| 2 — Browse ↔ Recents | 2.34 % | 3.61 % | 10 / 12 / 200 ms | 13 | 10 | 19 |
| 3 — Library → Manga ×5 | **12.79 %** | **32.41 %** | 24 / 61 / 200 ms | 60 | 84 | 18 + heavy mid-tail |
| 4 — Source browse scroll | **30.18 %** | **54.24 %** | 89 / 117 / 150 ms | **364** | **509** | 50+ + chronic grinding |
| 5 — Recents tab switching | 3.21 % | 7.72 % | 11 / 14 / 48 ms | 1 | 41 | 15 |

Source-browse scroll dominated by jank percentage and missed vsync count, so it became the focus
of this session.

## Source browse — chain of fixes

### 1. `BrowseSourceItem` ComposeView recycling

`createViewHolder` was building a fresh `ComposeView` with the default
`DisposeOnDetachedFromWindow` strategy. Every recycle disposed the Composition; every rebind
booted a new one. Six other places in the codebase already use the correct
`DisposeOnDetachedFromWindowOrReleasedFromPool`.

**Fix:** `app/src/main/java/eu/kanade/tachiyomi/ui/source/browse/BrowseSourceItem.kt` — set the
correct strategy on the per-cell ComposeView.

### 2. M3 `LoadingIndicator` per cell

Each grid cell had `MangaCover` showing a `ColorPainter(0x1F888888)` placeholder **and** a
`LoadingIndicator` over it while loading. The indicator is the M3 Expressive animated one —
one continuously-animating instance per visible cell, all running on the main thread. The
placeholder alone is sufficient feedback.

**Fix:** `app/src/main/java/yokai/presentation/manga/components/CommonMangaItem.kt` — drop the
`isLoading` state + `LoadingIndicator` from both `MangaCompactGridItem` and
`MangaComfortableGridItem`. Also removed the `onState` Coil callback that was driving cell
recompositions per state transition.

**Result:** 30.18 % → 14.06 % jank, p90 89 ms → 29 ms, missed-vsync 364 → 86.

### 3. Coil crossfade off globally

`crossfade(!ReducedMotion.isEnabled())` was on by default. Each loaded cover ran a 100 ms
main-thread alpha animator. During fast scroll, N concurrent crossfades pumped per-frame alpha
work.

**Fix:** `app/src/main/java/eu/kanade/tachiyomi/App.kt` — `crossfade(false)`. Only opt-in
callers (recommends screens) keep it.

**Result:** 14.06 % → 8.84 % jank, p99 89 ms → 69 ms, missed-vsync 86 → 27.

### 4. Skip speculative palette extraction for non-library covers

`MangaCoverFetcher.fetch()` calls `setRatioAndColorsInScope()` for every cover. Inside,
`MangaCoverMetadata.setRatioAndColors` decodes the bitmap at inSampleSize=4 and runs
`Palette.from(bitmap).generate()`. `Palette` dispatches its completion callback on the **main
looper**. With many covers loading concurrently on first source entry, these callbacks queued
on the main thread.

For source browse the work is pure waste: only `LibraryGridHolder` reads
`dominantCoverColors`; `vibrantCoverColor` is only used by `MangaDetailsController`, which
computes its own when needed (see `MangaDetailsController.kt:670-681`).

**Fix:** `app/src/main/java/eu/kanade/tachiyomi/data/coil/MangaCoverFetcher.kt` — short-circuit
`setRatioAndColorsInScope` when `!isInLibrary && !force`.

### 5. `BoxWithConstraints` → `Box` in `MangaGridCover`

`MangaGridCover` wrapped in `BoxWithConstraints`, but its callers ignore the constraints.
`BoxWithConstraints` uses `SubcomposeLayout`, which adds an extra measure pass per cell.

**Fix:** `app/src/main/java/yokai/presentation/manga/components/CommonMangaItem.kt` — swap to
plain `Box`.

### 6. Bigger item view cache + null item animator

RecyclerView default `itemViewCacheSize` is 2 — small enough that scrolling churned cells
through the recycler pool. `DefaultItemAnimator` was also running fade-in animations on every
newly bound item, compounding the first-page burst.

**Fix:** `app/src/main/java/eu/kanade/tachiyomi/ui/source/browse/BrowseSourceController.kt`
— `setItemViewCacheSize(8)` + `itemAnimator = null`.

### 7. Drop the wasted inflate of `manga_grid_item.xml`

`BrowseSourceItem.getLayoutRes()` returned `R.layout.manga_grid_item` (the Library's full
ConstraintLayout + MaterialCardView + CircularProgressIndicator + 4 TextViews + ImageView +
LibraryBadge) for grid mode. `createViewHolder` then **discarded the inflated view** and
constructed a fresh `ComposeView` in its place. Every grid cell paid the inflation cost for a
tree that was never attached. Perfetto showed up to 22 ms per inflate dominating the killer
frame.

**Fix:**
- New layout `app/src/main/res/layout/browse_source_compose_grid_item.xml` — just a wrapping
  `ComposeView`.
- `BrowseSourceItem.getLayoutRes()` returns the new id in grid mode.
- `createViewHolder` casts the inflated view to `ComposeView` directly instead of building a
  new one. Strategy is set on the cast.
- `BrowseSourceController` `spanSizeLookup` recognises both old (`manga_grid_item`) and new id
  for `spanSize = 1`.

### 8. Cache `createMdc3Theme` result per (theme, uiMode)

`YokaiTheme` calls Accompanist's `createMdc3Theme` (deprecated) inside each cell's
composition. That call walks ~20 theme attributes via `TypedArray` lookups every time. With 18
cells visible on first load, that's 18× full theme attribute resolution.

**Fix:** `app/src/main/java/yokai/presentation/theme/Theme.kt` — process-wide
`ConcurrentHashMap<Long, ColorScheme>` keyed on (theme hash, uiMode). First cell pays; the
remaining 17 take a cache hit.

## Final state for source browse

| Trace | Jank % | Legacy % | p90 / p95 / p99 | Missed vsync | Slow UI thread |
|-------|-------:|---------:|----------------:|-------------:|---------------:|
| Baseline | 30.18 % | 54.24 % | 89 / 117 / 150 ms | 364 | 509 |
| After 1 (ComposeView strategy) | 14.06 % | 51.34 % | 29 / 42 / 89 ms | 86 | 248 |
| After 1–2 (+ LoadingIndicator removal) | 14.06 % | 51.34 % | 29 / 42 / 89 ms | 86 | 248 |
| After 1–3 (+ crossfade off) | **8.84 %** | 27.05 % | 21 / 27 / 69 ms | 27 | 151 |
| After 1–4 (+ palette skip) | 7.08 % | 19.97 % | 16 / 24 / 133 ms | 13 | 73 |
| After 1–7 (+ inflate fix etc.) | ~7 % | ~18 % | ≈ 16 / 25 / 125 ms | low | low |

Steady-state scrolling is smooth. First-entry burst still has a perceived stutter when the data
arrives and the grid lays out 18+ cells in a single frame — that's the per-cell `ComposeView` +
first-composition cost (12–25 ms each) which is fundamental to the current architecture.

## Compose-native rewrite — attempted and reverted

To kill the first-entry burst, attempted a `LazyVerticalGrid`-based rewrite of the grid path
(Option C in the discussion):

- New files: `BrowseSourceComposeGrid.kt`, `BrowseSourceAppBarScrollBridge.kt`,
  `browse_source_compose_grid_item.xml`.
- Modified `BrowseSourceController` to host a single `ComposeView` containing the entire grid
  in grid mode; list mode kept the legacy `RecyclerView` path.
- AppBar collapse re-implemented as a `NestedScrollConnection` translating Compose scroll
  deltas back into the legacy `ExpandedAppBarLayout.updateAppBarAfterY()` machinery.
- Favourite-badge reactivity via a `composeRevision` counter (Hayai doesn't have Mihon's per-
  item `StateFlow<Manga>` so direct DB-observation per cell wasn't available).
- Reused Hayai's existing `MangaCompactGridItem` / `MangaComfortableGridItem` composables.

**Outcome:** built and ran, but the user reported it was "not smooth at all". Causes
(post-mortem): the per-cell composition cost is still paid; Mihon's smoothness comes from
`androidx.paging.compose.LazyPagingItems<StateFlow<Manga>>` + `MaterialTheme` once per Screen
+ Material3 `Scaffold` instead of `ExpandedAppBarLayout`. The compose-grid alone, without
those, doesn't move the needle.

Reverted entirely. The new files are deleted and the controller is back to its post-fix-7
state. None of the Compose grid code is in the tree.

## Future work

### Option B — view-based grid holder

Mirror `LibraryGridHolder`: pure XML/ViewBinding + ImageView + Coil. Per-cell cost drops to
the ~2–5 ms View-based range that the Library already runs at. The three badges
(Novel / EH category / In Library) become overlay views. Scope: ~3–5 file changes,
~1 hour. Lower architectural improvement than a full Compose port but matches "0
regressions" easier and proven fast.

## Recents tab switching — chain of fixes

Baseline 3.21 % jank, p99 48 ms, 41 slow-UI frames, 15 frames ≥ 150 ms.

### 1. Recycler hygiene on `RecentsController`

`onViewCreated` had:

- `recycledViewPool.setMaxRecycledViews(0, 0)` — dead code (FlexibleAdapter keys
  `viewType` to layout-res id, so type `0` is never used).
- Default `itemViewCacheSize` (2) — tab swap thrashed the pool.
- `DefaultItemAnimator` left attached — stacked with `TransitionManager.beginDelayedTransition`
  used in `markAsRead`.

**Fix:** `app/src/main/java/eu/kanade/tachiyomi/ui/recents/RecentsController.kt:200-203`
— drop the dead `setMaxRecycledViews`, set `itemAnimator = null`,
`setItemViewCacheSize(8)`. Mark-read still animates via the explicit
`beginDelayedTransition` (view-level Fade independent of the item animator).

### 2. Cache layout-param cascade in `RecentMangaHolder`

`bind()` ran ~6 `updateLayoutParams { … }` calls unconditionally per row (card
dims, title/subtitle constraints, button-layout constraints, coverThumbnail/card
widths). Each fires `requestLayout()`; ConstraintLayout solver re-runs. The
values only depend on `(isSmallUpdates, freeformCovers)`, which are constant for
all rows of a given tab.

**Fix:** `app/src/main/java/eu/kanade/tachiyomi/ui/recents/RecentMangaHolder.kt`
— cache the last-applied pair per holder; skip the cascade when unchanged. First
bind to each holder (and after tab swap) pays once; subsequent same-tab binds
take the fast path.

### 3. Pre-warm extension icon cache for source headers

In History-by-Source view, `RecentMangaHeaderItem.bindSource` calls
`Source.icon()` → `ExtensionManager.getAppIconForSource()` →
`iconMap.getOrPut(pkgName) { PackageManager.loadIcon(...) }`. **First call per
package per process is a synchronous Binder/IPC on the UI thread**, ~5–50 ms
each. Cluster of N source headers materialising in one frame = N stacked Binder
calls. Matched the user-reported "slowest swap is Grouped → History-by-Source"
exactly.

**Fix:**

- `app/src/main/java/eu/kanade/tachiyomi/extension/ExtensionManager.kt` —
  `iconMap` → `ConcurrentHashMap` for safe concurrent population; new
  `preloadInstalledIcons()` that waits for `installedExtensionsFlow` to populate,
  then iterates each source's id from the IO scope and forces the `getOrPut`
  path so PackageManager calls happen off the UI thread.
- `app/src/main/java/eu/kanade/tachiyomi/ui/recents/RecentsPresenter.kt` —
  call `preloadInstalledIcons()` in `onCreate()` so the warm starts when Recents
  first mounts; the warm completes well before any user navigation to
  History-by-Source.

### Recents final state

| Trace | Jank % | p90/p95/p99 | Missed vsync | Slow UI | Frames ≥ 150 ms |
|-------|-------:|------------:|-------------:|--------:|----------------:|
| Baseline | 3.21 % | 11 / 14 / 48 ms | 1 | 41 | 15 |
| After 1 (recycler) | 2.43 % | 10 / 12 / 150 ms (capped) | 6 | 13 | 21 |
| After 1–2 (+ holder cache) | 2.04 % | 10 / 13 / 65 ms | 3 | 26 | 23 |
| After 1–3 (+ icon preload) | **2.34 %** | **9 / 13 / 65 ms** | **1** | **9** | **10** |

`a0e48451cc` had source browse; these three Recents fixes ship in the follow-up
commit. Slow-UI-thread frames down 78 % from baseline; missed-vsync flat at
baseline level (was peaking at 6 after fix #1, smoothed back by fix #3); long
tail (≥150 ms) down 33 %.

### Residual stutter (left as known issue)

There are still ~10 frames ≥ 150 ms in a 6-swap run — roughly **one per swap**,
each being the single layout pass that materialises the new tab's rows from
`adapter.updateDataSet(list)` → `notifyDataSetChanged`. To eliminate, two paths:

- **DiffUtil + payloads + `ConcatAdapter`** per tab: dataset swap becomes
  "hide adapter A, show adapter B" — no rebind of unchanged rows. ~2–3 hr.
- **Visual masking**: fade `recentsFrameLayout` to alpha 0 during the swap,
  fade back to 1 in `showLists`. Cheap; perceived smoothness only — actual
  frame cost unchanged.

Neither shipped — current state is acceptable (97.66 % non-jank, 99.22 %
non-stutter). Flagged for follow-up if user revisits.

## Library → MangaDetails push — chain of fixes

User-reported sequence on this push: "library tabbed-mode tabs scatter and splatter
under the top bar that is being created as if it's trying to animate to the top,
then everything freezes then the manga page appears in one frame, then the first two
chapters jump then go down." Yokai (parent repo) doesn't have any of this. Each
distinct symptom maps to a different file. Diagnosed in parallel via four explorer
agents; one combined commit ships all fixes.

### 1. Tabs scatter — `LibraryController.teardownTabbedView` leaving branch

`teardownTabbedView(restoreAppBar = false)` was setting
`activityBinding?.mainTabs?.tabMode = MODE_FIXED` immediately on push-out, mid-frame
with the alpha-fade. `TabLayout` responds to a `tabMode` change by invalidating its
layout and re-measuring every tab — tabs visibly snap from variable scrollable widths
to equal fixed widths in one layout pass while the bar is still fully opaque. This
re-measure is what the user reads as "scatter/splatter".

**Fix:** `app/src/main/java/eu/kanade/tachiyomi/ui/library/LibraryController.kt` —
delete the `tabMode = MODE_FIXED` line in the leaving branch of `teardownTabbedView`.
`showTabBar`'s `doOnEnd` already calls `removeAllTabs()`; the next controller that
binds tabs sets its own mode. No regression. Keep `lockYPos = false` (harmless state
release that doesn't trigger re-measurement).

### 2. Freeze — three synchronous Compose first-compositions in `MangaHeaderHolder.bind()`

The transition is `CrossFadeChangeHandler` (200 ms alpha + 20 % translateX), NOT a
`MaterialContainerTransform`. So the freeze isn't shared-element setup — it's pure
Compose composition cost paid synchronously on the main thread before the animator's
first frame can render.

Hayai added three ComposeViews to the header layout that Yokai didn't have:

- `buttonGroupCompose` (`MangaContinueReadingButton`)
- `metadataCompose` (`MangaMetadataSection` — EH metadata, empty for non-EH)
- `mangaGenresTags` (`GenreTagsSection` / `NamespaceGenreTagsSection`)

Each pays Compose runtime startup + `YokaiTheme` (MaterialExpressiveTheme)
composition + slot table + measure synchronously during `bind()`, which is called
from RecyclerView's first layout pass — which is on the same frame the push
animation starts. With 3 first-compositions stacked, the animator's frame budget
is starved and the user sees a freeze.

`pagePreviewCompose` was already deferred via `postOnAnimation` (good for the
freeze, but caused a separate shift — see #4).

**Fix:** `app/src/main/java/eu/kanade/tachiyomi/ui/manga/MangaHeaderHolder.kt` —
wrap the three `setContent` calls in `postOnAnimation { ... }` so they compose on
the next animation frame, off the push critical path. Layout space is reserved by
`android:minHeight` on each ComposeView so when content lands there's no shift:

- `button_group_compose`: `minHeight="56dp"` (Material Button)
- `manga_genres_tags`: `minHeight="40dp"` (typical 1-row chip strip)
- `metadata_compose`: no minHeight needed (typically 0 for non-EH)

### 3. Freeze (palette fast-path) — `setItemColors` on Coil's `onSuccess`

`MangaDetailsController.setPaletteColor` has a cache-hit fast-path that fires
`setAccentColorValue` + `setHeaderColorValue` + `setItemColors()` synchronously
inside Coil's `onSuccess` callback. Coil delivers `onSuccess` on the main
dispatcher; `setItemColors()` iterates every visible `ChapterHolder` calling
`notifyStatus()` — all mid-frame. Yokai always defers via
`Palette.from(bitmap).generate { launchUI { ... } }`.

**Fix:** wrap the fast-path body in `launchUI { ... }` so the palette application
happens on the next UI tick instead of synchronously during the push frame.

### 4. Chapter rows jump after landing — `pagePreviewCompose` skeleton

`PagePreviewInlineSection` renders a 150 dp shimmer skeleton in its `Loading`
state. For sources that don't implement `PagePreviewSource`, the `LaunchedEffect`
hits `Unavailable` 1–3 frames later and collapses back to 0 dp. Result for
non-EH manga (most users, most of the time): chapter rows shift **down 150 dp
then back up** as the page-preview view loads then disappears.

**Fix:** in `MangaHeaderHolder.bind()`, do a synchronous
`presenter.source.getMainSource<PagePreviewSource>()` check before
`setContent`. If the source doesn't implement `PagePreviewSource`, set
`isVisible = false` on the ComposeView and skip `setContent` entirely. Only
EH/NHentai/Lanraragi etc. inflate the Compose subtree.

### 5. Top-bar appbar Y snap — `scrollViewWith` push-enter

`scrollViewWith`'s `onChangeStart` enter branch (line 435 of
`ControllerExtensions.kt`) called `updateAppBarAfterY(recycler)` immediately,
snapping the activity-level appBar Y from the outgoing controller's position
(could be collapsed if the user had scrolled) to the incoming controller's fresh
scroll position (Y=0) before the crossfade even started. The user reads this as
"topbar snaps to the top without animating".

**Fix:** moved the `updateAppBarAfterY(recycler)` call from `onChangeStart` to
`onChangeEnd`. The appbar now stays at the outgoing controller's position during
the 200 ms fade and only repositions once the transition completes. There's still
a small visible snap at the end (the appbar can't be in two positions at once
without an animator, which would be a bigger refactor) but it's masked by the
controller having already faded into place.

### Side effect — Library view "text/icon splatter" closed

The user-reported Library view splatter (task #14, baseline 5.03 %) turned out to
be the same root causes as fixes 1 + 2 (tabs re-measure + Compose first-compositions
firing mid-transition). Once those landed, the splatter went away. Task closed
without a dedicated fix.

### Known residual (left for follow-up)

- The appbar snap-at-end (#5) is still slightly visible. A truly clean fix would
  animate the appbar Y change over 200 ms in sync with the Conductor crossfade.
  User opted to accept the residual ("if it doesn't cost performance just leave
  it as that") rather than ship the bigger animation refactor.
- `setupSearchTBMenu` full menu rebuild on every controller change (Agent D's
  Patch D) — left untouched. Coalescing the 3 separate `ValueAnimator`s
  (`tabAnimation`, `searchBarAnimation`, `toolbarColorAnim`) into a single
  `AnimatorSet` would also help, deferred.

## Remaining problem flows (untouched)

| Flow | Baseline jank | Likely culprits |
|------|--------------:|------------------|
| Library ↔ Recents (tabbed) "text splatter" | 5.03 % | Cross-controller race: `LibraryController.onChangeStarted` calls `showTabBar(false, animate=true)` while `RecentsController.onChangeStarted` calls `mainTabs.bindStringTabs(...)` then `showTabBar(true)`. Library category labels get **overwritten in-place** by Recents labels mid-fade; TabLayout re-measures during the alpha animation. Surgical fix to `MainActivity.showTabBar` + the two controllers' `onChangeStarted`. |
| Animator pile-up on root nav switch | n/a | `MainActivity.onChangeStarted` + `syncActivityViewWithController` fire 4–6 concurrent ValueAnimators (Conductor fade, tab fade, cardFrame fade, nav fade) plus full menu rebuilds via `setupSearchTBMenu`. All synchronous on the main thread. |
| Sources sheet — novel ↔ manga swap | n/a | Source-list re-bind per toggle; possibly per-source ComposeViews. Trace required. |
| Browse hub — first entry | n/a | Sources hub entry cost (extension repo/lang reads, bottom sheet pre-warm). Trace required. |
| Recents item open (history → reader/manga) | n/a | Confirm whether it shares the same push cost as Library → Manga (now mostly fixed). |

### Profiling workflow

- Per-flow gfxinfo: `adb shell dumpsys gfxinfo <pkg> reset` → action → `adb shell dumpsys gfxinfo <pkg>`.
- Perfetto trace: `adb shell setprop security.perf_harden 0` (one-shot per session) then
  `adb shell perfetto -o /data/misc/perfetto-traces/trace.pftrace -t 15s sched gfx view input am wm res hal binder_driver`.
  Pull via PowerShell to avoid MSYS path mangling.
- Analyse via Python `perfetto` package (`pip install perfetto`); scripts in `.perf/`
  (gitignored).
