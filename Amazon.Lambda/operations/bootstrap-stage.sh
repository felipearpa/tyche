#!/bin/bash
set -euo pipefail

REGION=us-east-2
TOKEN_PARAMETER=/tyche/football-data/token
PARENT_DOMAIN=felipearpa.com
HOSTED_ZONE_ID=Z09565921NSDP1O1BZLH3
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

api_domain() {
  echo "tyche-api-$STAGE.$PARENT_DOMAIN"
}

existing_certificate_arn() {
  aws acm list-certificates --region "$REGION" --certificate-statuses PENDING_VALIDATION ISSUED \
    --query "CertificateSummaryList[?DomainName=='$(api_domain)'].CertificateArn | [0]" --output text
}

request_certificate() {
  aws acm request-certificate --region "$REGION" --domain-name "$(api_domain)" \
    --validation-method DNS --idempotency-token "tycheapi$STAGE" \
    --query CertificateArn --output text
}

validation_record_field() {
  aws acm describe-certificate --region "$REGION" --certificate-arn "$CERTIFICATE_ARN" \
    --query "Certificate.DomainValidationOptions[0].ResourceRecord.$1" --output text
}

wait_for_validation_record() {
  for _ in $(seq 30); do
    VALIDATION_NAME="$(validation_record_field Name)"
    VALIDATION_VALUE="$(validation_record_field Value)"
    [ "$VALIDATION_NAME" != None ] && return
    sleep 2
  done
  echo "ACM did not publish a validation record for $CERTIFICATE_ARN" >&2
  exit 1
}

current_record() {
  aws route53 list-resource-record-sets --profile "$PROD_PROFILE" --hosted-zone-id "$HOSTED_ZONE_ID" \
    --start-record-name "$1" --start-record-type "$2" --max-items 1 \
    --query "ResourceRecordSets[?Name=='$1' && Type=='$2'] | [0].$3" --output text
}

upsert_record() {
  local change_id
  change_id="$(aws route53 change-resource-record-sets --profile "$PROD_PROFILE" \
    --hosted-zone-id "$HOSTED_ZONE_ID" --change-batch "$1" --query ChangeInfo.Id --output text)"
  aws route53 wait resource-record-sets-changed --profile "$PROD_PROFILE" --id "$change_id"
}

validation_change_batch() {
  printf '{"Changes":[{"Action":"UPSERT","ResourceRecordSet":{"Name":"%s","Type":"CNAME","TTL":300,"ResourceRecords":[{"Value":"%s"}]}}]}' \
    "$VALIDATION_NAME" "$VALIDATION_VALUE"
}

ensure_validation_record() {
  wait_for_validation_record
  local current
  current="$(current_record "$VALIDATION_NAME" CNAME 'ResourceRecords[0].Value')"
  if [ "${current%.}" = "${VALIDATION_VALUE%.}" ]; then
    echo "Validation record $VALIDATION_NAME already exists"
    return
  fi
  upsert_record "$(validation_change_batch)"
  echo "Wrote validation record $VALIDATION_NAME in $PARENT_DOMAIN"
}

ensure_certificate() {
  CERTIFICATE_ARN="$(existing_certificate_arn)"
  if [ "$CERTIFICATE_ARN" = None ]; then
    CERTIFICATE_ARN="$(request_certificate)"
    echo "Requested certificate for $(api_domain)"
  else
    echo "Certificate for $(api_domain) already exists"
  fi
  ensure_validation_record
  aws acm wait certificate-validated --region "$REGION" --certificate-arn "$CERTIFICATE_ARN"
  echo "Certificate for $(api_domain) is issued"
}

custom_domain_exists() {
  local error
  if error="$(aws apigatewayv2 get-domain-name --region "$REGION" --domain-name "$(api_domain)" 2>&1 >/dev/null)"; then
    return 0
  fi
  if ! grep -q NotFoundException <<<"$error"; then
    echo "$error" >&2
    exit 1
  fi
  return 1
}

ensure_custom_domain() {
  if custom_domain_exists; then
    echo "Custom domain $(api_domain) already exists"
    return
  fi
  aws apigatewayv2 create-domain-name --region "$REGION" --domain-name "$(api_domain)" \
    --domain-name-configurations "CertificateArn=$CERTIFICATE_ARN,EndpointType=REGIONAL,SecurityPolicy=TLS_1_2" \
    --query DomainName --output text >/dev/null
  echo "Created custom domain $(api_domain)"
}

stack_api_id() {
  aws cloudformation describe-stack-resource --region "$REGION" --stack-name TycheApi \
    --logical-resource-id AuthGatewayHttpApi --query StackResourceDetail.PhysicalResourceId --output text
}

root_mapping_field() {
  aws apigatewayv2 get-api-mappings --region "$REGION" --domain-name "$(api_domain)" \
    --query "Items[?ApiMappingKey==''] | [0].$1" --output text
}

ensure_api_mapping() {
  local api_id mapped_api_id
  api_id="$(stack_api_id)"
  mapped_api_id="$(root_mapping_field ApiId)"
  if [ "$mapped_api_id" = "$api_id" ]; then
    echo "$(api_domain) already maps to API $api_id"
    return
  fi
  if [ "$mapped_api_id" = None ]; then
    aws apigatewayv2 create-api-mapping --region "$REGION" --domain-name "$(api_domain)" \
      --api-id "$api_id" --stage '$default' --query ApiMappingId --output text >/dev/null
  else
    aws apigatewayv2 update-api-mapping --region "$REGION" --domain-name "$(api_domain)" \
      --api-mapping-id "$(root_mapping_field ApiMappingId)" --api-id "$api_id" --stage '$default' \
      --query ApiMappingId --output text >/dev/null
  fi
  echo "Mapped $(api_domain) to API $api_id"
}

custom_domain_field() {
  aws apigatewayv2 get-domain-name --region "$REGION" --domain-name "$(api_domain)" \
    --query "DomainNameConfigurations[0].$1" --output text
}

alias_change_batch() {
  printf '{"Changes":[{"Action":"UPSERT","ResourceRecordSet":{"Name":"%s","Type":"A","AliasTarget":{"HostedZoneId":"%s","DNSName":"%s","EvaluateTargetHealth":false}}}]}' \
    "$(api_domain)." "$1" "$2"
}

ensure_alias_record() {
  local target_zone target_name
  target_zone="$(custom_domain_field HostedZoneId)"
  target_name="$(custom_domain_field ApiGatewayDomainName)"
  if [ "$(current_record "$(api_domain)." A AliasTarget.DNSName)" = "$target_name." ]; then
    echo "Alias record $(api_domain) already exists"
    return
  fi
  upsert_record "$(alias_change_batch "$target_zone" "$target_name")"
  echo "Wrote alias record $(api_domain) in $PARENT_DOMAIN"
}

print_api_url() {
  echo "TycheApi in $STAGE answers at https://$(api_domain)/"
}

STAGE="${1:-}"
require_stage
load_stage_settings
refuse_production_account
ensure_deploy_bucket
ensure_tables
ensure_football_data_token
deploy_stack
ensure_certificate
ensure_custom_domain
ensure_api_mapping
ensure_alias_record
print_api_url
