# Specification: Hayai UI Migration (Yokai Parity)

## Overview
This track focuses on achieving full UI and UX parity between Hayai and the local Yokai reference checkout (`2a600e3e`). The goal is to ensure Hayai matches Yokai's "best-in-class" reader and library experience while preserving Hayai-only unique features.

## Scope
- **Shell & Navigation:** Finalize the 3-destination primary nav (Library, Recents, Browse).
- **Library Parity:** Audit and realign category behavior, staggered grids, and interaction spacing against Yokai.
- **Browse Parity:** Re-align source headers, search ownership, and tabbed navigation.
- **Manga Details:** Rebuild action hierarchy and chapter header module to match Yokai's flow.
- **Reader Parity:** Re-diff and realign the reader chrome, settings sheet, and chapter list.
- **Recents Parity:** Finalize the merged timeline and download queue sheet behavior.

## Success Criteria
- Side-by-side visual parity with Yokai for all core surfaces.
- Automated tests covering new navigation logic and state restoration.
- No regressions in Hayai-only features (Recommendations, Info Edit, etc.).
- Performance metrics (frame time and load speed) matching or exceeding Yokai.

## Technical Constraints
- Must remain UI-only; do not adopt Yokai backend/file-structure patterns.
- Maintain compatibility with Mihon upstream.
- Use Jetpack Compose for all new or modified UI components.
