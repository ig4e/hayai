# Hayai Fork Recovery Matrix

This document tracks the pre-`e344cadfec` `ig4e` fork work and how it should be treated on top of current `master`.

## Status Legend

- `preserved as-is`
- `needs cleanup`
- `missing and must be reintroduced`
- `obsolete/transient`

## Commit Matrix

| Commit | Area | Intent | Status | Notes |
| --- | --- | --- | --- | --- |
| `f570b55792` | Theme system, download flow | Extra themes and download behavior tuning | `needs cleanup` | Theme expansion survived; download behavior should be audited against current updater/downloader flow. |
| `b6676ca63e` | Design system, manga/home/updates UX | Broader UI refresh and component polish | `needs cleanup` | Primitives mostly survived, including restored Outfit typography; large screen-level diffs should be selectively forward-ported. |
| `333cbdbd34` | Settings UX | Searchable settings and modern settings items | `preserved as-is` | Main building blocks are already present on `master`. |
| `b3f12918fd` | Design system | Custom dropdown/text field/material wrappers | `preserved as-is` | Shared UI primitives already exist in `presentation-core`. |
| `0c545c9d02` | Branding | Hayai rename and app identity | `needs cleanup` | Hayai branding is current target; most app/workflow/resource identity is normalized, with only package/code namespace decisions left. |
| `467f167415` | Branding churn | Temporary rename rollback | `obsolete/transient` | Historical churn, not target state. |
| `c28df09eee` | CI/release | Workflow and release references | `needs cleanup` | Workflow structure is cleaner now, but release copy/changelog policy still needs a final pass. |
| `63c682abd4` | Branding/changelog | Hayai logo text and release copy cleanup | `needs cleanup` | Branding kept; changelog and visible copy are cleaner, but some compatibility-facing naming remains. |
| `82946073ba` | App identity, settings/data UX, assets | Package/version adjustments and settings polish | `needs cleanup` | User-facing UX pieces are mostly restored; remaining work is deliberate compatibility trimming, not raw reapplication. |
| `5948e38ef9` | Design system | Shared custom text fields | `preserved as-is` | Current `CustomTextField` and `CustomLabelTextField` cover this. |
| `5756652903` | Manga detail flow | Compose `EditMangaDialog` and richer edit UX | `preserved as-is` | Reintroduced on top of the current `Manga` model and Compose dialog stack. |
| `e762cbf9ee` | Manga metadata flow | Rich metadata screen and manga UI affordance | `preserved as-is` | Reintroduced with the current metadata screen model and navigation flow. |
| `e058036a86` | Data saver/runtime | URL encoding fix | `preserved as-is` | No action unless regression is found. |
| `4e01762f2c` | Search behavior | Shared fuzzy-search utility adoption | `needs cleanup` | Keep centralized fuzzy-search behavior and verify current callers. |
| `c53b4c3b86` | Category/search UX | Category dialog search | `needs cleanup` | Behavior survived partially; keep aligned with current UI. |
| `33473361b9` | Onboarding UX | Multi-step onboarding redesign | `needs cleanup` | Current onboarding retains structure but lost some richer presentation. |
| `f39be79f1e` | Updater flow | Automatic install notification and update handling | `preserved as-is` | `AppUpdateDownloadJob` and notifier remain present. |
| `72f9689600` | Migration/search, branding assets | Migration/search improvements and launcher assets | `needs cleanup` | Restore useful migration/search behavior; keep only assets still referenced. |
| `a0ca9b16bb` | Assets/icons | Custom icons and drawables | `needs cleanup` | Dead Compose-era leftovers are being removed; launcher/splash compatibility assets still need a final pass. |
| `4e7a67b6a9` | Versioning/release copy | Release metadata and changelog updates | `obsolete/transient` | Historical release bump, not a direct implementation target. |
| `d3f5591da9` | CI | Build/upload workflow | `needs cleanup` | Rebuild on current branch/release strategy. |
| `8e4bc6e174` | Search utility | Shared fuzzy search helper move | `needs cleanup` | Keep one authoritative helper and eliminate drift. |
| `c2bb9e0f05` | Updater flow | Automatic update checks at startup | `preserved as-is` | Startup checks now live in `App` and the About screen remains the explicit/manual path. |
| `4b60c0b8d4` | Theme system | Expanded Hayai theme palette | `preserved as-is` | Present and now normalized through shared theme fallback/selection wiring. |

## Immediate Recovery Targets

1. Finish the remaining theme and screen-level cleanup pass after the restored manga/detail flows.
2. Audit Hayai assets and remove dead/debug-only resources left by earlier merges.
3. Normalize release copy/changelog/version metadata after the workflow cleanup.
4. Keep compatibility-only identities explicit and documented until they can be safely dropped, including ComicInfo and deep-link aliases.
