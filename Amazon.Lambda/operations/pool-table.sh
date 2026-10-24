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
#   ./pool-table.sh ensure-kickoff-index [table]   add GetFinishedPoolGamblerBetsByMatchDate-index
#                                                  if missing
#   ./pool-table.sh create-copy <pool-table> <layout-table>
#                                                  isolated copies with the same schema, for
#                                                  verification only
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
source "$HERE/common.sh"
INDEX_FILE="$HERE/ranking-index.json"
KICKOFF_INDEX_FILE="$HERE/finished-by-match-date-index.json"

describe() {
  aws dynamodb describe-table --region "$REGION" --table-name "$1" --output json \
    --query 'Table.{keys:KeySchema,stream:StreamSpecification,streamArn:LatestStreamArn,billing:BillingModeSummary.BillingMode,indexes:GlobalSecondaryIndexes[].{name:IndexName,keys:KeySchema,projection:Projection.ProjectionType,status:IndexStatus}}'
}

# ensure_index <table> <index-name> <index-file> <attribute-definition>...
ensure_index() {
  local table="$1" name="$2" file="$3"
  shift 3
  if aws dynamodb describe-table --region "$REGION" --table-name "$table" \
      --query "Table.GlobalSecondaryIndexes[?IndexName=='$name'].IndexName" \
      --output text | grep -q "$name"; then
    echo "$name already exists on $table"
  else
    aws dynamodb update-table --region "$REGION" --table-name "$table" \
      --attribute-definitions "$@" \
      --global-secondary-index-updates "[{\"Create\": $(cat "$file")}]" \
      --query 'TableDescription.TableStatus' --output text
  fi
  echo "Waiting for $table and its indexes to become active"
  until [ "$(aws dynamodb describe-table --region "$REGION" --table-name "$table" \
      --query "length(Table.GlobalSecondaryIndexes[?IndexStatus!='ACTIVE'])" --output text)" = "0" ]; do
    sleep 15
  done
  echo "Active"
}

ensure_ranking_index() {
  ensure_index "$1" GetPoolMatchBetsByTotal-index "$INDEX_FILE" \
    AttributeName=getPoolMatchBetsByTotalPk,AttributeType=S \
    AttributeName=getPoolMatchBetsByTotalSk,AttributeType=S
}

# Finished details by kickoff, for the grouped History and Timeline feeds. Only computed
# details carry computedMatchDateTime, so pending details never enter it.
ensure_kickoff_index() {
  ensure_index "$1" GetFinishedPoolGamblerBetsByMatchDate-index "$KICKOFF_INDEX_FILE" \
    AttributeName=pk,AttributeType=S \
    AttributeName=computedMatchDateTime,AttributeType=S
}

wait_for_table() {
  echo "Waiting for $1 to become active"
  aws dynamodb wait table-exists --region "$REGION" --table-name "$1"
}

key() {
  echo "{\"AttributeName\":\"$1\",\"KeyType\":\"$2\"}"
}

index() {
  echo "{\"IndexName\":\"$1\",\"KeySchema\":[$(key "$2" HASH)${3:+,$(key "$3" RANGE)}],\"Projection\":{\"ProjectionType\":\"ALL\"}}"
}

create_pool_table() {
  aws dynamodb create-table --region "$REGION" --table-name "$1" \
    --billing-mode PAY_PER_REQUEST \
    --stream-specification StreamEnabled=true,StreamViewType=NEW_AND_OLD_IMAGES \
    --attribute-definitions \
      AttributeName=pk,AttributeType=S AttributeName=sk,AttributeType=S \
      AttributeName=matchDateTime,AttributeType=S AttributeName=computedDateTime,AttributeType=S \
      AttributeName=computedMatchDateTime,AttributeType=S \
      AttributeName=score,AttributeType=N AttributeName=gamblerUsername,AttributeType=S \
      AttributeName=getPoolGamblerScoresByMatchPk,AttributeType=S AttributeName=getPoolGamblerScoresByMatchSk,AttributeType=S \
      AttributeName=getGamblersByPoolLayoutPk,AttributeType=S AttributeName=getGamblersByPoolLayoutSk,AttributeType=S \
      AttributeName=getPoolMatchBetsByTotalPk,AttributeType=S AttributeName=getPoolMatchBetsByTotalSk,AttributeType=S \
    --key-schema AttributeName=pk,KeyType=HASH AttributeName=sk,KeyType=RANGE \
    --global-secondary-indexes "[
      $(index GetPendingPoolGamblerBets-index pk matchDateTime),
      $(index GetFinishedPoolGamblerBets-index pk computedDateTime),
      $(cat "$KICKOFF_INDEX_FILE"),
      $(index GetPoolGamblerScoresByGambler-index sk score),
      $(index GetPoolGamblerScoresByPool-index pk score),
      $(index GetPoolGamblerScoresByMatch-index getPoolGamblerScoresByMatchPk getPoolGamblerScoresByMatchSk),
      $(index GetGamblersByPoolLayout-index getGamblersByPoolLayoutPk getGamblersByPoolLayoutSk),
      $(index GetPoolGamblersByUsername-index pk gamblerUsername),
      $(cat "$INDEX_FILE")
    ]" --query 'TableDescription.TableStatus' --output text
}

create_layout_table() {
  aws dynamodb create-table --region "$REGION" --table-name "$1" \
    --billing-mode PAY_PER_REQUEST \
    --stream-specification StreamEnabled=true,StreamViewType=NEW_AND_OLD_IMAGES \
    --attribute-definitions AttributeName=pk,AttributeType=S AttributeName=sk,AttributeType=S \
      AttributeName=status,AttributeType=S AttributeName=startDateTime,AttributeType=S \
    --key-schema AttributeName=pk,KeyType=HASH AttributeName=sk,KeyType=RANGE \
    --global-secondary-indexes "[$(index GetOpenedPoolLayout-index status startDateTime)]" \
    --query 'TableDescription.TableStatus' --output text
}

create_account_table() {
  aws dynamodb create-table --region "$REGION" --table-name "$1" \
    --billing-mode PAY_PER_REQUEST \
    --attribute-definitions AttributeName=pk,AttributeType=S AttributeName=email,AttributeType=S \
    --key-schema AttributeName=pk,KeyType=HASH \
    --global-secondary-indexes "[$(index GetByEmail-index email)]" \
    --query 'TableDescription.TableStatus' --output text
}

create_match_score_ingestion_table() {
  aws dynamodb create-table --region "$REGION" --table-name "$1" \
    --billing-mode PAY_PER_REQUEST \
    --stream-specification StreamEnabled=true,StreamViewType=NEW_AND_OLD_IMAGES \
    --attribute-definitions AttributeName=pk,AttributeType=S \
    --key-schema AttributeName=pk,KeyType=HASH \
    --query 'TableDescription.TableStatus' --output text
}

create_copy() {
  local pool="$1" layout="$2"
  create_pool_table "$pool"
  create_layout_table "$layout"
  wait_for_table "$pool"
  wait_for_table "$layout"
}

ensure_table() {
  if exists_unless ResourceNotFoundException aws dynamodb describe-table --region "$REGION" --table-name "$1"; then
    echo "$1 already exists"
  else
    "$2" "$1"
  fi
  wait_for_table "$1"
}

create_all() {
  ensure_table Pool create_pool_table
  ensure_table PoolLayout create_layout_table
  ensure_table Account create_account_table
  ensure_table MatchScoreIngestion create_match_score_ingestion_table
}

case "${1:-}" in
  describe) describe "${2:-Pool}" ;;
  ensure-ranking-index) ensure_ranking_index "${2:-Pool}" ;;
  ensure-kickoff-index) ensure_kickoff_index "${2:-Pool}" ;;
  create-copy) create_copy "$2" "$3" ;;
  create-all) create_all ;;
  *) echo "usage: $0 describe|ensure-ranking-index|ensure-kickoff-index [table] | create-copy <pool-table> <layout-table> | create-all" >&2; exit 2 ;;
esac
