#!/usr/bin/env bash
set -euo pipefail

# 真实子商户号切换一键脚本：
# 1) 映射导入 dry-run
# 2) 映射导入 apply
# 3) 映射审计（要求 GREEN）
# 4) 值守状态刷新（要求 GREEN）
# 5) 切换上线拦截规则复核（要求 GREEN）

CSV_FILE="${CSV_FILE:-}"
REPORT_DATE="${REPORT_DATE:-}"
ORDER_NO="${ORDER_NO:-}"
REQUIRE_APPLY_READY="${REQUIRE_APPLY_READY:-0}"
REQUIRE_BOOKING_REPAIR_PASS="${REQUIRE_BOOKING_REPAIR_PASS:-0}"
NO_ALERT=0
OUT_DIR="${OUT_DIR:-}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

usage() {
  cat <<'USAGE'
用法：
  ./shell/payment_store_mapping_cutover.sh --csv FILE [--date YYYY-MM-DD] [--order-no ORDER_NO] [--require-apply-ready 0|1] [--require-booking-repair-pass 0|1] [--no-alert] [--out-dir PATH]

参数：
  --csv FILE                   真实映射CSV（storeId,sub_mchid）
  --date YYYY-MM-DD            业务日期（默认昨天）
  --order-no ORDER_NO          真实订单号（可选；传入后上线拦截规则会按 GO_LAUNCH 判定）
  --require-apply-ready 0|1    上线拦截规则是否要求 apply-ready（默认 0）
  --require-booking-repair-pass 0|1 是否要求 booking repair=PASS（默认 0）
  --no-alert                   不推送机器人告警
  --out-dir PATH               输出目录（默认 runtime/payment_store_mapping_cutover）

说明：
  - 本脚本要求 sub_mchid 为真实微信商户号（10位数字），占位号 99xxxxxxxx 会在 preflight 被阻断。
  - 数据库连接使用环境变量 DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASS 或 MYSQL_DEFAULTS_FILE。

退出码：
  0  全流程通过
  2  存在阻断项
  1  执行错误
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --csv)
      CSV_FILE="$2"
      shift 2
      ;;
    --date)
      REPORT_DATE="$2"
      shift 2
      ;;
    --order-no)
      ORDER_NO="$2"
      shift 2
      ;;
    --require-apply-ready)
      REQUIRE_APPLY_READY="$2"
      shift 2
      ;;
    --require-booking-repair-pass)
      REQUIRE_BOOKING_REPAIR_PASS="$2"
      shift 2
      ;;
    --no-alert)
      NO_ALERT=1
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

if [[ -z "${CSV_FILE}" ]]; then
  echo "参数错误: --csv 不能为空"
  exit 1
fi
if [[ ! -f "${CSV_FILE}" ]]; then
  echo "参数错误: CSV 不存在 -> ${CSV_FILE}"
  exit 1
fi
if [[ -z "${REPORT_DATE}" ]]; then
  REPORT_DATE="$(date -d 'yesterday' +%F)"
fi
if ! [[ "${REPORT_DATE}" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]]; then
  echo "参数错误: --date 需要 YYYY-MM-DD"
  exit 1
fi
if [[ "${REQUIRE_APPLY_READY}" != "0" && "${REQUIRE_APPLY_READY}" != "1" ]]; then
  echo "参数错误: --require-apply-ready 仅支持 0 或 1"
  exit 1
fi
if [[ "${REQUIRE_BOOKING_REPAIR_PASS}" != "0" && "${REQUIRE_BOOKING_REPAIR_PASS}" != "1" ]]; then
  echo "参数错误: --require-booking-repair-pass 仅支持 0 或 1"
  exit 1
fi

if [[ -z "${OUT_DIR}" ]]; then
  OUT_DIR="${ROOT_DIR}/runtime/payment_store_mapping_cutover"
fi
RUN_ID="$(date '+%Y%m%d%H%M%S')-$$"
RUN_DIR="${OUT_DIR}/run-${RUN_ID}"
mkdir -p "${RUN_DIR}"
SUMMARY_FILE="${RUN_DIR}/summary.txt"
REPORT_FILE="${RUN_DIR}/report.md"

run_step() {
  local step="$1"
  shift
  local log="${RUN_DIR}/${step}.log"
  set +e
  "$@" > "${log}" 2>&1
  local rc=$?
  set -e
  printf '%s' "${rc}"
}

echo "[mapping-cutover] run_dir=${RUN_DIR}"
echo "[mapping-cutover] csv=${CSV_FILE}, date=${REPORT_DATE}, order_no=${ORDER_NO:-<none>}"

dry_rc="$(run_step "01_import_dryrun" ./shell/payment_store_mapping_import.sh --csv "${CSV_FILE}" --strict-submchid-format --strict-submchid-unique --strict-store-exists 1 --reject-placeholder-submchid 1 --conflict-strategy block)"
apply_rc="-"
if [[ "${dry_rc}" == "0" ]]; then
  apply_rc="$(run_step "02_import_apply" ./shell/payment_store_mapping_import.sh --csv "${CSV_FILE}" --strict-submchid-format --strict-submchid-unique --strict-store-exists 1 --reject-placeholder-submchid 1 --conflict-strategy block --apply --confirm)"
else
  apply_rc="1"
fi

audit_rc="-"
if [[ "${apply_rc}" == "0" ]]; then
  audit_rc="$(run_step "03_mapping_audit" ./shell/payment_store_mapping_audit.sh --strict-missing 0 --out-dir "${RUN_DIR}/mapping_audit" --no-alert)"
else
  audit_rc="1"
fi

ops_rc="-"
if [[ "${audit_rc}" == "0" ]]; then
  ops_rc="$(run_step "04_ops_status" ./shell/payment_ops_status.sh --date "${REPORT_DATE}" --refresh --refresh-require-apply-ready "${REQUIRE_APPLY_READY}" --require-booking-repair-pass "${REQUIRE_BOOKING_REPAIR_PASS}" --require-decision-chain-pass 1 --out-dir "${RUN_DIR}/ops_status" --no-alert)"
else
  ops_rc="1"
fi

gate_rc="-"
if [[ "${audit_rc}" == "0" ]]; then
  gate_cmd=(./shell/payment_cutover_gate.sh --date "${REPORT_DATE}" --require-apply-ready "${REQUIRE_APPLY_READY}" --require-booking-repair-pass "${REQUIRE_BOOKING_REPAIR_PASS}" --require-mapping-green 1 --require-mock-green 1 --out-dir "${RUN_DIR}/cutover_gate")
  if [[ -n "${ORDER_NO}" ]]; then
    gate_cmd+=(--order-no "${ORDER_NO}")
  fi
  if [[ ${NO_ALERT} -eq 1 ]]; then
    gate_cmd+=(--no-alert)
  fi
  gate_rc="$(run_step "05_cutover_gate" "${gate_cmd[@]}")"
else
  gate_rc="1"
fi

latest_gate_summary="$(find "${RUN_DIR}/cutover_gate" -maxdepth 3 -type f -name summary.txt -printf '%T@ %p\n' 2>/dev/null | sort -n | tail -n 1 | cut -d' ' -f2- || true)"
latest_ops_summary="$(find "${RUN_DIR}/ops_status" -maxdepth 3 -type f -name summary.txt -printf '%T@ %p\n' 2>/dev/null | sort -n | tail -n 1 | cut -d' ' -f2- || true)"
latest_audit_summary="$(find "${RUN_DIR}/mapping_audit" -maxdepth 3 -type f -name summary.txt -printf '%T@ %p\n' 2>/dev/null | sort -n | tail -n 1 | cut -d' ' -f2- || true)"

ops_overall="$(grep -E '^overall=' "${latest_ops_summary}" 2>/dev/null | head -n1 | cut -d'=' -f2- || true)"
gate_overall="$(grep -E '^overall=' "${latest_gate_summary}" 2>/dev/null | head -n1 | cut -d'=' -f2- || true)"

overall="GREEN"
exit_code=0
if [[ "${dry_rc}" != "0" || "${apply_rc}" != "0" || "${audit_rc}" != "0" || "${gate_rc}" != "0" ]]; then
  overall="RED"
  exit_code=2
fi
if [[ "${ops_rc}" == "1" || "${ops_overall}" == "RED" ]]; then
  overall="RED"
  exit_code=2
fi
if [[ "${gate_overall}" != "GREEN" ]]; then
  overall="RED"
  exit_code=2
fi

{
  echo "run_id=${RUN_ID}"
  echo "run_time=$(date '+%Y-%m-%d %H:%M:%S')"
  echo "report_date=${REPORT_DATE}"
  echo "csv=${CSV_FILE}"
  echo "order_no=${ORDER_NO}"
  echo "require_apply_ready=${REQUIRE_APPLY_READY}"
  echo "require_booking_repair_pass=${REQUIRE_BOOKING_REPAIR_PASS}"
  echo "strict_store_exists=1"
  echo "reject_placeholder_submchid=1"
  echo "overall=${overall}"
  echo "import_dryrun_rc=${dry_rc}"
  echo "import_apply_rc=${apply_rc}"
  echo "mapping_audit_rc=${audit_rc}"
  echo "ops_status_rc=${ops_rc}"
  echo "ops_status_overall=${ops_overall}"
  echo "cutover_gate_rc=${gate_rc}"
  echo "cutover_gate_overall=${gate_overall}"
  echo "mapping_audit_summary=${latest_audit_summary}"
  echo "ops_status_summary=${latest_ops_summary}"
  echo "cutover_gate_summary=${latest_gate_summary}"
  echo "run_dir=${RUN_DIR}"
} > "${SUMMARY_FILE}"

cat > "${REPORT_FILE}" <<REPORT
# 真实子商户号切换报告

- run_id: ${RUN_ID}
- run_time: $(date '+%Y-%m-%d %H:%M:%S')
- report_date: ${REPORT_DATE}
- csv: ${CSV_FILE}
- order_no: ${ORDER_NO:-<none>}
- overall: ${overall}

## 步骤结果
- import_dryrun_rc: ${dry_rc}
- import_apply_rc: ${apply_rc}
- mapping_audit_rc: ${audit_rc}
- ops_status_rc: ${ops_rc}
- ops_status_overall: ${ops_overall:-<none>}
- cutover_gate_rc: ${gate_rc}
- cutover_gate_overall: ${gate_overall:-<none>}

## 追溯
- summary: ${SUMMARY_FILE}
- mapping_audit_summary: ${latest_audit_summary:-<none>}
- ops_status_summary: ${latest_ops_summary:-<none>}
- cutover_gate_summary: ${latest_gate_summary:-<none>}
REPORT

echo "[mapping-cutover] summary=${SUMMARY_FILE}"
echo "[mapping-cutover] report=${REPORT_FILE}"
echo "[mapping-cutover] overall=${overall}, dry=${dry_rc}, apply=${apply_rc}, audit=${audit_rc}, ops=${ops_rc}, gate=${gate_rc}"

exit "${exit_code}"
