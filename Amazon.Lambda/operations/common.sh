# Shared settings for the operator scripts. Override with environment variables to target the
# isolated verification resources.
REGION="${AWS_REGION:-us-east-2}"
STACK="${TYCHE_STACK:-TycheApi}"
POOL_TABLE="${POOL_TABLE_NAME:-Pool}"
POOL_LAYOUT_TABLE="${POOL_LAYOUT_TABLE_NAME:-PoolLayout}"

stack_output() {
  aws cloudformation describe-stacks --region "$REGION" --stack-name "$STACK" \
    --query "Stacks[0].Outputs[?OutputKey=='$1'].OutputValue" --output text
}

command_queue_url() { echo "${POOL_COMMAND_QUEUE_URL:-$(stack_output PoolCommandQueueUrl)}"; }
maintenance_queue_url() { echo "${POOL_MAINTENANCE_QUEUE_URL:-$(stack_output PoolMaintenanceQueueUrl)}"; }

# Distinct pool IDs that have a detail for the match, from every page of the match index.
pools_with_match() {
  aws dynamodb query --region "$REGION" --table-name "$POOL_TABLE" \
    --index-name GetPoolGamblerScoresByMatch-index \
    --key-condition-expression "getPoolGamblerScoresByMatchPk = :pk" \
    --expression-attribute-values "{\":pk\":{\"S\":\"MATCH#$1\"}}" \
    --projection-expression getPoolGamblerScoresByMatchSk --output json \
    | jq -r '.Items[].getPoolGamblerScoresByMatchSk.S | split("#")[1]' | sort -u
}
