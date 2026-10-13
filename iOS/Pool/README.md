# Pool

The pool screens: My pools, a pool's leaderboard, pool creation and joining, and gambler management.

## Trend-group movement on the leaderboard and My pools

The leaderboard (`GamblerScore/`) and My pools (`PoolScore/`) show each row's rank movement across the pool's trend group: the newest kickoff group with a settled match.

### Score contract

- `GET pools/{poolId}/gamblers` (the leaderboard) and `GET gamblers/{gamblerId}/pools` (My pools) return `PoolGamblerScore` rows. Each row has `position`, `beforePosition`, and an optional `trendGroup`.
- `trendGroup` is `{ kickoffDateTime, computedMatches, totalMatches }`. It is absent until a match of the pool settles. Both endpoints send the same values for the same pool.
- `beforePosition` is the row's position before the trend group's first settlement. It resets only when a settlement opens a newer kickoff group. Later settlements of the same group, late results, and corrections keep it.
- `PoolGamblerScoreModel.trendGroup` keeps the counts as a `KickoffGroupProgress`. The kickoff instant itself is not needed on these screens.

### Movement

`PoolGamblerScoreModel.movement` is the shared `Core.RankMovement` (`RankMovement.trendGroup`):

| `trendGroup` | `beforePosition` and `position` | State |
| --- | --- | --- |
| absent | any | pending |
| present | either missing | unavailable, never zero |
| present | both present, `computedMatches < totalMatches` | `beforePosition - position` places with the `1 of 2 computed` qualifier |
| present | both present, counts equal | `beforePosition - position` places; zero is steady |

A row without `position` keeps the em dash in its rank badge.

### Presentation

- `GamblerScoreItem` and `PoolScoreItem` render the movement with `UI`'s `TrendIndicator` in the rank rail. Pending and unavailable show their labels, never the steady symbol.
- Each row is announced once, with the movement last: rank, username, score, then movement on the leaderboard; pool name, rank, score, member count, then movement on My pools. A partial group's qualifier follows the direction, for example "Up 3 places, 1 of 2 matches computed". The indicator adds no focus stop of its own.
- At accessibility text sizes the leaderboard row reflows instead of clipping: the rank rail, avatar, and score share the first line, and the username wraps across the full width below. My pools moves the pool name and counts below the rank rail and invite control.
- Loading rows render the production rows with `isPlaceholder: true`. They expose no values, ignore touches, and stay out of the accessibility tree; leaderboard placeholders request no avatar.
- Pull to refresh reloads the list from its first page. The leaderboard's append Retry repeats the failed append through `UI`'s `AppendPagingVStack`.
