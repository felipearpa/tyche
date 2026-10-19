# Design

See `proposal.md` for motivation, `reference/README.md` for the boards, and `specs/bet-entry/spec.md` for the behavior. This document covers how both apps get there.

## Context

- **iOS** (`iOS/Bet/Sources/Bet/Pending/`): `PendingBetList` pages `PoolGamblerBetModel` rows on the lazy-paging container with sticky date headers. `PendingBetItemView` wraps `StatefulPendingBetItemView`, which switches on `PendingBetItemViewState` (`.visualization` / `.edition`) and `MutationState` to pick one of five action bars (Default, Editable, NonEditable, Loading, Failure). `PendingBetItemViewModel` is a per-row `@StateObject` holding `PartialPoolGamblerBetModel` (two string drafts plus `isValid`) and `bet(TeamScore)`. `BetTextField` is a `.numberPad` capsule field (`StandardTextFieldStyle`) with auto-focus via `@FocusState`. The draft already survives paging re-binds.
- **Android** (`Android/bet/.../pending/`): the same split — `PendingBetList`, `PendingBetItemView`, `PendingBetItem`, `BetTextField` (`OutlinedTextField`, `KeyboardType.Number`, wrapped in `KeepVisibleAboveKeyboard`), a per-row `PendingBetItemViewModel`, and `PartialPoolGamblerBetModel`. There is no auto-focus today.
- Both call the existing place-bet use case (`BetUseCase` / `PlaceBet`); a `forbidden` error locks the row locally. The server lock is ten minutes before kickoff (`LockPolicy.fs`). The model has `matchDateTime`, `isLocked`, `isComputed`, `betScore: TeamScore<Int>?`, team IDs that double as flag codes, and no group or stage.
- Shared pieces already exist on both platforms: `BallSpinner` (two-second linear turn, Reduce Motion aware), `FlagImage`, the pulse placeholder treatment with `isPlaceholder`, `BoxSpacing`, and the Material Symbol drawables (`done`, `lock`, `error`, `pending`) with iOS image sets derived from the Android drawables.
- Colors: iOS pure black system background and `secondarySystemBackground` cards; Android `#121212` surface and `surfaceVariant` cards; accent green `#4CAF50` (dark) / `#2E7D32` (light); secondary amber `#FFC107`; error `#CF6679` / `#D32F2F`.
- Existing specs that touch this screen: `ios-liquid-glass` (standard materials for prediction inputs), `android-edge-to-edge` (keyboard insets), `loading-placeholders`, `backend-pagination`. Their deltas in this change restate the prediction scenarios; the rest is unchanged.

## Goals / Non-Goals

**Goals:**
- One row state model per platform that drives both the card and the status pill, with no edit mode.
- Automatic, debounced, coalescing saves that never lose the gambler's latest intent.
- Reuse the paging container, per-row view model, `BallSpinner`, `FlagImage`, placeholder treatment, and use cases; change presentation and the row state machine, not infrastructure.

**Non-Goals:**
- Any backend or API change; a "save all" or batch endpoint; a bulk "quick bet" flow (dropped during design).
- Popular-bet or opponent-aware quick picks (bets are hidden before kickoff).
- Showing group/stage/venue (not in the model).
- Changing the History, Scores, or match screens.

## Decisions

### 1. Row state is a draft plus a mutation state; no `.visualization`/`.edition`

Each row holder keeps `draft: (home: Int?, away: Int?)` and one save state, `MutationState<SavedBet>`, where `SavedBet` is `none` / `placed(TeamScore<Int>)` (a Swift enum on iOS, a sealed interface on Android) so "no bet saved" is explicit and the same on both platforms; the saved prediction is the state's active value. The card derives one of: `locked`, `empty` (both nil, nothing saved), `incomplete` (exactly one nil), `saving`, `saved`, `failed`. The `PendingBetItemViewState` enum and the five action bars go away; `PartialPoolGamblerBetModel`'s string drafts become optional ints because digits-only parsing happens at the input boundary.

Why: the reference has no mode switch, so a mode enum would only exist to be always `.edition`. Deriving the visual state from the draft keeps "Incomplete" and "No bet" impossible to desynchronize from what the boxes show.

Alternative: keep `.edition` as the only state — leaves dead branches and a misleading name.

### 2. Debounced, coalescing auto-save in the row view model

`setHome`/`setAway`/`setBoth` update the draft and, when both sides are set and differ from `saved`, schedule a save 700 ms later (`Task.sleep` on iOS, `delay` in a `viewModelScope` job on Android), cancelling any pending schedule. If a save is in flight when the timer fires, the view model marks `pendingResave` and submits the newest draft when the in-flight request returns. On success `saved` becomes the submitted value and the state returns to `saved` unless the draft moved on. On failure the draft stays, state is `failed`, and `retry()` resubmits the current draft.

Why 700 ms: long enough to swallow a burst of stepper taps and the home→away keystroke pair, short enough that the spinner appears before the gambler scrolls on. Why not save on blur only: steppers and chips have no blur; one rule for all three inputs is simpler to reason about and test.

Alternative: optimistic "saved" with background retry queue — hides failures the gambler must know about before the lock.

### 3. Score box is a real text field; steppers write the draft directly

The score box replaces `BetTextField`'s capsule with a 52 pt rounded square (dashed outline when nil, filled when set, amber dashed when the other side is set, green border when focused). On focus the current value is selected so typing replaces it. Input filter: digits only, max two digits, leading zeros stripped (today's three-digit cap drops to two; no real match needs more). After the first digit lands in the home box, focus moves to the away box (`@FocusState` on iOS; `FocusRequester` on Android, which also fixes the missing auto-focus today). Steppers call `setHome(draft.home ± 1)` with the empty-card rules from the spec. Quick-pick chips (1–0, 2–1, …) were built and then removed on 2026-10-19: with the Incomplete hint they were the content that made cards grow and shrink, which caused neighbour and end-of-list motion that could not be made consistent across platforms. Cards now keep one height while open; the keyboard and steppers cover entry. The failure banner is the one deliberate exception besides locking: a Retry inside the status pill was mocked up and rejected, because an unsaved bet must be impossible to miss.

Why not a hidden keyboard behind the steppers only: the user asked for keyboard entry, and a visible box is the discoverable affordance for it.

### 4. Filters are a client-side view over loaded pages

`PendingBetListViewModel` gains `filter: All | NoBet | Placed` and exposes counts computed from the loaded rows' current state (the row view models report their `saved`/draft state up through a small callback, as Manage gamblers does with `GamblerRowOutcomes` on iOS; on Android the list view model creates and keeps plain `PendingBetRowHolder` objects (not Jetpack `ViewModel`s) on its own `viewModelScope` and reads them directly. Both platforms name the per-row type `PendingBetRowHolder`, kept in `rowHolders` and read through `rowHolder(for:)`; its save status is `MutationState<SavedBet>` (a `StateFlow` on Android), and the lock lives on the match. Android Manage gamblers' Compose-created row view models plus a registry were not reused, because the filter counts need every loaded row, including rows a filter keeps out of composition). Filtering hides cards without unmounting their view models, so drafts and in-flight saves survive. Hidden cards render nothing; a small trigger after the last loaded slot reports access so the pager keeps loading under a filter (rendering thousands of zero-height slots froze iOS for about 13 s on a 3,000-match pool). Date headers follow the visible cards. The empty-filter message reuses the existing empty-list styling.

Why client-side: the feed is one cursor-paged query sorted by kickoff; adding server filters would be a backend change for a presentational convenience, and the counts would still be partial until every page loads. The spec states counts cover loaded matches.

### 5. "Locks in N min" from kickoff minus ten minutes, ticking by the minute

Lock time is `matchDateTime − 10 min`. A card shows the note when `0 < lockTime − now ≤ 60 min`. iOS uses `TimelineView(.periodic(from:by: 60))` around the note; Android uses a minute ticker `Flow` collected by the list. This is a UI clock, not a poller: nothing fetches, and the server still decides the lock. When the note reaches zero the card stays open until the next refresh or a forbidden save flips it to locked, as today.

Day headers use the same clock: a header's label is computed from `now` and the calendar/time zone (Today, Tomorrow, Yesterday, weekday within six days, else a short named date with the year only when it differs), so the minute tick also flips "Tomorrow" to "Today" at midnight. Labels come from `Date.FormatStyle` on iOS and the locale formatters `HistoryMatchDateFormat` uses on Android, with only Today/Tomorrow/Yesterday as new strings where the platform has no relative-day name.

### 6. Visual tokens

Card: `secondarySystemBackground` (iOS) / `surfaceVariant` (Android), 16 pt radius, 14/16 pt padding. Score digits 34 pt bold tabular; flags 44 pt; steppers 52×44 pt with 12 pt radius on a softer tint than the score boxes (final values chosen on real screens: Android onSurface at 7 % light / 9 % dark, giving about 1.14:1 / 1.25:1 against the card; iOS rgb(118,118,128) at 8 % light / 19.5 % dark, giving about 1.09:1 / 1.25:1, because iOS's score boxes are lighter in light mode and the steppers must stay softer than them on both platforms), visibly softer than the score boxes with glyphs in the secondary text colour, so the score boxes stay the visual focus and the glyphs keep at least 4.5:1 on the tint in both themes (decided 2026-10-19 from the "Current layout — lighter steppers" board; the outlined variant was rejected because it made the steppers look like the empty dashed score boxes); pills 26 pt tall capsules: Saved = primary at 16 % with primary text, Incomplete and the lock note = secondary (amber) at 14 %, Not saved = error at 14 %, Locked and Saving backgrounds = `tertiarySystemFill`. Filter chips are 36 pt outlined capsules; the selected filter uses the app's existing selection pair, `primaryContainer` fill with `onPrimaryContainer` text (as the pool creator's selected item), on both platforms and in both themes; unselected chips keep the outline. Light mode on Android follows the same roles with the light palette. The "Locks soon" card gets a 1 pt amber inset outline.

### 7. Icons and spinner

`add` and `remove` Material Symbols drawables are added on Android and derived into iOS image sets from the drawables' `pathData` exactly as the Manage gamblers change did; `done`, `lock`, `error` already exist on both. The saving indicator is `BallSpinner` at 20 pt in the status pill's place.

### 8. Strings

New keys (en / es / es-ES, iOS `extractionState: manual`): `bets_filter_all`, `bets_filter_no_bet`, `bets_filter_placed`, `bet_status_no_bet`, `bet_status_incomplete`, `bet_status_saved`, `bet_status_not_saved`, `bet_status_locked`, `bet_saving_accessibility_value`, `bet_locks_in_minutes` (plural), `bet_save_failure_text`, `bets_filter_empty_message` (No bet) and `bets_filter_placed_empty_message` (Placed), the stepper/box accessibility labels, and the missing-box label naming the team whose score is needed (replacing the removed `bet_incomplete_hint` line and the quick-pick chip labels). The failure banner's Retry reuses the existing shared Retry key (Android shared `retry_action`, iOS `SharedStringResource.retryAction`), as Manage gamblers does. Removed: the shared `editAction`/`saveAction` (iOS) and `edit_action`/`save_action` (Android) keys once the bet row no longer uses them; `cancelAction` stays because other screens use it. Spanish uses "apuesta" throughout; es-ES keeps its existing variants.

### 9. Transitions and layout stability

The status slot is a fixed 26 pt-tall, right-aligned container that hosts either the pill or the 20 pt `BallSpinner`, so swapping them never changes the header row's height. The status is one trailing-anchored pill: text only for No bet and Incomplete, `done`/`error`/`lock` icon plus text for Saved/Not saved/Locked, and the 20 pt `BallSpinner` for Saving. Each state change is one transaction on the shared curve: the content crossfades while the pill's width animates from the old size to the new one with the trailing edge fixed (`.transition(.opacity)` plus an animated frame on iOS; `AnimatedContent` crossfade inside `animateContentSize(alignment = Alignment.End)` on Android). This removes the size jump between a text pill and the bare ball with one motion and no sequencing. A collapse-into-the-ball-then-expand variant was rejected: two width movements per save on a screen edited constantly, a visible shrink-grow flicker when a save returns just before a queued resave, and extra icons on every open card. Reduce Motion: crossfade only, width changes instantly. Score boxes and stepper columns have fixed sizes in every state; only their fill, border, and text color animate (`.animation(.easeInOut(duration: 0.15), value: state)` / `animateColorAsState`). An open card keeps one height in every state; only the failure banner and the lock transition change it, and they are inserted and removed with a height animation so neighbouring cards slide instead of jumping (`.transition(.move(edge: .top).combined(with: .opacity))` on a card that animates its frame; `AnimatedVisibility(expandVertically + fadeIn, shrinkVertically + fadeOut)` plus `Modifier.animateContentSize()` on the card on Android). Lists animate item removal when a filter hides a card (`animateItem()` on Android; SwiftUI's default insert/remove transition on iOS). Reduce Motion: iOS reads `accessibilityReduceMotion` and drops movement transitions to opacity only; Android follows the system animator scale, which Compose honours, and additionally skips `expandVertically`/`shrinkVertically` when `LocalAccessibilityManager` reports reduced motion is unavailable — fall back to fade. Fixed sizes are independent of the setting.

Why fixed-size slots over measuring: the spinner and pills differ by a few points and a measured slot would still resize when the pill text changes language; one reserved height is the only approach that cannot jump.

### 10. Tests

- iOS (Swift Testing): row view model — typing home then away schedules one save; three stepper taps in 300 ms produce one submission; edit during flight resubmits once; incomplete never submits; failure keeps draft and retry resubmits; forbidden locks. List view model — counts and filter membership react to row state. ViewInspector tests for the card's state → pill/controls mapping run on the iOS 18.1 simulator.
- Android (JUnit 5 + Kotest, `runTest` with virtual time): the same row and list cases. `PoolHomePendingBetKeyboardTest` is updated for the score box, auto-advance, and card visibility above the keyboard.

## Risks / Trade-offs

- [Auto-advance after one digit blocks two-digit scores by typing] → The gambler taps the box again (value is selected) or uses `+`; two-digit scores are rare and the stepper covers them.
- [A save fires while the gambler is still adjusting] → Debounce resets on every change; an in-flight save is followed by one resave of the newest draft, so the final state always matches the last input.
- [Filtering hides a card mid-save] → State lives in the row view model, which stays alive while filtered out; the Placed count updates when the save lands.
- [Paging rebind resets drafts] → Row view models stay keyed by `matchId` as today; task-level test covers draft retention across a page append.
- [Minute ticker runs while the tab is hidden] → Scoped to the list's composition/view lifetime; it stops with the screen.
- [Two-digit cap vs today's three] → No competition in scope reaches 100 goals; cap documented in the spec.
- [ViewInspector crashes on local runtimes] → Use the iOS 18.1 throwaway simulator; CI runs 26.6.

## Migration Plan

Client-only; ships in the next iOS and Android releases independently. Rollback is reverting the commit. No data migration: drafts are in-memory only.
