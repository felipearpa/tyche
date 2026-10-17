# Design

See `proposal.md` for motivation and `reference/README.md` for the artboards. The behavior contract is `specs/manage-gamblers/spec.md`; this document covers how Android meets it.

## Context

- The screen lives in `Android/pool/.../managegamblers/`. `ManageGamblersView` is a `Scaffold` with an empty-title `TopAppBar`, an Edit/Done `TextButton`, a `RefreshableLazyPagingColumn` of `ManageGamblerRow`s (Material 3 `SwipeToDismissBox`, end-to-start only, confirm threshold opens the dialog), and a floating `ManageGamblerErrorBanner`. `ManageGamblerItem` already renders the shared avatar, the ball spinner while removing, and the `isPlaceholder` treatment.
- `ManageGamblersViewModel` (Koin `koinViewModel`, scoped to the navigation entry) owns the `Pager` and a `MutableStateFlow<Map<String, MutationState<PoolMemberModel>>>` of removal states keyed by gambler ID. `failedGambler` takes the first failure in the map; `dismissFailure()` exists but is never called.
- `ManageGamblersRoute(poolId, gamblerId)` is opened from `PoolHomeNavViewProvider`. The drawer's Gamblers badge comes from the existing `GetPool` use case (`pool.gamblerCount`, a counter the backend keeps on the pool row and adjusts on join and remove), not from the members list.
- The committed build is on Lifecycle 2.10.0 (`lifecycle-runtime-ktx`); this change raises it to 2.11.0 with AGP 9.4.1. Compose BOM 2026.05.00, compileSdk 36 (raised to 37, see decision 7), Koin 4.2.1. `lifecycle-viewmodel-compose` arrives transitively through `koin-compose-viewmodel`; it is not pinned.
- UI state types come from `com.felipearpa:viewing-state` (`MutationState`, `LoadState`, `SaveState`), already a dependency of the pool module; this change introduces no state type of its own.
- Redesigned screens (`MatchGamblerBetItem`, History/Timeline) use a scrolling header as the first list item, `LocalExtendedColorScheme.currentUserContainer`/`onCurrentUserContainer` for the viewer's row, `HistoryRowDivider`-style separators, and the Material Symbols pipeline in `assets/icons/README.md`.

## Goals / Non-Goals

**Goals:**
- One state owner per removal so several removals, failures, and retries never interfere and in-flight work survives scrolling and rotation.
- Reuse the existing row, avatar, spinner, placeholder, and paging components; change presentation and state ownership, not infrastructure.
- Keep the backend, the route's navigation entry ViewModel, and the iOS screen untouched.

**Non-Goals:**
- Pinning the owner first (would need a backend sort or a second data source).
- A long-press context menu as a second removal route (can be added later without changing this design).
- New confirmation copy; the existing `remove_gambler_alert_*` strings stay.

## Decisions

### 1. Per-gambler `RemoveGamblerViewModel` scoped with Lifecycle 2.11 composable-scoped stores

`ManageGamblersView` hoists one `ViewModelStoreProvider` with `rememberViewModelStoreProvider()` (parent: the navigation entry's `LocalViewModelStoreOwner`). Each non-owner row calls `rememberViewModelStoreOwner(key = member.gamblerId, provider = provider)` and provides it through `CompositionLocalProvider(LocalViewModelStoreOwner provides owner)` so `koinViewModel<RemoveGamblerViewModel> { parametersOf(poolId, member, registry) }` resolves against that owner. The registry is the entry-scoped `ManageGamblersViewModel` (decision 2), passed as a parameter because Koin cannot resolve it with `get()`.

`RemoveGamblerViewModel(poolId, member, removeGambler, registry)` exposes `state: StateFlow<MutationState<PoolMemberModel>>` (`Idle` → `Mutating` → `Mutated` | `Failure`) and `remove()`/`retry()`/`dismissFailure()`. On success it calls `registry.markRemoved(gamblerId)`.

Why the keyed overload: the keyed `rememberViewModelStoreOwner(key, provider)` does **not** clear its store when the row leaves composition; only `provider.clearKey(key)` or disposing the provider does. That is what keeps an in-flight removal alive while the row is scrolled out of a `LazyColumn` and across rotation (the provider survives configuration changes with its parent). The unkeyed overload would clear the store, cancel `viewModelScope`, and lose the request's outcome.

Why key by gambler ID: Paging's `itemKey` is already the gambler ID; keying the store the same way means a reorder or a refreshed page never hands one gambler's state to another.

Alternatives: keeping the map in `ManageGamblersViewModel` is fewer parts and was the previous design; it was set aside because the user chose composable-scoped ViewModels for this screen. A `remember`ed state holder per row would not survive rotation.

### 2. `ManageGamblersViewModel` keeps the paging and the set of removed IDs

The screen ViewModel keeps the `Pager` and gains `removedGamblerIds: StateFlow<Set<String>>` with `markRemoved(id)`. It is the `RemovalRegistry` collaborator passed to each row ViewModel as a Koin parameter (a small interface so the row ViewModel does not depend on the screen ViewModel). `failedGambler`, `removalStates`, `remove()`, and `dismissFailure()` are removed from it.

Why a set in the screen ViewModel: once a row's store is cleared, a later recomposition of that gambler (still present in the cached paging data until refresh) would create a fresh `Idle` ViewModel and the row would reappear. The screen filters its items against `removedGamblerIds` so the gambler stays gone until a refresh returns data without them.

The screen ViewModel also takes `GetPool` and exposes `gamblerCount: StateFlow<LoadState<Int?>>`: requested on creation and again from the refresh callback, alongside the paging refresh. The heading shows `count - removedGamblerIds.size`; a refresh clears `removedGamblerIds` once the new first page has landed, since the refreshed data no longer contains the removed gamblers and the fresh count already excludes them. `Loading` renders the placeholder line, `Failure` omits the line (no retry of its own; the next pull-to-refresh retries it), and the route is unchanged.

Alternatives: carrying `gamblerCount` on the route from the drawer avoids the request but is a snapshot — joins while the screen is open never show, and refresh cannot update it. Counting loaded rows is wrong past the first page. Making the members endpoint return a total is a backend change this client-only change avoids; `GetPool` already returns the number the drawer shows.

### 3. Releasing row stores

- **Success:** after the row's exit animation, the screen calls `provider.clearKey(gamblerId)` (`LaunchedEffect` keyed on `removedGamblerIds`). The ID stays in the set.
- **Pull to refresh:** the collaborator from decision 2 is a `RemovalRegistry` interface with `markRemoved(id)`, `markFailed(id)`, and `clearFailed(id)`, implemented by `ManageGamblersViewModel` as `removedGamblerIds` and `failedGamblerIds` flows. A row ViewModel reports `Failure` with `markFailed` and clears it on retry. The refresh callback calls `clearFailed(id)` for every ID in `failedGamblerIds`; each row ViewModel observes `failedGamblerIds` and returns from `Failure` to `Idle` when its ID leaves the set, whether or not the row is composed. In-flight removals are left alone. Failed rows' stores are not cleared on refresh: in Lifecycle 2.11, `clearKey` on a still-composed row only marks its store to be cleared when the row leaves composition, so it would not reset a visible row and would cancel a later in-flight removal on that row once it scrolled away. Those stores stay until the entry is disposed.
- **Dismiss:** the failed row's ✕ calls the row ViewModel's `dismissFailure()` (`clearFailed` on the registry, state back to `Idle`); the key is released when the entry is disposed. Keeping the store alive after dismiss costs one idle ViewModel and avoids racing the row's own recomposition.
- **No cancel while removing:** a removal is one server call; cancelling the coroutine could leave a gambler shown who is already gone. The row is non-swipeable while `Mutating` and always ends in `Mutated` or `Failure`.
- **Leaving the screen:** the provider is disposed with its parent entry, clearing all stores.

### 4. Presentation

- `TopAppBar(title = {})` with Back only; Back carries a localized content description. The first list `item` is `ManageGamblersHeader(count: LoadState<Int?>)` (placeholder while loading, line omitted on failure or null) — heading `headlineLarge`/bold and `bodyMedium` count in `onSurfaceVariant`.
- Owner row: `currentUserContainer` background, `onCurrentUserContainer` text, `AccountAvatarFallback(backgroundColor = currentUser, foregroundColor = onCurrentUser)`, "You" as `labelMedium` semibold beside the username (same line, after it), "Owner" pill (`CircleShape` surface at 16 % `currentUser` tint, `labelLarge`). No `SwipeToDismissBox`.
- Swipe background: error container, `person_remove` icon + "Remove" `labelMedium`, both tinted `onError` (white in light, `#1E1E1E` in dark, 4.63:1 on dark `error`; white was 3.6:1), icon 24 dp growing to 28 dp past the threshold (existing behavior).
- Swipe threshold: a fixed 56 dp positional threshold through the current Material 3 `SwipeToDismissBox`/`SwipeToDismissBoxState` API (threshold on the state and `onDismiss`), replacing the deprecated `confirmValueChange`, which in Material 3 1.4 resolved only once the drag passed about half the row.
- Failed row: `ManageGamblerItem` gains a `failure` slot: email line replaced by `Row(error icon 16 dp, "Couldn't remove")` in `error`, and, at the trailing edge, a `Retry` pill — Material 3 `FilledTonalButton`, `CircleShape`, `filledTonalButtonColors(containerColor = errorContainer, contentColor = onErrorContainer)` (stock tokens; see decision 8), `retry_action` label, `sizeIn(minHeight = 44.dp)`, `contentDescription` "Retry removing @username" — followed by a Material 3 `IconButton` (48 dp target) with the `close` drawable tinted `onSurfaceVariant` (`contentDescription` "Dismiss, couldn't remove @username"). The paging error views keep their full-width filled `Button`s; the row-level pill is intentionally quieter. The text has `liveRegion = Polite` so TalkBack announces it when it appears. `LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.Reject)` on the transition to `Failure`.
- Empty state: when `loadState.refresh` is `NotLoading`, `append.endOfPaginationReached`, and `itemCount == 1` with the owner, append an `item` with the `group` icon, `no_other_gamblers_title`, and `no_other_gamblers_message`.
- `ManageGamblerRow` removes `isEditing`; previews updated accordingly.

### 5. Strings

Remove `manage_gamblers_edit_action`, `manage_gamblers_done_action`, `remove_gambler_failure_message`, and `remove_gambler_failure_title` from the three `strings.xml`. Add `manage_gamblers_title` ("Gamblers"), plural `manage_gamblers_count` ("%1$d gambler in this pool" / "%1$d gamblers in this pool"; es "jugador"/"jugadores" + "polla", es-rES "quiniela"), `manage_gamblers_you_label` ("You"/"Tú"), `manage_gamblers_owner_label` ("Owner"/"Dueño"), `remove_gambler_failure_text` ("Couldn't remove"/"No se pudo eliminar"), `remove_gambler_failure_announcement` ("Couldn't remove %1$s"/"No se pudo eliminar a %1$s"), `retry_remove_gambler_action` ("Retry removing %1$s"), `dismiss_remove_gambler_failure_action` ("Dismiss, couldn't remove %1$s"). `remove_gambler_action` ("Remove") is reused for the swipe label.

### 6. Icons

Add `assets/icons/person_remove.svg`, `error.svg`, `group.svg`, and `close.svg` (Material Symbols Outlined, fill 0, weight 400, grade 0, 24 px, from the pinned `material-design-icons` commit) with README entries and SHA-256s. Derive `Android/ui/src/main/res/drawable/person_remove.xml` and `close.xml` the way `add.xml` is; verify the existing `error.xml` and `group.xml` match their sources and re-derive if not. `delete_forever.xml` stays for other callers (check; delete if unused); `remove_circle.xml` is unused after the Edit mode goes and is deleted.

### 7. Dependencies

`libs.versions.toml`: `lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle-runtime-ktx" }`; `pool/build.gradle.kts`: `implementation(libs.lifecycle.viewmodel.compose)`. `lifecycle-runtime-ktx` goes from 2.10.0 to 2.11.0, and this pins what the code now calls directly rather than relying on Koin's transitive version. Lifecycle 2.11's Compose artifacts require compileSdk 37, so `Android/gradle.properties` raises `projectCompileSdk` from 36 to 37 (platform android-37 must be installed); targetSdk is unchanged.

### 8. Retune the dark `errorContainer` tokens instead of a custom tint

`errorContainer`/`onErrorContainer` are defined in `Android/ui/.../theme/Color.kt` and `Theme.kt` but referenced nowhere in the app. The dark pair (`#B71C1C` / `#FFCDD2`) is a saturated Material 2 error red that, as a container, outweighs the row. Change `darkErrorContainer` to `#342124` (the dark `error` `#CF6679` at 18 % over the `#121212` surface) and `darkOnErrorContainer` to `#D47586` (the dark `error` lightened 15 % toward white: 4.81:1 on `#342124`, where `#CF6679` gave 4.2:1); leave the light pair (`#FFCDD2` / `#B71C1C`) as is. The Retry pill then uses stock tokens with no alpha arithmetic in the pool module, and any future error-tinted container inherits the same look.

Alternative considered: `error.copy(alpha = 0.18f)` on the button only. It gives the same pixels but hides a theme decision inside one screen and leaves a wrong token for the next consumer.

## Risks / Trade-offs

- [Row ViewModels outlive their rows until `clearKey`] → Success clears keys explicitly; failed and dismissed rows keep an idle store until the provider is disposed with the navigation entry. A unit test covers "success marks removed; failure does not".
- [A removed gambler reappears from cached paging data] → `removedGamblerIds` filters the items until refresh; spec scenario "Removal succeeds" is the check.
- [`koinViewModel` resolving against the wrong owner] → Pass the owner explicitly (`koinViewModel(viewModelStoreOwner = owner)`) rather than relying on the composition local alone.
- [Compose `compileSdk` requirements of lifecycle 2.11 Compose artifacts] → Lifecycle 2.11's Compose artifacts require compileSdk 37 (`checkAarMetadata` fails on 36); decision 7 raises `projectCompileSdk` to 37 and the platform must be installed before the app builds.
- [Retuned `errorContainer` surprises a future consumer] → The pair has no consumer today; the change is recorded here and visible in `TycheTheme` previews. The light pair is untouched.
- [Error haptic constant availability] → `HapticFeedbackType.Reject` ships with Compose UI 1.8+ (within BOM 2026.05). If unavailable on a device it is a no-op.
- [Users lose the Edit mode route] → Swipe plus the TalkBack custom action cover touch and assistive access; the drawer entry text does not change.

## Migration Plan

Pure client change shipped with the next Android release; no data or API migration. Rollback is reverting the commit.
