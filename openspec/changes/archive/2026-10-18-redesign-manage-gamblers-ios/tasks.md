# Tasks

## 1. iOS: icons, strings, swipe threshold

Icons derive from the Android drawables (the canonical sources since `assets/icons/` went away in #38).

- [x] 1.1 Add `iOS/UI/.../Icons/person_remove.imageset` and `close.imageset` whose SVG path data is copied verbatim from `Android/ui/src/main/res/drawable/person_remove.xml` and `close.xml` (viewBox `0 -960 960 960`, template rendering, vector data preserved, like `add.imageset`); verify the `d` attribute equals the drawable's `pathData` and the `UI` package builds.
- [x] 1.2 Compare `error.imageset` and `group.imageset` path data with `error.xml` and `group.xml`; re-derive the image sets if they differ; verify the two `d`/`pathData` pairs are equal.
- [x] 1.3 Add a fixed-distance threshold option to `SwipeToDismissBox` (fraction stays the default) and verify its existing callers compile unchanged and a Swift Testing test covers "56 pt confirms, 40 pt snaps back" on the threshold math.
- [x] 1.4 Update `Pool/Localizable/Localizable.xcstrings`: remove `removing_gambler_text`; add the heading, count plural, You, Owner, "Couldn't remove", announcement, retry, dismiss and "Removing" accessibility-value keys in en/es/es-ES with `extractionState: manual` and the Android strings as the translations; verify the generated symbols build (`xcodebuild -workspace Tyche.xcworkspace -scheme Tyche build`) and `git status` shows no unintended `Package.resolved` change.

## 2. iOS: view model

- [x] 2.1 Add `GamblerRowHolder` (`ObservableObject`: `removal` state, `remove()`, `retry()`, `dismissFailure()`, its `Task` cancelled in `deinit`) reporting through a `GamblerRowOutcomes` protocol; verify Swift Testing tests cover remove → mutated + reported, failure → reported, retry without confirmation, dismiss without a request.
- [x] 2.2 Rework `ManageGamblersListViewModel`: `rowHolders: [String: GamblerRowHolder]` created on first remove, dropped on success (after the exit animation), on dismiss, and for failures on refresh; `removedGamblerIds` kept until the refreshed first page lands; `getPoolUseCase` with `gamblerCount: LoadState<Int?>` loaded on init and on refresh; delete `removalStates`, `failedGambler`, and the old `dismissFailure()`; pass `GetPoolUseCase` from `PoolHomeRouter`; verify Swift Testing tests cover holder creation and reuse, success dropping the holder and lowering the count, dismiss/refresh dropping failed holders, refresh pruning, and count load/failure/reload.

## 3. iOS: presentation

- [x] 3.1 Replace the toolbar Edit/Done button and `isEditing` with nothing (Back only) and delete the `manage_gamblers_edit_action`/`manage_gamblers_done_action` keys, add `ManageGamblersHeader` (title, count minus removed entries, placeholder on the count line) as the first row, and extend `PoolPlaceholderSurfacesTests` for the header placeholder; verify previews and the tests on the iOS 18.1 simulator.
- [x] 3.2 Restyle the owner row as the current-user row (container and text colors, current-user avatar fallback, "You" beside the username, "Owner" capsule, no swipe box, no VoiceOver Remove action) and the other rows to `HistoryStyle` metrics with `HistoryRowDivider`; verify previews and a ViewInspector test that the owner row exposes no Remove action.
- [x] 3.3 Restyle the swipe background (error fill, `person_remove` glyph growing past the 56 pt threshold, "Remove" label, both `onError`) and remove the minus control; verify previews and, on a simulator, a 56 pt drag opens the alert while a 40 pt drag snaps back.
- [x] 3.4 Add the failed state to `ManageGamblerItem` (error glyph + "Couldn't remove" in place of the email, Retry capsule and ✕ button each ≥ 44 pt with gambler-named accessibility labels, error haptic and VoiceOver announcement on entering `.failure`), have each non-owner row observe its `GamblerRowHolder` when one exists (`@ObservedObject` in a row-state subview) and render idle otherwise, wire Retry to `holder.retry()` and ✕ to `holder.dismissFailure()`, and delete `ManageGamblerErrorBanner` with its `remove_gambler_failure_title`/`remove_gambler_failure_message` keys; set `removing_gambler_accessibility_value` as the removing row's VoiceOver value; verify previews, a ViewInspector test that a holder state change redraws the row, a test that a removing row carries that value, and, on a simulator in airplane mode, two failed rows each show Retry and ✕, Retry re-enters removing without an alert, ✕ returns the row to idle with the count unchanged, and pull-to-refresh returns the rest to idle.
- [x] 3.5 Add the "No other gamblers yet" empty row (group glyph, existing title and message) shown only when pagination has ended with the owner as the only member; verify a preview and a simulator pool with only the owner shows it with the singular count line.

## 4. iOS: integration check

- [x] 4.1 Run the Pool and UI package tests on the iOS 18.1 simulator and build the Tyche scheme, then walk the spec scenarios on a simulator in English and Spanish (es-CO, es-ES): open from the drawer, swipe/cancel/confirm, two concurrent removals, failure + retry + dismiss, only-owner empty state, first-load placeholders, load error without the empty state; record screenshots under `verification/` and each scenario's result in `verification.md`, noting anything the simulator cannot show (haptics).
