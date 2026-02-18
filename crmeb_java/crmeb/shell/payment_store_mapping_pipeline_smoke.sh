#!/usr/bin/env bash
set -euo pipefail

# 门店映射链路 smoke：
# 1) 占位号默认阻断
# 2) 占位号可显式放开
# 3) 无效门店默认阻断
# 4) 无效门店可显式放开
# 5) 审计 summary 合同字段存在

OUT_DIR="${OUT_DIR:-}"
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-crmeb_java}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:-}"
MYSQL_DEFAULTS_FILE="${MYSQL_DEFAULTS_FILE:-}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

usage() {
  cat <<'USAGE'
用法：
  ./shell/payment_store_mapping_pipeline_smoke.sh [--out-dir PATH]

参数：
  --out-dir PATH   输出目录（默认 runtime/payment_store_mapping_smoke）

环境变量：
  DB_HOST DB_PORT DB_NAME DB_USER DB_PASS MYSQL_DEFAULTS_FILE

退出码：
  0  smoke 全部通过
  2  smoke 有失败用例
  1  脚本执行失败
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
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

if ! [[ "${DB_PORT}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: DB_PORT 必须为正整数"
  exit 1
fi
if [[ -n "${MYSQL_DEFAULTS_FILE}" && ! -f "${MYSQL_DEFAULTS_FILE}" ]]; then
  echo "参数错误: MYSQL_DEFAULTS_FILE 不存在 -> ${MYSQL_DEFAULTS_FILE}"
  exit 1
fi

if [[ -z "${OUT_DIR}" ]]; then
  OUT_DIR="${ROOT_DIR}/runtime/payment_store_mapping_smoke"
fi
RUN_ID="$(date '+%Y%m%d%H%M%S')-$$"
RUN_DIR="${OUT_DIR}/run-${RUN_ID}"
mkdir -p "${RUN_DIR}"

SUMMARY_FILE="${RUN_DIR}/summary.txt"
REPORT_FILE="${RUN_DIR}/report.md"

MYSQL_CMD=(mysql)
if [[ -n "${MYSQL_DEFAULTS_FILE}" ]]; then
  MYSQL_CMD+=(--defaults-extra-file="${MYSQL_DEFAULTS_FILE}")
  if [[ "${DB_USER}" == "root" && -z "${DB_PASS}" ]]; then
    DB_USER=""
  fi
fi
MYSQL_CMD+=(-h "${DB_HOST}" -P "${DB_PORT}")
if [[ -n "${DB_USER}" ]]; then
  MYSQL_CMD+=(-u "${DB_USER}")
fi
if [[ -n "${DB_PASS}" && -z "${MYSQL_DEFAULTS_FILE}" ]]; then
  MYSQL_CMD+=(--password="${DB_PASS}")
fi
MYSQL_CMD+=("${DB_NAME}" --default-character-set=utf8mb4 --batch --raw --skip-column-names)

set +e
sample_store_id="$("${MYSQL_CMD[@]}" -e "SELECT id FROM eb_system_store WHERE IFNULL(is_del,0)=0 ORDER BY id LIMIT 1;" 2>"${RUN_DIR}/mysql.err")"
db_rc=$?
set -e
if [[ ${db_rc} -ne 0 || -z "${sample_store_id}" ]]; then
  echo "数据库或门店数据不可用: $(tr '\n' ' ' < "${RUN_DIR}/mysql.err")"
  exit 1
fi

sample_store_id="$(echo "${sample_store_id}" | tr -d '[:space:]')"
placeholder_sub_mchid="9900000001"
normal_sub_mchid="1900001001"
missing_store_id="99999999"

csv_placeholder="${RUN_DIR}/case_placeholder.csv"
csv_missing="${RUN_DIR}/case_missing_store.csv"
printf "storeId,sub_mchid\n%s,%s\n" "${sample_store_id}" "${placeholder_sub_mchid}" > "${csv_placeholder}"
printf "storeId,sub_mchid\n%s,%s\n" "${missing_store_id}" "${normal_sub_mchid}" > "${csv_missing}"

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

rc_placeholder_block="$(run_step "01_placeholder_block" ./shell/payment_store_mapping_import.sh --csv "${csv_placeholder}" --strict-submchid-format)"
rc_placeholder_allow="$(run_step "02_placeholder_allow" ./shell/payment_store_mapping_import.sh --csv "${csv_placeholder}" --strict-submchid-format --allow-placeholder-submchid --conflict-strategy overwrite)"
rc_missing_block="$(run_step "03_missing_store_block" ./shell/payment_store_mapping_import.sh --csv "${csv_missing}" --strict-submchid-format)"
rc_missing_allow="$(run_step "04_missing_store_allow" ./shell/payment_store_mapping_import.sh --csv "${csv_missing}" --strict-submchid-format --strict-store-exists 0 --conflict-strategy overwrite)"

rc_audit="$(run_step "05_mapping_audit_contract" ./shell/payment_store_mapping_audit.sh --strict-missing 0 --out-dir "${RUN_DIR}/audit" --no-alert)"
audit_summary="$(find "${RUN_DIR}/audit" -maxdepth 3 -type f -name summary.txt -printf '%T@ %p\n' 2>/dev/null | sort -n | tail -n 1 | cut -d' ' -f2- || true)"

contract_placeholder_count=0
contract_placeholder_file=0
if [[ -n "${audit_summary}" ]]; then
  if grep -q '^placeholder_count=' "${audit_summary}"; then
    contract_placeholder_count=1
  fi
  if grep -q '^placeholder_file=' "${audit_summary}"; then
    contract_placeholder_file=1
  fi
fi

pass_placeholder_block=0
pass_placeholder_allow=0
pass_missing_block=0
pass_missing_allow=0
pass_audit_contract=0

if [[ "${rc_placeholder_block}" == "1" ]]; then pass_placeholder_block=1; fi
if [[ "${rc_placeholder_allow}" == "0" ]]; then pass_placeholder_allow=1; fi
if [[ "${rc_missing_block}" == "1" ]]; then pass_missing_block=1; fi
if [[ "${rc_missing_allow}" == "0" ]]; then pass_missing_allow=1; fi
if [[ "${contract_placeholder_count}" == "1" && "${contract_placeholder_file}" == "1" ]]; then pass_audit_contract=1; fi

overall="GREEN"
exit_code=0
if [[ "${pass_placeholder_block}" != "1" || "${pass_placeholder_allow}" != "1" || "${pass_missing_block}" != "1" || "${pass_missing_allow}" != "1" || "${pass_audit_contract}" != "1" ]]; then
  overall="RED"
  exit_code=2
fi

{
  echo "run_id=${RUN_ID}"
  echo "run_time=$(date '+%Y-%m-%d %H:%M:%S')"
  echo "overall=${overall}"
  echo "sample_store_id=${sample_store_id}"
  echo "rc_placeholder_block=${rc_placeholder_block}"
  echo "rc_placeholder_allow=${rc_placeholder_allow}"
  echo "rc_missing_block=${rc_missing_block}"
  echo "rc_missing_allow=${rc_missing_allow}"
  echo "rc_audit=${rc_audit}"
  echo "pass_placeholder_block=${pass_placeholder_block}"
  echo "pass_placeholder_allow=${pass_placeholder_allow}"
  echo "pass_missing_block=${pass_missing_block}"
  echo "pass_missing_allow=${pass_missing_allow}"
  echo "pass_audit_contract=${pass_audit_contract}"
  echo "audit_summary=${audit_summary}"
  echo "run_dir=${RUN_DIR}"
} > "${SUMMARY_FILE}"

cat > "${REPORT_FILE}" <<REPORT
# 门店映射链路 Smoke 报告

- run_id: ${RUN_ID}
- run_time: $(date '+%Y-%m-%d %H:%M:%S')
- overall: ${overall}
- sample_store_id: ${sample_store_id}

## 用例结果
- placeholder_block（期望 rc=1）: rc=${rc_placeholder_block}, pass=${pass_placeholder_block}
- placeholder_allow（期望 rc=0）: rc=${rc_placeholder_allow}, pass=${pass_placeholder_allow}
- missing_store_block（期望 rc=1）: rc=${rc_missing_block}, pass=${pass_missing_block}
- missing_store_allow（期望 rc=0）: rc=${rc_missing_allow}, pass=${pass_missing_allow}
- mapping_audit_contract（期望含 placeholder_count/placeholder_file）: rc=${rc_audit}, pass=${pass_audit_contract}

## 追溯
- summary: ${SUMMARY_FILE}
- audit_summary: ${audit_summary}
- logs: ${RUN_DIR}
REPORT

echo "[store-mapping-smoke] summary=${SUMMARY_FILE}"
echo "[store-mapping-smoke] report=${REPORT_FILE}"
echo "[store-mapping-smoke] overall=${overall}"

exit "${exit_code}"
