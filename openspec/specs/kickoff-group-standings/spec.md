# kickoff-group-standings Specification

## Purpose
Give Fortuna one rank movement per kickoff, shared by the leaderboard, My pools, History, and Timeline, taken from a baseline that is reset once per kickoff group and updated as the group's matches settle.

## Requirements

### Requirement: Kickoff groups are identified by pool and kickoff instant

The service SHALL identify a kickoff group by the pool and the scheduled kickoff instant of its layout matches, normalized to UTC. Timestamps that denote the same instant with different offsets SHALL identify the same group; different instants SHALL be different groups even on the same calendar day or when their local labels coincide. Group membership and the total match count SHALL come from the pool's layout, independently of which bets, rows, or pages a client has loaded. Every response that exposes a group SHALL identify it by its kickoff instant as an ISO-8601 UTC string, and clients SHALL group by that value rather than by a formatted or local date. A layout match SHALL count as computed when the layout holds a result for it.

#### Scenario: Two fixtures share one kickoff
- **GIVEN** two layout matches of one pool have the same normalized scheduled instant
- **WHEN** standings or either feed expose their group
- **THEN** both matches carry the same kickoff instant and the group's total count is two
- **AND** a match of another pool's layout cannot enter that group

#### Scenario: Local labels are not group keys
- **GIVEN** two timestamps denote the same instant with different offsets, and a third denotes a different instant with the same local clock label
- **WHEN** groups are identified
- **THEN** the equivalent instants share a group and the third does not

#### Scenario: A result exists before its settlement completes
- **GIVEN** a two-match group where one match has a layout result whose pool settlement is still running
- **WHEN** the group's counts are read
- **THEN** it reports one computed match out of two

### Requirement: The rank baseline resets once per kickoff group

When a match settlement is applied to a pool, the service SHALL compare the settling match's kickoff instant with the pool's current trend kickoff. If the pool has no trend kickoff or the settling kickoff is newer, the service SHALL record each membership's position before the operation as its `beforePosition` and SHALL set the pool's trend kickoff to the settling kickoff when the operation completes. If the settling kickoff equals the trend kickoff, or is older than it, the service SHALL leave every membership's `beforePosition` unchanged. Current positions SHALL be recalculated in every case. The settled match detail SHALL receive its own before and after position snapshot on the operation's first write in every case. Standings reconciliation after a join or removal and rejoin restoration SHALL keep their existing rule of moving `position` and leaving `beforePosition` unchanged. The decision SHALL be stable across retries and resumed executions of the same operation.

#### Scenario: Two matches of one group settle separately
- **GIVEN** a gambler is eighth before a two-match group
- **WHEN** the first match settles and leaves the gambler fifth, and the second settles and leaves the gambler sixth
- **THEN** after the first settlement `beforePosition` is eight and `position` is five
- **AND** after the second settlement `beforePosition` is still eight and `position` is six

#### Scenario: A newer kickoff settles
- **GIVEN** memberships hold a baseline for a 14:00 group
- **WHEN** the first match of an 18:00 group settles
- **THEN** every membership's `beforePosition` becomes its position before that settlement
- **AND** the pool's trend kickoff becomes 18:00 when the operation completes

#### Scenario: An older result arrives after a newer group settled
- **GIVEN** the pool's trend kickoff is 18:00 and positions were recalculated for it
- **WHEN** a late result or a correction for a 14:00 match settles
- **THEN** positions are recalculated and every membership's `beforePosition` is unchanged
- **AND** the pool's trend kickoff remains 18:00

#### Scenario: The operation is resumed after a deadline yield
- **GIVEN** a settlement for a newer kickoff was interrupted after some memberships were written
- **WHEN** another worker resumes the operation
- **THEN** it applies the same baseline decision to the remaining memberships
- **AND** memberships written before the yield are not reset again within the operation

### Requirement: Score responses expose the trend group

Pool score responses SHALL keep `position` and `beforePosition` and SHALL add an optional `trendGroup` object with `kickoffDateTime`, `computedMatches`, and `totalMatches`. `trendGroup` SHALL be present when the pool has a trend kickoff and absent otherwise. `beforePosition` SHALL denote the gambler's position before the trend group's first settlement. Counts SHALL be read from the pool's layout for the trend kickoff. The leaderboard and My pools SHALL receive identical `trendGroup` values for the same pool at the same time. Movement SHALL be derived by clients as `beforePosition` minus `position`; an absent `trendGroup` SHALL be presented as pending movement, and a present `trendGroup` with a missing `beforePosition` or `position` SHALL be presented as unavailable movement.

#### Scenario: Partial group on the leaderboard
- **GIVEN** the trend group has two matches and one has a result
- **WHEN** a leaderboard page is read
- **THEN** each row carries `trendGroup` with that kickoff, one computed match, and two total matches

#### Scenario: Pool without any settled match
- **GIVEN** a pool whose settlement has never run
- **WHEN** its score rows are read
- **THEN** `trendGroup` is absent and clients present pending movement without a numeric value

#### Scenario: Leaderboard and My pools agree
- **GIVEN** one gambler and one pool
- **WHEN** the leaderboard row and the My pools row are read at the same time
- **THEN** both carry the same `position`, `beforePosition`, and `trendGroup`

### Requirement: Grouped feeds carry the group's movement from stored snapshots

Each History or Timeline group item SHALL carry `kickoffDateTime`, `totalMatches`, `computedMatches`, `movement`, and the group's visible match rows ordered by match identifier. `movement` SHALL contain `beforePosition` taken from the group's computed child with the earliest computation time and `position` taken from the computed child with the latest computation time, for the gambler whose feed is requested. When the group has no computed child for that gambler, or either snapshot is missing, `movement` SHALL be null. The service SHALL NOT sum per-match differences, recompute standings on demand, or read an unrelated current position to fill a missing snapshot. Counts SHALL NOT reveal predictions that the existing visibility rules hide.

#### Scenario: A completed two-match group
- **GIVEN** a gambler's two computed children in one group hold snapshots 8 to 5 and 5 to 6 in computation order
- **WHEN** the group is returned
- **THEN** `movement` is `beforePosition` 8 and `position` 6
- **AND** it is neither a movement of minus one from the last child nor plus four from adding magnitudes

#### Scenario: A legacy child lacks snapshots
- **GIVEN** a computed child without a stored before or after position
- **WHEN** its group is returned
- **THEN** `movement` is null and the match rows, points, and dates are still returned

#### Scenario: A group has no computed child for this gambler
- **GIVEN** a Timeline group whose matches all lack a result
- **WHEN** the group is returned
- **THEN** `computedMatches` is zero and `movement` is null
- **AND** clients present the group's movement as pending rather than unavailable or zero

### Requirement: A changed kickoff follows the layout

When a layout match's kickoff instant changes, the service SHALL write the new instant to every gambler's detail for that match in every pool using that layout, updating the pending-feed date on every detail and the computed-feed date on computed details. Repeated delivery of the same change SHALL converge without further writes. Group membership, counts, and feed placement SHALL follow the new instant on the next read. The pool's trend kickoff SHALL NOT be rewritten by this propagation. A match rescheduled before any of its results settled SHALL join its new group without any movement effect.

#### Scenario: A pending match is postponed
- **GIVEN** a match without a result moves from Saturday 16:00 to Sunday 16:00
- **WHEN** the change is propagated and Timeline is refreshed
- **THEN** the match appears under the Sunday group with that group's counts
- **AND** the Saturday group no longer counts it

#### Scenario: The change is delivered twice
- **GIVEN** the kickoff change was already propagated
- **WHEN** the same change is delivered again
- **THEN** no detail changes and no error is raised

### Requirement: Existing pools are backfilled before cutover

Before the grouped feeds are released, a restartable per-pool maintenance job SHALL write the computed-feed date on every computed detail that lacks it, copied from the detail's kickoff, and SHALL set the pool's trend kickoff, when absent, to the kickoff of the match in the pool's most recently completed result settlement. When the pool has no completed result settlement record, because its results were settled before such records existed, the job SHALL instead use the latest kickoff among the layout matches that have a computed detail in that pool. A pool with no computed detail SHALL keep no trend kickoff. Rerunning the job SHALL make no further changes. Memberships' existing `beforePosition` values SHALL be kept as they are.

#### Scenario: Backfilling a pool mid-tournament
- **GIVEN** a pool with computed details and completed settlements but no trend kickoff
- **WHEN** the job runs
- **THEN** every computed detail has the computed-feed date equal to its kickoff
- **AND** the root's trend kickoff is the kickoff of the most recently settled match

#### Scenario: Backfilling a pool settled before settlement records existed
- **GIVEN** a pool with computed details but no completed result settlement record and no trend kickoff
- **WHEN** the job runs
- **THEN** the root's trend kickoff is the latest kickoff among the layout matches with a computed detail in that pool
- **AND** memberships keep their existing `beforePosition`

#### Scenario: Job rerun
- **GIVEN** the job already completed for a pool
- **WHEN** it runs again
- **THEN** it completes without writes
