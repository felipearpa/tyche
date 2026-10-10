#!/bin/bash
# Shows a pool's active operation and, after the cause of its failures is fixed, clears the
# blocked mark so the next delivery can resume it.
#
#   ./unblock-pool.sh <poolId>            show the checkpoint
#   ./unblock-pool.sh <poolId> --unblock  clear blocked and the attempt count
set -euo pipefail
source "$(cd "$(dirname "$0")" && pwd)/common.sh"

pool="$1"
key="{\"pk\":{\"S\":\"POOL#$pool\"},\"sk\":{\"S\":\"POOL#$pool\"}}"
aws dynamodb get-item --region "$REGION" --table-name "$POOL_TABLE" --consistent-read --key "$key" \
  --query 'Item.activeOperation.M' --output json

if [ "${2:-}" = "--unblock" ]; then
  aws dynamodb update-item --region "$REGION" --table-name "$POOL_TABLE" --key "$key" \
    --update-expression "SET activeOperation.blocked = :false, activeOperation.attemptsWithoutProgress = :zero" \
    --condition-expression "activeOperation.blocked = :true" \
    --expression-attribute-values '{":false":{"BOOL":false},":true":{"BOOL":true},":zero":{"N":"0"}}'
  echo "Unblocked. Redrive the dead-lettered command (README.md) or send any command for the pool to resume it."
fi
