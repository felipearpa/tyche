#!/bin/bash
# Enqueues a maintenance job for one pool or for every pool.
#
#   ./enqueue-maintenance.sh INITIALIZE_IDENTITIES|BACKFILL_PROJECTION|BACKFILL_KICKOFF_GROUPS|VERIFY_READINESS|RECONCILE_POOL <poolId|--all>
#   ./enqueue-maintenance.sh PROPAGATE_TOTAL <poolId> <gamblerId>
#   ./enqueue-maintenance.sh RETIRE_MEMBERSHIP <poolId> <gamblerId> [membershipGeneration]
#
# Jobs are version guarded, so enqueueing one twice is safe.
set -euo pipefail
source "$(cd "$(dirname "$0")" && pwd)/common.sh"

kind="$1"; target="$2"
queue="$(maintenance_queue_url)"

send() {
  aws sqs send-message --region "$REGION" --queue-url "$queue" --message-body "$1" --query MessageId --output text
}

case "$kind" in
  PROPAGATE_TOTAL)
    body="{\"schema\":1,\"kind\":\"PROPAGATE_TOTAL\",\"poolId\":\"$target\",\"gamblerId\":\"$3\"}"
    send "$body" ;;
  RETIRE_MEMBERSHIP)
    generation="${4:-}"
    body="{\"schema\":1,\"kind\":\"RETIRE_MEMBERSHIP\",\"poolId\":\"$target\",\"gamblerId\":\"$3\""
    [ -n "$generation" ] && body="$body,\"generation\":\"$generation\""
    send "$body}" ;;
  INITIALIZE_IDENTITIES|BACKFILL_PROJECTION|BACKFILL_KICKOFF_GROUPS|VERIFY_READINESS|RECONCILE_POOL)
    if [ "$target" = "--all" ]; then
      pools=$(aws dynamodb scan --region "$REGION" --table-name "$POOL_TABLE" \
        --filter-expression "begins_with(pk, :p) AND pk = sk" \
        --expression-attribute-values '{":p":{"S":"POOL#"}}' --projection-expression pk --output json \
        | jq -r '.Items[].pk.S | sub("^POOL#"; "")')
    else
      pools="$target"
    fi
    # Ten jobs per SendMessageBatch call.
    echo "$pools" | jq -R -s -c --arg kind "$kind" '
        split("\n") | map(select(length > 0)) | to_entries
        | map({Id: (.key | tostring), MessageBody: ({schema: 1, kind: $kind, poolId: .value} | tojson)})
        | _nwise(10)' |
      while read -r entries; do
        aws sqs send-message-batch --region "$REGION" --queue-url "$queue" --entries "$entries" \
          --query '{sent: length(Successful), failed: Failed}' --output json
      done ;;
  *) echo "unknown job kind $kind" >&2; exit 2 ;;
esac
