# Changelog

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
