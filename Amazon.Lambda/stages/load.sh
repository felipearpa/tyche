case "$1" in
  dev | prod) ;;
  *) return 2 ;;
esac

source "$(dirname "${BASH_SOURCE[0]}")/$1.sh"
export AWS_PROFILE
REGION=us-east-2
STACK=TycheApi

exists_unless() {
  local code="$1" error
  shift
  if error="$("$@" 2>&1 >/dev/null)"; then
    return 0
  fi
  if grep -q "$code" <<<"$error"; then
    return 1
  fi
  echo "$error" >&2
  exit 1
}
