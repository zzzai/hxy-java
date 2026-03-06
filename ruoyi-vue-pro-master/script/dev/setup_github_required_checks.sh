#!/usr/bin/env bash
set -euo pipefail

OWNER="${OWNER:-}"
REPO="${REPO:-}"
BRANCH="${BRANCH:-main}"
STRICT="${STRICT:-1}"
ENFORCE_ADMINS="${ENFORCE_ADMINS:-0}"
DRY_RUN="${DRY_RUN:-1}"
GITHUB_API="${GITHUB_API:-https://api.github.com}"
INCLUDE_STAGEA_CHECKS="${INCLUDE_STAGEA_CHECKS:-0}"
INCLUDE_OPS_STAGEB_CHECKS="${INCLUDE_OPS_STAGEB_CHECKS:-0}"
ENABLE_OPS_STAGEB_P1="${ENABLE_OPS_STAGEB_P1:-0}"

CONTEXTS=()

usage() {
  cat <<'USAGE'
Usage:
  script/dev/setup_github_required_checks.sh [options]

Options:
  --repo-owner <owner>       GitHub owner（推荐；可用 OWNER 环境变量）
  --repo-name <repo>         GitHub repo（推荐；可用 REPO 环境变量）
  --owner <owner>            GitHub owner（兼容旧参数）
  --repo <repo>              GitHub repo（兼容旧参数）
  --branch <branch>          分支名（默认 main）
  --context <name>           Required check context（可重复）
  --strict <0|1>             是否要求分支与目标分支保持最新（默认 1）
  --enforce-admins <0|1>     是否对管理员也生效（默认 0）
  --dry-run [0|1]            仅打印 payload 与 gh 命令（默认 1；支持仅写 --dry-run）
  --apply                    真正提交分支保护（等价 --dry-run 0）
  --github-api <url>         GitHub API 地址（默认 https://api.github.com）
  --include-stagea-checks <0|1> 是否附加 stageA #17/#18、#19/#20、P0-23（默认 0）
  --include-ops-stageb-checks <0|1> 是否附加 ops-stageb-p1 门禁（默认 0）
  --enable-ops-stageb-p1     快捷开关：等价 include ops-stageb-p1 checks
  -h, --help                 Show help

Env:
  GITHUB_TOKEN               需要 repo admin 权限；dry-run=1 时可不提供
  OWNER/REPO 未传时，脚本会尝试从 git remote origin 自动解析

Default contexts (if none provided):
  - hxy-crmeb-compat-regression / regression
  - hxy-pay-notify-pipeline-smoke / smoke
  - hxy-pay-notify-replay-acceptance / replay-acceptance
  - hxy-naming-guard / naming-guard
  - hxy-memory-guard / memory-guard
USAGE
}

infer_owner_repo_from_origin() {
  local remote_url
  remote_url="$(git config --get remote.origin.url 2>/dev/null || true)"
  if [[ -z "${remote_url}" ]]; then
    return 1
  fi

  # git@github.com:owner/repo.git
  if [[ "${remote_url}" =~ ^git@[^:]+:([^/]+)/([^/.]+)(\.git)?$ ]]; then
    OWNER="${BASH_REMATCH[1]}"
    REPO="${BASH_REMATCH[2]}"
    return 0
  fi

  # https://github.com/owner/repo.git
  if [[ "${remote_url}" =~ ^https?://[^/]+/([^/]+)/([^/.]+)(\.git)?$ ]]; then
    OWNER="${BASH_REMATCH[1]}"
    REPO="${BASH_REMATCH[2]}"
    return 0
  fi

  return 1
}

dedupe_contexts() {
  declare -A seen=()
  local dedup=()
  local ctx
  for ctx in "${CONTEXTS[@]}"; do
    if [[ -n "${seen["${ctx}"]+x}" ]]; then
      continue
    fi
    seen["${ctx}"]=1
    dedup+=("${ctx}")
  done
  CONTEXTS=("${dedup[@]}")
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --repo-owner)
      OWNER="$2"
      shift 2
      ;;
    --repo-name)
      REPO="$2"
      shift 2
      ;;
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
      if [[ $# -ge 2 && "${2}" != --* ]]; then
        DRY_RUN="$2"
        shift 2
      else
        DRY_RUN=1
        shift
      fi
      ;;
    --apply)
      DRY_RUN=0
      shift
      ;;
    --github-api)
      GITHUB_API="$2"
      shift 2
      ;;
    --include-stagea-checks)
      INCLUDE_STAGEA_CHECKS="$2"
      shift 2
      ;;
    --include-ops-stageb-checks)
      INCLUDE_OPS_STAGEB_CHECKS="$2"
      shift 2
      ;;
    --enable-ops-stageb-p1)
      ENABLE_OPS_STAGEB_P1=1
      shift
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
  if ! infer_owner_repo_from_origin; then
    echo "Missing --owner/--repo (or OWNER/REPO env), and failed to infer from git remote origin" >&2
    exit 1
  fi
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
if ! [[ "${INCLUDE_STAGEA_CHECKS}" =~ ^[01]$ ]]; then
  echo "Invalid --include-stagea-checks: ${INCLUDE_STAGEA_CHECKS}" >&2
  exit 1
fi
if ! [[ "${INCLUDE_OPS_STAGEB_CHECKS}" =~ ^[01]$ ]]; then
  echo "Invalid --include-ops-stageb-checks: ${INCLUDE_OPS_STAGEB_CHECKS}" >&2
  exit 1
fi
if ! [[ "${ENABLE_OPS_STAGEB_P1}" =~ ^[01]$ ]]; then
  echo "Invalid --enable-ops-stageb-p1/ENABLE_OPS_STAGEB_P1: ${ENABLE_OPS_STAGEB_P1}" >&2
  exit 1
fi
if [[ "${ENABLE_OPS_STAGEB_P1}" == "1" ]]; then
  INCLUDE_OPS_STAGEB_CHECKS=1
fi

if [[ ${#CONTEXTS[@]} -eq 0 ]]; then
  CONTEXTS=(
    "hxy-crmeb-compat-regression / regression"
    "hxy-pay-notify-pipeline-smoke / smoke"
    "hxy-pay-notify-replay-acceptance / replay-acceptance"
    "hxy-naming-guard / naming-guard"
    "hxy-memory-guard / memory-guard"
  )
fi

if [[ "${INCLUDE_STAGEA_CHECKS}" == "1" ]]; then
  CONTEXTS+=(
    "hxy-payment-stagea-p0-17-18 / stagea-p0-17-18"
    "hxy-payment-stagea-p0-19-20 / stagea-p0-19-20"
    "hxy-payment-stagea-p0-23 / stagea-p0-23"
  )
fi

if [[ "${INCLUDE_OPS_STAGEB_CHECKS}" == "1" ]]; then
  CONTEXTS+=(
    "hxy-ops-stageb-p1-guard / ops-stageb-p1-guard"
  )
fi

dedupe_contexts

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
payload_cmd_file=""
gh_out_file=""
gh_err_file=""
cleanup() {
  rm -f "${payload_file}" "${gh_out_file}" "${gh_err_file}" 2>/dev/null || true
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
api_path="/repos/${OWNER}/${REPO}/branches/${BRANCH}/protection"
github_api_host="$(printf '%s' "${GITHUB_API}" | sed -E 's#https?://([^/]+)/?.*#\1#')"
gh_host="${GH_HOST:-}"
if [[ -z "${gh_host}" && "${github_api_host}" != "api.github.com" ]]; then
  gh_host="${github_api_host}"
fi
payload_cmd_file="/tmp/github_required_checks_${OWNER}_${REPO}_${BRANCH//\//_}.json"
cp "${payload_file}" "${payload_cmd_file}"

if [[ -n "${gh_host}" ]]; then
  gh_dry_run_cmd="GH_HOST=${gh_host} gh api --method GET \"${api_path}\" -H \"Accept: application/vnd.github+json\" -H \"X-GitHub-Api-Version: 2022-11-28\""
  gh_apply_cmd="GH_HOST=${gh_host} gh api --method PUT \"${api_path}\" -H \"Accept: application/vnd.github+json\" -H \"X-GitHub-Api-Version: 2022-11-28\" --input \"${payload_cmd_file}\""
else
  gh_dry_run_cmd="gh api --method GET \"${api_path}\" -H \"Accept: application/vnd.github+json\" -H \"X-GitHub-Api-Version: 2022-11-28\""
  gh_apply_cmd="gh api --method PUT \"${api_path}\" -H \"Accept: application/vnd.github+json\" -H \"X-GitHub-Api-Version: 2022-11-28\" --input \"${payload_cmd_file}\""
fi

echo "[github-required-checks] owner=${OWNER}"
echo "[github-required-checks] repo=${REPO}"
echo "[github-required-checks] branch=${BRANCH}"
echo "[github-required-checks] strict=${STRICT}"
echo "[github-required-checks] enforce_admins=${ENFORCE_ADMINS}"
echo "[github-required-checks] include_stagea_checks=${INCLUDE_STAGEA_CHECKS}"
echo "[github-required-checks] include_ops_stageb_checks=${INCLUDE_OPS_STAGEB_CHECKS}"
echo "[github-required-checks] enable_ops_stageb_p1=${ENABLE_OPS_STAGEB_P1}"
if [[ "${DRY_RUN}" == "1" ]]; then
  echo "[github-required-checks] mode=dry-run"
else
  echo "[github-required-checks] mode=apply"
fi
if [[ "${INCLUDE_STAGEA_CHECKS}" == "1" && "${INCLUDE_OPS_STAGEB_CHECKS}" == "1" ]]; then
  echo "[github-required-checks] profile=stagea+stageb"
elif [[ "${INCLUDE_STAGEA_CHECKS}" == "1" ]]; then
  echo "[github-required-checks] profile=stagea-only"
elif [[ "${INCLUDE_OPS_STAGEB_CHECKS}" == "1" ]]; then
  echo "[github-required-checks] profile=stageb-only"
else
  echo "[github-required-checks] profile=base-only"
fi
if [[ "${INCLUDE_OPS_STAGEB_CHECKS}" == "1" ]]; then
  echo "[github-required-checks] stageb_guard_scope=stock,lifecycle,booking-refund-notify,booking-refund-audit,four-account-audit-regression"
  echo "[github-required-checks] stageb_guard_context_unchanged=hxy-ops-stageb-p1-guard / ops-stageb-p1-guard"
fi
echo "[github-required-checks] contexts_count=${#CONTEXTS[@]}"
for ctx in "${CONTEXTS[@]}"; do
  echo "[github-required-checks] context=${ctx}"
done
echo "[github-required-checks] api_url=${api_url}"
echo "[github-required-checks] payload_cmd_file=${payload_cmd_file}"
echo "[github-required-checks] gh_dry_run_cmd=${gh_dry_run_cmd}"
echo "[github-required-checks] gh_apply_cmd=${gh_apply_cmd}"
echo "[github-required-checks] helper_setup_dry_run_cmd=bash ruoyi-vue-pro-master/script/dev/setup_github_required_checks.sh --repo-owner ${OWNER} --repo-name ${REPO} --branch ${BRANCH} --include-stagea-checks ${INCLUDE_STAGEA_CHECKS} --include-ops-stageb-checks ${INCLUDE_OPS_STAGEB_CHECKS} --dry-run 1"
echo "[github-required-checks] helper_setup_apply_cmd=bash ruoyi-vue-pro-master/script/dev/setup_github_required_checks.sh --repo-owner ${OWNER} --repo-name ${REPO} --branch ${BRANCH} --include-stagea-checks ${INCLUDE_STAGEA_CHECKS} --include-ops-stageb-checks ${INCLUDE_OPS_STAGEB_CHECKS} --apply"
echo "[github-required-checks] helper_rollback_cmd=bash ruoyi-vue-pro-master/script/dev/rollback_ops_stageb_required_checks.sh --repo-owner ${OWNER} --repo-name ${REPO} --branch ${BRANCH}"

if [[ "${DRY_RUN}" == "1" ]]; then
  echo "[github-required-checks] dry-run=1 (skip request)"
  echo "[github-required-checks] payload:"
  cat "${payload_file}"
  echo "[github-required-checks] manual apply command:"
  echo "  ${gh_apply_cmd}"
  exit 0
fi

if command -v gh >/dev/null 2>&1; then
  if [[ -n "${GITHUB_TOKEN:-}" && -z "${GH_TOKEN:-}" ]]; then
    export GH_TOKEN="${GITHUB_TOKEN}"
  fi
  gh_out_file="$(mktemp)"
  gh_err_file="$(mktemp)"
  set +e
  eval "${gh_apply_cmd}" >"${gh_out_file}" 2>"${gh_err_file}"
  gh_rc=$?
  set -e
  if [[ "${gh_rc}" == "0" ]]; then
    echo "[github-required-checks] success via gh api"
    exit 0
  fi
  echo "[github-required-checks] failed via gh api, rc=${gh_rc}" >&2
  cat "${gh_err_file}" >&2 || true
  exit 1
fi

if [[ -z "${GITHUB_TOKEN:-}" ]]; then
  echo "Missing GITHUB_TOKEN (required when --dry-run=0 and gh is unavailable)" >&2
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
