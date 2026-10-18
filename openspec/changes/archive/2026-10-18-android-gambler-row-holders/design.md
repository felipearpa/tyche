# Design

## Context

See proposal.md for the motivation. Today, `ManageGamblersView` hoists a `ViewModelStoreProvider`, and each non-owner row resolves a keyed `RemoveGamblerViewModel` through `koinViewModel(viewModelStoreOwner = ...)` with `parametersOf(poolId, member, registry)`. `ManageGamblersViewModel` implements `RemovalRegistry` and owns `removedGamblerIds`, `failedGamblerIds`, and the gambler count. A row view model watches `failedGamblerIds` so a refresh can return it to idle, and the view clears a removed row's store with a `LaunchedEffect`.

The reference implementation is iOS `GamblerRowHolder` / `ManageGamblersListViewModel` (`iOS/Pool/Sources/Pool/ManageGamblers/`), plus the holder convention recorded for Bets in `openspec/changes/redesign-bet-entry/design.md`: a `<Feature>RowHolder` held in `rowHolders` and read through `rowHolder(for:)`, a plain class owned by the list view model, with status in `MutationState`. The Android Bets holder (`PendingBetRowHolder`) is not built yet, so this change sets the Android shape first.

The behavior contract is `openspec/specs/manage-gamblers/spec.md` and does not change.

## Goals / Non-Goals

**Goals:**
- The list view model is the single owner of per-row removal state.
- No state flows back from the screen into a row to keep the two in sync.
- The Android shape matches iOS closely enough that one reads as a translation of the other.

**Non-Goals:**
- Any change to presentation, strings, haptics, accessibility, paging, or the count logic.
- Restoring removal state after process death. The old row view models had no `SavedStateHandle` use either.
- Building the Bets holder. That is redesign-bet-entry task 4.1, which can follow this shape.

## Decisions

### 1. `GamblerRowHolder` is a plain class on the list view model's scope

```kotlin
class GamblerRowHolder(
    val member: PoolMemberModel,
    private val poolId: String,
    private val removeGambler: RemoveGambler,
    private val scope: CoroutineScope,      // ManageGamblersViewModel.viewModelScope
    private val outcomes: GamblerRowOutcomes,
) {
    val removal: StateFlow<MutationState<PoolMemberModel>>
    fun remove()          // from Idle or Failure; ignored while Mutating or after Mutated
    fun retry()           // from Failure only
    fun dismissFailure()  // Failure -> Idle, no request
}
```

Transitions are those of today's `RemoveGamblerViewModel`. Starting a removal from `Failure` (a retry, or a swipe-and-confirm on a failed row) reports `failureCleared` first. On success the state becomes `Mutated` and the holder reports `removed`. On error the holder reports `failed` before it publishes `Failure`, so the screen's failed set never lags the row. A started request is never cancelled by the row. It ends only when the list view model is cleared, which today's stores also do on entry disposal.

*Alternatives:* keeping Jetpack `ViewModel`s gives each row `onCleared` and `SavedStateHandle`, but neither is used, and it needs the store provider plus a Koin factory with runtime parameters. Holder-local scopes (a `SupervisorJob` child per holder) would allow cancelling one holder, but no flow cancels a removal.

### 2. Outcomes go one way, through `GamblerRowOutcomes`

`GamblerRowOutcomes` is a small interface (`removed`, `failed`, `failureCleared`) mirroring iOS. The list view model implements it with a private object, so it does not show up in the view model's public API and the holder does not depend on the screen. Unlike `RemovalRegistry`, nothing flows back: the holder never observes the screen's sets.

*Alternative:* lambdas in the holder constructor. That works, but it puts three parameters in place of one named contract and differs from iOS.

### 3. `rowHolders` is an immutable map in a `StateFlow`, created lazily

`ManageGamblersViewModel` exposes `rowHolders: StateFlow<Map<String, GamblerRowHolder>>`, keyed by gambler ID and replaced on each change (never a mutable map), and `fun rowHolder(gamblerId: String): GamblerRowHolder?`, the Kotlin form of iOS `rowHolder(for:)` (`for` is a keyword in Kotlin), which returns the holder or `null`. `remove(member)` gets or creates the holder and calls `remove()`. Retry and dismiss are called on the holder the row already has. A row with no holder is idle, so no holder is created for rows that are only displayed.

The holder is dropped:
- **on `removed`**, at once. The view treats a gambler in `removedGamblerIds` with no holder as `Mutated`, which renders exactly like the holder's last state, so the exit animation is undisturbed and no timer is needed. This replaces the iOS exit-duration delay, which SwiftUI needs because the iOS row reads only its holder.
- **on `failureCleared`, when the holder is idle**, that is, after a dismissal. A retry leaves the holder `Mutating`, so it is kept.
- **on `refresh()`**: every holder whose state is `Failure` is dropped and `failedGamblerIds` is emptied, so those rows render idle. In-flight holders are kept.

`failedGamblerIds` and `removedGamblerIds` stay as `StateFlow<Set<String>>`, updated only by the outcomes. They still drive the haptic, the count, and the hidden rows, with the same semantics as today, including `onMembersRefreshed`.

### 4. The view reads holders; the store plumbing goes

`ManageGamblersView` collects `rowHolders` and passes each removable row its holder, or `null`. The row collects `holder.removal` when present, and otherwise uses `Mutated(member)` if the gambler is in `removedGamblerIds` and `Idle(member)` if not. Confirming calls `viewModel.remove(member)`. The confirmation flag stays local `remember` state, as today. `rememberViewModelStoreProvider`, `rememberViewModelStoreOwner`, the `clearKey` effect, and the per-row `koinViewModel` call are deleted, and so are `RemoveGamblerViewModel`, `RemovalRegistry`, and its Koin entry. `ManageGamblersViewModel` gets `removeGambler = get()`.

## Risks / Trade-offs

- [A holder dropped while its row still animates] → Only `removed` drops a holder whose row is still visible, and the view's `removedGamblerIds` fallback keeps the same `Mutated` state. Unit tests cover the drop, and the emulator pass watches the exit animation.
- [Behavior drift while porting] → Each existing test case is ported one to one (same names where they still apply), and the emulator pass repeats the removal scenarios from the previous change's `verification.md`.
- [Rotation] → Holders live in the entry-scoped list view model, which survives configuration changes. The emulator pass rotates during an in-flight removal, using a slow or offline network to make it observable.
