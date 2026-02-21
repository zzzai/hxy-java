#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

OUT_DIR="${OUT_DIR:-/tmp/pay_notify_ci_artifacts_$(date +%Y%m%d_%H%M%S)}"
RUN_REPLAY=0
BASE_URL="${BASE_URL:-http://127.0.0.1:48080/admin-api/pay/notify}"
BIZ_TYPE="${BIZ_TYPE:-order}"
CHANNEL_ID="${CHANNEL_ID:-}"
SCENARIO="${SCENARIO:-duplicate}"
REPEAT_TIMES="${REPEAT_TIMES:-3}"
DELAY_SECONDS="${DELAY_SECONDS:-120}"
FIRST_BODY_FILE="${FIRST_BODY_FILE:-}"
SECOND_BODY_FILE="${SECOND_BODY_FILE:-}"
HEADER_FILE="${HEADER_FILE:-}"
QUERY_FILE="${QUERY_FILE:-}"
INSECURE=0
MERCHANT_ORDER_ID="${MERCHANT_ORDER_ID:-}"
LOOKBACK_HOURS="${LOOKBACK_HOURS:-48}"
GRACE_MINUTES="${GRACE_MINUTES:-10}"
EMIT_ON_WARN=0
NOTIFY_DRY_RUN=1
WORKFLOW_RUN_URL="${WORKFLOW_RUN_URL:-local-run}"

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-}"
DB_NAME="${DB_NAME:-ruoyi-vue-pro}"

usage() {
  cat <<'USAGE'
Usage:
  script/dev/run_pay_notify_replay_pipeline.sh [options]

Core:
  --out-dir <dir>                输出目录
  --run-replay <0|1>             是否执行回放（默认 0）
  --merchant-order-id <id>       商户单号（必填）
  --emit-on-warn <0|1>           WARN 是否工单化（默认 0）
  --notify-dry-run <0|1>         通知 dry-run（默认 1）
  --workflow-run-url <url>       运行链接标识（默认 local-run）

Replay:
  --base-url <url>               回调地址前缀
  --biz-type <type>              order|refund|transfer
  --channel-id <id>              渠道 ID（run-replay=1 时必填）
  --scenario <name>              duplicate|out_of_order|delayed
  --repeat-times <n>             duplicate 场景次数
  --delay-seconds <n>            delayed 场景延迟秒数
  --first-body-file <file>       第一条回调 body（run-replay=1 时必填）
  --second-body-file <file>      第二条 body（out_of_order 必填）
  --header-file <file>           请求头文件（run-replay=1 推荐）
  --query-file <file>            query 参数文件（可选）
  --insecure                     curl -k

Acceptance:
  --db-host <host>               DB host
  --db-port <port>               DB port
  --db-user <user>               DB user
  --db-password <password>       DB password
  --db-name <name>               DB name
  --lookback-hours <hours>       验收窗口小时
  --grace-minutes <minutes>      任务宽限分钟

  -h, --help                     Show help
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --out-dir) OUT_DIR="$2"; shift 2 ;;
    --run-replay) RUN_REPLAY="$2"; shift 2 ;;
    --base-url) BASE_URL="$2"; shift 2 ;;
    --biz-type) BIZ_TYPE="$2"; shift 2 ;;
    --channel-id) CHANNEL_ID="$2"; shift 2 ;;
    --scenario) SCENARIO="$2"; shift 2 ;;
    --repeat-times) REPEAT_TIMES="$2"; shift 2 ;;
    --delay-seconds) DELAY_SECONDS="$2"; shift 2 ;;
    --first-body-file) FIRST_BODY_FILE="$2"; shift 2 ;;
    --second-body-file) SECOND_BODY_FILE="$2"; shift 2 ;;
    --header-file) HEADER_FILE="$2"; shift 2 ;;
    --query-file) QUERY_FILE="$2"; shift 2 ;;
    --insecure) INSECURE=1; shift ;;
    --merchant-order-id) MERCHANT_ORDER_ID="$2"; shift 2 ;;
    --lookback-hours) LOOKBACK_HOURS="$2"; shift 2 ;;
    --grace-minutes) GRACE_MINUTES="$2"; shift 2 ;;
    --emit-on-warn) EMIT_ON_WARN="$2"; shift 2 ;;
    --notify-dry-run) NOTIFY_DRY_RUN="$2"; shift 2 ;;
    --workflow-run-url) WORKFLOW_RUN_URL="$2"; shift 2 ;;
    --db-host) DB_HOST="$2"; shift 2 ;;
    --db-port) DB_PORT="$2"; shift 2 ;;
    --db-user) DB_USER="$2"; shift 2 ;;
    --db-password) DB_PASSWORD="$2"; shift 2 ;;
    --db-name) DB_NAME="$2"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown option: $1" >&2; usage; exit 1 ;;
  esac
done

if [[ -z "${MERCHANT_ORDER_ID}" ]]; then
  echo "Missing --merchant-order-id" >&2
  exit 1
fi
if [[ "${RUN_REPLAY}" == "1" ]]; then
  if [[ -z "${CHANNEL_ID}" ]]; then
    echo "--run-replay=1 requires --channel-id" >&2
    exit 1
  fi
  if [[ -z "${FIRST_BODY_FILE}" ]]; then
    echo "--run-replay=1 requires --first-body-file" >&2
    exit 1
  fi
fi

ARTIFACT_DIR="${OUT_DIR}"
REPLAY_OUT_DIR="${ARTIFACT_DIR}/replay"
LOG_DIR="${ARTIFACT_DIR}/logs"
mkdir -p "${REPLAY_OUT_DIR}" "${LOG_DIR}" "${ARTIFACT_DIR}/tickets"

REPLAY_LOG="${LOG_DIR}/replay.log"
ACCEPTANCE_LOG="${LOG_DIR}/acceptance.log"
NOTIFY_LOG="${LOG_DIR}/ticket_notify.log"
FINAL_GATE_LOG="${LOG_DIR}/final_gate.log"
SUMMARY_FILE="${ARTIFACT_DIR}/summary.txt"
REPORT_FILE="${ARTIFACT_DIR}/pay_notify_replay_report.md"
TICKET_TSV="${ARTIFACT_DIR}/tickets/tickets.tsv"
NOTIFY_SUMMARY_FILE="${ARTIFACT_DIR}/tickets/notify_summary.txt"
ARTIFACT_INDEX="${ARTIFACT_DIR}/artifact_index.md"

replay_rc=0
acceptance_rc=0
final_gate_rc=0
final_gate_reason="Replay acceptance gate passed"

if [[ "${RUN_REPLAY}" == "1" ]]; then
  set +e
  cmd=(
    bash "${ROOT_DIR}/script/dev/replay_pay_notify.sh"
    --base-url "${BASE_URL}"
    --biz-type "${BIZ_TYPE}"
    --channel-id "${CHANNEL_ID}"
    --scenario "${SCENARIO}"
    --repeat-times "${REPEAT_TIMES}"
    --delay-seconds "${DELAY_SECONDS}"
    --first-body-file "${FIRST_BODY_FILE}"
    --out-dir "${REPLAY_OUT_DIR}"
  )
  if [[ -n "${SECOND_BODY_FILE}" ]]; then
    cmd+=(--second-body-file "${SECOND_BODY_FILE}")
  fi
  if [[ -n "${HEADER_FILE}" ]]; then
    cmd+=(--header-file "${HEADER_FILE}")
  fi
  if [[ -n "${QUERY_FILE}" ]]; then
    cmd+=(--query-file "${QUERY_FILE}")
  fi
  if [[ "${INSECURE}" == "1" ]]; then
    cmd+=(--insecure)
  fi
  "${cmd[@]}" 2>&1 | tee "${REPLAY_LOG}"
  replay_rc=${PIPESTATUS[0]}
  set -e
else
  echo "run_replay=0, skip replay" | tee "${REPLAY_LOG}"
fi

set +e
DB_HOST="${DB_HOST}" DB_PORT="${DB_PORT}" DB_USER="${DB_USER}" DB_PASSWORD="${DB_PASSWORD}" DB_NAME="${DB_NAME}" \
bash "${ROOT_DIR}/script/dev/check_pay_notify_replay.sh" \
  --merchant-order-id "${MERCHANT_ORDER_ID}" \
  --lookback-hours "${LOOKBACK_HOURS}" \
  --grace-minutes "${GRACE_MINUTES}" \
  2>&1 | tee "${ACCEPTANCE_LOG}"
acceptance_rc=${PIPESTATUS[0]}
set -e

if [[ "${replay_rc}" != "0" ]]; then
  final_gate_rc="${replay_rc}"
  final_gate_reason="Replay failed: rc=${replay_rc}"
elif [[ "${acceptance_rc}" != "0" ]]; then
  final_gate_rc="${acceptance_rc}"
  final_gate_reason="Acceptance failed: rc=${acceptance_rc}"
fi
{
  echo "generated_at=$(date '+%Y-%m-%d %H:%M:%S')"
  echo "replay_rc=${replay_rc}"
  echo "acceptance_rc=${acceptance_rc}"
  echo "final_gate_exit_code=${final_gate_rc}"
  echo "final_gate_reason=${final_gate_reason}"
} > "${FINAL_GATE_LOG}"

bash "${ROOT_DIR}/script/dev/pay_notify_replay_artifact_index.sh" \
  --out-dir "${ARTIFACT_DIR}" \
  --replay-out-dir "${REPLAY_OUT_DIR}" \
  --replay-log "${REPLAY_LOG}" \
  --acceptance-log "${ACCEPTANCE_LOG}" \
  --final-gate-log "${FINAL_GATE_LOG}" \
  --run-replay "${RUN_REPLAY}" \
  --replay-exit-code "${replay_rc}" \
  --acceptance-exit-code "${acceptance_rc}" \
  --merchant-order-id "${MERCHANT_ORDER_ID}" \
  --scenario "${SCENARIO}" \
  --biz-type "${BIZ_TYPE}" \
  --channel-id "${CHANNEL_ID}" \
  --report-file "${REPORT_FILE}" \
  --ticket-dir "${ARTIFACT_DIR}/tickets" \
  --notify-summary-file "${NOTIFY_SUMMARY_FILE}" \
  --workflow-run-url "${WORKFLOW_RUN_URL}"

bash "${ROOT_DIR}/script/dev/generate_pay_notify_replay_report.sh" \
  --artifact-dir "${ARTIFACT_DIR}" \
  --summary-file "${SUMMARY_FILE}" \
  --acceptance-log "${ACCEPTANCE_LOG}" \
  --replay-log "${REPLAY_LOG}" \
  --output-file "${REPORT_FILE}"

bash "${ROOT_DIR}/script/dev/create_pay_notify_block_ticket.sh" \
  --artifact-dir "${ARTIFACT_DIR}" \
  --summary-file "${SUMMARY_FILE}" \
  --acceptance-log "${ACCEPTANCE_LOG}" \
  --ticket-dir "${ARTIFACT_DIR}/tickets" \
  --emit-on-warn "${EMIT_ON_WARN}" \
  --output-tsv "${TICKET_TSV}"

notify_cmd=(
  bash "${ROOT_DIR}/script/dev/notify_pay_notify_tickets.sh"
  --ticket-tsv "${TICKET_TSV}"
  --summary-file "${SUMMARY_FILE}"
  --report-file "${REPORT_FILE}"
  --artifact-index "${ARTIFACT_INDEX}"
  --notify-summary-file "${NOTIFY_SUMMARY_FILE}"
)
if [[ "${NOTIFY_DRY_RUN}" == "1" ]]; then
  notify_cmd+=(--dry-run)
fi
"${notify_cmd[@]}" 2>&1 | tee "${NOTIFY_LOG}" || true

bash "${ROOT_DIR}/script/dev/pay_notify_replay_artifact_index.sh" \
  --out-dir "${ARTIFACT_DIR}" \
  --replay-out-dir "${REPLAY_OUT_DIR}" \
  --replay-log "${REPLAY_LOG}" \
  --acceptance-log "${ACCEPTANCE_LOG}" \
  --final-gate-log "${FINAL_GATE_LOG}" \
  --run-replay "${RUN_REPLAY}" \
  --replay-exit-code "${replay_rc}" \
  --acceptance-exit-code "${acceptance_rc}" \
  --merchant-order-id "${MERCHANT_ORDER_ID}" \
  --scenario "${SCENARIO}" \
  --biz-type "${BIZ_TYPE}" \
  --channel-id "${CHANNEL_ID}" \
  --report-file "${REPORT_FILE}" \
  --ticket-dir "${ARTIFACT_DIR}/tickets" \
  --notify-summary-file "${NOTIFY_SUMMARY_FILE}" \
  --workflow-run-url "${WORKFLOW_RUN_URL}"

echo "[pipeline] artifact_dir=${ARTIFACT_DIR}"
echo "[pipeline] index=${ARTIFACT_INDEX}"
echo "[pipeline] report=${REPORT_FILE}"
echo "[pipeline] replay_rc=${replay_rc}, acceptance_rc=${acceptance_rc}, final_gate_rc=${final_gate_rc}"

if [[ "${final_gate_rc}" != "0" ]]; then
  exit "${final_gate_rc}"
fi
exit 0
