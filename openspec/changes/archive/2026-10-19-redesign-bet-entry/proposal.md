# Proposal

## Why

Placing a prediction in Fortuna's Bets tab takes four to five taps per match (Edit, home score, away score, Save, plus a focus tap on Android) through a modal edit mode with Edit/Save/Cancel buttons, dashes-and-footnotes rows, and no way to see at a glance which matches still need a bet. The tab is the most frequently used screen before each matchday, and it is the only redesigned-era screen still using the older row layout, so it should be as fast and self-explanatory as the redesigned History, Scores, and Manage gamblers screens.

## What Changes

- Redesign the iOS and Android Bets tab (pending bets) around the supplied design references: [bets list](reference/bets-list.png) and [row states](reference/bet-row-states.png). [Reference notes](reference/README.md) distinguish illustrative data from the behavior specified here.
- Replace the per-row Edit → fields → Save/Cancel edit mode with **direct score entry**: each team's score is a tappable score box that opens the numeric keyboard, plus `+`/`−` steppers around it. Entering the home score moves focus to the away score automatically.
- **Save automatically** once both scores are set, after a short debounce, without an explicit Save button. The row shows the shared ball spinner while saving, a Saved pill on success, and an inline Retry banner on failure that keeps the draft. Cancel is removed; a changed value is simply typed over.
- Make empty bets unmistakable: rows without a bet show dashed `?` boxes and a No bet pill; a row with only one side entered shows an amber **Incomplete** pill, an amber dashed box on the missing side, and a hint naming the missing team. Nothing is saved until both sides exist; the missing side is never auto-filled with 0.
- Group matches under date headers inside card rows with country flags, kickoff time, and a status pill (No bet / Incomplete / Saved / Locked), with an amber "Locks in N min" note during the last hour before the ten-minute pre-kickoff lock. Locked rows show the saved prediction read-only with no controls.
- Add a **filter row** (All / No bet / Placed) with live counts so a gambler can jump to the matches still missing a prediction.
- Keep the existing feed, paging, pull-to-refresh, lock policy, forbidden-bet handling, draft retention across paging updates, placeholder rows, and keyboard inset behavior. The native tab bar stays as the platform renders it (iOS Liquid Glass tab bar, Android primary tab row); the reference's floating bar is the OS chrome, not app UI.

## Capabilities

### New Capabilities

- `bet-entry`: the Bets tab's card layout, direct score entry (tap-to-type, steppers), automatic saving with spinner/saved/failure states, empty and incomplete bet presentation, filter row, lock-approaching note, locked read-only rows, and accessible localized behavior on iOS and Android.

### Modified Capabilities

- `ios-liquid-glass`: "Content-layer controls use standard materials" names prediction-row edit, save, and cancel controls and an edit mode; the requirement and its prediction scenarios are restated for direct entry (score boxes, steppers, retry) with no edit mode.
- `android-edge-to-edge`: the keyboard scenario for pending bets refers to "pending-bet action controls"; it is restated so the focused score box, its steppers, and the Retry banner stay reachable above the keyboard and pool tabs.

## Impact

- iOS: `Bet/Sources/Bet/Pending/*` (`PendingBetList`, `PendingBetItemView`, `PendingBetItem`, `BetTextField`, `PendingBetItemViewModel`, view state), shared `UI` components (`BallSpinner`, button/text-field styles, flag image), `Localizable.xcstrings` in `Bet` and `UI`, Bet package unit and ViewInspector tests.
- Android: `bet/.../pending/*` (`PendingBetList`, `PendingBetItemView`, `PendingBetItem`, `BetTextField`, `PendingBetItemViewModel`, view state), shared `ui` theme/components, `strings.xml` (en/es/es-ES), unit tests and the `PoolHomePendingBetKeyboardTest` instrumented test.
- Backend and API: none. The pending feed, `PlaceBet`/`BetUseCase`, lock policy, and scoring are unchanged.
- Planning assets: two design-board images exported from the design canvas and a reference README.
