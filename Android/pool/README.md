# Pool

The pool screens: My pools, a pool's leaderboard, pool creation and joining, and gambler management.

## Trend-group movement on the leaderboard and My pools

The leaderboard (`gamblerscore/`) and My pools (`poolscore/`) show each row's rank movement across the pool's trend group: the newest kickoff group with a settled match.

### Score contract

- `GET pools/{poolId}/gamblers` (the leaderboard) and `GET gamblers/{gamblerId}/pools` (My pools) return `PoolGamblerScore` rows, decoded in `:data:pool`. Each row has `position`, `beforePosition`, and an optional `trendGroup`.
- `trendGroup` is `{ kickoffDateTime, computedMatches, totalMatches }`. It is absent until a match of the pool settles; an explicit JSON `null` decodes the same way. Both endpoints send the same values for the same pool.
- `beforePosition` is the row's position before the trend group's first settlement. It resets only when a settlement opens a newer kickoff group. Later settlements of the same group, late results, and corrections keep it.
- `PoolGamblerScoreModel.trendGroup` keeps the counts as a `core` `KickoffGroupProgress`. The kickoff instant itself is not needed on these screens.

### Movement

`PoolGamblerScoreModel.movement()` is the shared `core` `RankMovement` (`RankMovement.ofTrendGroup`):

| `trendGroup` | `beforePosition` and `position` | State |
| --- | --- | --- |
| absent | any | pending |
| present | either missing | unavailable, never zero |
| present | both present, `computedMatches < totalMatches` | `beforePosition - position` places with the `1 of 2 computed` qualifier |
| present | both present, counts equal | `beforePosition - position` places; zero is steady |

A row without `position` keeps the em dash in its rank badge.

### Presentation

- `GamblerScoreItem` and `PoolScoreItem` render the movement with `:ui`'s `TrendIndicator` in the rank rail. Pending and unavailable show their labels, never the steady symbol. The rail is at least as wide as the label's widest word, so a label never breaks mid-word.
- Each row is announced once, with the movement last: rank, username, score, then movement on the leaderboard ("Rank 1, El mono, 676 points, Rank unchanged"); pool name, rank, score, member count, then movement on My pools ("Prueba, Rank 3, 4 points, 3 members, Rank unchanged"). A partial group's qualifier follows the direction, for example "Up 3 places, 2 of 4 matches computed". The indicator adds no focus stop of its own.
- The signed-in gambler's leaderboard row is not an action. Every other row opens that gambler's Timeline.
- At font scale 1.5 or more both rows reflow instead of clipping. The leaderboard puts the rank rail, avatar, and score on the first line (the score moves below when even that does not fit) and wraps the username across the full width. My pools puts the rank rail and invite control on the first line and the pool name and counts below at full width. At ordinary scales the username stays on one line with an ellipsis.
- Loading rows render the production rows with `isPlaceholder = true`. They expose no values, ignore touches, and stay out of the accessibility tree; leaderboard placeholders request no avatar.
- Pull to refresh reloads the list from its first page. The leaderboard's append Retry repeats the failed append.

## Checks

From `Android/`:

```bash
# Unit tests, as CI runs them
./gradlew testDebugUnitTest :app:testProdDebugUnitTest -Dorg.gradle.jvmargs="-Xmx4g -Dfile.encoding=UTF-8"

# Instrumented tests for this module, one class at a time, on an emulator (set ANDROID_SERIAL)
./gradlew :pool:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.felipearpa.tyche.pool.poolscore.PoolScoreItemAccessibilityTest
```

Instrumented classes for these screens: `poolscore.PoolScoreItemAccessibilityTest`, `poolscore.PoolScoreListAccessibilityTest`, `poolscore.PoolScoreListInsetsTest`, `gamblerscore.GamblerScoreListTest`, `gamblerscore.GamblerScorePlaceholderTest`, and `gamblerscore.GamblerScoreItemRankPreviewTest`. Unit tests: `PoolGamblerScoreModelTest`, `:data:pool`'s `PoolGamblerScoreResponseTest`, and `core`'s `RankMovementTest`.
