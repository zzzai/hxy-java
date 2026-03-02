#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(git -C "${SCRIPT_DIR}" rev-parse --show-toplevel)"
CHECK_STAGED="${CHECK_STAGED:-1}"
CHECK_UNSTAGED="${CHECK_UNSTAGED:-0}"
CHECK_UNTRACKED="${CHECK_UNTRACKED:-0}"
GIT_DIFF_RANGE="${GIT_DIFF_RANGE:-}"

CORE_MEMORY_FILES=(
  "hxy/00_governance/HXY-项目事实基线-v1-2026-03-01.md"
  "hxy/00_governance/HXY-架构决策记录-ADR-v1.md"
  "hxy/06_roadmap/HXY-执行状态看板-v1-2026-03-01.md"
)
MEMORY_INDEX_FILE="hxy/99_index/memory-index.yaml"

usage() {
  cat <<'USAGE'
Usage:
  ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh

Environment:
  CHECK_STAGED=<0|1>            check staged files (default: 1)
  CHECK_UNSTAGED=<0|1>          check unstaged files (default: 0)
  CHECK_UNTRACKED=<0|1>         check untracked files (default: 0)
  GIT_DIFF_RANGE=<base...head>  include files from a git diff range

Exit Code:
  0 PASS
  2 BLOCK (memory update required)
  1 script/config error
USAGE
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

for flag in "${CHECK_STAGED}" "${CHECK_UNSTAGED}" "${CHECK_UNTRACKED}"; do
  if ! [[ "${flag}" =~ ^[01]$ ]]; then
    echo "[hxy-memory-guard][FAIL] invalid check flag: ${flag}" >&2
    exit 1
  fi
done

if ! command -v git >/dev/null 2>&1; then
  echo "[hxy-memory-guard][FAIL] missing command: git" >&2
  exit 1
fi

for file in "${CORE_MEMORY_FILES[@]}" "${MEMORY_INDEX_FILE}"; do
  if [[ ! -f "${REPO_ROOT}/${file}" ]]; then
    echo "[hxy-memory-guard][FAIL] required memory file missing: ${file}" >&2
    exit 1
  fi
done

declare -a CANDIDATES=()

collect_name_status() {
  local mode="$1"
  case "${mode}" in
    staged)
      git -c core.quotepath=false -C "${REPO_ROOT}" diff --cached --name-status --diff-filter=ACMR
      ;;
    unstaged)
      git -c core.quotepath=false -C "${REPO_ROOT}" diff --name-status --diff-filter=ACMR
      ;;
    range)
      git -c core.quotepath=false -C "${REPO_ROOT}" diff --name-status --diff-filter=ACMR "${GIT_DIFF_RANGE}"
      ;;
  esac
}

append_candidates() {
  while IFS=$'\t' read -r status col2 col3; do
    [[ -z "${status:-}" ]] && continue
    case "${status}" in
      A|M) CANDIDATES+=("${col2}") ;;
      R*) CANDIDATES+=("${col3}") ;;
      C*) CANDIDATES+=("${col3}") ;;
    esac
  done
}

if [[ -n "${GIT_DIFF_RANGE}" ]]; then
  append_candidates < <(collect_name_status range)
fi
if [[ "${CHECK_STAGED}" == "1" ]]; then
  append_candidates < <(collect_name_status staged)
fi
if [[ "${CHECK_UNSTAGED}" == "1" ]]; then
  append_candidates < <(collect_name_status unstaged)
fi
if [[ "${CHECK_UNTRACKED}" == "1" ]]; then
  while IFS= read -r path; do
    [[ -z "${path}" ]] && continue
    CANDIDATES+=("${path}")
  done < <(git -c core.quotepath=false -C "${REPO_ROOT}" ls-files --others --exclude-standard)
fi

if [[ "${#CANDIDATES[@]}" -eq 0 ]]; then
  echo "[hxy-memory-guard] result=PASS checked_files=0"
  exit 0
fi

mapfile -t UNIQUE_CANDIDATES < <(printf '%s\n' "${CANDIDATES[@]}" | sed '/^\s*$/d' | sort -u)

declare -A CANDIDATE_SET=()
for path in "${UNIQUE_CANDIDATES[@]}"; do
  CANDIDATE_SET["${path}"]=1
done

detect_domain() {
  local path="$1"
  case "${path}" in
    ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/*) echo "product" ;;
    ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/*) echo "trade" ;;
    ruoyi-vue-pro-master/yudao-module-pay/*) echo "pay" ;;
    ruoyi-vue-pro-master/sql/mysql/hxy/*) echo "sql" ;;
    yudao-mall-uniapp/*) echo "uniapp" ;;
    ruoyi-vue-pro-master/yudao-ui-admin/*|\
    ruoyi-vue-pro-master/yudao-ui-admin-vue3/*|\
    ruoyi-vue-pro-master/yudao-ui-admin-vben/*|\
    ruoyi-vue-pro-master/yudao-ui-admin-*/*) echo "admin" ;;
    *) echo "" ;;
  esac
}

declare -A CORE_DOMAIN_SET=()
declare -a CORE_DOMAINS=()
for path in "${UNIQUE_CANDIDATES[@]}"; do
  domain="$(detect_domain "${path}")"
  if [[ -z "${domain}" ]]; then
    continue
  fi
  if [[ -z "${CORE_DOMAIN_SET[${domain}]:-}" ]]; then
    CORE_DOMAIN_SET["${domain}"]=1
    CORE_DOMAINS+=("${domain}")
  fi
done

if [[ "${#CORE_DOMAINS[@]}" -eq 0 ]]; then
  echo "[hxy-memory-guard] result=PASS checked_files=${#UNIQUE_CANDIDATES[@]} core_domains=0"
  exit 0
fi

is_trivial_memory_line() {
  local line="$1"
  if [[ -z "${line}" ]]; then
    return 0
  fi
  if [[ "${line}" =~ ^(日期|时间|更新时间|更新日期|Date|Updated|updated_at|last_updated|generated_at|timestamp|revision|rev|version|版本|修订)[[:space:]:：=-].*$ ]]; then
    return 0
  fi
  if [[ "${line}" =~ ^[0-9]{4}[-/][0-9]{2}[-/][0-9]{2}([[:space:]].*)?$ ]]; then
    return 0
  fi
  if [[ "${line}" =~ ^[[:punct:][:space:]]+$ ]]; then
    return 0
  fi
  return 1
}

has_meaningful_change() {
  local file="$1"
  local patch_line content trimmed

  while IFS= read -r patch_line; do
    case "${patch_line}" in
      "+++"*|"---"*|"@@"*) continue ;;
      "+"*|"-"*)
        content="${patch_line:1}"
        trimmed="$(printf '%s' "${content}" | sed 's/^[[:space:]]*//; s/[[:space:]]*$//')"
        [[ -z "${trimmed}" ]] && continue
        if is_trivial_memory_line "${trimmed}"; then
          continue
        fi
        return 0
        ;;
    esac
  done < <(
    {
      if [[ -n "${GIT_DIFF_RANGE}" ]]; then
        git -C "${REPO_ROOT}" diff -U0 "${GIT_DIFF_RANGE}" -- "${file}" || true
      fi
      if [[ "${CHECK_STAGED}" == "1" ]]; then
        git -C "${REPO_ROOT}" diff --cached -U0 -- "${file}" || true
      fi
      if [[ "${CHECK_UNSTAGED}" == "1" ]]; then
        git -C "${REPO_ROOT}" diff -U0 -- "${file}" || true
      fi
    }
  )

  if [[ "${CHECK_UNTRACKED}" == "1" ]] && git -c core.quotepath=false -C "${REPO_ROOT}" ls-files --others --exclude-standard -- "${file}" | grep -q .; then
    while IFS= read -r line; do
      trimmed="$(printf '%s' "${line}" | sed 's/^[[:space:]]*//; s/[[:space:]]*$//')"
      [[ -z "${trimmed}" ]] && continue
      if is_trivial_memory_line "${trimmed}"; then
        continue
      fi
      return 0
    done < "${REPO_ROOT}/${file}"
  fi

  return 1
}

declare -a MISSING_MEMORY_FILES=()
declare -a INVALID_MEMORY_FILES=()
for mem in "${CORE_MEMORY_FILES[@]}"; do
  if [[ -z "${CANDIDATE_SET[${mem}]:-}" ]]; then
    MISSING_MEMORY_FILES+=("${mem}")
    continue
  fi
  if ! has_meaningful_change "${mem}"; then
    INVALID_MEMORY_FILES+=("${mem}")
  fi
done

declare -a MISSING_INDEX_DOMAINS=()
for domain in "${CORE_DOMAINS[@]}"; do
  if ! grep -Eq "^  ${domain}:" "${REPO_ROOT}/${MEMORY_INDEX_FILE}"; then
    MISSING_INDEX_DOMAINS+=("${domain}")
  fi
done

if [[ "${#MISSING_MEMORY_FILES[@]}" -gt 0 || "${#INVALID_MEMORY_FILES[@]}" -gt 0 || "${#MISSING_INDEX_DOMAINS[@]}" -gt 0 ]]; then
  echo "[hxy-memory-guard][BLOCK] memory governance check failed."
  echo "[hxy-memory-guard] changed_core_domains=$(IFS=,; echo "${CORE_DOMAINS[*]}")"
  if [[ "${#MISSING_MEMORY_FILES[@]}" -gt 0 ]]; then
    echo "[hxy-memory-guard] missing_memory_updates:"
    for file in "${MISSING_MEMORY_FILES[@]}"; do
      echo "  - ${file}"
    done
  fi
  if [[ "${#INVALID_MEMORY_FILES[@]}" -gt 0 ]]; then
    echo "[hxy-memory-guard] invalid_memory_updates(only-empty-or-timestamp):"
    for file in "${INVALID_MEMORY_FILES[@]}"; do
      echo "  - ${file}"
    done
  fi
  if [[ "${#MISSING_INDEX_DOMAINS[@]}" -gt 0 ]]; then
    echo "[hxy-memory-guard] memory-index missing domain mapping:"
    for domain in "${MISSING_INDEX_DOMAINS[@]}"; do
      echo "  - ${domain} (in ${MEMORY_INDEX_FILE})"
    done
  fi
  exit 2
fi

echo "[hxy-memory-guard] result=PASS checked_files=${#UNIQUE_CANDIDATES[@]} changed_core_domains=$(IFS=,; echo "${CORE_DOMAINS[*]}")"
