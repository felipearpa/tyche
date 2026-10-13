# Changelog

## 2.0.0

- Profile picture uploads: a new authenticated Account endpoint issues a short-lived presigned S3 upload URL for the caller's own avatar — locked to the caller's account, JPEG only, 1 MB max, 5-minute expiry
- Avatar photos live at a fixed per-account address and are overwritten on each change, with cache revalidation so clients never keep showing a superseded photo
- The gambler pool listing now returns each pool's member count, so clients can show how many gamblers each pool has
- A removed gambler who rejoins a pool now gets their history back: retained scored bets move to the new membership, their earned points count toward the new total exactly once, and they reappear in the ranked player lists of matches they played
- Rejoin restoration leaves predictions, match awards, and each gambler's previous position unchanged, recalculates current standings, and lets later score corrections adjust the restored total correctly; bets deleted at removal are not recreated, and matches scored while the gambler was away still earn zero
- Internal: restoration runs as an ordered, idempotent per-pool operation on the existing FIFO queue, safe under retries, duplicate delivery, concurrent settlement, and a repeat removal, with an operator runbook script for inspecting and replaying held restores
- Rank trend now covers every match that shares a kickoff: the baseline position resets only when a newer kickoff group gets its first result, so later results, late scores, and corrections in the same window keep the comparison
- Pool score responses carry a `trendGroup` with the group's kickoff time and how many of its matches are scored versus the total, so clients can show progress like 1 of 2; the existing position fields keep their meaning
- History and Timeline now return one item per kickoff group, never split across pages, each with its match rows, scored and total counts, and the gambler's rank movement computed from stored position snapshots, or marked unavailable when a snapshot is missing
- Timeline switches between upcoming and finished at the group level, History no longer takes a search filter, and continuations from the previous finished-bets listing restart at the first page with `INVALID_CURSOR`
- Rescheduling a match in the pool layout now copies the new kickoff onto every gambler's bet for that match, so groups follow the change
- Internal: finished bets are queried through a new kickoff-ordered index, a maintenance backfill copies kickoffs onto scored bets and records each pool's current trend group before cutover, and the settlement and maintenance consumers explicitly allow their bounded self-requeue loops
- Internal: pool root items are batch-fetched via `BatchGetItem` (chunked with unprocessed-key retries) to enrich score pages with gambler counts

## 1.7.1

- Scoring values doubled: a correct winner is now worth 10 points, each exact team score 4, and the goal-difference margin 2 — so an exact prediction earns 20 points instead of 10

## 1.7.0

- Automatic match score ingestion: final results are now fetched and recorded without manual intervention, so standings and bet scores settle on their own once a match ends
- New `MatchScoreIngestion` bounded context with its own DynamoDB control table whose stream drives self-terminating EventBridge schedules that poll football-data per match and stop once a final score is recorded — no always-on poller
- Pool layout repository can now persist a match's final score, fanning the result out to every gambler's bet via existing DynamoDB Streams
- Internal: stream-filtered ingestion events wired into the Amazon.Lambda host, with event-filter unit test coverage

## 1.6.2

- Finished bets are now returned newest first, so the bet timeline lists most recently scored matches at the top

## 1.6.1

- Goal-difference scoring now rewards the absolute margin between the teams, so the 1-point goal-difference bonus is awarded even when the predicted winner is reversed (e.g. predicting 1-2 for an actual 2-1 still matches the one-goal margin)

## 1.6.0

- New `GET /pools/{poolId}/members` endpoint listing a pool's members, with the pool owner flagged and cursor pagination
- New `DELETE /pools/{poolId}/members/{gamblerId}` endpoint — owner-only removal of a gambler; rejects removing the owner and cascades cleanup of the gambler's score row and bets via DynamoDB Streams
- Caller-to-gambler resolution and ownership authorization for the new member endpoints

## 1.5

- New `PATCH /accounts` endpoint that updates an account username and cascades the change to every related pool gambler and bet row in a single transaction
- `gamblerEmail` denormalized onto pool gambler and bet entities so clients can render avatars and contact info without an extra account lookup
- Internal: account `PoolEvent` extended for username propagation, new request builders and integration tests covering the cascade

## 1.4

- New `DELETE /pools/{poolId}` endpoint that cascades the removal of every gambler, score, and bet in the pool

## 1.3

- Public `/version` endpoint exposing the deployed assembly version
- New endpoint to fetch a single gambler bet by id, powering the match bet detail flow
- Pool layout versioning: new matches added to a layout are fanned out and backfilled to every gambler with zero-score bets
- Bet timeline cursor now transitions seamlessly from live to finished bets in a single paginated stream
- Forbidden (HTTP 403) responses surfaced when a gambler's pool access has been revoked
- Live bet retrieval no longer applies a time-based lookback floor
- Increased Lambda memory allocation for faster execution
- Internal: BetFunction unit test coverage and DynamoDB schema documentation

## 1.1

First public release.

- HTTP API for creating and joining prediction pools
- Match prediction submission and live standings
- Score and position computation via Lambda with SQS FIFO ordering
- Tournament data including World Cup 2026
- Email link sign-in token issuance for Firebase Authentication
- DynamoDB single-table storage for pools, gamblers, matches, and bets
