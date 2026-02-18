#!/usr/bin/env bash
set -euo pipefail

# 批量执行人工退款收敛命令：
# 输入 payment_refund_convergence_check 生成的 R05 actions 文件，
# 支持 dry-run 预览与 execute 实际执行。

ACTIONS_FILE="${ACTIONS_FILE:-}"
EXECUTE=0
MAX_ITEMS="${MAX_ITEMS:-0}"
STOP_ON_ERROR=0
OUT_DIR="${OUT_DIR:-}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

usage() {
  cat <<'USAGE'
用法：
  ./shell/payment_refund_manual_converge_batch.sh --actions-file PATH [--execute] [--max N] [--stop-on-error] [--out-dir PATH]

参数：
  --actions-file PATH   actions 文件路径（必填）
  --execute             实际执行；默认仅 dry-run 预览
  --max N               最多执行 N 条（0=全部，默认 0）
  --stop-on-error       任一失败立即停止
  --out-dir PATH        输出目录（默认 runtime/payment_refund_manual_batch）

退出码：
  0  全部成功 / dry-run
  2  存在失败项
  1  参数或执行错误
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --actions-file)
      ACTIONS_FILE="$2"
      shift 2
      ;;
    --execute)
      EXECUTE=1
      shift
      ;;
    --max)
      MAX_ITEMS="$2"
      shift 2
      ;;
    --stop-on-error)
      STOP_ON_ERROR=1
      shift
      ;;
    --out-dir)
      OUT_DIR="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "未知参数: $1"
      usage
      exit 1
      ;;
  esac
done

if [[ -z "${ACTIONS_FILE}" ]]; then
  echo "参数错误: --actions-file 必填"
  exit 1
fi
if [[ ! -f "${ACTIONS_FILE}" ]]; then
  echo "参数错误: actions 文件不存在 -> ${ACTIONS_FILE}"
  exit 1
fi
if ! [[ "${MAX_ITEMS}" =~ ^[0-9]+$ ]]; then
  echo "参数错误: --max 必须是非负整数"
  exit 1
fi

if [[ -z "${OUT_DIR}" ]]; then
  OUT_DIR="${ROOT_DIR}/runtime/payment_refund_manual_batch"
fi
RUN_ID="$(date '+%Y%m%d%H%M%S')-$$"
RUN_DIR="${OUT_DIR}/run-${RUN_ID}"
mkdir -p "${RUN_DIR}"
SUMMARY_FILE="${RUN_DIR}/summary.txt"
DETAIL_FILE="${RUN_DIR}/detail.log"

mapfile -t ACTION_LINES < <(grep 'payment_refund_manual_converge.sh' "${ACTIONS_FILE}" || true)
TOTAL="${#ACTION_LINES[@]}"

if (( MAX_ITEMS > 0 && TOTAL > MAX_ITEMS )); then
  ACTION_LINES=("${ACTION_LINES[@]:0:${MAX_ITEMS}}")
fi
PLAN_TOTAL="${#ACTION_LINES[@]}"

if (( EXECUTE == 0 )); then
  {
    echo "mode=dry_run"
    echo "actions_file=${ACTIONS_FILE}"
    echo "total_actions=${TOTAL}"
    echo "planned_actions=${PLAN_TOTAL}"
    echo "run_dir=${RUN_DIR}"
  } > "${SUMMARY_FILE}"
  {
    echo "[dry-run] actions_file=${ACTIONS_FILE}"
    echo "[dry-run] total_actions=${TOTAL}, planned_actions=${PLAN_TOTAL}"
    for idx in "${!ACTION_LINES[@]}"; do
      echo "[$((idx + 1))] ${ACTION_LINES[$idx]}"
    done
  } > "${DETAIL_FILE}"
  cat "${SUMMARY_FILE}"
  echo "detail_file=${DETAIL_FILE}"
  exit 0
fi

SUCCESS=0
FAILED=0

{
  echo "[execute] actions_file=${ACTIONS_FILE}"
  echo "[execute] total_actions=${TOTAL}, planned_actions=${PLAN_TOTAL}, stop_on_error=${STOP_ON_ERROR}"
} > "${DETAIL_FILE}"

for idx in "${!ACTION_LINES[@]}"; do
  cmd="${ACTION_LINES[$idx]}"
  echo "[run][$((idx + 1))/${PLAN_TOTAL}] ${cmd}" >> "${DETAIL_FILE}"
  set +e
  bash -lc "${cmd}" >> "${DETAIL_FILE}" 2>&1
  rc=$?
  set -e
  if [[ "${rc}" == "0" ]]; then
    SUCCESS=$((SUCCESS + 1))
    echo "[ok][$((idx + 1))] rc=${rc}" >> "${DETAIL_FILE}"
  else
    FAILED=$((FAILED + 1))
    echo "[fail][$((idx + 1))] rc=${rc}" >> "${DETAIL_FILE}"
    if (( STOP_ON_ERROR == 1 )); then
      break
    fi
  fi
done

RESULT="GREEN"
EXIT_CODE=0
if (( FAILED > 0 )); then
  RESULT="RED"
  EXIT_CODE=2
fi

{
  echo "mode=execute"
  echo "result=${RESULT}"
  echo "actions_file=${ACTIONS_FILE}"
  echo "total_actions=${TOTAL}"
  echo "planned_actions=${PLAN_TOTAL}"
  echo "success_count=${SUCCESS}"
  echo "failed_count=${FAILED}"
  echo "run_dir=${RUN_DIR}"
} > "${SUMMARY_FILE}"

cat "${SUMMARY_FILE}"
echo "detail_file=${DETAIL_FILE}"
exit "${EXIT_CODE}"
