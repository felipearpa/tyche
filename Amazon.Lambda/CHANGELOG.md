# Changelog

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
