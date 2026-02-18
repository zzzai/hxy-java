#!/usr/bin/env bash
set -euo pipefail

# D61: 清理门店映射占位号（99xxxxxxxx）
# 默认 dry-run，仅在 --apply 时真正落库。

OUT_DIR="${OUT_DIR:-}"
RUNTIME_ROOT="${RUNTIME_ROOT:-}"
APPLY=0
NO_ALERT=0

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-crmeb_java}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:-}"
MYSQL_DEFAULTS_FILE="${MYSQL_DEFAULTS_FILE:-}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ALERT_SCRIPT="${ROOT_DIR}/shell/payment_alert_notify.sh"
PLACEHOLDER_REGEX='^99[0-9]{8}$'

usage() {
  cat <<'USAGE'
用法：
  ./shell/payment_store_mapping_placeholder_cleanup.sh [--apply] [--out-dir PATH] [--runtime-root PATH] [--no-alert]

参数：
  --apply            真正执行清理（默认 dry-run）
  --out-dir PATH     输出目录（默认 runtime/payment_store_mapping_placeholder_cleanup）
  --runtime-root PATH runtime 根目录（默认 <repo>/runtime）
  --no-alert         非 GREEN 时不推送机器人

数据库环境变量：
  DB_HOST DB_PORT DB_NAME DB_USER DB_PASS MYSQL_DEFAULTS_FILE

退出码：
  0  清理后无占位号（GREEN）
  2  仍存在占位号（YELLOW/RED）
  1  执行失败
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --apply)
      APPLY=1
      shift
      ;;
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
  OUT_DIR="${RUNTIME_ROOT}/payment_store_mapping_placeholder_cleanup"
fi

RUN_ID="$(date '+%Y%m%d%H%M%S')-$$"
RUN_DIR="${OUT_DIR}/run-${RUN_ID}"
mkdir -p "${RUN_DIR}"

SUMMARY_FILE="${RUN_DIR}/summary.txt"
REPORT_FILE="${RUN_DIR}/report.md"
DETAIL_FILE="${RUN_DIR}/placeholder_before.tsv"
DETAIL_AFTER_FILE="${RUN_DIR}/placeholder_after.tsv"
APPLY_SQL_FILE="${RUN_DIR}/apply.sql"
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

query_rows() {
  "${MYSQL_CMD[@]}" -e "SELECT name, IFNULL(value,'') FROM eb_system_config WHERE name='pay_routine_sub_mchid' OR name LIKE 'pay_routine_sub_mchid_%' ORDER BY name;"
}

collect_placeholder() {
  local out_file="$1"
  local rows="$2"
  local count=0
  printf "name\tvalue\n" > "${out_file}"
  while IFS=$'\t' read -r name value; do
    [[ -n "${name}" ]] || continue
    if [[ "${value}" =~ ${PLACEHOLDER_REGEX} ]]; then
      printf "%s\t%s\n" "${name}" "${value}" >> "${out_file}"
      count=$((count + 1))
    fi
  done <<< "${rows}"
  printf '%s' "${count}"
}

set +e
before_rows="$(query_rows 2>"${MYSQL_ERR_FILE}")"
before_rc=$?
set -e
if [[ ${before_rc} -ne 0 ]]; then
  echo "数据库查询失败: $(tr '\n' ' ' < "${MYSQL_ERR_FILE}")"
  exit 1
fi

placeholder_before_count="$(collect_placeholder "${DETAIL_FILE}" "${before_rows}")"
updated_count=0

if [[ "${APPLY}" == "1" && "${placeholder_before_count}" -gt 0 ]]; then
  {
    echo "START TRANSACTION;"
    while IFS=$'\t' read -r cfg_name cfg_value; do
      [[ -n "${cfg_name}" ]] || continue
      if [[ "${cfg_name}" == "name" ]]; then
        continue
      fi
      esc_name="$(printf '%s' "${cfg_name}" | sed "s/'/''/g")"
      echo "UPDATE eb_system_config SET value='' WHERE name='${esc_name}';"
      updated_count=$((updated_count + 1))
    done < "${DETAIL_FILE}"
    echo "COMMIT;"
  } > "${APPLY_SQL_FILE}"

  set +e
  "${MYSQL_CMD[@]}" < "${APPLY_SQL_FILE}" 2>>"${MYSQL_ERR_FILE}"
  apply_rc=$?
  set -e
  if [[ ${apply_rc} -ne 0 ]]; then
    echo "数据库更新失败: $(tr '\n' ' ' < "${MYSQL_ERR_FILE}")"
    exit 1
  fi
fi

set +e
after_rows="$(query_rows 2>>"${MYSQL_ERR_FILE}")"
after_rc=$?
set -e
if [[ ${after_rc} -ne 0 ]]; then
  echo "数据库复查失败: $(tr '\n' ' ' < "${MYSQL_ERR_FILE}")"
  exit 1
fi

placeholder_after_count="$(collect_placeholder "${DETAIL_AFTER_FILE}" "${after_rows}")"

overall="GREEN"
exit_code=0
if [[ "${APPLY}" == "0" ]]; then
  if (( placeholder_before_count > 0 )); then
    overall="YELLOW"
    exit_code=2
  fi
else
  if (( placeholder_after_count > 0 )); then
    overall="RED"
    exit_code=2
  fi
fi

mode="dry-run"
if [[ "${APPLY}" == "1" ]]; then
  mode="apply"
fi

{
  echo "run_id=${RUN_ID}"
  echo "run_time=$(date '+%Y-%m-%d %H:%M:%S')"
  echo "mode=${mode}"
  echo "overall=${overall}"
  echo "placeholder_before_count=${placeholder_before_count}"
  echo "placeholder_after_count=${placeholder_after_count}"
  echo "updated_count=${updated_count}"
  echo "detail_file=${DETAIL_FILE}"
  echo "detail_after_file=${DETAIL_AFTER_FILE}"
  echo "apply_sql_file=${APPLY_SQL_FILE}"
  echo "run_dir=${RUN_DIR}"
} > "${SUMMARY_FILE}"

cat > "${REPORT_FILE}" <<REPORT
# 门店映射占位号清理报告

- run_id: ${RUN_ID}
- run_time: $(date '+%Y-%m-%d %H:%M:%S')
- mode: ${mode}
- overall: ${overall}

## 统计
- placeholder_before_count: ${placeholder_before_count}
- placeholder_after_count: ${placeholder_after_count}
- updated_count: ${updated_count}

## 追溯
- summary: ${SUMMARY_FILE}
- detail_before: ${DETAIL_FILE}
- detail_after: ${DETAIL_AFTER_FILE}
- apply_sql_file: ${APPLY_SQL_FILE}
REPORT

echo "[placeholder-cleanup] summary=${SUMMARY_FILE}"
echo "[placeholder-cleanup] report=${REPORT_FILE}"
echo "[placeholder-cleanup] overall=${overall}, mode=${mode}, before=${placeholder_before_count}, after=${placeholder_after_count}, updated=${updated_count}"

if (( exit_code != 0 )) && [[ ${NO_ALERT} -eq 0 ]] && [[ -x "${ALERT_SCRIPT}" ]]; then
  "${ALERT_SCRIPT}" \
    --title "门店映射占位号清理告警" \
    --content "overall=${overall}; mode=${mode}; before=${placeholder_before_count}; after=${placeholder_after_count}; report=${REPORT_FILE}" || true
fi

exit "${exit_code}"
