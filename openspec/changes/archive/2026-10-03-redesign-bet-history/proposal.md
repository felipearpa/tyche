# Proposal

## Why

Fortuna's History screen places final scores and predictions in adjacent columns, making their meaning hard to scan, and offers no prominent total of points earned. The supplied reference establishes a clearer hierarchy: total earned points, large match results, and a separately labeled prediction and points award.

## What Changes

- Redesign the signed-in gambler's History tab on iOS and Android using the [supplied reference](reference/history-reference.png): a History title in the same style as the other pool-home tabs on each platform, an earned-points summary, and flat match rows with dividers.
- Show the authoritative total earned in the current pool beneath the title. Load it through the existing pool-score API and refresh it alongside History; never calculate it from the currently loaded pages.
- Replace History's sticky date groups with a date/time line inside every match row. Place flags and team names around a prominent final score, then show the labeled prediction and a trailing points pill.
- Distinguish positive points, actual zero, and unavailable data. Localize new labels and date formats and support accessible text sizes and screen readers.
- Preserve match navigation, list ordering, refresh, pagination, retry, empty/error states, and production-component loading placeholders.
- Keep the redesign within History. Preserve other gamblers' timelines, the Scores and Bets tabs, existing toolbar actions and icons, native iOS navigation, and Android's current tab shell. Use the established light/dark canvases and safe-area behavior.

## Capabilities

### New Capabilities

- `bet-history`: Cross-platform History presentation, authoritative pool-points summary, match result/prediction hierarchy, state handling, and accessibility.

### Modified Capabilities

None. This change consumes the existing `loading-placeholders`, `ios-liquid-glass`, `android-edge-to-edge`, and navigation contracts without changing their requirements.

## Impact

- iOS: History list/view model and presentation in `iOS/Bet`, pool-home composition and dependency injection in `iOS/Tyche`, and English/Spanish bet resources. Reuse `DataPool.GetPoolGamblerScoreUseCase` and existing flags and theme values.
- Android: History list/view model and presentation in `Android/bet`, pool-home composition in `Android/app`, dependency injection, and English/Spanish bet resources. Reuse `GetPoolGamblerScore`, existing flags, theme values, and paging infrastructure.
- Add or adapt History-specific row and summary presentation without changing the shared finished-row behavior used by another gambler's timeline.
- No backend, persistence, scoring-rule, API-contract, or new external-library changes are expected. The reference's numbers are illustrative; real results and awards remain server-provided.
