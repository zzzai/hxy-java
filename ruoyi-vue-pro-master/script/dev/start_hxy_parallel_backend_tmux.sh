#!/usr/bin/env bash
set -euo pipefail

# Open 2/3 backend lanes via tmux. Optional isolated git worktrees per lane.

usage() {
  cat <<'USAGE'
Usage:
  script/dev/start_hxy_parallel_backend_tmux.sh [options]

Options:
  --lanes <2|3>              number of lanes (default: 3)
  --session <name>           tmux session name (default: hxy-be-dev)
  --with-worktree <0|1>      create/use isolated worktree for lane2/3 (default: 1)
  --worktree-base <path>     worktree base dir (default: <repo>/.worktrees)
  --attach <0|1>             attach tmux after creation (default: 1)
  -h, --help                 show help

Examples:
  script/dev/start_hxy_parallel_backend_tmux.sh --lanes 3
  script/dev/start_hxy_parallel_backend_tmux.sh --lanes 2 --session hxy-hotfix
  script/dev/start_hxy_parallel_backend_tmux.sh --with-worktree 0
USAGE
}

LANES=3
SESSION_NAME="hxy-be-dev"
WITH_WORKTREE=1
ATTACH=1

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(git -C "${SCRIPT_DIR}" rev-parse --show-toplevel)"
WORKTREE_BASE="${REPO_ROOT}/.worktrees"
PROJECT_ROOT_NAME="$(basename "${REPO_ROOT}")"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --lanes)
      LANES="${2:-}"
      shift 2
      ;;
    --session)
      SESSION_NAME="${2:-}"
      shift 2
      ;;
    --with-worktree)
      WITH_WORKTREE="${2:-}"
      shift 2
      ;;
    --worktree-base)
      WORKTREE_BASE="${2:-}"
      shift 2
      ;;
    --attach)
      ATTACH="${2:-}"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "[parallel-dev][FAIL] unknown option: $1" >&2
      usage
      exit 1
      ;;
  esac
done

if ! [[ "${LANES}" =~ ^[23]$ ]]; then
  echo "[parallel-dev][FAIL] --lanes must be 2 or 3" >&2
  exit 1
fi
if ! [[ "${WITH_WORKTREE}" =~ ^[01]$ ]]; then
  echo "[parallel-dev][FAIL] --with-worktree must be 0 or 1" >&2
  exit 1
fi
if ! [[ "${ATTACH}" =~ ^[01]$ ]]; then
  echo "[parallel-dev][FAIL] --attach must be 0 or 1" >&2
  exit 1
fi

if ! command -v tmux >/dev/null 2>&1; then
  echo "[parallel-dev][FAIL] missing command: tmux" >&2
  exit 1
fi

if [[ "${WITH_WORKTREE}" == "1" ]]; then
  # Safety: prevent worktree content from entering git index accidentally.
  if [[ "${WORKTREE_BASE}" == "${REPO_ROOT}/.worktrees" ]]; then
    if ! git -C "${REPO_ROOT}" check-ignore -q ".worktrees"; then
      echo "[parallel-dev][FAIL] .worktrees is not ignored by git. Add '.worktrees/' to .gitignore first." >&2
      exit 1
    fi
  fi
  mkdir -p "${WORKTREE_BASE}"
fi

lane_path() {
  local lane="$1"
  if [[ "${lane}" == "1" || "${WITH_WORKTREE}" == "0" ]]; then
    printf '%s\n' "${REPO_ROOT}"
    return 0
  fi
  printf '%s/lane%s\n' "${WORKTREE_BASE}" "${lane}"
}

ensure_lane_worktree() {
  local lane="$1"
  local path
  path="$(lane_path "${lane}")"
  if [[ "${lane}" == "1" || "${WITH_WORKTREE}" == "0" ]]; then
    return 0
  fi
  if [[ -d "${path}/.git" || -f "${path}/.git" ]]; then
    return 0
  fi
  local ts
  ts="$(date +%Y%m%d%H%M%S)"
  local branch="hxy/lane${lane}-${ts}"
  git -C "${REPO_ROOT}" worktree add "${path}" -b "${branch}" >/dev/null
}

for lane in $(seq 2 "${LANES}"); do
  ensure_lane_worktree "${lane}"
done

if tmux has-session -t "${SESSION_NAME}" 2>/dev/null; then
  echo "[parallel-dev][FAIL] tmux session already exists: ${SESSION_NAME}" >&2
  echo "[parallel-dev] use: tmux attach -t ${SESSION_NAME}" >&2
  exit 1
fi

LANE1_PATH="$(lane_path 1)"
LANE2_PATH="$(lane_path 2)"
LANE3_PATH="$(lane_path 3)"

tmux new-session -d -s "${SESSION_NAME}" -n "lane1" -c "${LANE1_PATH}"
tmux send-keys -t "${SESSION_NAME}:lane1" "cd ruoyi-vue-pro-master" C-m
tmux send-keys -t "${SESSION_NAME}:lane1" "echo '[lane1] base chain: api/service integration'" C-m

tmux new-window -t "${SESSION_NAME}" -n "lane2" -c "${LANE2_PATH}"
tmux send-keys -t "${SESSION_NAME}:lane2" "cd ruoyi-vue-pro-master" C-m
tmux send-keys -t "${SESSION_NAME}:lane2" "echo '[lane2] independent branch: tests/sql/docs'" C-m

if [[ "${LANES}" == "3" ]]; then
  tmux new-window -t "${SESSION_NAME}" -n "lane3" -c "${LANE3_PATH}"
  tmux send-keys -t "${SESSION_NAME}:lane3" "cd ruoyi-vue-pro-master" C-m
  tmux send-keys -t "${SESSION_NAME}:lane3" "echo '[lane3] independent branch: jobs/observability/ops scripts'" C-m
fi

cat <<INFO
[parallel-dev] session=${SESSION_NAME}
[parallel-dev] repo=${PROJECT_ROOT_NAME}
[parallel-dev] lanes=${LANES}
[parallel-dev] with_worktree=${WITH_WORKTREE}
[parallel-dev] lane1=${LANE1_PATH}
[parallel-dev] lane2=${LANE2_PATH}
INFO

if [[ "${LANES}" == "3" ]]; then
  echo "[parallel-dev] lane3=${LANE3_PATH}"
fi

echo "[parallel-dev] attach: tmux attach -t ${SESSION_NAME}"
echo "[parallel-dev] list:   tmux list-windows -t ${SESSION_NAME}"

if [[ "${ATTACH}" == "1" ]]; then
  exec tmux attach -t "${SESSION_NAME}"
fi

