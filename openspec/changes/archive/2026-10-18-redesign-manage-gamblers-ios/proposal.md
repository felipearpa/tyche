# Proposal

## Why

Android's Manage gamblers screen was redesigned in `redesign-manage-gamblers-android` (archived 2026-10-17, shipped in #38) and the `manage-gamblers` capability now describes that screen. iOS still has the earlier presentation: a blank top bar with an Edit/Done mode, a solid green owner row, a floating failure banner that shows only the first failure, and no gambler count. Bringing iOS to the same behavior closes the gap between the platforms and lets one spec govern both.

## What Changes

- Redesign the iOS Manage gamblers screen to the behavior the `manage-gamblers` spec already defines, using the same [design canvas](reference/README.md): scrolling "Gamblers" heading with the gambler count, owner row as the current-user row with "You" and "Owner", flat rows with separators, swipe-only removal with one confirmation, removing in place, failure shown in the row with Retry and ✕, "No other gamblers yet" empty state, loading placeholders.
- **BREAKING** (UI): remove the Edit/Done toolbar mode and its per-row minus control. Swipe and the VoiceOver "Remove" action are the removal routes.
- Replace the swipe background's trash glyph with the person-remove glyph and the "Remove" label, with a fixed 56 pt confirmation threshold.
- Drop the floating failure banner; a failed row shows "Couldn't remove", a Retry capsule and a ✕ dismiss button.
- Load the gambler count with the existing `GetPoolUseCase` on open and on pull-to-refresh.
- Own removal state through one `GamblerRowHolder` per touched gambler, held by the screen's view model and dropped when the row's work is done. A SwiftUI `@StateObject` dies with its row, so the screen view model is the owner that survives scrolling; the holder keeps each row's operations encapsulated for the planned per-gambler features (e.g. marking who has paid), the role Android's per-row `RemoveGamblerViewModel` already plays there.
- Make the `manage-gamblers` spec platform-neutral: it was written for Android; its requirements now apply to both platforms with screen-reader and point/dp wording that fits each.
- Add `person_remove` and `close` iOS image sets derived from the Android vector drawables, which are the canonical icon sources now that `assets/icons/` is gone (deleted by #38, not being restored).

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `manage-gamblers`: every requirement that named Android or TalkBack now applies to iOS and Android with platform-neutral wording; the icon requirement names the Android drawables as the canonical source both platforms draw from. No behavior is added or removed.

## Impact

- `iOS/Pool/Sources/Pool/ManageGamblers/`: `ManageGamblersListView.swift`, `ManageGamblersList.swift`, `ManageGamblerItem.swift`, `ManageGamblersListViewModel.swift` (gains `GetPoolUseCase` and the holder map), `PoolMemberModel.swift`; new `GamblerRowHolder.swift`, header and failed-row views.
- `iOS/Tyche/Tyche/UI/PoolHome/PoolHomeRouter.swift`: resolve `GetPoolUseCase` for the screen's view model.
- `iOS/UI/Sources/UI/SwipeToDismissBox.swift`: a fixed-distance threshold option alongside the fraction.
- `iOS/Pool/Sources/Pool/Localizable/Localizable.xcstrings`: remove the Edit/Done, banner and `removing_gambler_text` strings; add the heading, count, "You", "Owner", "Couldn't remove", announcement, retry, dismiss and "Removing" labels (`extractionState: manual`).
- `iOS/UI/Sources/UI/Assets/Assets.xcassets/Icons/`: `person_remove.imageset`, `close.imageset` (and `error`/`group` re-derived if their path data differs from the drawables).
- Tests: `GamblerRowHolder` and `ManageGamblersListViewModel` tests (Swift Testing); `PoolPlaceholderSurfacesTests` updated for the header placeholder; ViewInspector row tests on the iOS 18.1 simulator.
- No backend or Android code changes.
