#!/bin/bash
# Requests a match reconciliation in every pool that has the match, for example to replay a
# PoolLayout stream record retained in the failure bucket. Each pool settles whatever result
# the match currently has; pools already settled complete without changes.
#
#   ./request-match-reconciliation.sh <matchId> [poolId ...]
set -euo pipefail
source "$(cd "$(dirname "$0")" && pwd)/common.sh"

match="$1"; shift
pools="${*:-$(pools_with_match "$match")}"
source_id="OPERATOR-$(date -u +%Y%m%dT%H%M%SZ)"
queue="$(command_queue_url)"

for pool in $pools; do
  body="{\"schema\":1,\"operationId\":\"MATCH#$match#$source_id\",\"poolId\":\"$pool\",\"kind\":\"RECONCILE_MATCH\",\"matchId\":\"$match\"}"
  aws sqs send-message --region "$REGION" --queue-url "$queue" --message-body "$body" \
    --message-group-id "$pool" --message-deduplication-id "$pool#MATCH#$match#$source_id" --query MessageId --output text
done
