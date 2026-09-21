## Why

The pool leaderboard was given a full accessibility contract in the leaderboard redesign — each row is one logical announcement covering rank, identity, score, and movement, trend is never color-only, and loading placeholders announce nothing. The pool score list (the "My pools" board, the first screen a signed-in gambler lands on) never received that pass, and it is measurably worse than the leaderboard it feeds into. On iOS the row has no accessibility element at all, so it shatters into five or six separate VoiceOver stops. On Android the row is merged only incidentally, by `Modifier.clickable`, so TalkBack reads a raw concatenation of visual fragments — "4, 1, Liga de Campeones, 8 pts., 101 members" — where the bare "4" is the rank tile and the bare "1" is the trend indicator, with no word for either. On both platforms the invite control is an unlabeled icon button, and the shimmer placeholders read their filler content aloud. A screen-reader user cannot reliably tell which pool a row is, what their standing in it is, or how to open it.

## What Changes

- Each pool score row becomes a single accessibility element that announces, in one logical order: pool name, rank, points, member count, and rank movement — the leaderboard's announcement shape, extended with the pool identity and member count the leaderboard row does not carry.
- The row is exposed with a button role and an explicit activation that opens the pool, replacing the untyped tap gesture iOS exposes today and making Android's incidental `clickable` merge deliberate.
- The invite/join control gets a localized accessibility label and is exposed as a distinct action on the row, so it is reachable and nameable rather than announcing as an unlabeled image button.
- Loading placeholder rows are hidden from the accessibility tree, so the `XXXXXXXX` / `X`-repeat filler in `poolGamblerScorePlaceholderModel()` is never spoken — matching the leaderboard placeholder behavior.
- Rows with missing rank or missing score announce an explicit "unavailable" phrase instead of silently dropping the value, reusing the leaderboard's existing `leaderboard_rank_missing_accessibility` / `leaderboard_score_missing_accessibility` strings.
- The reused leaderboard accessibility strings are reconciled so both platforms announce the same words. iOS moves on six of the seven keys, Android on one, and iOS's two `..._missing_...` keys are renamed to Android's spelling so one logical key has one name. See design.md — D5a for the table and the reasoning.
- New localized accessibility strings are added for the pool-specific actions (invite, open pool) in all three existing locales: English, Spanish (`es`, Colombian "polla" register) and Spanish–Spain (`es-ES`, "quiniela" register). The member count reuses the existing `gamblers_text`, which is already spelled out.
- Counted nouns are given plural forms on both platforms, so `n = 1` stops reading "Up 1 places", "1 points" and "1 members". This covers the pool list, the leaderboard row it shares strings with, and the pool-home drawer's score suffix. See design.md — D5b.
- Visible layout, colors, spacing, paging, refresh, and navigation behavior are unchanged. **Two visible strings do change**, both only at `n = 1`: the member count on the pool row ("1 member", not "1 members") and the drawer's score suffix ("1 point", not "1 points"). Everything else is accessibility semantics only.

## Capabilities

### New Capabilities
- `pool-score-list`: The "My pools" list — its row anatomy as it exists today, and its screen-reader semantics: single-element row announcement, exposed open and invite actions, placeholder suppression, missing-value phrasing, dynamic type / font scaling, and localization across the three supported locales.

### Modified Capabilities

- `pool-leaderboard`: the plural-agreement requirement. D5b makes counted nouns in the shared announcement agree with their counts, and the leaderboard row reuses every one of those strings — so the invariant binds that capability too, not only the new one. The delta adds it there; the wording reconciliation (D5a) needs no delta, because `pool-leaderboard` already demands "equivalent VoiceOver/TalkBack meaning for rank, identity, score, and movement" without prescribing words, and aligning the tables makes both platforms conform to a requirement that already exists.

For the record, this change does edit shipped leaderboard copy: six iOS entries and one Android entry changed value, two iOS keys were renamed, and four keys on each platform became plural.

## Impact

- **iOS**: `iOS/Pool/Sources/Pool/PoolScore/PoolScoreItem.swift`, `iOS/Pool/Sources/Pool/PoolScore/PoolScoreList.swift`, `iOS/Pool/Sources/Pool/Localizable/Localizable.xcstrings` (new keys must be added with `extractionState: manual` or the generated symbols will be skipped). D5a and D5b also touch `iOS/Pool/Sources/Pool/GamblerScore/GamblerScoreItem.swift` (renamed key literals, then every call site moved onto generated symbols) and delete `LocalizedFormat.swift`. The plural work reaches the app target too: `iOS/Tyche/Tyche/Localizable/Localizable.xcstrings` (`suffix_point_text`; its call site needs no edit, the generated symbol already carries the count). New tests under `iOS/Pool/Tests/PoolTests/`, and `GamblerScoreItemRankPreviewTests` is re-baselined.
- **Android**: `Android/pool/src/main/java/com/felipearpa/tyche/pool/poolscore/PoolScoreItem.kt`, `.../poolscore/PoolScoreList.kt`, `.../gamblerscore/GamblerScoreItem.kt`, and all three `Android/pool/src/main/res/values*/strings.xml`. The D5a reconciliation changes one Spanish value in each `es` table. The D5b plural work additionally converts four keys from `<string>` to `<plurals>` in all three tables — **changing the English output at `n = 1`** — and moves their call sites to `pluralStringResource`. It also reaches the app module: `Android/app/src/main/res/values*/strings.xml` (`suffix_point_text`) and `.../poolhome/drawer/DrawerView.kt`. Two shipped leaderboard tests are re-baselined (`GamblerScoreItemRankPreviewTest`, `GamblerScoreListTest`). New instrumented tests under `Android/pool/src/androidTest/`.
- **Backend**: none. No API, model, or persistence change — `PoolGamblerScoreModel` already carries every value the announcement needs.
- **Shared UI**: none required. `TrendIndicator` already pairs each trend color with an arrow/rule glyph on both platforms, so the "not by color alone" rule is already satisfied and its components are not touched.
