#!/usr/bin/env bash
set -euo pipefail

OWNER="${OWNER:-}"
REPO="${REPO:-}"
BRANCH="${BRANCH:-main}"
STRICT="${STRICT:-1}"
ENFORCE_ADMINS="${ENFORCE_ADMINS:-0}"
DRY_RUN="${DRY_RUN:-1}"
GITHUB_API="${GITHUB_API:-https://api.github.com}"

CONTEXTS=()

usage() {
  cat <<'USAGE'
Usage:
  script/dev/setup_github_required_checks.sh [options]

Options:
  --owner <owner>            GitHub owner（可用 OWNER 环境变量）
  --repo <repo>              GitHub repo（可用 REPO 环境变量）
  --branch <branch>          分支名（默认 main）
  --context <name>           Required check context（可重复）
  --strict <0|1>             是否要求分支与目标分支保持最新（默认 1）
  --enforce-admins <0|1>     是否对管理员也生效（默认 0）
  --dry-run <0|1>            是否仅打印 payload 不提交（默认 1）
  --github-api <url>         GitHub API 地址（默认 https://api.github.com）
  -h, --help                 Show help

Env:
  GITHUB_TOKEN               需要 repo admin 权限；dry-run=1 时可不提供

Default contexts (if none provided):
  - crmeb-compat-regression / regression
  - pay-notify-pipeline-smoke / smoke
  - pay-notify-replay-acceptance / replay-acceptance
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --owner)
      OWNER="$2"
      shift 2
      ;;
    --repo)
      REPO="$2"
      shift 2
      ;;
    --branch)
      BRANCH="$2"
      shift 2
      ;;
    --context)
      CONTEXTS+=("$2")
      shift 2
      ;;
    --strict)
      STRICT="$2"
      shift 2
      ;;
    --enforce-admins)
      ENFORCE_ADMINS="$2"
      shift 2
      ;;
    --dry-run)
      DRY_RUN="$2"
      shift 2
      ;;
    --github-api)
      GITHUB_API="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage
      exit 1
      ;;
  esac
done

if [[ -z "${OWNER}" || -z "${REPO}" ]]; then
  echo "Missing --owner/--repo (or OWNER/REPO env)" >&2
  exit 1
fi
if ! [[ "${STRICT}" =~ ^[01]$ ]]; then
  echo "Invalid --strict: ${STRICT}" >&2
  exit 1
fi
if ! [[ "${ENFORCE_ADMINS}" =~ ^[01]$ ]]; then
  echo "Invalid --enforce-admins: ${ENFORCE_ADMINS}" >&2
  exit 1
fi
if ! [[ "${DRY_RUN}" =~ ^[01]$ ]]; then
  echo "Invalid --dry-run: ${DRY_RUN}" >&2
  exit 1
fi

if [[ ${#CONTEXTS[@]} -eq 0 ]]; then
  CONTEXTS=(
    "crmeb-compat-regression / regression"
    "pay-notify-pipeline-smoke / smoke"
    "pay-notify-replay-acceptance / replay-acceptance"
  )
fi

json_escape() {
  printf '%s' "$1" | sed 's/\\/\\\\/g; s/"/\\"/g'
}

contexts_json_items=""
for ctx in "${CONTEXTS[@]}"; do
  esc_ctx="$(json_escape "${ctx}")"
  if [[ -n "${contexts_json_items}" ]]; then
    contexts_json_items+=", "
  fi
  contexts_json_items+="\"${esc_ctx}\""
done
contexts_json="[${contexts_json_items}]"

strict_bool="false"
if [[ "${STRICT}" == "1" ]]; then
  strict_bool="true"
fi
enforce_admins_bool="false"
if [[ "${ENFORCE_ADMINS}" == "1" ]]; then
  enforce_admins_bool="true"
fi

payload_file="$(mktemp)"
cleanup() {
  rm -f "${payload_file}" 2>/dev/null || true
}
trap cleanup EXIT

cat > "${payload_file}" <<EOF
{
  "required_status_checks": {
    "strict": ${strict_bool},
    "contexts": ${contexts_json}
  },
  "enforce_admins": ${enforce_admins_bool},
  "required_pull_request_reviews": null,
  "restrictions": null
}
EOF

api_url="${GITHUB_API}/repos/${OWNER}/${REPO}/branches/${BRANCH}/protection"

echo "[github-required-checks] owner=${OWNER}"
echo "[github-required-checks] repo=${REPO}"
echo "[github-required-checks] branch=${BRANCH}"
echo "[github-required-checks] strict=${STRICT}"
echo "[github-required-checks] enforce_admins=${ENFORCE_ADMINS}"
echo "[github-required-checks] contexts_count=${#CONTEXTS[@]}"
for ctx in "${CONTEXTS[@]}"; do
  echo "[github-required-checks] context=${ctx}"
done
echo "[github-required-checks] api_url=${api_url}"

if [[ "${DRY_RUN}" == "1" ]]; then
  echo "[github-required-checks] dry-run=1 (skip request)"
  echo "[github-required-checks] payload:"
  cat "${payload_file}"
  exit 0
fi

if [[ -z "${GITHUB_TOKEN:-}" ]]; then
  echo "Missing GITHUB_TOKEN (required when --dry-run=0)" >&2
  exit 1
fi

resp_file="$(mktemp)"
trap 'cleanup; rm -f "${resp_file}" 2>/dev/null || true' EXIT

http_code="$(
  curl -sS -o "${resp_file}" -w "%{http_code}" \
    -X PUT "${api_url}" \
    -H "Authorization: Bearer ${GITHUB_TOKEN}" \
    -H "Accept: application/vnd.github+json" \
    -H "X-GitHub-Api-Version: 2022-11-28" \
    --data-binary "@${payload_file}"
)"

if [[ "${http_code}" =~ ^2 ]]; then
  echo "[github-required-checks] success http=${http_code}"
  exit 0
fi

echo "[github-required-checks] failed http=${http_code}" >&2
cat "${resp_file}" >&2
exit 1
