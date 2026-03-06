#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SETUP_SCRIPT="${ROOT_DIR}/script/dev/setup_github_required_checks.sh"

OWNER="${OWNER:-}"
REPO="${REPO:-}"
BRANCH="${BRANCH:-main}"
STRICT="${STRICT:-1}"
ENFORCE_ADMINS="${ENFORCE_ADMINS:-0}"
GITHUB_API="${GITHUB_API:-https://api.github.com}"

usage() {
  cat <<'USAGE'
Usage:
  script/dev/rollback_ops_stageb_required_checks.sh [options]

Options:
  --repo-owner <owner>    GitHub owner（可不传，自动从 origin 解析）
  --repo-name <repo>      GitHub repo（可不传，自动从 origin 解析）
  --branch <branch>       目标分支（默认 main）
  --strict <0|1>          required_status_checks.strict（默认 1）
  --enforce-admins <0|1>  enforce_admins（默认 0）
  --github-api <url>      GitHub API 地址（默认 https://api.github.com）
  -h, --help              Show help
USAGE
}

infer_owner_repo_from_origin() {
  local remote_url
  remote_url="$(git -C "${ROOT_DIR}" config --get remote.origin.url 2>/dev/null || true)"
  if [[ -z "${remote_url}" ]]; then
    return 1
  fi
  if [[ "${remote_url}" =~ ^git@[^:]+:([^/]+)/([^/.]+)(\.git)?$ ]]; then
    OWNER="${BASH_REMATCH[1]}"
    REPO="${BASH_REMATCH[2]}"
    return 0
  fi
  if [[ "${remote_url}" =~ ^https?://[^/]+/([^/]+)/([^/.]+)(\.git)?$ ]]; then
    OWNER="${BASH_REMATCH[1]}"
    REPO="${BASH_REMATCH[2]}"
    return 0
  fi
  return 1
}

print_plan() {
  cat <<'PLAN'
[ops-stageb-checks-rollback] target contexts:
  - hxy-crmeb-compat-regression / regression
  - hxy-pay-notify-pipeline-smoke / smoke
  - hxy-pay-notify-replay-acceptance / replay-acceptance
  - hxy-naming-guard / naming-guard
  - hxy-memory-guard / memory-guard
  - hxy-payment-stagea-p0-17-18 / stagea-p0-17-18
  - hxy-payment-stagea-p0-19-20 / stagea-p0-19-20
  - hxy-payment-stagea-p0-23 / stagea-p0-23

Removed context:
  - hxy-ops-stageb-p1-guard / ops-stageb-p1-guard
PLAN
}

print_protection_summary() {
  local api_path="/repos/${OWNER}/${REPO}/branches/${BRANCH}/protection"
  local github_api_host
  local gh_host="${GH_HOST:-}"
  github_api_host="$(printf '%s' "${GITHUB_API}" | sed -E 's#https?://([^/]+)/?.*#\1#')"
  if [[ -z "${gh_host}" && "${github_api_host}" != "api.github.com" ]]; then
    gh_host="${github_api_host}"
  fi

  if ! command -v gh >/dev/null 2>&1; then
    echo "[ops-stageb-checks-rollback] summary=SKIP (gh not found)"
    return 0
  fi

  local -a gh_cmd=(gh api --method GET "${api_path}" -H "Accept: application/vnd.github+json" -H "X-GitHub-Api-Version: 2022-11-28")
  if [[ -n "${gh_host}" ]]; then
    gh_cmd=(env GH_HOST="${gh_host}" "${gh_cmd[@]}")
  fi

  echo "[ops-stageb-checks-rollback] branch_protection_summary:"
  set +e
  "${gh_cmd[@]}" --jq '.required_status_checks.contexts[]' | sed 's/^/  - /'
  local rc=$?
  set -e
  if [[ "${rc}" != "0" ]]; then
    echo "[ops-stageb-checks-rollback] summary=FAIL (cannot read branch protection)" >&2
    return 1
  fi
  echo "[ops-stageb-checks-rollback] summary=PASS"
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
    --branch)
      BRANCH="$2"
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
  if ! infer_owner_repo_from_origin; then
    echo "Missing owner/repo and failed to infer from git remote origin" >&2
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

print_plan
echo "[ops-stageb-checks-rollback] repo=${OWNER}/${REPO} branch=${BRANCH}"
echo "[ops-stageb-checks-rollback] step=dry-run"
bash "${SETUP_SCRIPT}" \
  --repo-owner "${OWNER}" \
  --repo-name "${REPO}" \
  --branch "${BRANCH}" \
  --strict "${STRICT}" \
  --enforce-admins "${ENFORCE_ADMINS}" \
  --github-api "${GITHUB_API}" \
  --include-stagea-checks 1 \
  --include-ops-stageb-checks 0 \
  --dry-run

echo "[ops-stageb-checks-rollback] step=apply"
bash "${SETUP_SCRIPT}" \
  --repo-owner "${OWNER}" \
  --repo-name "${REPO}" \
  --branch "${BRANCH}" \
  --strict "${STRICT}" \
  --enforce-admins "${ENFORCE_ADMINS}" \
  --github-api "${GITHUB_API}" \
  --include-stagea-checks 1 \
  --include-ops-stageb-checks 0 \
  --apply

print_protection_summary

