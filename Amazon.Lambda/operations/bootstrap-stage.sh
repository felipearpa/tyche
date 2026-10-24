#!/bin/bash
set -euo pipefail

REGION=us-east-2
TOKEN_PARAMETER=/tyche/football-data/token
LAMBDA_DIR="$(cd "$(dirname "$0")/.." && pwd)"

usage() {
  echo "usage: $0 <stage>   (any stage with a stages/<stage>.sh file except prod)" >&2
  exit 2
}

refuse_prod_stage() {
  if [ "$STAGE" = prod ]; then
    echo "bootstrap-stage.sh never runs against prod; production is managed by hand." >&2
    exit 2
  fi
}

require_stage() {
  [ -n "$STAGE" ] || usage
  refuse_prod_stage
  [ -f "$LAMBDA_DIR/stages/$STAGE.sh" ] || usage
}

prod_profile() {
  (source "$LAMBDA_DIR/stages/prod.sh" && echo "$AWS_PROFILE")
}

load_stage_settings() {
  PROD_PROFILE="$(prod_profile)"
  source "$LAMBDA_DIR/stages/$STAGE.sh"
  export AWS_PROFILE
}

account_of() {
  aws sts get-caller-identity --profile "$1" --query Account --output text
}

refuse_production_account() {
  local stage_account
  stage_account="$(account_of "$AWS_PROFILE")"
  if [ "$stage_account" = "$(account_of "$PROD_PROFILE")" ]; then
    echo "Profile $AWS_PROFILE resolves to the production account; refusing." >&2
    exit 1
  fi
  echo "Bootstrapping $STAGE in account $stage_account ($AWS_PROFILE)"
}

ensure_deploy_bucket() {
  if aws s3api head-bucket --region "$REGION" --bucket "$DEPLOY_BUCKET" 2>/dev/null; then
    echo "Deploy bucket $DEPLOY_BUCKET already exists"
    return
  fi
  aws s3api create-bucket --region "$REGION" --bucket "$DEPLOY_BUCKET" \
    --create-bucket-configuration "LocationConstraint=$REGION" --query Location --output text
}

ensure_tables() {
  "$LAMBDA_DIR/operations/pool-table.sh" create-all "$AWS_PROFILE"
}

stage_has_football_data_token() {
  local error
  if error="$(aws ssm get-parameter --region "$REGION" --name "$TOKEN_PARAMETER" 2>&1 >/dev/null)"; then
    return 0
  fi
  if ! grep -q ParameterNotFound <<<"$error"; then
    echo "$error" >&2
    exit 1
  fi
  return 1
}

pipe_football_data_token_from_prod_to_stage() {
  aws ssm get-parameter --profile "$PROD_PROFILE" --region "$REGION" --name "$TOKEN_PARAMETER" \
    --with-decryption --query Parameter.Value --output text \
    | tr -d '\r\n' \
    | aws ssm put-parameter --region "$REGION" --name "$TOKEN_PARAMETER" --type SecureString \
      --value file:///dev/stdin --query Version --output text >/dev/null
}

ensure_football_data_token() {
  if stage_has_football_data_token; then
    echo "$TOKEN_PARAMETER already exists"
    return
  fi
  pipe_football_data_token_from_prod_to_stage
  echo "Copied $TOKEN_PARAMETER from production"
}

deploy_stack() {
  "$LAMBDA_DIR/deploy-lambda.sh" "$STAGE"
}

print_api_url() {
  local url
  url="$(aws cloudformation describe-stacks --region "$REGION" --stack-name TycheApi \
    --query "Stacks[0].Outputs[?OutputKey=='ApiURL'].OutputValue" --output text)"
  echo "TycheApi in $STAGE answers at $url"
}

STAGE="${1:-}"
require_stage
load_stage_settings
refuse_production_account
ensure_deploy_bucket
ensure_tables
ensure_football_data_token
deploy_stack
print_api_url
