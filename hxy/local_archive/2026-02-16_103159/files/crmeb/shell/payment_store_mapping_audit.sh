#!/usr/bin/env bash
set -euo pipefail

# D42: storeId -> sub_mchid 映射审计
# 目标：识别缺失、格式异常、重复绑定、孤儿配置。

OUT_DIR="${OUT_DIR:-}"
RUNTIME_ROOT="${RUNTIME_ROOT:-}"
INCLUDE_DELETED="${INCLUDE_DELETED:-0}"
STRICT_MISSING="${STRICT_MISSING:-0}"
NO_ALERT=0

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-crmeb_java}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:-}"
MYSQL_DEFAULTS_FILE="${MYSQL_DEFAULTS_FILE:-}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ALERT_SCRIPT="${ROOT_DIR}/shell/payment_alert_notify.sh"
SUB_MCHID_REGEX='^[1-9][0-9]{9}$'

usage() {
  cat <<'USAGE'
用法：
  ./shell/payment_store_mapping_audit.sh [--out-dir PATH] [--runtime-root PATH] [--include-deleted 0|1] [--strict-missing 0|1] [--no-alert]

参数：
  --out-dir PATH          输出目录（默认 runtime/payment_store_mapping）
  --runtime-root PATH     runtime 根目录（默认 <repo>/runtime）
  --include-deleted 0|1   是否包含已删除门店（默认 0）
  --strict-missing 0|1    是否把缺失映射视为阻断（默认 0，仅告警）
  --no-alert              风险时不推送机器人

数据库环境变量：
  DB_HOST DB_PORT DB_NAME DB_USER DB_PASS MYSQL_DEFAULTS_FILE

退出码：
  0  审计通过（GREEN）
  2  审计存在风险（YELLOW/RED）
  1  执行失败
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --out-dir)
      OUT_DIR="$2"
      shift 2
      ;;
    --runtime-root)
      RUNTIME_ROOT="$2"
      shift 2
      ;;
    --include-deleted)
      INCLUDE_DELETED="$2"
      shift 2
      ;;
    --strict-missing)
      STRICT_MISSING="$2"
      shift 2
      ;;
    --no-alert)
      NO_ALERT=1
      shift
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

if [[ "${INCLUDE_DELETED}" != "0" && "${INCLUDE_DELETED}" != "1" ]]; then
  echo "参数错误: --include-deleted 仅支持 0 或 1"
  exit 1
fi
if [[ "${STRICT_MISSING}" != "0" && "${STRICT_MISSING}" != "1" ]]; then
  echo "参数错误: --strict-missing 仅支持 0 或 1"
  exit 1
fi
if ! [[ "${DB_PORT}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: DB_PORT 必须为正整数"
  exit 1
fi
if [[ -n "${MYSQL_DEFAULTS_FILE}" && ! -f "${MYSQL_DEFAULTS_FILE}" ]]; then
  echo "参数错误: MYSQL_DEFAULTS_FILE 不存在 -> ${MYSQL_DEFAULTS_FILE}"
  exit 1
fi

if [[ -z "${RUNTIME_ROOT}" ]]; then
  RUNTIME_ROOT="${ROOT_DIR}/runtime"
fi
if [[ -z "${OUT_DIR}" ]]; then
  OUT_DIR="${RUNTIME_ROOT}/payment_store_mapping"
fi

RUN_ID="$(date '+%Y%m%d%H%M%S')-$$"
RUN_DIR="${OUT_DIR}/audit-${RUN_ID}"
mkdir -p "${RUN_DIR}"

SUMMARY_FILE="${RUN_DIR}/summary.txt"
REPORT_FILE="${RUN_DIR}/report.md"
DETAIL_FILE="${RUN_DIR}/detail.tsv"
MISSING_FILE="${RUN_DIR}/missing.tsv"
DUPLICATE_FILE="${RUN_DIR}/duplicate.tsv"
ORPHAN_FILE="${RUN_DIR}/orphan.tsv"
MYSQL_ERR_FILE="${RUN_DIR}/mysql.err"

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

where_clause="1=1"
if [[ "${INCLUDE_DELETED}" == "0" ]]; then
  where_clause="${where_clause} AND IFNULL(s.is_del,0)=0"
fi

store_query="SELECT s.id, IFNULL(s.name,''), IFNULL(s.phone,''), IFNULL(s.is_del,0), IFNULL(c.value,'')
FROM eb_system_store s
LEFT JOIN eb_system_config c ON c.name = CONCAT('pay_routine_sub_mchid_', s.id)
WHERE ${where_clause}
ORDER BY s.id;"

orphan_query="SELECT c.name, IFNULL(c.value,'')
FROM eb_system_config c
LEFT JOIN eb_system_store s ON c.name = CONCAT('pay_routine_sub_mchid_', s.id)
WHERE c.name LIKE 'pay_routine_sub_mchid_%' AND s.id IS NULL
ORDER BY c.name;"

set +e
store_rows="$("${MYSQL_CMD[@]}" -e "${store_query}" 2>"${MYSQL_ERR_FILE}")"
store_rc=$?
orphan_rows="$("${MYSQL_CMD[@]}" -e "${orphan_query}" 2>>"${MYSQL_ERR_FILE}")"
orphan_rc=$?
set -e
if [[ ${store_rc} -ne 0 || ${orphan_rc} -ne 0 ]]; then
  echo "数据库查询失败: $(tr '\n' ' ' < "${MYSQL_ERR_FILE}")"
  exit 1
fi

declare -A STORE_SUB=()
declare -A STORE_NAME=()
declare -A STORE_PHONE=()
declare -A STORE_IS_DEL=()
declare -A SUB_COUNT=()
declare -A SUB_STORES=()
declare -A STORE_DUP=()
declare -A STORE_INVALID=()
declare -A STORE_MISSING=()

store_total=0
mapped_count=0
missing_count=0
invalid_count=0

while IFS=$'\t' read -r store_id store_name store_phone store_is_del sub_mchid; do
  [[ -n "${store_id}" ]] || continue
  store_total=$((store_total + 1))

  STORE_NAME["${store_id}"]="${store_name}"
  STORE_PHONE["${store_id}"]="${store_phone}"
  STORE_IS_DEL["${store_id}"]="${store_is_del}"
  STORE_SUB["${store_id}"]="${sub_mchid}"

  if [[ -z "${sub_mchid}" ]]; then
    STORE_MISSING["${store_id}"]=1
    missing_count=$((missing_count + 1))
    continue
  fi

  mapped_count=$((mapped_count + 1))
  if [[ ! "${sub_mchid}" =~ ${SUB_MCHID_REGEX} ]]; then
    STORE_INVALID["${store_id}"]=1
    invalid_count=$((invalid_count + 1))
  fi

  SUB_COUNT["${sub_mchid}"]=$(( ${SUB_COUNT["${sub_mchid}"]:-0} + 1 ))
  if [[ -n "${SUB_STORES["${sub_mchid}"]-}" ]]; then
    SUB_STORES["${sub_mchid}"]="${SUB_STORES["${sub_mchid}"]},${store_id}"
  else
    SUB_STORES["${sub_mchid}"]="${store_id}"
  fi
done <<< "${store_rows}"

duplicate_sub_count=0
duplicate_store_count=0
for sub_mchid in "${!SUB_COUNT[@]}"; do
  if (( SUB_COUNT["${sub_mchid}"] > 1 )); then
    duplicate_sub_count=$((duplicate_sub_count + 1))
    IFS=',' read -r -a stores <<< "${SUB_STORES["${sub_mchid}"]}"
    for sid in "${stores[@]}"; do
      sid="${sid// /}"
      [[ -z "${sid}" ]] && continue
      if [[ -z "${STORE_DUP["${sid}"]-}" ]]; then
        STORE_DUP["${sid}"]=1
        duplicate_store_count=$((duplicate_store_count + 1))
      fi
    done
  fi
done

orphan_count=0
orphan_non_empty_count=0
orphan_empty_count=0
printf "config_name\tsub_mchid\tstore_id_guess\tseverity\n" > "${ORPHAN_FILE}"
while IFS=$'\t' read -r config_name sub_mchid; do
  [[ -n "${config_name}" ]] || continue
  orphan_count=$((orphan_count + 1))
  store_guess="${config_name#pay_routine_sub_mchid_}"
  severity="WARN"
  if [[ -n "${sub_mchid}" ]]; then
    orphan_non_empty_count=$((orphan_non_empty_count + 1))
    severity="P1"
  else
    orphan_empty_count=$((orphan_empty_count + 1))
  fi
  printf "%s\t%s\t%s\t%s\n" "${config_name}" "${sub_mchid}" "${store_guess}" "${severity}" >> "${ORPHAN_FILE}"
done <<< "${orphan_rows}"

printf "store_id\tstore_name\tphone\tis_del\tsub_mchid\tstatus\treason\n" > "${DETAIL_FILE}"
printf "store_id\tstore_name\tsub_mchid\treason\n" > "${MISSING_FILE}"
printf "sub_mchid\tstore_ids\tstore_count\n" > "${DUPLICATE_FILE}"

for store_id in $(printf '%s\n' "${!STORE_SUB[@]}" | sort -n); do
  store_name="${STORE_NAME["${store_id}"]}"
  store_phone="${STORE_PHONE["${store_id}"]}"
  store_is_del="${STORE_IS_DEL["${store_id}"]}"
  sub_mchid="${STORE_SUB["${store_id}"]}"
  status="OK"
  reason="-"

  if [[ -n "${STORE_MISSING["${store_id}"]-}" ]]; then
    status="MISSING"
    reason="sub_mchid 未配置"
    printf "%s\t%s\t%s\t%s\n" "${store_id}" "${store_name}" "${sub_mchid}" "${reason}" >> "${MISSING_FILE}"
  elif [[ -n "${STORE_INVALID["${store_id}"]-}" ]]; then
    status="INVALID_FORMAT"
    reason="sub_mchid 非10位数字"
  fi

  if [[ -n "${STORE_DUP["${store_id}"]-}" ]]; then
    if [[ "${status}" == "OK" ]]; then
      status="DUPLICATE"
      reason="sub_mchid 被多个 storeId 复用"
    else
      status="${status}+DUPLICATE"
      reason="${reason}; sub_mchid 被多个 storeId 复用"
    fi
  fi

  printf "%s\t%s\t%s\t%s\t%s\t%s\t%s\n" \
    "${store_id}" "${store_name}" "${store_phone}" "${store_is_del}" "${sub_mchid}" "${status}" "${reason}" >> "${DETAIL_FILE}"
done

for sub_mchid in $(printf '%s\n' "${!SUB_COUNT[@]}" | sort); do
  if (( SUB_COUNT["${sub_mchid}"] > 1 )); then
    printf "%s\t%s\t%s\n" "${sub_mchid}" "${SUB_STORES["${sub_mchid}"]}" "${SUB_COUNT["${sub_mchid}"]}" >> "${DUPLICATE_FILE}"
  fi
done

critical_count=$((invalid_count + duplicate_store_count + orphan_non_empty_count))
warn_count=$((orphan_empty_count))
if [[ "${STRICT_MISSING}" == "1" ]]; then
  critical_count=$((critical_count + missing_count))
else
  warn_count=$((warn_count + missing_count))
fi

overall="GREEN"
exit_code=0
if (( critical_count > 0 )); then
  overall="RED"
  exit_code=2
elif (( warn_count > 0 )); then
  overall="YELLOW"
  exit_code=2
fi

{
  echo "run_id=${RUN_ID}"
  echo "run_time=$(date '+%Y-%m-%d %H:%M:%S')"
  echo "overall=${overall}"
  echo "include_deleted=${INCLUDE_DELETED}"
  echo "strict_missing=${STRICT_MISSING}"
  echo "store_total=${store_total}"
  echo "mapped_count=${mapped_count}"
  echo "missing_count=${missing_count}"
  echo "invalid_count=${invalid_count}"
  echo "duplicate_store_count=${duplicate_store_count}"
  echo "duplicate_sub_count=${duplicate_sub_count}"
  echo "orphan_count=${orphan_count}"
  echo "orphan_non_empty_count=${orphan_non_empty_count}"
  echo "orphan_empty_count=${orphan_empty_count}"
  echo "critical_count=${critical_count}"
  echo "warn_count=${warn_count}"
  echo "detail_file=${DETAIL_FILE}"
  echo "missing_file=${MISSING_FILE}"
  echo "duplicate_file=${DUPLICATE_FILE}"
  echo "orphan_file=${ORPHAN_FILE}"
  echo "run_dir=${RUN_DIR}"
} > "${SUMMARY_FILE}"

{
  echo "# 门店支付映射审计"
  echo
  echo "- run_id: \`${RUN_ID}\`"
  echo "- run_time: \`$(date '+%Y-%m-%d %H:%M:%S')\`"
  echo "- overall: **${overall}**"
  echo "- include_deleted: \`${INCLUDE_DELETED}\`"
  echo "- strict_missing: \`${STRICT_MISSING}\`"
  echo
  echo "## 统计"
  echo
  echo "| 项 | 数值 |"
  echo "|---|---:|"
  echo "| store_total | ${store_total} |"
  echo "| mapped_count | ${mapped_count} |"
  echo "| missing_count | ${missing_count} |"
  echo "| invalid_count | ${invalid_count} |"
  echo "| duplicate_store_count | ${duplicate_store_count} |"
  echo "| duplicate_sub_count | ${duplicate_sub_count} |"
  echo "| orphan_count | ${orphan_count} |"
  echo "| orphan_non_empty_count | ${orphan_non_empty_count} |"
  echo "| orphan_empty_count | ${orphan_empty_count} |"
  echo "| critical_count | ${critical_count} |"
  echo "| warn_count | ${warn_count} |"
  echo
  echo "## 追溯文件"
  echo
  echo "- summary: \`${SUMMARY_FILE}\`"
  echo "- detail: \`${DETAIL_FILE}\`"
  echo "- missing: \`${MISSING_FILE}\`"
  echo "- duplicate: \`${DUPLICATE_FILE}\`"
  echo "- orphan: \`${ORPHAN_FILE}\`"
} > "${REPORT_FILE}"

echo "[store-mapping-audit] summary=${SUMMARY_FILE}"
echo "[store-mapping-audit] report=${REPORT_FILE}"
echo "[store-mapping-audit] overall=${overall}, critical_count=${critical_count}, warn_count=${warn_count}"

if (( exit_code != 0 )) && [[ ${NO_ALERT} -eq 0 ]] && [[ -x "${ALERT_SCRIPT}" ]]; then
  "${ALERT_SCRIPT}" \
    --title "门店支付映射审计告警" \
    --content "overall=${overall}; critical=${critical_count}; warn=${warn_count}; report=${REPORT_FILE}" || true
fi

exit "${exit_code}"

