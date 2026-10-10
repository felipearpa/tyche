#!/bin/bash
# Rolls back ranked reads for one pool: records it as not ready, so ranked requests for it
# answer 503 RANKING_NOT_READY while requests without sort are unaffected. Run
# `./enqueue-maintenance.sh VERIFY_READINESS <poolId>` to re-verify and re-enable it.
#
#   ./mark-pool-not-ready.sh <poolId> [reason]
set -euo pipefail
source "$(cd "$(dirname "$0")" && pwd)/common.sh"

pool="$1"; reason="${2:-operator rollback}"
now="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
values=$(jq -n -c --arg reason "$reason" --arg now "$now" \
  '{":readiness": {M: {state: {S: "NOT_READY"}, schemaVersion: {N: "1"}, reason: {S: $reason}, markedAt: {S: $now}}}}')

aws dynamodb update-item --region "$REGION" --table-name "$POOL_TABLE" \
  --key "{\"pk\":{\"S\":\"POOL#$pool\"},\"sk\":{\"S\":\"POOL#$pool\"}}" \
  --update-expression "SET rankingReadiness = :readiness" \
  --condition-expression "attribute_exists(pk)" \
  --expression-attribute-values "$values"
echo "Pool $pool marked NOT_READY ($reason)."
