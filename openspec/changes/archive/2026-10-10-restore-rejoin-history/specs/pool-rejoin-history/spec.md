# Spec Delta

## Purpose

Let a gambler who was removed from a pool and later rejoins it continue with the bets and points they earned before removal, consistently across pool totals, positions, ranked match lists, and later result corrections.

## ADDED Requirements

### Requirement: Rejoining restores retained history

When a removed gambler rejoins a pool, the system SHALL restore every computed bet retained from that gambler's earlier memberships in the pool to the new membership. The new membership's total SHALL include each restored bet's match award exactly once, in addition to any points earned by the new membership itself. Restoration SHALL happen automatically for every rejoin, without an owner or gambler choice. Restoration SHALL NOT change a restored bet's prediction or its historical match positions.

#### Scenario: A gambler rejoins after earning points
- **GIVEN** a gambler earned 10 points on match 1 and 4 points on match 2, was removed from the pool, and those computed bets were retained
- **WHEN** the gambler rejoins the pool and restoration completes
- **THEN** the new membership's total is 14 points
- **AND** the bets for matches 1 and 2 keep their original predictions and awards

#### Scenario: A gambler was removed and rejoined more than once
- **GIVEN** a gambler has retained computed bets from two earlier memberships in the same pool
- **WHEN** the gambler rejoins and restoration completes
- **THEN** the retained bets from both earlier memberships belong to the new membership
- **AND** each restored award is counted in the total exactly once

#### Scenario: A gambler rejoins without retained bets
- **GIVEN** a removed gambler has no retained computed bets in the pool
- **WHEN** the gambler rejoins
- **THEN** the new membership's total starts at zero, as before this change

### Requirement: Restored awards reflect the current result

A restored bet's award SHALL reflect the match's current authoritative result under the existing scoring formula. When a match result was corrected while the gambler was absent, restoration SHALL apply the award for the corrected result rather than the award recorded before the correction.

#### Scenario: A result was corrected during the absence
- **GIVEN** a retained bet earned 10 points, and the match result was corrected while the gambler was absent so that the bet now earns 4 points
- **WHEN** the gambler rejoins and restoration completes
- **THEN** the restored bet shows 4 points
- **AND** the new membership's total includes 4 points for that match, not 10

### Requirement: Bets deleted at removal are not restored

Restoration SHALL NOT recreate bets that were deleted when the gambler was removed. Matches without a retained bet SHALL follow the existing rules for a joining gambler: matches still open to predictions SHALL accept a new prediction, and matches scored while the gambler was absent SHALL follow the existing late-join rule of zero points.

#### Scenario: Uncomputed bet deleted at removal, match still open
- **GIVEN** the gambler's bet on an open match was deleted at removal
- **WHEN** the gambler rejoins
- **THEN** the previous prediction is not restored
- **AND** the gambler can submit a new prediction while the match remains open

#### Scenario: Match scored during the absence
- **GIVEN** a match was scored while the gambler was not a member
- **WHEN** the gambler rejoins and restoration completes
- **THEN** that match follows the existing late-join rule and contributes zero points

### Requirement: Restored history appears in standings and ranked match lists

After restoration, the gambler's current pool position SHALL reflect the restored total. After background propagation and index updates converge, every match row of the new membership, including restored rows, SHALL carry the restored total, and the gambler SHALL appear in the ranked participant lists of the restored matches with their original prediction and match award. Temporary lag during propagation SHALL be allowed, as for any other total change.

#### Scenario: Viewing an older match after a rejoin
- **GIVEN** a rejoined gambler's bet on match 1 was restored
- **WHEN** propagation converges and a member opens the ranked list for match 1
- **THEN** the gambler appears at the position given by their restored pool total
- **AND** their row shows the original prediction and match award

#### Scenario: Current standings after a rejoin
- **GIVEN** restoration added points to the rejoined gambler's total
- **WHEN** restoration completes
- **THEN** current pool positions are recalculated using the restored total
- **AND** no gambler's position before the latest match is changed
- **AND** previously recorded positions for earlier matches are not rewritten

#### Scenario: Previous membership reached a higher score version
- **GIVEN** the gambler's earlier membership had its total change many more times than the new membership has
- **WHEN** the gambler rejoins and propagation converges
- **THEN** every restored row carries the restored total and appears in its match's ranked list

### Requirement: Restoration is exactly-once and safe under concurrency

Restoration SHALL be durable and SHALL complete after interruptions, retries, and duplicate delivery without counting any award twice. It SHALL be ordered with other operations on the same pool, so it does not interleave unsafely with settlement, corrections, or position calculation. If the gambler is removed again before restoration finishes, no restored award SHALL be added to a membership that no longer exists, and restoration of later memberships SHALL remain possible. A result correction applied after restoration SHALL adjust the restored total by the difference from the restored award.

#### Scenario: A retained bet cannot be restored
- **GIVEN** a retained bet is malformed or its match no longer exists
- **WHEN** restoration runs
- **THEN** that bet is held for repair and excluded from the total
- **AND** restoration completes for the remaining bets and is not requested again for the held bet until it is repaired

#### Scenario: Restoration is interrupted and retried
- **GIVEN** restoration re-linked some retained bets before failing
- **WHEN** restoration is retried or delivered again
- **THEN** the remaining bets are restored
- **AND** no award is added to the total more than once

#### Scenario: A result arrives while restoration is pending
- **GIVEN** restoration has been requested and a new match result is settled for the same pool
- **WHEN** both operations complete
- **THEN** the gambler's total equals the restored awards plus the new match's award
- **AND** neither operation's contribution is lost or duplicated

#### Scenario: The gambler is removed again during restoration
- **GIVEN** restoration has started and the gambler is removed again before it finishes
- **WHEN** restoration continues or is retried
- **THEN** no award is added to the removed membership
- **AND** the retained bets remain available for restoration if the gambler rejoins later

#### Scenario: A correction after restoration
- **GIVEN** a restored bet's award of 10 points is included in the rejoined gambler's total
- **WHEN** that match result is corrected so the bet earns 4 points
- **THEN** the gambler's total decreases by 6 exactly once
