#!/bin/bash
# Inspects a gambler's rejoin restoration in a pool and, after a held detail is repaired,
# clears its exclusion marker and replays the restoration.
#
#   ./restore-rejoin-history.sh <poolId> <gamblerId>                    show the membership, the
#                                                                       restore receipt, and the
#                                                                       computed details not bound
#                                                                       to the membership (read-only)
#   ./restore-rejoin-history.sh <poolId> <gamblerId> --clear <matchId>  clear the detail's
#                                                                       exclusion marker for the
#                                                                       current membership
#   ./restore-rejoin-history.sh <poolId> <gamblerId> --replay           enqueue the restoration
#                                                                       again under a new operation ID
set -euo pipefail
source "$(cd "$(dirname "$0")" && pwd)/common.sh"

pool="$1"
gambler="$2"
action="${3:-}"

membership_key="{\"pk\":{\"S\":\"POOL#$pool\"},\"sk\":{\"S\":\"GAMBLER#$gambler\"}}"
generation="$(aws dynamodb get-item --region "$REGION" --table-name "$POOL_TABLE" --consistent-read \
  --key "$membership_key" --query 'Item.membershipGeneration.S' --output text)"

if [ "$generation" = "None" ]; then
  echo "No current membership for gambler $gambler in pool $pool." >&2
  [ -z "$action" ] || exit 1
fi

case "$action" in
  "")
    echo "Membership:"
    aws dynamodb get-item --region "$REGION" --table-name "$POOL_TABLE" --consistent-read --key "$membership_key" \
      --projection-expression "membershipGeneration, #score, scoreVersion, #position, beforePosition" \
      --expression-attribute-names '{"#score":"score","#position":"position"}' \
      --query 'Item' --output json

    if [ "$generation" != "None" ]; then
      echo "Restore receipt RESTORE#$gambler#$generation:"
      aws dynamodb get-item --region "$REGION" --table-name "$POOL_TABLE" --consistent-read \
        --key "{\"pk\":{\"S\":\"POOL#$pool\"},\"sk\":{\"S\":\"SETTLEMENT#RESTORE#$gambler#$generation\"}}" \
        --projection-expression "operationState, operationRevision, unresolvedDetails, admittedDateTime, completedDateTime" \
        --query 'Item' --output json
    fi

    echo "Computed details not bound to the membership (retained or held for repair):"
    aws dynamodb query --region "$REGION" --table-name "$POOL_TABLE" --consistent-read \
      --key-condition-expression "pk = :pk" \
      --filter-expression "attribute_exists(computedRequestId) AND (attribute_not_exists(poolMembershipGeneration) OR poolMembershipGeneration <> :generation)" \
      --projection-expression "matchId, poolMembershipGeneration, #score, resultVersion, homeTeamBet, awayTeamBet, restoreExcludedGeneration, restoreExclusionReason" \
      --expression-attribute-names '{"#score":"score"}' \
      --expression-attribute-values "{\":pk\":{\"S\":\"GAMBLER#$gambler#POOL#$pool\"},\":generation\":{\"S\":\"$generation\"}}" \
      --output json \
      | jq '[.Items[] | {matchId: .matchId.S, generation: .poolMembershipGeneration.S, award: .score.N, resultVersion: .resultVersion.N,
                        prediction: (if .homeTeamBet then "\(.homeTeamBet.N)-\(.awayTeamBet.N)" else null end),
                        heldFor: .restoreExcludedGeneration.S, reason: .restoreExclusionReason.S}]'
    ;;
  --clear)
    match="${4:?matchId required}"
    aws dynamodb update-item --region "$REGION" --table-name "$POOL_TABLE" \
      --key "{\"pk\":{\"S\":\"GAMBLER#$gambler#POOL#$pool\"},\"sk\":{\"S\":\"MATCH#$match\"}}" \
      --update-expression "REMOVE restoreExcludedGeneration, restoreExclusionReason" \
      --condition-expression "restoreExcludedGeneration = :generation" \
      --expression-attribute-values "{\":generation\":{\"S\":\"$generation\"}}"
    echo "Cleared. Replay the restoration with --replay once every held detail of the gambler is repaired."
    ;;
  --replay)
    operation="RESTORE#$gambler#$generation#OPERATOR-$(date -u +%Y%m%dT%H%M%SZ)"
    body="{\"schema\":1,\"operationId\":\"$operation\",\"poolId\":\"$pool\",\"kind\":\"RESTORE_REJOIN_HISTORY\",\"gamblerId\":\"$gambler\",\"membershipGeneration\":\"$generation\"}"
    aws sqs send-message --region "$REGION" --queue-url "$(command_queue_url)" --message-body "$body" \
      --message-group-id "$pool" --message-deduplication-id "$pool#$operation" --query MessageId --output text
    echo "Enqueued $operation."
    ;;
  *)
    echo "Unknown action $action" >&2
    exit 1
    ;;
esac
