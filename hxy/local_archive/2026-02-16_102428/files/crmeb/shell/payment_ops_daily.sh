#!/usr/bin/env bash
set -euo pipefail

# 支付日常一键巡检：
# preflight -> monitor -> reconcile
# 适合无订单号阶段的日常自检。

RECON_DATE="${RECON_DATE:-}"
WINDOW_MINUTES="${WINDOW_MINUTES:-15}"
TAIL_LINES="${TAIL_LINES:-3000}"
STRICT_PREFLIGHT=0
NO_ALERT=0
OUTPUT_DIR="${OUTPUT_DIR:-}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEFAULT_OUTPUT_DIR="${ROOT_DIR}/runtime/payment_ops_daily"
ALERT_SCRIPT="${ROOT_DIR}/shell/payment_alert_notify.sh"

usage() {
  cat <<'USAGE'
用法：
  ./shell/payment_ops_daily.sh [--date YYYY-MM-DD] [--window MINUTES] [--tail LINES] [--strict-preflight] [--no-alert] [--output-dir PATH]

参数：
  --date YYYY-MM-DD       对账日期（默认昨天）
  --window MINUTES        监控扫描窗口（默认 15）
  --tail LINES            监控每文件读取行数（默认 3000）
  --strict-preflight      preflight 将 WARN 视为失败（退出码=2）
  --no-alert              即便异常也不推送告警
  --output-dir PATH       报告输出根目录（默认 runtime/payment_ops_daily）

环境变量：
  DB_HOST DB_PORT DB_NAME DB_USER DB_PASS MYSQL_DEFAULTS_FILE
  ALERT_WEBHOOK_URL ALERT_WEBHOOK_TYPE

退出码：
  0  全部通过
  2  存在告警项（阈值超标/对账差异/preflight严格告警）
  1  脚本执行失败
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --date)
      RECON_DATE="$2"
      shift 2
      ;;
    --window)
      WINDOW_MINUTES="$2"
      shift 2
      ;;
    --tail)
      TAIL_LINES="$2"
      shift 2
      ;;
    --strict-preflight)
      STRICT_PREFLIGHT=1
      shift
      ;;
    --no-alert)
      NO_ALERT=1
      shift
      ;;
    --output-dir)
      OUTPUT_DIR="$2"
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

if [[ -z "${RECON_DATE}" ]]; then
  RECON_DATE="$(date -d 'yesterday' +%F)"
fi
if ! [[ "${RECON_DATE}" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]]; then
  echo "参数错误: --date 需要 YYYY-MM-DD"
  exit 1
fi
if ! [[ "${WINDOW_MINUTES}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: --window 必须为正整数"
  exit 1
fi
if ! [[ "${TAIL_LINES}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: --tail 必须为正整数"
  exit 1
fi
if [[ -n "${MYSQL_DEFAULTS_FILE:-}" && ! -f "${MYSQL_DEFAULTS_FILE}" ]]; then
  echo "参数错误: MYSQL_DEFAULTS_FILE 文件不存在 -> ${MYSQL_DEFAULTS_FILE}"
  exit 1
fi

if [[ -z "${OUTPUT_DIR}" ]]; then
  OUTPUT_DIR="${DEFAULT_OUTPUT_DIR}"
fi
RUN_ID="$(date '+%Y%m%d%H%M%S')"
RUN_DIR="${OUTPUT_DIR}/run-${RUN_ID}"
mkdir -p "${RUN_DIR}"

PREFLIGHT_LOG="${RUN_DIR}/01_preflight.log"
MONITOR_LOG="${RUN_DIR}/02_monitor.log"
RECON_LOG="${RUN_DIR}/03_reconcile.log"
SUMMARY_FILE="${RUN_DIR}/summary.txt"

run_step() {
  local log_file="$1"
  shift
  set +e
  "$@" >"${log_file}" 2>&1
  local rc=$?
  set -e
  return "${rc}"
}

echo "[ops-daily] run_dir=${RUN_DIR}"
echo "[ops-daily] recon_date=${RECON_DATE}, window=${WINDOW_MINUTES}, tail=${TAIL_LINES}"

PREFLIGHT_CMD=(./shell/payment_preflight_check.sh --out-dir "${RUN_DIR}")
if [[ ${STRICT_PREFLIGHT} -eq 1 ]]; then
  PREFLIGHT_CMD+=(--strict)
fi
run_step "${PREFLIGHT_LOG}" "${PREFLIGHT_CMD[@]}" || preflight_rc=$?
preflight_rc="${preflight_rc:-0}"

run_step "${MONITOR_LOG}" ./shell/payment_monitor_quickcheck.sh --window "${WINDOW_MINUTES}" --tail "${TAIL_LINES}" || monitor_rc=$?
monitor_rc="${monitor_rc:-0}"

run_step "${RECON_LOG}" ./shell/payment_reconcile_daily.sh --date "${RECON_DATE}" --output-dir "${RUN_DIR}" || recon_rc=$?
recon_rc="${recon_rc:-0}"

severity="OK"
exit_code=0

if (( preflight_rc == 1 || monitor_rc == 1 || recon_rc == 1 )); then
  severity="ERROR"
  exit_code=1
elif (( preflight_rc == 2 || monitor_rc == 2 || recon_rc == 2 )); then
  severity="ALERT"
  exit_code=2
fi

cat > "${SUMMARY_FILE}" <<TXT
run_id=${RUN_ID}
run_time=$(date '+%Y-%m-%d %H:%M:%S')
severity=${severity}
recon_date=${RECON_DATE}
window_minutes=${WINDOW_MINUTES}
tail_lines=${TAIL_LINES}
preflight_rc=${preflight_rc}
monitor_rc=${monitor_rc}
reconcile_rc=${recon_rc}
preflight_log=${PREFLIGHT_LOG}
monitor_log=${MONITOR_LOG}
reconcile_log=${RECON_LOG}
reconcile_summary=${RUN_DIR}/${RECON_DATE}/summary.txt
TXT

echo "[ops-daily] severity=${severity}, summary=${SUMMARY_FILE}"

if (( exit_code != 0 && NO_ALERT == 0 )) && [[ -x "${ALERT_SCRIPT}" ]]; then
  "${ALERT_SCRIPT}" \
    --title "支付日常巡检告警" \
    --content "severity=${severity}; recon_date=${RECON_DATE}; preflight_rc=${preflight_rc}; monitor_rc=${monitor_rc}; reconcile_rc=${recon_rc}; summary=${SUMMARY_FILE}" || true
fi

exit "${exit_code}"
