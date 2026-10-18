# Design

See `proposal.md` for motivation, `reference/README.md` for the artboards, and `openspec/specs/manage-gamblers/spec.md` (with this change's delta) for the behavior. The Android counterpart's design is archived at `openspec/changes/archive/2026-10-17-redesign-manage-gamblers-android/design.md`; this document covers where iOS differs.

## Context

- The screen lives in `iOS/Pool/Sources/Pool/ManageGamblers/`. `ManageGamblersListView` holds a `@StateObject ManageGamblersListViewModel`, an inline empty title, the Edit/Done toolbar button, the `.alert` confirmation, and the bottom `ManageGamblerErrorBanner` overlay. `ManageGamblersList` builds rows on `RefreshableLazyPagingVStack` (lazy-paging-swift 0.0.5: `ScrollView` + `LazyVStack`, rows identified by `PoolMemberModel.id == gamblerId`) with the app's own `SwipeToDismissBox` (fraction threshold, `onConfirm` opens the alert). `ManageGamblerItem` already renders `EmailAvatar`, the `BallSpinner` on `surfaceVariant` while removing, and the `isPlaceholder` treatment.
- `ManageGamblersListViewModel` owns the pager and `removalStates: [String: MutationState<PoolMemberModel>]` keyed by gambler ID (`viewing-state-swift`). `failedGambler` reads the first failure of an unordered dictionary; `dismissFailure()` is never called.
- `PoolHomeRouter` builds the view model from `diResolver`; `PoolHomeDrawerViewModel` already uses `GetPoolUseCase` for the drawer's gambler count badge.
- Redesigned screens (`MatchGamblerBetItem`, History/Timeline) use `HistoryStyle` (16 pt gutter, `onSurface` 0.7 secondary text, `currentUser`/`currentUserContainer` for the viewer's row, `HistoryRowDivider`), `AccountAvatar` with `AccountAvatarFallback(backgroundColor: currentUser, foregroundColor: onCurrentUser)`, and inline-titled native bars per `ios-liquid-glass`.
- Deployment target iOS 16: `ObservableObject`/`@StateObject`, `UINotificationFeedbackGenerator`, `UIAccessibility.post(notification: .announcement, …)`.
- `assets/icons/` was deleted by #38 and is not coming back: the Android vector drawables under `Android/ui/src/main/res/drawable/` are the canonical icon sources now (their path data is the Material Symbol's, unchanged, in a 960 × 960 viewport with `translateY="960"` standing in for the symbol's `0 -960 960 960` viewBox). `iOS/UI` has image sets for `error`, `group`, `delete_forever`; `person_remove` and `close` do not exist yet.

## Goals / Non-Goals

**Goals:**
- Match the spec's behavior with the state owned where SwiftUI can keep it alive: the screen view model.
- Reuse `SwipeToDismissBox`, `ManageGamblerItem`, `BallSpinner`, the placeholder treatment, and the paging container; change presentation, not infrastructure.
- Derive the iOS icons from the Android drawables so both platforms draw one committed geometry.

**Non-Goals:**
- A row `@StateObject` as the owner of removal state (see decision 1).
- Native `swipeActions` (needs a `List`; see decision 3).
- Pinning the owner first; new confirmation copy; Android changes.

## Decisions

### 1. A row holder per touched gambler, owned by the screen view model

`ManageGamblersListViewModel` owns `rowHolders: [String: GamblerRowHolder]`, empty until the owner acts on a row. `GamblerRowHolder` is an `ObservableObject` with `@Published removal: MutationState<PoolMemberModel>`, `remove()`, `retry()`, `dismissFailure()`, and the `Task` running its request; it reports outcomes to the screen view model through a small `GamblerRowOutcomes` protocol (`removed(id)`, `failed(id)`, `failureCleared(id)`) so it does not depend on the screen view model type. The screen view model creates a holder on the first `remove` for a gambler (`holder(for:)`), drops it when the removal succeeds (after the row's exit animation) and when a failure is dismissed or cleared by refresh, and keeps `removedGamblerIds` so cached pages cannot bring a removed gambler back until the refreshed first page lands. Untouched rows have no holder and render idle.

Why this shape: SwiftUI has no keyed store that outlives a row (a `@StateObject` dies with its view identity; `List` recycles rows, `LazyVStack` retaining them is behavior, not contract), so the owner that survives scrolling and rotation must be the screen view model. Owning per-row holders there keeps each row's operations encapsulated — the planned "paid" toggle is another method and state on the same holder — without the screen view model accumulating one method per operation per row. Holders exist only for rows the owner touched, so memory never grows with scrolling. Android keeps its shipped shape (a `RemoveGamblerViewModel` per row in a keyed Lifecycle 2.11 store); both are "one state owner per row, kept alive above the row", differing only in who keeps it alive — the framework's store on Android, the screen view model on iOS, where no such store exists.

Rows observe their holder directly (`GamblerRowStateView(holder:)` with `@ObservedObject`); a nested `ObservableObject` does not forward its changes to the parent, and forwarding is not needed when the row observes the holder itself.

Alternatives: a plain `[id: MutationState]` map (simplest today, but every future per-row operation lands as more methods and a growing value type on the screen view model); a row `@StateObject` as in Pending bets (recycled with the row; an in-flight removal cannot be rebuilt from `PoolMemberModel`); a row `@StateObject` with the screen as source of truth (state in two places).

### 2. Gambler count from `GetPoolUseCase`

The view model gains `getPoolUseCase` and `@Published var gamblerCount: LoadState<Int?>`: `.loading` on init and on refresh, `.loaded(pool.gamblerCount)`, `.failure` leaves the line out (no retry of its own; the next pull-to-refresh retries). The header shows `count - removedGamblerIds.count`; a completed refresh prunes the IDs that were already removed when the refresh started once the new first page has landed, and drops every holder in `.failure` (the spec's "pull to refresh with a failed row"). Holders in `.mutating` are left alone. `PoolHomeRouter` resolves `GetPoolUseCase` into the view model.

### 3. Swipe keeps the custom `SwipeToDismissBox`, with a fixed threshold

Native `swipeActions` only works inside a `List`; the redesigned lists are `LazyVStack`-based and the `enableViewSwipeActions()` wrapper (a one-row `List` per row) would fight pull-to-refresh and paging. Keep `SwipeToDismissBox` and add a `threshold: .distance(56)` option beside the existing fraction (parameter with the fraction as default so other callers are untouched). The background becomes `ManageGamblerSwipeBackground`: error fill, `person_remove` glyph scaled 1.22 past the threshold plus the "Remove" label, both `onError`. Removing, failed-then-retrying, and owner rows pass `isEnabled: false`.

### 4. Presentation

- Toolbar: no title, no trailing item; `.navigationBarTitleDisplayMode(.inline)` stays so the bar only carries Back. The heading is the first row of the stack: `ManageGamblersHeader(count: LoadState<Int?>, isPlaceholder)` with `.largeTitle.bold()` "Gamblers" and a `.subheadline` count in `HistoryStyle.secondaryText`, 16 pt gutter; placeholder via `PulsingPlaceholderContent` on the count line only.
- Row: `HistoryStyle.horizontalPadding`, 12 pt vertical, `HistoryRowDivider`. Owner row: `currentUserContainer` background, `onCurrentUserContainer` text, `AccountAvatarFallback(backgroundColor: currentUser, foregroundColor: onCurrentUser)`, "You" (`.caption.weight(.semibold)`, `currentUser`) beside the username, "Owner" capsule (`currentUser` text on `currentUser` at 16 %). No swipe box around it.
- Failed row: `ManageGamblerItem` gains a `failure` state: the email line becomes `error` glyph + "Couldn't remove" in `error`; trailing `Retry` capsule (`.body.weight(.semibold)`, `error` on `error` at 18 %, min 44 pt hit area) and a 44 pt `close` button in `secondary`. Accessibility labels "Retry removing @username" / "Dismiss, couldn't remove @username". On entering `.failure`: `UINotificationFeedbackGenerator().notificationOccurred(.error)` and `UIAccessibility.post(notification: .announcement, argument: "Couldn't remove @username")`, triggered from `onChange` of the state in the row.
- VoiceOver: the row stays one element with `.accessibilityAction(named: "Remove")` for removable rows and a value of "Removing" (`removing_gambler_accessibility_value`) while in flight; placeholders stay hidden.
- Empty: when the pager reports end of pagination with exactly one loaded member who is the owner, append `NoOtherGamblersView` (`group` glyph, existing title and message).
- `ManageGamblerErrorBanner`, the Edit/Done button, `isEditing`, and the minus control are deleted.

### 5. Strings (`Pool/Localizable/Localizable.xcstrings`, `extractionState: manual`)

Remove `manage_gamblers_edit_action` and `manage_gamblers_done_action` with the Edit/Done button (task 3.1), `remove_gambler_failure_message` and `remove_gambler_failure_title` with the banner (task 3.4), and `removing_gambler_text`. Add `manage_gamblers_title`, `manage_gamblers_count` (plural, en/es/es-ES with "polla"/"quiniela"), `manage_gamblers_you_label`, `manage_gamblers_owner_label`, `remove_gambler_failure_text`, `remove_gambler_failure_announcement`, `retry_remove_gambler_action`, `dismiss_remove_gambler_failure_action`, with the Android values as the source of the translations, and `removing_gambler_accessibility_value` ("Removing"/"Eliminando", the row's VoiceOver value while in flight; Android has no equivalent, so its translations come from the old `removing_gambler_text` without the ellipsis).

### 6. Icons

The Android drawables are the source. For each glyph, the iOS image set's SVG is built from the drawable's `pathData` verbatim: `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 -960 960 960"><path d="…"/></svg>` (the drawable's `translateY="960"` group is what maps that viewBox onto its 960 × 960 viewport, so the path string itself is identical). The image set renders as a template and preserves vector data, like `add.imageset`. Add `person_remove.imageset` and `close.imageset`; compare `error.imageset` and `group.imageset` against `error.xml` and `group.xml` and re-derive them if the path data differs (#38 re-derived both drawables). `delete_forever.imageset` stays for any other caller; remove it if none remains.

### 7. Tests

`GamblerRowHolderTests` (Swift Testing): remove → `.mutating` then `.mutated` and `removed(id)` reported; failure → `.failure` and `failed(id)`; retry → `.mutating` with no second confirmation; dismiss → `.idle` and `failureCleared(id)`, no request. `ManageGamblersListViewModelTests`: a holder is created on first remove and reused; success drops the holder and keeps the ID in `removedGamblerIds` so the count drops; dismiss and refresh drop failed holders; refresh prunes removed IDs after the first page; count load, failure, and reload. Row tests with ViewInspector run on the throwaway iOS 18.1 simulator (`PoolPlaceholderSurfacesTests` extended for the header placeholder).

## Risks / Trade-offs

- [`LazyVStack` disposes a removing row] → Its holder lives in the screen view model; the recreated row finds it by ID, so nothing is lost.
- [A holder's `Task` outlives its holder] → The holder cancels its task in `deinit`; only idle or finished holders are ever dropped, so no in-flight request is cancelled.
- [Row does not redraw when the holder changes] → The row observes the holder directly with `@ObservedObject`; task 3.4 verifies a state change redraws the row.
- [`SwipeToDismissBox` threshold change affects other callers] → New parameter with the existing fraction as default; grep callers in task 1.3.
- [Haptic and announcement fire on every recomposition] → Trigger from `onChange(of: state)` edge to `.failure`, not from `body`.
- [ViewInspector crashes on local runtimes] → Use the iOS 18.1 simulator noted for UI-package tests; CI runs 26.6.

## Migration Plan

Client-only; ships with the next iOS release. Rollback is reverting the commit.
