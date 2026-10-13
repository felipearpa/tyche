# Bet

The bet screens of a pool: pending and live bets, the match screen, the signed-in gambler's History, and another gambler's Timeline.

## Grouped History and Timeline

History (`Finished/`) and Timeline (`Timeline/`) show the same grouped feed. Each item is one kickoff group: the matches of the pool that share a scheduled kickoff instant.

### Feed contract

- `GET pools/{poolId}/gamblers/{gamblerId}/bets/finished` serves History, and `GET pools/{poolId}/gamblers/{gamblerId}/bets/timeline` serves Timeline. Each takes an optional `next` cursor and no search filter. `DataBet` decodes each item as a `KickoffGroup`:
  - `kickoffDateTime`: the UTC kickoff instant and the group's identity.
  - `totalMatches` and `computedMatches`: counts from the pool's layout, never from the rows the client has loaded.
  - `movement`: `{ beforePosition, position }` from the group's stored snapshots, or `null`.
  - `matches`: the visible match rows, ordered by match identifier.
- The server sends whole groups in their final order and never splits a group across pages. Timeline sends unsettled groups first, then finished ones.
- A rejected continuation (`400 INVALID_CURSOR` or `CURSOR_CONTEXT_MISMATCH`) maps to `KickoffGroupFeedError.cursorRejected`.

### Paging (`KickoffGroup/KickoffGroupFeed.swift`)

- Pages are appended as they arrive. The client merges, sorts, recounts, and derives nothing.
- Every first-page request starts a new generation. A response that arrives after a newer generation started is dropped, so a stale page never merges into the newer list.
- A kickoff is shown once per generation. A group that comes back on a later page is dropped, as when a Timeline group settles between pages; the next refresh shows its settled copy. A page left empty by this is followed by the next page at once, up to three times.
- The first rejected continuation restarts from the first page automatically, keeping the loaded groups visible until the new first page arrives. A second rejection in a row shows "The list changed" with a Retry that restarts from the first page.
- Lists render through `UI`'s `AppendPagingVStack`, so an append Retry repeats the failed append even when only one group is loaded.

### Movement

`KickoffGroupModel.movement` is a `Core.RankMovement`:

| Feed data | State |
| --- | --- |
| `movement` present | `beforePosition - position` places; zero is steady. A group with `computedMatches < totalMatches` adds the `1 of 2 computed` qualifier |
| `movement` null, no computed match among the rows | pending |
| `movement` null, some computed match | unavailable, never zero |

### Presentation

- `KickoffGroupSection` shows a scrolling, non-sticky `KickoffGroupHeader`, then the group's rows. A subdued vertical rail joins the rows, and thin dividers separate them.
  - The header shows the localized date, then the time with the pluralized match count (omitted for one match), and a trailing `TrendIndicator` badge.
  - At accessibility text sizes the badge moves below the date and time.
- The header is announced once, as a heading, with the date, time, count, and movement. It is not an action. Each row keeps its own match action, press feedback, and announcement. A row reached directly keeps its own date and time but not the group's count or movement.
- History rows say `Your bet`, and Timeline rows say `Bet` (`HistoryOwner`).
- Loading slots render the production section from `kickoffGroupPlaceholderModel()` with `isPlaceholder: true`. They open nothing and stay out of the accessibility tree.

### Points summary and recovery

The earned-points total above the groups comes from the pool score endpoint, not from the loaded rows. It loads independently of the groups:

- The summary's Retry requests only the total.
- The list's Retry, on a failed first page, requests only the groups.
- Pull to refresh reloads both together and ends when the slower one finishes. Each result appears as soon as it arrives, and one failing keeps the other.
