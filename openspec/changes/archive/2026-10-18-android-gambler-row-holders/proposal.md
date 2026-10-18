# Proposal

## Why

Android Manage gamblers keeps each row's removal in a Compose-created `RemoveGamblerViewModel` held in a `ViewModelStoreProvider`. The screen still has to know every row's outcome (removed gamblers stay hidden, failed rows are released on refresh, the count drops, the error haptic plays), so the state lives in two places and is kept in sync through a `RemovalRegistry` round trip: rows report to the screen, and each row also watches the screen's failed set to return to idle. iOS Manage gamblers and the new Bets screen instead keep plain per-row holders owned by the list view model. Doing the same on Android gives the state one owner, removes the sync plumbing, and makes both platforms read the same.

## What Changes

- `ManageGamblersViewModel` creates and keeps plain `GamblerRowHolder` objects (not Jetpack `ViewModel`s) in a `rowHolders` map read through `rowHolder(for:)`. A holder is created on the first action on a row and dropped when its work is done, as on iOS.
- Each holder exposes its removal as a `StateFlow<MutationState<PoolMemberModel>>` and runs on the list view model's `viewModelScope`.
- Remove `RemoveGamblerViewModel`, `RemovalRegistry`, the `rememberViewModelStoreProvider` / `rememberViewModelStoreOwner` plumbing in `ManageGamblersView.kt`, and the row view model's Koin registration.
- Port `ManageGamblersViewModelTest` and `RemoveGamblerViewModelTest` to JUnit 5 + Kotest `runTest` tests over the list view model and its holders.
- No user-visible change: confirm, removing, failure with Retry and dismiss, removed gamblers hidden while cached pages still contain them, a failed row released on pull-to-refresh, the gambler count, and survival across scrolling and rotation all behave as before.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
<!-- none: the manage-gamblers requirements are unchanged; this is an Android implementation refactor (skip_specs: true) -->

## Impact

- `Android/pool/src/main/java/com/felipearpa/tyche/pool/managegamblers/`: `ManageGamblersViewModel.kt` (gains `RemoveGambler`, the holders, and the outcome handling), new `GamblerRowHolder.kt`, `ManageGamblersView.kt` (rows read their holder from the list view model); `RemoveGamblerViewModel.kt` and `RemovalRegistry.kt` deleted.
- `Android/pool/src/main/java/com/felipearpa/tyche/pool/di/PoolViewModelModule.kt`: `RemoveGamblerViewModel` registration removed, `removeGambler` passed to `ManageGamblersViewModel`.
- `Android/pool/src/test/.../managegamblers/`: tests ported; `RemoveGamblerViewModelTest.kt` replaced by `GamblerRowHolderTest.kt`.
- `Android/pool/build.gradle.kts`, `Android/gradle/libs.versions.toml`, `Android/gradle.properties`: unchanged; the explicit `lifecycle-viewmodel-compose` dependency stays.
- Nothing changes in iOS, the backend, or the strings.
