# Bet

The bet screens of a pool: pending and live bets, the match screen, the signed-in gambler's History, and another gambler's Timeline.

## Grouped History and Timeline

History (`finished/`) and Timeline (`timeline/`) show the same grouped feed. Each item is one kickoff group: the matches of the pool that share a scheduled kickoff instant. The shared pieces live in `kickoffgroup/`.

### Feed contract

- `GET pools/{poolId}/gamblers/{gamblerId}/bets/finished` serves History, and `GET pools/{poolId}/gamblers/{gamblerId}/bets/timeline` serves Timeline. Each takes an optional `next` cursor and no search filter. `:data:bet` decodes each item as a `KickoffGroup`:
  - `kickoffDateTime`: the UTC kickoff instant and the group's identity, kept as a `kotlin.time.Instant`. Groups are never told apart by a formatted or local date-time, so two instants that read alike across a DST change stay two groups.
  - `totalMatches` and `computedMatches`: counts from the pool's layout, never from the rows the client has loaded.
  - `movement`: `{ beforePosition, position }` from the group's stored snapshots, or `null`.
  - `matches`: the visible match rows, ordered by match identifier.
- The server sends whole groups in their final order and never splits a group across pages. Timeline sends unsettled groups first, then finished ones.
- A rejected continuation (`400 INVALID_CURSOR` or `CURSOR_CONTEXT_MISMATCH`) maps to `KickoffGroupFeedException.CursorRejected`. Other failures keep the usual network exceptions.

### Paging (`kickoffgroup/KickoffGroupFeed.kt`)

- Pages are appended as they arrive. The client merges, sorts, recounts, and derives nothing.
- Each paging source is one generation, started by a first-page load and discarded by the next refresh. Paging ignores the responses of an invalidated source, so a stale page never merges into the newer list.
- A kickoff is shown once per generation, and the lazy-list key is the kickoff instant (`KickoffGroupModel.key`). A group that comes back on a later page is dropped, as when a Timeline group settles between pages; the next refresh shows its settled copy. A page left empty by this does not stall the list: Paging requests the next page at once while the loaded items stay within the prefetch distance.
- The first rejected continuation restarts from the first page automatically (`LoadResult.Invalid`), keeping the loaded groups visible until the new first page arrives. A second rejection in a row shows "The list changed" with a Retry that restarts from the first page. A successful append, a pull to refresh, or a list Retry allows one more automatic restart.
- Pages have no `prevKey`, so prepend is always complete, and Paging's `retry()` repeats the failed append even when only one group is loaded.

### Movement

`KickoffGroupModel.movement` is the shared `core` `RankMovement` (`RankMovement.ofKickoffGroup`):

| Feed data | State |
| --- | --- |
| `movement` present | `beforePosition - position` places; zero is steady. A group with `computedMatches < totalMatches` adds the `1 of 2 computed` qualifier |
| `movement` null, no computed match among the rows | pending |
| `movement` null, some computed match | unavailable, never zero |

### Presentation

- `KickoffGroupSection` shows a scrolling, non-sticky `KickoffGroupHeader`, then the group's `HistoryBetItem` rows. A subdued vertical rail, drawn behind the rows, joins them, and thin dividers separate them.
  - The header shows the localized date, then the time with the pluralized match count (omitted for one match), and a trailing `TrendIndicator` badge. A partial group's qualifier sits under the badge.
  - The header is a `FlowRow`: when a long date or large text leaves no room, the badge moves below the date and time instead of clipping either.
  - A row whose team name cannot fit its column, at large text or narrow widths, stacks the teams around the result.
- The header is announced once, as a heading, with the date and time, the count, and the movement, for example "Saturday, Jun 27, 9:00 PM. 2 matches. Rank unchanged". It is not an action. Each row keeps its own match action, press feedback, and announcement, and opens the match screen. A row reached directly keeps its own date and time but not the group's count or movement.
- History rows say `Your bet`, and Timeline rows say `Bet` (`HistoryOwner`).
- Loading slots render the production section from `kickoffGroupPlaceholderModel()` with `isPlaceholder = true`. They open nothing and expose no text, description, or click action to TalkBack.

### Points summary and recovery

The earned-points total above the groups comes from the pool score endpoint (`GET pools/{poolId}/gamblers/{gamblerId}`), not from the loaded rows. `HistoryPointsSummaryLoader` loads it independently of the groups:

- The summary's Retry requests only the total.
- The list's Retry, on a failed first page, requests only the groups.
- Pull to refresh reloads both together and ends when the slower one finishes. Each result appears as soon as it arrives, and one failing keeps the other.

## Checks

From `Android/`:

```bash
# Unit tests, as CI runs them
./gradlew testDebugUnitTest :app:testProdDebugUnitTest -Dorg.gradle.jvmargs="-Xmx4g -Dfile.encoding=UTF-8"

# Instrumented tests for this module, one class at a time, on an emulator (set ANDROID_SERIAL)
./gradlew :bet:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.felipearpa.tyche.bet.finished.HistoryPresentationTest
```

Instrumented classes for the grouped screens: `finished.HistoryGroupedListTest`, `finished.HistoryPresentationTest`, `finished.HistoryListRetryTest`, `finished.HistoryPointsRetryTest`, `timeline.BetTimelineListTest`, and `BetPlaceholderAccessibilityTest`. Unit tests: `kickoffgroup.KickoffGroupFeedTest`, `timeline.BetTimelineListViewModelTest`, `finished.FinishedBetListViewModelPointsSummaryTest`, and `:data:bet`'s `KickoffGroupFeedRequestTest`.
