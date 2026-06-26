---
name: add-pool-layout-match
description: Use when adding a new match (fixture) to an existing pool layout in the Tyche PoolLayout DynamoDB table — e.g. a knockout-stage game that wasn't known at launch, or a forgotten fixture. Covers the single PutItem that the PoolLayout stream fans out to every gambler's bet sheet, the required attributes, the poolLayoutVersion rule, ULID/time formatting, and verification. Trigger on requests like "add a match to the pool layout", "add the <teamA> vs <teamB> match", "insert a fixture".
---

# Add a Match to a Pool Layout

Adding a match is **one `PutItem`** into the `PoolLayout` table. There is no app endpoint for it — `IPoolLayoutRepository` only exposes read + `SetMatchFinalScore`. The insert is the whole operation because it is **stream-driven**.

## Why one PutItem is enough (the fan-out)

```
put-item (new MATCH# row in PoolLayout)
  → PoolLayout DynamoDB stream INSERT event
    → PoolLayoutEvent.OnPoolLayoutChangeAsync
      → PoolLayoutEventFilter.extractInsertedMatches   (fires on EventName = "INSERT")
        → FanOutMatchToGamblers.ExecuteAsync            (walks GetGamblersByPoolLayout-index)
          → MaterializeMatchForGamblerAsync per gambler
            → Step 1: PUT empty bet   ← the match is copied to the gambler
            → Step 2: bump gambler row poolLayoutVersion (condition: current < new; may no-op)
```

Key files: `Felipearpa.Tyche.AmazonLambda/src/Event/PoolLayoutEvent.fs`, `.../Event/PoolLayoutEventFilter.fs`, `Felipearpa.Tyche.Pool/src/Application/FanOutMatchToGamblers.fs`, `.../Infrastructure/PoolGamblerBetDynamoDbRepository.fs` (`MaterializeMatchForGamblerAsync`).

**Never hand-create the per-gambler bet rows.** The stream does it. Writing them yourself duplicates work and can race the fan-out.

The trigger is the **INSERT of a `MATCH#` item**, not a version change. The bet PUT (Step 1) is **not** gated on version — it only checks the bet doesn't already exist. So every current gambler gets the match regardless of the number you choose.

## Required attributes

`extractInsertedMatches` skips the record (no fan-out) if **any** of these 8 are missing — get them all right:

| Attribute | Type | Notes |
|---|---|---|
| `pk` | S | `POOLLAYOUT#<layoutId>` |
| `sk` | S | `MATCH#<matchId>` |
| `matchId` | S | a **fresh ULID**, unique to this match (also in `sk`) |
| `poolLayoutId` | S | the layout's ULID (no prefix) |
| `poolLayoutVersion` | N | **the layout root's current version** — see rule below |
| `homeTeamId` | S | reuse the existing team id (lowercase code, e.g. `za`, `ca`) |
| `homeTeamName` | S | display name, e.g. `Sudáfrica` |
| `awayTeamId` | S | existing team id |
| `awayTeamName` | S | display name |
| `matchDateTime` | S | UTC ISO, `YYYY-MM-DDTHH:MM:SS.000Z` |

Optional:
- `round` (S) — defaults to `"Fase de grupos"` if omitted. For knockout matches set it explicitly (e.g. `"Dieciseisavos de final"`, `"Octavos de final"`, `"Cuartos de final"`, `"Semifinal"`, `"Final"`).
- `groupName` (S) — only for group-stage matches (`"A"`…). **Omit for knockout** (`GroupName` is `string option`).
- **Do not** set `homeTeamScore` / `awayTeamScore` — leaving them off keeps the match pending. The real result flows in later via `SetMatchFinalScore`, which the existing fan-out turns into scored bets.

## poolLayoutVersion rule

**Use the layout root's current `poolLayoutVersion` as-is. Do not bump it.**

The number only matters for **future** gamblers who join later: `JoinPool` resolves the *layout root* version (`PoolLayoutVersionResolver.fs`) and materializes matches where `poolLayoutVersion <= rootVersion` (`GetPendingPoolLayoutMatchesRequestBuilder.fs`). So:

| Choice | Existing gamblers | Future joiners | Writes |
|---|---|---|---|
| match = root version (e.g. `1`) | ✓ get bet (stream) | ✓ `v ≤ root` | 1 |
| match = root+1, **root left alone** | ✓ get bet (stream) | ✗ **miss it** (`v > root`) | 1 — **broken** |
| match = root+1, root bumped too | ✓ get bet | ✓ | 2 — same result, two synced writes |

Bumping buys nothing here and risks the broken middle row, so match-the-root is the clean choice. Only bump (and then you **must** also update the layout root row) if you have a deliberate reason to version a whole batch.

## Procedure

1. **Find the layout id + current version** (skip if you already know it):
   ```bash
   aws dynamodb scan --table-name PoolLayout \
     --filter-expression "begins_with(sk, :p)" \
     --expression-attribute-values '{":p":{"S":"POOLLAYOUT#"}}' \
     --output json
   ```
   Read `poolLayoutId`, `poolLayoutName`, `status`, `poolLayoutVersion` off the root rows.

2. **Get the team ids** (reuse existing — don't invent). List the layout's matches and read off `homeTeamId`/`awayTeamId` for the teams you need:
   ```bash
   aws dynamodb query --table-name PoolLayout \
     --key-condition-expression "pk = :pk AND begins_with(sk, :m)" \
     --expression-attribute-values '{":pk":{"S":"POOLLAYOUT#<layoutId>"},":m":{"S":"MATCH#"}}' \
     --output json
   ```
   Ids are lowercase country codes (`za`, `ca`, `mx`, `gb_eng`, `gb_sct`, …).

3. **Confirm kickoff time in UTC.** Convert local → UTC (Colombia is UTC−5, so 14:00 COT = `19:00Z`). Format `YYYY-MM-DDTHH:MM:SS.000Z`.

4. **Confirm orientation** (which team is home vs away) and the `round` label with the user before writing — this is live, user-facing data.

5. **Mint a ULID and insert** (one Bash call so the id can't go stale):
   ```bash
   MATCH_ID=$(python3 - <<'PY'
   import os, time
   ALPH="0123456789ABCDEFGHJKMNPQRSTVWXYZ"
   def enc(n,length):
       s=""
       for _ in range(length):
           s=ALPH[n & 31]+s; n>>=5
       return s
   ts=int(time.time()*1000); rnd=int.from_bytes(os.urandom(10),"big")
   print(enc(ts,10)+enc(rnd,16))
   PY
   )
   echo "matchId = $MATCH_ID"
   aws dynamodb put-item --table-name PoolLayout --item "{
     \"pk\":                {\"S\": \"POOLLAYOUT#<layoutId>\"},
     \"sk\":                {\"S\": \"MATCH#$MATCH_ID\"},
     \"matchId\":           {\"S\": \"$MATCH_ID\"},
     \"poolLayoutId\":      {\"S\": \"<layoutId>\"},
     \"poolLayoutVersion\": {\"N\": \"<rootVersion>\"},
     \"homeTeamId\":        {\"S\": \"<homeId>\"},
     \"homeTeamName\":      {\"S\": \"<Home Name>\"},
     \"awayTeamId\":        {\"S\": \"<awayId>\"},
     \"awayTeamName\":      {\"S\": \"<Away Name>\"},
     \"matchDateTime\":     {\"S\": \"<YYYY-MM-DDTHH:MM:SS.000Z>\"},
     \"round\":             {\"S\": \"<Round label>\"}
   }" --condition-expression "attribute_not_exists(pk)" \
     && echo "PUT OK: $MATCH_ID"
   ```
   `attribute_not_exists(pk)` makes the insert idempotent — a re-run won't duplicate or overwrite.

6. **Verify the fan-out** materialized bets (wait a couple seconds for the stream). Count should equal the number of gamblers across pools on this layout:
   ```bash
   aws dynamodb query --table-name Pool \
     --index-name GetPoolGamblerScoresByMatch-index \
     --key-condition-expression "getPoolGamblerScoresByMatchPk = :m" \
     --expression-attribute-values '{":m":{"S":"MATCH#<matchId>"}}' \
     --select COUNT
   ```
   `Count: 0` after a few seconds means the insert was missing a required attribute (no fan-out) — recheck the 8 fields.

## Safety

- This writes **live, user-facing** data and fans a bet row out to every gambler — hard to undo (you'd have to delete the match row + every materialized bet). **Confirm orientation, time, round, and layout with the user before running.**
- Run `aws` inline per the repo convention — no wrapper script saved to `/tmp`.

## Related

- Setting a result later goes through the `SetMatchFinalScore` path (`homeTeamScore`/`awayTeamScore` via `UpdateItem`), which the score fan-out turns into scored bets. The `project_correct_wrong_match_score` and `project_auto_score_ingestion` auto-memories cover the scoring/runbook side.
