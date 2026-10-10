#!/bin/bash
# Deployment owner of the externally managed Pool table's schema additions.
#
# The Pool, PoolLayout, and MatchScoreIngestion tables predate the SAM stack and are not
# CloudFormation resources; importing them would risk replacement for no gain. This script is
# the reviewed, repeatable definition of what this repository adds to the Pool table. Every
# command is additive and idempotent: it never deletes or changes an existing table, key, or
# index.
#
#   ./pool-table.sh describe [table]               keys, indexes, stream, billing
#   ./pool-table.sh ensure-ranking-index [table]   add GetPoolMatchBetsByTotal-index if missing
#   ./pool-table.sh create-copy <pool-table> <layout-table>
#                                                  isolated copies with the same schema, for
#                                                  verification only
set -euo pipefail

REGION="${AWS_REGION:-us-east-2}"
HERE="$(cd "$(dirname "$0")" && pwd)"
INDEX_FILE="$HERE/ranking-index.json"

describe() {
  aws dynamodb describe-table --region "$REGION" --table-name "$1" --output json \
    --query 'Table.{keys:KeySchema,stream:StreamSpecification,streamArn:LatestStreamArn,billing:BillingModeSummary.BillingMode,indexes:GlobalSecondaryIndexes[].{name:IndexName,keys:KeySchema,projection:Projection.ProjectionType,status:IndexStatus}}'
}

ensure_ranking_index() {
  local table="$1"
  if aws dynamodb describe-table --region "$REGION" --table-name "$table" \
      --query "Table.GlobalSecondaryIndexes[?IndexName=='GetPoolMatchBetsByTotal-index'].IndexName" \
      --output text | grep -q GetPoolMatchBetsByTotal-index; then
    echo "GetPoolMatchBetsByTotal-index already exists on $table"
  else
    aws dynamodb update-table --region "$REGION" --table-name "$table" \
      --attribute-definitions \
        AttributeName=getPoolMatchBetsByTotalPk,AttributeType=S \
        AttributeName=getPoolMatchBetsByTotalSk,AttributeType=S \
      --global-secondary-index-updates "[{\"Create\": $(cat "$INDEX_FILE")}]" \
      --query 'TableDescription.TableStatus' --output text
  fi
  echo "Waiting for $table and its indexes to become active"
  until [ "$(aws dynamodb describe-table --region "$REGION" --table-name "$table" \
      --query "length(Table.GlobalSecondaryIndexes[?IndexStatus!='ACTIVE'])" --output text)" = "0" ]; do
    sleep 15
  done
  echo "Active"
}

create_copy() {
  local pool="$1" layout="$2"
  local all='{"ProjectionType":"ALL"}'
  local index
  index() { echo "{\"IndexName\":\"$1\",\"KeySchema\":[{\"AttributeName\":\"$2\",\"KeyType\":\"HASH\"},{\"AttributeName\":\"$3\",\"KeyType\":\"RANGE\"}],\"Projection\":$all}"; }
  aws dynamodb create-table --region "$REGION" --table-name "$pool" \
    --billing-mode PAY_PER_REQUEST \
    --stream-specification StreamEnabled=true,StreamViewType=NEW_AND_OLD_IMAGES \
    --attribute-definitions \
      AttributeName=pk,AttributeType=S AttributeName=sk,AttributeType=S \
      AttributeName=matchDateTime,AttributeType=S AttributeName=computedDateTime,AttributeType=S \
      AttributeName=score,AttributeType=N AttributeName=gamblerUsername,AttributeType=S \
      AttributeName=getPoolGamblerScoresByMatchPk,AttributeType=S AttributeName=getPoolGamblerScoresByMatchSk,AttributeType=S \
      AttributeName=getGamblersByPoolLayoutPk,AttributeType=S AttributeName=getGamblersByPoolLayoutSk,AttributeType=S \
      AttributeName=getPoolMatchBetsByTotalPk,AttributeType=S AttributeName=getPoolMatchBetsByTotalSk,AttributeType=S \
    --key-schema AttributeName=pk,KeyType=HASH AttributeName=sk,KeyType=RANGE \
    --global-secondary-indexes "[
      $(index GetPendingPoolGamblerBets-index pk matchDateTime),
      $(index GetFinishedPoolGamblerBets-index pk computedDateTime),
      {\"IndexName\":\"GetPoolGamblerScoresByGambler-index\",\"KeySchema\":[{\"AttributeName\":\"sk\",\"KeyType\":\"HASH\"},{\"AttributeName\":\"score\",\"KeyType\":\"RANGE\"}],\"Projection\":$all},
      {\"IndexName\":\"GetPoolGamblerScoresByPool-index\",\"KeySchema\":[{\"AttributeName\":\"pk\",\"KeyType\":\"HASH\"},{\"AttributeName\":\"score\",\"KeyType\":\"RANGE\"}],\"Projection\":$all},
      $(index GetPoolGamblerScoresByMatch-index getPoolGamblerScoresByMatchPk getPoolGamblerScoresByMatchSk),
      $(index GetGamblersByPoolLayout-index getGamblersByPoolLayoutPk getGamblersByPoolLayoutSk),
      $(index GetPoolGamblersByUsername-index pk gamblerUsername),
      $(cat "$INDEX_FILE")
    ]" --query 'TableDescription.TableStatus' --output text
  aws dynamodb create-table --region "$REGION" --table-name "$layout" \
    --billing-mode PAY_PER_REQUEST \
    --stream-specification StreamEnabled=true,StreamViewType=NEW_AND_OLD_IMAGES \
    --attribute-definitions AttributeName=pk,AttributeType=S AttributeName=sk,AttributeType=S \
    --key-schema AttributeName=pk,KeyType=HASH AttributeName=sk,KeyType=RANGE \
    --query 'TableDescription.TableStatus' --output text
  aws dynamodb wait table-exists --region "$REGION" --table-name "$pool"
  aws dynamodb wait table-exists --region "$REGION" --table-name "$layout"
}

case "${1:-}" in
  describe) describe "${2:-Pool}" ;;
  ensure-ranking-index) ensure_ranking_index "${2:-Pool}" ;;
  create-copy) create_copy "$2" "$3" ;;
  *) echo "usage: $0 describe|ensure-ranking-index [table] | create-copy <pool-table> <layout-table>" >&2; exit 2 ;;
esac
