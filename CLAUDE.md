# Tyche

## What is Tyche?

Tyche is a mobile app for playing **"polla"** (Colombia) / **"quiniela"** (Spain) — a prediction pool game where:

- A group of people joins a pool
- Each participant (gambler) predicts the results of a set of matches (typically football/soccer)
- Points are awarded based on prediction accuracy (exact score, correct winner, etc.)
- The person with the most points wins

## Tech Stack

- **Backend:** F# on AWS Lambda, DynamoDB (single-table design per bounded context), SQS FIFO
- **Mobile:** iOS (Swift), Android (Kotlin)
- **Hosting:** Firebase Hosting (privacy policy page)

## DynamoDB Model

Four tables, one per bounded context: `Account`, `Pool`, `PoolLayout`, `MatchScoreIngestion` — each **single-table design** internally, with entity types distinguished by prefixed key values (`POOL#<ulid>`, `GAMBLER#<ulid>`, `MATCH#<ulid>`).

Key map (real column names are `pk`/`sk`):

| Entity | Table | `pk` | `sk` |
|---|---|---|---|
| Account | `Account` | `ACCOUNT#<accountId>` | — |
| Email lock | `Account` | `EMAIL#<email>` | — |
| Pool root | `Pool` | `POOL#<poolId>` | `POOL#<poolId>` |
| Pool gambler / score | `Pool` | `POOL#<poolId>` | `GAMBLER#<gamblerId>` |
| Pool gambler bet | `Pool` | `GAMBLER#<gamblerId>#POOL#<poolId>` | `MATCH#<matchId>` |
| Pool layout root | `PoolLayout` | `POOLLAYOUT#<layoutId>` | `POOLLAYOUT#<layoutId>` |
| Pool layout match | `PoolLayout` | `POOLLAYOUT#<layoutId>` | `MATCH#<matchId>` |
| Match score ingestion | `MatchScoreIngestion` | `MATCH#<matchId>` | — |

Full ER diagram, item examples, GSIs, and design rationale: **[docs/database-model.md](docs/database-model.md)**. Read it before any task that touches persistence.

## Code Preferences

- Idiomatic F#: no mutable collections, prefer functional approaches
- No NoOp/stub implementations for DI — it's a code smell
- Repositories = data access only, no messaging or orchestration
- Callbacks should be async when involving I/O
- Naming: verb-first for callbacks (e.g., `onComputePool`), avoid past tense
- Always question if the approach is the most optimal and cleanest before implementing
- Discuss tradeoffs before implementing architectural decisions

## OpenSpec Spec Authoring

When authoring future OpenSpec specifications that introduce or change app-rendered icons:

- Prefer Material Symbols when a semantically accurate representation exists.
- Otherwise, allow a custom icon without requiring or recording a justification.
- Require iOS and Android to use assets derived from the same committed canonical vector source.
- Do not prescribe SF Symbols on iOS or Compose Material `Icons.*` on Android.
- Exclude controls rendered entirely by the operating system.
- Do not retroactively revise existing specifications solely to apply this rule.

When authoring future mobile UI specifications that introduce or change model-backed loading placeholders:

- Require each affected platform to populate the production item or row component with a placeholder model.
- Apply the existing shared platform shimmer treatment.
- Do not prescribe a separately maintained skeleton-only layout.
- Allow placeholder-specific behavior to suppress remote loading, navigation, and accessibility exposure, but not to replace the production layout.
- Do not expose placeholder values as real user content.
- Do not retroactively revise existing specifications solely to apply this rule.

## Tooling

- For AWS operations (DynamoDB, Lambda, S3, etc.) use the `aws` CLI directly via the Bash tool. Don't wrap the calls in a saved bash script (`/tmp/migrate.sh`, etc.) — run the commands inline. A short inline `for`/`while` loop in a single Bash invocation is fine; the rule is "no script file as a side artifact."
