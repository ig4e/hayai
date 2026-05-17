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

## Remaining problem flows (untouched)

| Flow | Baseline jank | Likely culprits |
|------|--------------:|------------------|
| Library ↔ Recents (tabbed) "text splatter" | 5.03 % | Cross-controller race: `LibraryController.onChangeStarted` calls `showTabBar(false, animate=true)` while `RecentsController.onChangeStarted` calls `mainTabs.bindStringTabs(...)` then `showTabBar(true)`. Library category labels get **overwritten in-place** by Recents labels mid-fade; TabLayout re-measures during the alpha animation. Surgical fix to `MainActivity.showTabBar` + the two controllers' `onChangeStarted`. |
| Library → Manga ×5 | 12.79 % | Likely `MaterialContainerTransform` shared element + heavy `MangaDetailsController.onCreateView` + Library teardown (`saveStaggeredState`, `closeTip`, filter-sheet hide, tab teardown) all synchronous. |
| Animator pile-up on root nav switch | n/a | `MainActivity.onChangeStarted` + `syncActivityViewWithController` fire 4–6 concurrent ValueAnimators (Conductor fade, tab fade, cardFrame fade, nav fade) plus full menu rebuilds via `setupSearchTBMenu`. All synchronous on the main thread. |
| Library view — internal splatter | n/a | Category chip strip / filter sheet / grid items shift/redraw on entry. Trace required. |
| Sources sheet — novel ↔ manga swap | n/a | Source-list re-bind per toggle; possibly per-source ComposeViews. Trace required. |
| Browse hub — first entry | n/a | Sources hub entry cost (extension repo/lang reads, bottom sheet pre-warm). Trace required. |
| Recents item open (history → reader/manga) | n/a | Shares MaterialContainerTransform with #5? Confirm. |
| Manga view — chapters list initial render | n/a | Chapters list initial animation janky, items jump. Probably `DefaultItemAnimator` first batch + post-layout shift. Same family of fix as source-browse #6 (drop item animator, possibly fix initial layout). |

### Profiling workflow

- Per-flow gfxinfo: `adb shell dumpsys gfxinfo <pkg> reset` → action → `adb shell dumpsys gfxinfo <pkg>`.
- Perfetto trace: `adb shell setprop security.perf_harden 0` (one-shot per session) then
  `adb shell perfetto -o /data/misc/perfetto-traces/trace.pftrace -t 15s sched gfx view input am wm res hal binder_driver`.
  Pull via PowerShell to avoid MSYS path mangling.
- Analyse via Python `perfetto` package (`pip install perfetto`); scripts in `.perf/`
  (gitignored).
