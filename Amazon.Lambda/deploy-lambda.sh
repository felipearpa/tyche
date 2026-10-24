#!/bin/bash
set -euo pipefail

REGION=us-east-2
STACK=TycheApi
CAPABILITIES=(CAPABILITY_IAM CAPABILITY_NAMED_IAM CAPABILITY_AUTO_EXPAND)
HERE="$(cd "$(dirname "$0")" && pwd)"

usage() {
  echo "usage: $0 dev|prod" >&2
  exit 2
}

require_stage() {
  case "$STAGE" in
    dev | prod) ;;
    *) usage ;;
  esac
}

load_stage_settings() {
  source "$HERE/stages/$STAGE.sh"
  export AWS_PROFILE
}

preserved_stack_tag() {
  echo AWSServerlessAppNETCore=true
}

stream_arn_of() {
  local arn
  arn="$(aws dynamodb describe-table --region "$REGION" --table-name "$1" \
    --query 'Table.LatestStreamArn' --output text)"
  if [[ "$arn" != arn:aws:dynamodb:* ]]; then
    echo "Table $1 in stage $STAGE has no stream" >&2
    exit 1
  fi
  echo "$arn"
}

resolve_stream_arns() {
  echo "Reading table streams in $STAGE ($AWS_PROFILE)"
  POOL_STREAM_ARN="$(stream_arn_of Pool)"
  POOL_LAYOUT_STREAM_ARN="$(stream_arn_of PoolLayout)"
  MATCH_SCORE_INGESTION_STREAM_ARN="$(stream_arn_of MatchScoreIngestion)"
}

run_tests() {
  echo "Running tests"
  dotnet test "$HERE/Felipearpa.Tyche.slnx" --nologo --verbosity minimal
}

package_to_deploy_bucket() {
  PACKAGED_TEMPLATE="$(mktemp -t tyche-serverless)"
  trap 'rm -f "$PACKAGED_TEMPLATE"' EXIT
  echo "Packaging to s3://$DEPLOY_BUCKET"
  (
    cd "$HERE/Felipearpa.Tyche.AmazonLambda/src"
    dotnet lambda package-ci --disable-interactive true \
      --region "$REGION" --profile "$AWS_PROFILE" \
      --template serverless.template.yaml \
      --s3-bucket "$DEPLOY_BUCKET" --s3-prefix tyche-api/ \
      --output-template "$PACKAGED_TEMPLATE"
  )
}

cloudformation_deploy() {
  aws cloudformation deploy --region "$REGION" --stack-name "$STACK" \
    --template-file "$PACKAGED_TEMPLATE" \
    --capabilities "${CAPABILITIES[@]}" \
    --no-fail-on-empty-changeset \
    --tags "$(preserved_stack_tag)" \
    --parameter-overrides \
      "PoolStreamArn=$POOL_STREAM_ARN" \
      "PoolLayoutStreamArn=$POOL_LAYOUT_STREAM_ARN" \
      "MatchScoreIngestionStreamArn=$MATCH_SCORE_INGESTION_STREAM_ARN" \
      "PoolMaintenanceMaximumConcurrency=$POOL_MAINTENANCE_MAXIMUM_CONCURRENCY" \
    "$@"
}

deploy_to_dev() {
  cloudformation_deploy
}

print_changeset() {
  aws cloudformation describe-change-set --region "$REGION" --change-set-name "$1" \
    --query 'Changes[].ResourceChange.[Action, LogicalResourceId, ResourceType, Replacement || `-`]' \
    --output table
}

operator_confirms_execution() {
  local answer
  read -r -p "Execute this changeset on $STACK in production? [y/N] " answer || answer=""
  [ "$answer" = "y" ]
}

discard_changeset() {
  aws cloudformation delete-change-set --region "$REGION" --change-set-name "$1"
  echo "Not executed; the changeset was deleted."
  exit 1
}

execute_changeset() {
  aws cloudformation execute-change-set --region "$REGION" --change-set-name "$1"
  echo "Waiting for $STACK to finish updating"
  aws cloudformation wait stack-update-complete --region "$REGION" --stack-name "$STACK"
  echo "Deployed"
}

deploy_to_prod_after_confirmation() {
  local output change_set
  output="$(cloudformation_deploy --no-execute-changeset)"
  echo "$output"
  change_set="$(grep -o 'arn:aws:cloudformation:[^ ]*:changeSet/[^ ]*' <<<"$output" || true)"
  if [ -z "$change_set" ]; then
    return 0
  fi
  print_changeset "$change_set"
  if operator_confirms_execution; then
    execute_changeset "$change_set"
  else
    discard_changeset "$change_set"
  fi
}

deploy_to_stage() {
  echo "Deploying $STACK to $STAGE ($AWS_PROFILE)"
  if [ "$STAGE" = prod ]; then
    deploy_to_prod_after_confirmation
  else
    deploy_to_dev
  fi
}

STAGE="${1:-}"
require_stage
load_stage_settings
resolve_stream_arns
run_tests
package_to_deploy_bucket
deploy_to_stage
