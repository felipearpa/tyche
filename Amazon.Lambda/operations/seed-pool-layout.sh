#!/bin/bash
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
source "$HERE/common.sh"

SCRIPTING_DIR="$(cd "$HERE/../../Scripting" && pwd)"
PYTHON="$SCRIPTING_DIR/.venv/bin/python"
CSV_HEADER="Local Team,Away Team,Date"
LAYOUT_VERSION=1

usage() {
  echo "usage: TYCHE_STAGE=dev|prod $0 \"<name>\" <startDateTime> <matches.csv>" >&2
  echo "  name: 1 to 100 characters; startDateTime: UTC, like 2026-11-20T18:00:00.000Z" >&2
  echo "  matches.csv: header \"$CSV_HEADER\", dates in the same format" >&2
  exit 2
}

require_arguments() {
  [ $# -eq 3 ] || usage
  [ -n "$1" ] && [ "${#1}" -le 100 ] || usage
  [[ "$2" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\.000Z$ ]] || usage
  [ -f "$3" ] || usage
}

require_match_csv() {
  if [ "$(head -n 1 "$CSV" | tr -d '\r')" != "$CSV_HEADER" ]; then
    echo "$CSV must start with the header \"$CSV_HEADER\"" >&2
    exit 2
  fi
}

require_default_layout_table() {
  if [ "$POOL_LAYOUT_TABLE" != PoolLayout ]; then
    echo "upload_matches.py always writes to PoolLayout; unset POOL_LAYOUT_TABLE_NAME." >&2
    exit 2
  fi
}

require_python() {
  if ! "$PYTHON" -c 'import boto3, ulid' 2>/dev/null; then
    echo "Needs $PYTHON with boto3 and ulid-py:" >&2
    echo "  python3 -m venv $SCRIPTING_DIR/.venv && $PYTHON -m pip install -r $SCRIPTING_DIR/requirements.txt" >&2
    exit 1
  fi
}

new_layout_id() {
  "$PYTHON" -c 'import ulid; print(ulid.new().str)'
}

layout_root_item() {
  jq -n --arg id "$LAYOUT_ID" --arg name "$NAME" --arg start "$START" --arg version "$LAYOUT_VERSION" '{
    pk: {S: "POOLLAYOUT#\($id)"},
    sk: {S: "POOLLAYOUT#\($id)"},
    poolLayoutId: {S: $id},
    poolLayoutName: {S: $name},
    poolLayoutVersion: {N: $version},
    startDateTime: {S: $start},
    status: {S: "OPENED"}
  }'
}

put_layout_root() {
  aws dynamodb put-item --region "$REGION" --table-name "$POOL_LAYOUT_TABLE" \
    --item "$(layout_root_item)" --condition-expression "attribute_not_exists(pk)"
  echo "Wrote OPENED layout $LAYOUT_ID ($NAME) to $POOL_LAYOUT_TABLE in $TYCHE_STAGE" >&2
}

upload_matches() {
  echo "Uploading matches for layout $LAYOUT_ID to $POOL_LAYOUT_TABLE in $TYCHE_STAGE" >&2
  "$PYTHON" "$SCRIPTING_DIR/upload_matches.py" --file "$CSV" \
    --poolLayoutId "$LAYOUT_ID" --poolLayoutVersion "$LAYOUT_VERSION" >&2
}

require_arguments "$@"
NAME="$1"
START="$2"
CSV="$3"
require_match_csv
require_default_layout_table
require_python
LAYOUT_ID="$(new_layout_id)"
upload_matches
put_layout_root
echo "$LAYOUT_ID"
