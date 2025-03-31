# Bottom Sheet Migration

This document outlines the changes made to standardize the app's modal dialogs to use bottom sheets consistently.

## Overview

The app previously used two styles of modals:
1. Regular dialog modals that appear in the center of the screen
2. Bottom sheet modals that slide up from the bottom

To create a more consistent UX, all dialogs have been standardized to use bottom sheets.

## Implementation Changes

### New Components

1. `BottomSheetAlertDialog` - A replacement for `AlertDialog` that uses `AdaptiveSheet` with `isTabletUi = false` to force the bottom sheet style.

2. `BottomSheetDialog` - A replacement for `Dialog` that uses `AdaptiveSheet` with `isTabletUi = false` to force the bottom sheet style.

### Modified Components

1. `AdaptiveSheet` - Updated to accept an `isTabletUi` parameter that can be forced to `false` to always show as a bottom sheet.

2. `TabbedDialog` - Updated to accept an `isTabletUi` parameter that can be passed to `AdaptiveSheet`.

3. Various dialog components (`CategoryDialogs`, `MangaDialogs`, `ReaderPageActionsDialog`, etc.) - Updated to use the new bottom sheet dialog components.

## Files Modified

- `presentation-core/src/main/java/tachiyomi/presentation/core/components/material/BottomSheetAlertDialog.kt` (new)
- `presentation-core/src/main/java/tachiyomi/presentation/core/components/material/BottomSheetDialog.kt` (new)
- `app/src/main/java/eu/kanade/presentation/components/AdaptiveSheet.kt`
- `app/src/main/java/eu/kanade/presentation/components/TabbedDialog.kt`
- `app/src/main/java/eu/kanade/presentation/components/ActionsPill.kt` (new)
- `app/src/main/java/eu/kanade/presentation/category/components/CategoryDialogs.kt`
- `app/src/main/java/eu/kanade/presentation/manga/components/MangaDialogs.kt`
- `app/src/main/java/eu/kanade/presentation/manga/components/MangaCoverDialog.kt`
- `app/src/main/java/eu/kanade/presentation/reader/settings/ReaderSettingsDialog.kt`
- `app/src/main/java/eu/kanade/presentation/reader/ReaderPageActionsDialog.kt`

## Usage

To migrate an existing `AlertDialog` to use a bottom sheet:

```kotlin
// Old code
AlertDialog(
    onDismissRequest = { ... },
    confirmButton = { ... },
    dismissButton = { ... },
    title = { ... },
    text = { ... },
)

// New code
BottomSheetAlertDialog(
    onDismissRequest = { ... },
    confirmButton = { ... },
    dismissButton = { ... },
    title = { ... },
    text = { ... },
)
```

To migrate an existing `Dialog` to use a bottom sheet:

```kotlin
// Old code
Dialog(
    onDismissRequest = { ... },
    properties = DialogProperties(...),
) {
    // Content
}

// New code
BottomSheetDialog(
    onDismissRequest = { ... },
    properties = DialogProperties(...),
) {
    // Content
}
```
