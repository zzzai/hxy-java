#!/usr/bin/env bash
set -euo pipefail

# D62: 从门店台账生成真实映射 CSV（可直接用于导入）

OUT_DIR="${OUT_DIR:-}"
RUNTIME_ROOT="${RUNTIME_ROOT:-}"
INCLUDE_DELETED="${INCLUDE_DELETED:-0}"
ONLY_UNMAPPED="${ONLY_UNMAPPED:-0}"
FILL_FROM_CONFIG="${FILL_FROM_CONFIG:-1}"
PLACEHOLDER_AS_EMPTY="${PLACEHOLDER_AS_EMPTY:-1}"

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-crmeb_java}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:-}"
MYSQL_DEFAULTS_FILE="${MYSQL_DEFAULTS_FILE:-}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PLACEHOLDER_REGEX='^99[0-9]{8}$'

usage() {
  cat <<'USAGE'
用法：
  ./shell/payment_store_mapping_csv_generate.sh [--out-dir PATH] [--runtime-root PATH] [--include-deleted 0|1] [--only-unmapped 0|1] [--fill-from-config 0|1] [--placeholder-as-empty 0|1]

参数：
  --out-dir PATH            输出目录（默认 runtime/payment_store_mapping）
  --runtime-root PATH       runtime 根目录（默认 <repo>/runtime）
  --include-deleted 0|1     是否包含已删除门店（默认 0）
  --only-unmapped 0|1       是否仅导出未映射门店（默认 0）
  --fill-from-config 0|1    是否把当前配置值写入导出 CSV（默认 1）
  --placeholder-as-empty 0|1 是否把占位号导出为空（默认 1）

数据库环境变量：
  DB_HOST DB_PORT DB_NAME DB_USER DB_PASS MYSQL_DEFAULTS_FILE

退出码：
  0  导出成功
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
    --only-unmapped)
      ONLY_UNMAPPED="$2"
      shift 2
      ;;
    --fill-from-config)
      FILL_FROM_CONFIG="$2"
      shift 2
      ;;
    --placeholder-as-empty)
      PLACEHOLDER_AS_EMPTY="$2"
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

for sw in "${INCLUDE_DELETED}" "${ONLY_UNMAPPED}" "${FILL_FROM_CONFIG}" "${PLACEHOLDER_AS_EMPTY}"; do
  if [[ "${sw}" != "0" && "${sw}" != "1" ]]; then
    echo "参数错误: 开关参数仅支持 0 或 1"
    exit 1
  fi
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
  OUT_DIR="${RUNTIME_ROOT}/payment_store_mapping"
fi

RUN_ID="$(date '+%Y%m%d%H%M%S')-$$"
RUN_DIR="${OUT_DIR}/generate-${RUN_ID}"
mkdir -p "${RUN_DIR}"

SUMMARY_FILE="${RUN_DIR}/summary.txt"
REPORT_FILE="${RUN_DIR}/report.md"
TEMPLATE_FILE="${RUN_DIR}/mapping_template.csv"
REFERENCE_FILE="${RUN_DIR}/reference.tsv"
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

query="SELECT s.id, IFNULL(s.name,''), IFNULL(s.phone,''), IFNULL(s.is_del,0), IFNULL(c.value,'')
FROM eb_system_store s
LEFT JOIN eb_system_config c ON c.name = CONCAT('pay_routine_sub_mchid_', s.id)
WHERE ${where_clause}
ORDER BY s.id;"

set +e
rows="$("${MYSQL_CMD[@]}" -e "${query}" 2>"${MYSQL_ERR_FILE}")"
query_rc=$?
set -e
if [[ ${query_rc} -ne 0 ]]; then
  echo "数据库查询失败: $(tr '\n' ' ' < "${MYSQL_ERR_FILE}")"
  exit 1
fi

printf "storeId,sub_mchid\n" > "${TEMPLATE_FILE}"
printf "store_id\tstore_name\tphone\tis_del\tcurrent_sub_mchid\texport_sub_mchid\tstatus\n" > "${REFERENCE_FILE}"

total_store_count=0
exported_store_count=0
mapped_count=0
unmapped_count=0
placeholder_count=0

while IFS=$'\t' read -r store_id store_name store_phone is_del current_sub_mchid; do
  [[ -n "${store_id}" ]] || continue
  total_store_count=$((total_store_count + 1))

  status="UNMAPPED"
  if [[ -n "${current_sub_mchid}" ]]; then
    status="MAPPED"
  fi
  if [[ "${current_sub_mchid}" =~ ${PLACEHOLDER_REGEX} ]]; then
    status="PLACEHOLDER"
    placeholder_count=$((placeholder_count + 1))
  fi

  if [[ "${status}" == "UNMAPPED" || "${status}" == "PLACEHOLDER" ]]; then
    unmapped_count=$((unmapped_count + 1))
  else
    mapped_count=$((mapped_count + 1))
  fi

  export_sub_mchid=""
  if [[ "${FILL_FROM_CONFIG}" == "1" ]]; then
    export_sub_mchid="${current_sub_mchid}"
    if [[ "${PLACEHOLDER_AS_EMPTY}" == "1" && "${export_sub_mchid}" =~ ${PLACEHOLDER_REGEX} ]]; then
      export_sub_mchid=""
    fi
  fi

  if [[ "${ONLY_UNMAPPED}" == "1" && -n "${export_sub_mchid}" ]]; then
    continue
  fi

  printf "%s,%s\n" "${store_id}" "${export_sub_mchid}" >> "${TEMPLATE_FILE}"
  printf "%s\t%s\t%s\t%s\t%s\t%s\t%s\n" "${store_id}" "${store_name}" "${store_phone}" "${is_del}" "${current_sub_mchid}" "${export_sub_mchid}" "${status}" >> "${REFERENCE_FILE}"
  exported_store_count=$((exported_store_count + 1))
done <<< "${rows}"

{
  echo "run_id=${RUN_ID}"
  echo "run_time=$(date '+%Y-%m-%d %H:%M:%S')"
  echo "include_deleted=${INCLUDE_DELETED}"
  echo "only_unmapped=${ONLY_UNMAPPED}"
  echo "fill_from_config=${FILL_FROM_CONFIG}"
  echo "placeholder_as_empty=${PLACEHOLDER_AS_EMPTY}"
  echo "total_store_count=${total_store_count}"
  echo "mapped_count=${mapped_count}"
  echo "unmapped_count=${unmapped_count}"
  echo "placeholder_count=${placeholder_count}"
  echo "exported_store_count=${exported_store_count}"
  echo "template_file=${TEMPLATE_FILE}"
  echo "reference_file=${REFERENCE_FILE}"
  echo "run_dir=${RUN_DIR}"
} > "${SUMMARY_FILE}"

cat > "${REPORT_FILE}" <<REPORT
# 门店映射 CSV 生成报告

- run_id: ${RUN_ID}
- run_time: $(date '+%Y-%m-%d %H:%M:%S')
- include_deleted: ${INCLUDE_DELETED}
- only_unmapped: ${ONLY_UNMAPPED}
- fill_from_config: ${FILL_FROM_CONFIG}
- placeholder_as_empty: ${PLACEHOLDER_AS_EMPTY}

## 统计
- total_store_count: ${total_store_count}
- mapped_count: ${mapped_count}
- unmapped_count: ${unmapped_count}
- placeholder_count: ${placeholder_count}
- exported_store_count: ${exported_store_count}

## 产物
- template_file: ${TEMPLATE_FILE}
- reference_file: ${REFERENCE_FILE}
- summary: ${SUMMARY_FILE}
REPORT

echo "[mapping-generate] summary=${SUMMARY_FILE}"
echo "[mapping-generate] report=${REPORT_FILE}"
echo "[mapping-generate] exported_store_count=${exported_store_count}, total_store_count=${total_store_count}"

exit 0
