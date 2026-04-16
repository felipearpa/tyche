# Tyche

## What is Tyche?

Tyche is a mobile app for playing **"polla"** (Colombia) / **"quiniela"** (Spain) — a prediction pool game where:

- A group of people joins a pool
- Each participant (gambler) predicts the results of a set of matches (typically football/soccer)
- Points are awarded based on prediction accuracy (exact score, correct winner, etc.)
- The person with the most points wins

## Tech Stack

- **Backend:** F# on AWS Lambda, DynamoDB (single-table design), SQS FIFO
- **Mobile:** iOS (Swift), Android (Kotlin)
- **Hosting:** Firebase Hosting (privacy policy page)

## Code Preferences

- Idiomatic F#: no mutable collections, prefer functional approaches
- No NoOp/stub implementations for DI — it's a code smell
- Repositories = data access only, no messaging or orchestration
- Callbacks should be async when involving I/O
- Naming: verb-first for callbacks (e.g., `onComputePool`), avoid past tense
- Always question if the approach is the most optimal and cleanest before implementing
- Discuss tradeoffs before implementing architectural decisions
