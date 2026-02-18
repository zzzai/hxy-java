#!/usr/bin/env bash
set -euo pipefail

# D63: 门店映射跨渠道唯一性审计
# 检查范围：eb_system_config 中所有 name LIKE '%sub_mchid%' 的 store 级配置。

OUT_DIR="${OUT_DIR:-}"
RUNTIME_ROOT="${RUNTIME_ROOT:-}"
NO_ALERT=0

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-crmeb_java}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:-}"
MYSQL_DEFAULTS_FILE="${MYSQL_DEFAULTS_FILE:-}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ALERT_SCRIPT="${ROOT_DIR}/shell/payment_alert_notify.sh"

usage() {
  cat <<'USAGE'
用法：
  ./shell/payment_store_mapping_cross_channel_audit.sh [--out-dir PATH] [--runtime-root PATH] [--no-alert]

参数：
  --out-dir PATH      输出目录（默认 runtime/payment_store_mapping_cross_channel）
  --runtime-root PATH runtime 根目录（默认 <repo>/runtime）
  --no-alert          非 GREEN 时不推送机器人

数据库环境变量：
  DB_HOST DB_PORT DB_NAME DB_USER DB_PASS MYSQL_DEFAULTS_FILE

退出码：
  0  GREEN
  2  YELLOW/RED（存在冲突）
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
  OUT_DIR="${RUNTIME_ROOT}/payment_store_mapping_cross_channel"
fi

RUN_ID="$(date '+%Y%m%d%H%M%S')-$$"
RUN_DIR="${OUT_DIR}/run-${RUN_ID}"
mkdir -p "${RUN_DIR}"

SUMMARY_FILE="${RUN_DIR}/summary.txt"
REPORT_FILE="${RUN_DIR}/report.md"
STORE_CONFLICT_FILE="${RUN_DIR}/store_conflict.tsv"
SUB_CONFLICT_FILE="${RUN_DIR}/submchid_conflict.tsv"
SCAN_FILE="${RUN_DIR}/scan.tsv"
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

set +e
rows="$("${MYSQL_CMD[@]}" -e "SELECT name, IFNULL(value,'') FROM eb_system_config WHERE name LIKE '%sub_mchid%' ORDER BY name;" 2>"${MYSQL_ERR_FILE}")"
query_rc=$?
set -e
if [[ ${query_rc} -ne 0 ]]; then
  echo "数据库查询失败: $(tr '\n' ' ' < "${MYSQL_ERR_FILE}")"
  exit 1
fi

declare -A STORE_VALUES=()
declare -A STORE_CHANNELS=()
declare -A STORE_VALUE_COUNT=()
declare -A SUB_HITS=()
declare -A SUB_STORE_SET=()
declare -A SUB_CHANNEL_SET=()

append_unique() {
  local current="$1"
  local token="$2"
  if [[ -z "${token}" ]]; then
    printf '%s' "${current}"
    return
  fi
  if [[ -z "${current}" ]]; then
    printf '%s' "${token}"
    return
  fi
  if [[ ",${current}," == *",${token},"* ]]; then
    printf '%s' "${current}"
  else
    printf '%s,%s' "${current}" "${token}"
  fi
}

scan_count=0
store_channel_record_count=0
printf "name\tchannel_key\tstore_id\tsub_mchid\n" > "${SCAN_FILE}"

while IFS=$'\t' read -r cfg_name cfg_value; do
  [[ -n "${cfg_name}" ]] || continue
  scan_count=$((scan_count + 1))

  if [[ "${cfg_name}" =~ ^(.+)_([0-9]+)$ ]]; then
    channel_key="${BASH_REMATCH[1]}"
    store_id="${BASH_REMATCH[2]}"
  else
    continue
  fi

  printf "%s\t%s\t%s\t%s\n" "${cfg_name}" "${channel_key}" "${store_id}" "${cfg_value}" >> "${SCAN_FILE}"

  [[ -n "${cfg_value}" ]] || continue
  store_channel_record_count=$((store_channel_record_count + 1))

  STORE_VALUES["${store_id}"]="$(append_unique "${STORE_VALUES["${store_id}"]-}" "${cfg_value}")"
  STORE_CHANNELS["${store_id}"]="$(append_unique "${STORE_CHANNELS["${store_id}"]-}" "${channel_key}:${cfg_value}")"
  STORE_VALUE_COUNT["${store_id}"]=$(( ${STORE_VALUE_COUNT["${store_id}"]:-0} + 1 ))

  SUB_HITS["${cfg_value}"]="$(append_unique "${SUB_HITS["${cfg_value}"]-}" "${channel_key}@${store_id}")"
  SUB_STORE_SET["${cfg_value}"]="$(append_unique "${SUB_STORE_SET["${cfg_value}"]-}" "${store_id}")"
  SUB_CHANNEL_SET["${cfg_value}"]="$(append_unique "${SUB_CHANNEL_SET["${cfg_value}"]-}" "${channel_key}")"
done <<< "${rows}"

printf "store_id\tunique_sub_mchid_count\tsub_mchids\tchannel_bindings\n" > "${STORE_CONFLICT_FILE}"
store_conflict_count=0
for store_id in $(printf '%s\n' "${!STORE_VALUES[@]}" | sort -n); do
  value_csv="${STORE_VALUES["${store_id}"]}"
  channel_csv="${STORE_CHANNELS["${store_id}"]}"
  unique_count="$(awk -F',' '{print NF}' <<< "${value_csv}")"
  if [[ -z "${value_csv}" ]]; then
    unique_count=0
  fi
  if (( unique_count > 1 )); then
    store_conflict_count=$((store_conflict_count + 1))
    printf "%s\t%s\t%s\t%s\n" "${store_id}" "${unique_count}" "${value_csv}" "${channel_csv}" >> "${STORE_CONFLICT_FILE}"
  fi
done

printf "sub_mchid\tstore_count\tchannel_count\thits\tconflict_type\n" > "${SUB_CONFLICT_FILE}"
cross_store_conflict_count=0
cross_channel_conflict_count=0

for sub_mchid in $(printf '%s\n' "${!SUB_HITS[@]}" | sort); do
  hits_csv="${SUB_HITS["${sub_mchid}"]}"
  store_csv="${SUB_STORE_SET["${sub_mchid}"]-}"
  channel_csv="${SUB_CHANNEL_SET["${sub_mchid}"]-}"
  store_count=0
  channel_count=0
  if [[ -n "${store_csv}" ]]; then
    store_count="$(awk -F',' '{print NF}' <<< "${store_csv}")"
  fi
  if [[ -n "${channel_csv}" ]]; then
    channel_count="$(awk -F',' '{print NF}' <<< "${channel_csv}")"
  fi

  conflict_type=""
  if (( store_count > 1 )); then
    conflict_type="CROSS_STORE"
    cross_store_conflict_count=$((cross_store_conflict_count + 1))
  elif (( channel_count > 1 )); then
    conflict_type="CROSS_CHANNEL"
    cross_channel_conflict_count=$((cross_channel_conflict_count + 1))
  fi

  if [[ -n "${conflict_type}" ]]; then
    printf "%s\t%s\t%s\t%s\t%s\n" "${sub_mchid}" "${store_count}" "${channel_count}" "${hits_csv}" "${conflict_type}" >> "${SUB_CONFLICT_FILE}"
  fi
done

overall="GREEN"
exit_code=0
if (( cross_store_conflict_count > 0 || store_conflict_count > 0 )); then
  overall="RED"
  exit_code=2
elif (( cross_channel_conflict_count > 0 )); then
  overall="YELLOW"
  exit_code=2
fi

{
  echo "run_id=${RUN_ID}"
  echo "run_time=$(date '+%Y-%m-%d %H:%M:%S')"
  echo "overall=${overall}"
  echo "scan_count=${scan_count}"
  echo "store_channel_record_count=${store_channel_record_count}"
  echo "store_conflict_count=${store_conflict_count}"
  echo "cross_store_conflict_count=${cross_store_conflict_count}"
  echo "cross_channel_conflict_count=${cross_channel_conflict_count}"
  echo "scan_file=${SCAN_FILE}"
  echo "store_conflict_file=${STORE_CONFLICT_FILE}"
  echo "sub_conflict_file=${SUB_CONFLICT_FILE}"
  echo "run_dir=${RUN_DIR}"
} > "${SUMMARY_FILE}"

cat > "${REPORT_FILE}" <<REPORT
# 门店映射跨渠道唯一性审计报告

- run_id: ${RUN_ID}
- run_time: $(date '+%Y-%m-%d %H:%M:%S')
- overall: ${overall}

## 统计
- scan_count: ${scan_count}
- store_channel_record_count: ${store_channel_record_count}
- store_conflict_count: ${store_conflict_count}
- cross_store_conflict_count: ${cross_store_conflict_count}
- cross_channel_conflict_count: ${cross_channel_conflict_count}

## 产物
- scan_file: ${SCAN_FILE}
- store_conflict_file: ${STORE_CONFLICT_FILE}
- sub_conflict_file: ${SUB_CONFLICT_FILE}
- summary: ${SUMMARY_FILE}
REPORT

echo "[cross-channel-audit] summary=${SUMMARY_FILE}"
echo "[cross-channel-audit] report=${REPORT_FILE}"
echo "[cross-channel-audit] overall=${overall}, store_conflict_count=${store_conflict_count}, cross_store_conflict_count=${cross_store_conflict_count}, cross_channel_conflict_count=${cross_channel_conflict_count}"

if (( exit_code != 0 )) && [[ ${NO_ALERT} -eq 0 ]] && [[ -x "${ALERT_SCRIPT}" ]]; then
  "${ALERT_SCRIPT}" \
    --title "门店映射跨渠道冲突告警" \
    --content "overall=${overall}; store_conflict=${store_conflict_count}; cross_store=${cross_store_conflict_count}; cross_channel=${cross_channel_conflict_count}; report=${REPORT_FILE}" || true
fi

exit "${exit_code}"
