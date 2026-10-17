# Proposal

## Why

Manage gamblers is the last owner-only screen on Android that still uses the pre-redesign presentation: a blank top bar with an Edit/Done mode, a solid green owner row, a floating failure banner, and a removal flow that offers two routes to one action. The screens redesigned since (Match bets, Timeline, History) established a flat list with a scrolling heading, the current-user highlight, inline states, and shared loading placeholders; this screen should follow them. The failure banner also hides every failure after the first, so an owner who removes two gamblers and loses connection learns about only one of them.

## What Changes

- Redesign the Android Manage gamblers screen around the [design canvas](reference/README.md): a scrolling "Gamblers" heading with the gambler count, flat rows with the shared avatar, the owner's row highlighted as the current user with a "You" marker and an "Owner" pill, and thin separators.
- **BREAKING** (UI): remove the Edit/Done mode and its per-row minus control. Swiping a row from end to start is the only touch route to removal; the TalkBack custom action "Remove" remains.
- Replace the swipe background's trash icon with a person-remove glyph and a "Remove" label.
- Keep the confirmation dialog; confirming puts the row in a removing state (dimmed, the shared ball spinner in place of the avatar) without blocking the rest of the list.
- Show a failed removal inside its own row ("Couldn't remove" with a Retry pill) and drop the floating banner, so several failures are visible at once and no row is covered.
- Show the "No other gamblers yet" empty state when the fully loaded list contains only the owner.
- Scope removal state per gambler with Lifecycle 2.11's composable-scoped ViewModels: one `RemoveGamblerViewModel` per row, keyed by gambler ID from a `ViewModelStoreProvider` hoisted at the screen, so an in-flight removal survives scrolling and rotation and is released when the row is gone.
- Add the `person_remove` canonical icon source and derive the Android asset from it; record `error` and `group` as canonical sources for the failure and empty-state glyphs.
- iOS keeps its current Manage gamblers screen; its redesign is a separate change.

## Capabilities

### New Capabilities
- `manage-gamblers`: the pool owner's list of gamblers and the per-gambler removal flow on Android: presentation, swipe-to-remove with confirmation, removing/failed/removed row states, empty and loading states, refresh, and accessibility.

### Modified Capabilities
<!-- none: the drawer's Gamblers entry, the loading-placeholder treatment, and the avatar rules are unchanged -->

## Impact

- `Android/pool/.../managegamblers/`: `ManageGamblersView.kt`, `ManageGamblerItem.kt`, `ManageGamblersViewModel.kt` (gains the existing `GetPool` use case for the gambler count), the pool Koin module; new `RemoveGamblerViewModel.kt`. The route and its navigation entry are unchanged.
- `Android/pool/src/main/res/values{,-es,-es-rES}/strings.xml`: remove the Edit/Done and banner strings; add the heading, count, "You", "Owner", "Remove", "Couldn't remove", and Retry-for-gambler strings.
- `Android/gradle/libs.versions.toml`, `Android/pool/build.gradle.kts`: explicit `androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0`, with `lifecycle-runtime-ktx` raised from 2.10.0 to 2.11.0 and AGP to 9.4.1.
- `Android/gradle.properties`: `projectCompileSdk` 36 → 37, which Lifecycle 2.11's Compose artifacts require (targetSdk unchanged).
- `assets/icons/`: `person_remove.svg`, `error.svg`, `group.svg`, `close.svg` and their README entries; `Android/ui/src/main/res/drawable/person_remove.xml`, `close.xml`.
- `Android/ui/.../theme/Color.kt`: retuned dark `errorContainer`/`onErrorContainer` (currently unused anywhere).
- Tests: `ManageGamblersViewModel` and `RemoveGamblerViewModel` unit tests (JUnit 5 + Kotest assertions); existing placeholder-surface tests keep passing.
- No backend, iOS, or `navigation-drawer` changes; one extra `GET /pools/{id}` request when the screen opens and on refresh.
