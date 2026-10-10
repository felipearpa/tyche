#!/bin/bash
# Supported correction of a published match result.
#
#   ./correct-result.sh <poolLayoutId> <matchId> <homeTeamScore> <awayTeamScore> <expectedResultVersion>
#
# Replaces the old manual procedure (separate detail and total writes plus hand-sent position
# messages). This script only validates prerequisites and publishes the next result version
# on the layout match; the PoolLayout stream then dispatches one versioned command per pool,
# and each pool's ordered operation applies the award difference, the total, its score
# version, and positions atomically with the current scoring formula (BetEvaluator).
#
# Prerequisites, checked before anything is written (a refusal changes nothing and takes no
# pool fence):
#   - the match's current result version is the expected one, and
#   - every pool with the match has completed settlement of that version and has no active
#     operation for the match.
set -euo pipefail
source "$(cd "$(dirname "$0")" && pwd)/common.sh"

layout="$1"; match="$2"; home="$3"; away="$4"; expected="$5"

current=$(aws dynamodb get-item --region "$REGION" --table-name "$POOL_LAYOUT_TABLE" --consistent-read \
  --key "{\"pk\":{\"S\":\"POOLLAYOUT#$layout\"},\"sk\":{\"S\":\"MATCH#$match\"}}" --output json)
has_result=$(echo "$current" | jq -r '.Item.homeTeamScore.N // empty')
version=$(echo "$current" | jq -r '.Item.resultVersion.N // "1"')
[ -n "$has_result" ] || { echo "Refused: the match has no published result" >&2; exit 1; }
[ "$version" = "$expected" ] || { echo "Refused: current result version is $version, not $expected" >&2; exit 1; }

for pool in $(pools_with_match "$match"); do
  receipt=$(aws dynamodb get-item --region "$REGION" --table-name "$POOL_TABLE" --consistent-read \
    --key "{\"pk\":{\"S\":\"POOL#$pool\"},\"sk\":{\"S\":\"SETTLEMENT#RESULT#$match#$expected\"}}" \
    --output json | jq -r '.Item.operationState.S // "NONE"')
  active=$(aws dynamodb get-item --region "$REGION" --table-name "$POOL_TABLE" --consistent-read \
    --key "{\"pk\":{\"S\":\"POOL#$pool\"},\"sk\":{\"S\":\"POOL#$pool\"}}" \
    --output json | jq -r '.Item.activeOperation.M.matchId.S // empty')
  # Results settled before receipts existed have no receipt; their details carry the version.
  if [ "$receipt" != "COMPLETED" ] && [ "$expected" != "1" ]; then
    echo "Refused: pool $pool has not completed version $expected (receipt: $receipt)" >&2; exit 1
  fi
  if [ "$active" = "$match" ]; then
    echo "Refused: pool $pool is still settling this match" >&2; exit 1
  fi
done

next=$((expected + 1))
aws dynamodb update-item --region "$REGION" --table-name "$POOL_LAYOUT_TABLE" \
  --key "{\"pk\":{\"S\":\"POOLLAYOUT#$layout\"},\"sk\":{\"S\":\"MATCH#$match\"}}" \
  --update-expression "SET homeTeamScore = :home, awayTeamScore = :away, resultVersion = :next" \
  --condition-expression "attribute_exists(homeTeamScore) AND (resultVersion = :expected OR (attribute_not_exists(resultVersion) AND :expected = :one))" \
  --expression-attribute-values "{\":home\":{\"N\":\"$home\"},\":away\":{\"N\":\"$away\"},\":next\":{\"N\":\"$next\"},\":expected\":{\"N\":\"$expected\"},\":one\":{\"N\":\"1\"}}"
echo "Published result version $next ($home-$away). Follow receipts SETTLEMENT#RESULT#$match#$next per pool."
