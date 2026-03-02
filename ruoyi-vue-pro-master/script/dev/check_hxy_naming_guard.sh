#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
FORBIDDEN_WORDS_FILE="${FORBIDDEN_WORDS_FILE:-${ROOT_DIR}/script/dev/config/hxy_naming_forbidden_words.txt}"
IGNORE_PREFIX_FILE="${IGNORE_PREFIX_FILE:-${ROOT_DIR}/script/dev/config/hxy_naming_guard_ignore_prefixes.txt}"
CHECK_STAGED="${CHECK_STAGED:-1}"
CHECK_UNSTAGED="${CHECK_UNSTAGED:-1}"
CHECK_UNTRACKED="${CHECK_UNTRACKED:-0}"
GIT_DIFF_RANGE="${GIT_DIFF_RANGE:-}"

usage() {
  cat <<'USAGE'
Usage:
  script/dev/check_hxy_naming_guard.sh

Environment:
  FORBIDDEN_WORDS_FILE=<path>   forbidden token list file, one token per line
  IGNORE_PREFIX_FILE=<path>     ignored path prefixes file, one prefix per line
  CHECK_STAGED=<0|1>            check staged added files (default: 1)
  CHECK_UNSTAGED=<0|1>          check unstaged added files (default: 1)
  CHECK_UNTRACKED=<0|1>         check untracked files (default: 0)
  GIT_DIFF_RANGE=<base...head>  check added/renamed files from git diff range

Exit Code:
  0  PASS (no naming violation)
  2  BLOCK (violation found)
  1  script/config error
USAGE
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0 
fi

for flag in "${CHECK_STAGED}" "${CHECK_UNSTAGED}" "${CHECK_UNTRACKED}"; do
  if ! [[ "${flag}" =~ ^[01]$ ]]; then
    echo "[naming-guard][FAIL] invalid check flag: ${flag}" >&2 
    exit 1
  fi
done

if ! command -v git >/dev/null 2>&1; then
  echo "[naming-guard][FAIL] missing command: git" >&2
  exit 1
fi

if [[ ! -f "${FORBIDDEN_WORDS_FILE}" ]]; then
  echo "[naming-guard][FAIL] forbidden words file not found: ${FORBIDDEN_WORDS_FILE}" >&2
  exit 1
fi

if ! git -C "${ROOT_DIR}" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "[naming-guard][FAIL] not a git work tree: ${ROOT_DIR}" >&2
  exit 1
fi
GIT_ROOT="$(git -C "${ROOT_DIR}" rev-parse --show-toplevel)"

mapfile -t FORBIDDEN_WORDS < <(grep -Ev '^\s*($|#)' "${FORBIDDEN_WORDS_FILE}" | tr '[:upper:]' '[:lower:]')
if [[ "${#FORBIDDEN_WORDS[@]}" -eq 0 ]]; then
  echo "[naming-guard][FAIL] empty forbidden words list: ${FORBIDDEN_WORDS_FILE}" >&2
  exit 1
fi

declare -a IGNORE_PREFIXES=()
if [[ -f "${IGNORE_PREFIX_FILE}" ]]; then
  mapfile -t IGNORE_PREFIXES < <(grep -Ev '^\s*($|#)' "${IGNORE_PREFIX_FILE}")
fi

declare -a CANDIDATES=()

if [[ -n "${GIT_DIFF_RANGE}" ]]; then
  while IFS=$'\t' read -r status col2 col3; do
    [[ -z "${status:-}" ]] && continue
    case "${status}" in
      A) CANDIDATES+=("${col2}") ;;
      R*) CANDIDATES+=("${col3}") ;;
    esac
  done < <(git -C "${GIT_ROOT}" diff --name-status --diff-filter=AR "${GIT_DIFF_RANGE}")
fi

if [[ "${CHECK_STAGED}" == "1" ]]; then
  while IFS=$'\t' read -r status col2 col3; do
    [[ -z "${status:-}" ]] && continue
    case "${status}" in
      A) CANDIDATES+=("${col2}") ;;
      R*) CANDIDATES+=("${col3}") ;;
    esac
  done < <(git -C "${GIT_ROOT}" diff --cached --name-status --diff-filter=AR)
fi

if [[ "${CHECK_UNSTAGED}" == "1" ]]; then
  while IFS=$'\t' read -r status col2 col3; do
    [[ -z "${status:-}" ]] && continue
    case "${status}" in
      A) CANDIDATES+=("${col2}") ;;
      R*) CANDIDATES+=("${col3}") ;;
    esac
  done < <(git -C "${GIT_ROOT}" diff --name-status --diff-filter=AR)
fi

if [[ "${CHECK_UNTRACKED}" == "1" ]]; then
  while IFS= read -r path; do
    [[ -z "${path}" ]] && continue
    CANDIDATES+=("${path}")
  done < <(git -C "${GIT_ROOT}" ls-files --others --exclude-standard)
fi

if [[ "${#CANDIDATES[@]}" -eq 0 ]]; then
  echo "[naming-guard] result=PASS checked_files=0"
  exit 0 
fi

mapfile -t UNIQUE_CANDIDATES < <(printf '%s\n' "${CANDIDATES[@]}" | sed '/^\s*$/d' | sort -u)

shopt -s nocasematch
declare -a VIOLATIONS=()
for path in "${UNIQUE_CANDIDATES[@]}"; do
  skip_path=0
  for prefix in "${IGNORE_PREFIXES[@]}"; do
    if [[ "${path}" == "${prefix}"* ]]; then
      skip_path=1
      break
    fi
  done
  if [[ "${skip_path}" == "1" ]]; then
    continue
  fi

  base_name="$(basename "${path}")"
  lower_base="$(printf '%s' "${base_name}" | tr '[:upper:]' '[:lower:]')"
  for token in "${FORBIDDEN_WORDS[@]}"; do
    if [[ "${lower_base}" == *"${token}"* ]]; then
      VIOLATIONS+=("${path}|${base_name}|${token}")
      break
    fi
  done
done
shopt -u nocasematch

if [[ "${#VIOLATIONS[@]}" -gt 0 ]]; then
  echo "[naming-guard][BLOCK] found ${#VIOLATIONS[@]} naming violations:"
  for item in "${VIOLATIONS[@]}"; do
    IFS='|' read -r path base token <<<"${item}"
    echo "  - path=${path}, file=${base}, forbidden_token=${token}"
  done
  exit 2
fi

echo "[naming-guard] result=PASS checked_files=${#UNIQUE_CANDIDATES[@]}"
 
