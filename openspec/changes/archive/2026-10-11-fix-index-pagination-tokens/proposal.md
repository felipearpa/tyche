# Proposal

## Why

The shared DynamoDB continuation serializer retains only `pk` and `sk`, discarding additional index keys such as numeric `score`, `matchDateTime`, and composite index attributes. Nine paginated repository operations across eight indexes therefore emit incomplete `next` tokens when a storage page boundary is reached; this can stop both user-facing lists and background match fan-out.

## What Changes

- Preserve every attribute and scalar type in DynamoDB's continuation key through the shared token codec.
- Apply the fix to pending/live bets, finished bets, legacy match bets, gambler pool scores, pool leaderboards, pool members, gamblers-by-layout fan-out, and open pool layouts.
- Treat both absent and empty continuation keys as completion, while retaining continuations for empty filtered pages.
- Keep the public `items`/optional-string `next` response shape and Timeline's live/finished phase behavior. Preserve the separate ranked match-bets cursor and existing raw-key pagination paths.
- Define compatibility explicitly: complete legacy base-table cursors remain usable; incomplete legacy index cursors receive a controlled invalid-cursor response requiring a first-page refresh. Do not invent missing keys or silently return page one as an append.
- Add regression coverage that forces real storage pagination, checks typed key round trips for every affected query, and verifies HTTP and background consumers.
- Ship the dual-format reader and the typed writer in one release while Fortuna has no production traffic.
- Upgrade iOS `lazy-paging-swift` from 0.0.3 to 0.0.5, which fixes duplicate appends that complete multi-page continuations expose, and repeated first-page reloads.

## Capabilities

### New Capabilities

- `backend-pagination`: Lossless DynamoDB continuations, page completion, invalid-token handling, and compatibility across backend paginated reads.

### Modified Capabilities

None. Existing screen presentation, ordering, refresh, and ranked match-bets requirements remain in place; this change specifies the shared backend behavior needed to satisfy them.

## Impact

- Backend: `Felipearpa.Data.DynamoDb`, paginated Pool and PoolLayout repositories/request builders, their application consumers, and cursor-error mapping in both Lambda and HttpApi entry paths.
- Existing mobile clients continue to transport `next` as an opaque string. An in-flight incomplete token from the old backend requires an ordinary refresh; no automatic-restart behavior is required. iOS needs a release with `lazy-paging-swift` 0.0.5.
- No tables, indexes, key schemas, scoring rules, queues, or stored item migrations change. No new package dependency is required by the design.
- Verification must cover all nine affected index operations plus the shared serializer's base-table caller, not only the ranked endpoint, which already uses a separate complete cursor.
