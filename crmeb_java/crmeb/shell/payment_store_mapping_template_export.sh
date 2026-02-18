#!/usr/bin/env bash
set -euo pipefail

# D11 配套：导出 storeId -> sub_mchid 映射模板
# 默认从 eb_system_store 导出未删除门店，并带出现有 sub_mchid（如已配置）。

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUNTIME_ROOT="${ROOT_DIR}/runtime/payment_store_mapping"
RUN_ID="$(date '+%Y%m%d%H%M%S')-$$"
RUN_DIR="${RUNTIME_ROOT}/template-${RUN_ID}"
mkdir -p "${RUN_DIR}"

TEMPLATE_FILE="${RUN_DIR}/store_mapping_template.csv"
REFERENCE_FILE="${RUN_DIR}/store_mapping_reference.tsv"
WARNINGS_FILE="${RUN_DIR}/warnings.txt"
SUMMARY_FILE="${RUN_DIR}/summary.txt"

INCLUDE_DELETED=0
BLANK_SUB_MCHID=0
ONLY_UNMAPPED=0
OUT_FILE=""

usage() {
  cat <<'EOF'
用法：
  ./shell/payment_store_mapping_template_export.sh [--out /path/store_mapping.csv] [--include-deleted 0|1] [--blank-sub-mchid 0|1] [--only-unmapped 0|1]

参数：
  --out FILE                 导出的模板 CSV 路径（默认 runtime/payment_store_mapping/template-*/store_mapping_template.csv）
  --include-deleted 0|1      是否包含已删除门店（默认 0，仅导出 is_del=0）
  --blank-sub-mchid 0|1      是否把模板 sub_mchid 全部置空（默认 0，保留当前值）
  --only-unmapped 0|1        仅导出当前未配置 sub_mchid 的门店（默认 0）
  -h, --help                 查看帮助

数据库连接（可选环境变量）：
  DB_HOST=127.0.0.1
  DB_PORT=3306
  DB_NAME=crmeb_java
  DB_USER=root
  DB_PASS=
  MYSQL_DEFAULTS_FILE=/path/to/.my.cnf

输出文件：
  - store_mapping_template.csv   可直接给 import 脚本使用
  - store_mapping_reference.tsv  门店参考清单（含名称、电话、当前映射）
  - warnings.txt                 格式异常提示
  - summary.txt                  导出摘要
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --out)
      OUT_FILE="$2"
      shift 2
      ;;
    --include-deleted)
      INCLUDE_DELETED="$2"
      shift 2
      ;;
    --blank-sub-mchid)
      BLANK_SUB_MCHID="$2"
      shift 2
      ;;
    --only-unmapped)
      ONLY_UNMAPPED="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "错误：未知参数 $1"
      usage
      exit 1
      ;;
  esac
done

if [[ "${INCLUDE_DELETED}" != "0" && "${INCLUDE_DELETED}" != "1" ]]; then
  echo "错误：--include-deleted 仅支持 0 或 1"
  exit 1
fi
if [[ "${BLANK_SUB_MCHID}" != "0" && "${BLANK_SUB_MCHID}" != "1" ]]; then
  echo "错误：--blank-sub-mchid 仅支持 0 或 1"
  exit 1
fi
if [[ "${ONLY_UNMAPPED}" != "0" && "${ONLY_UNMAPPED}" != "1" ]]; then
  echo "错误：--only-unmapped 仅支持 0 或 1"
  exit 1
fi

if ! command -v mysql >/dev/null 2>&1; then
  echo "错误：未找到 mysql 客户端"
  exit 1
fi

if [[ -n "${OUT_FILE}" ]]; then
  mkdir -p "$(dirname "${OUT_FILE}")"
  TEMPLATE_FILE="${OUT_FILE}"
fi

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-crmeb_java}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:-}"
MYSQL_DEFAULTS_FILE="${MYSQL_DEFAULTS_FILE:-}"

MYSQL_CMD=(mysql)
if [[ -n "${MYSQL_DEFAULTS_FILE}" ]]; then
  if [[ ! -f "${MYSQL_DEFAULTS_FILE}" ]]; then
    echo "错误：MYSQL_DEFAULTS_FILE 不存在 -> ${MYSQL_DEFAULTS_FILE}"
    exit 1
  fi
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

query="SELECT s.id, IFNULL(s.name,''), IFNULL(s.phone,''), IFNULL(c.value,'')
FROM eb_system_store s
LEFT JOIN eb_system_config c ON c.name = CONCAT('pay_routine_sub_mchid_', s.id)
WHERE ${where_clause}
ORDER BY s.id;"

set +e
rows="$("${MYSQL_CMD[@]}" -e "${query}" 2>"${RUN_DIR}/mysql.err")"
mysql_rc=$?
set -e
if [[ ${mysql_rc} -ne 0 ]]; then
  echo "错误：查询门店失败 -> $(tr '\n' ' ' < "${RUN_DIR}/mysql.err")"
  exit 1
fi

csv_escape() {
  local v="$1"
  v="${v//$'\r'/ }"
  v="${v//$'\n'/ }"
  if [[ "${v}" == *","* || "${v}" == *"\""* ]]; then
    v="${v//\"/\"\"}"
    printf '"%s"' "${v}"
  else
    printf '%s' "${v}"
  fi
}

: > "${WARNINGS_FILE}"
printf "store_id\tstore_name\tphone\tcurrent_sub_mchid\ttemplate_sub_mchid\n" > "${REFERENCE_FILE}"
printf "storeId,sub_mchid\n" > "${TEMPLATE_FILE}"

total_count=0
current_mapped_count=0
template_prefilled_count=0
template_blank_count=0
invalid_sub_count=0
exported_count=0
sub_mchid_regex='^[1-9][0-9]{9}$'

while IFS=$'\t' read -r store_id store_name phone current_sub; do
  [[ -n "${store_id}" ]] || continue
  total_count=$((total_count + 1))

  template_sub="${current_sub}"
  if [[ "${BLANK_SUB_MCHID}" == "1" ]]; then
    template_sub=""
  fi

  if [[ -n "${current_sub}" ]]; then
    current_mapped_count=$((current_mapped_count + 1))
    if [[ ! "${current_sub}" =~ ${sub_mchid_regex} ]]; then
      invalid_sub_count=$((invalid_sub_count + 1))
      printf 'storeId=%s 当前 sub_mchid 非10位数字: %s\n' "${store_id}" "${current_sub}" >> "${WARNINGS_FILE}"
    fi
  fi

  if [[ -n "${template_sub}" ]]; then
    template_prefilled_count=$((template_prefilled_count + 1))
  else
    template_blank_count=$((template_blank_count + 1))
  fi

  if [[ "${ONLY_UNMAPPED}" == "1" && -n "${current_sub}" ]]; then
    continue
  fi
  exported_count=$((exported_count + 1))
  printf "%s,%s\n" "$(csv_escape "${store_id}")" "$(csv_escape "${template_sub}")" >> "${TEMPLATE_FILE}"
  printf "%s\t%s\t%s\t%s\t%s\n" "${store_id}" "${store_name}" "${phone}" "${current_sub}" "${template_sub}" >> "${REFERENCE_FILE}"
done <<< "${rows}"

cat > "${SUMMARY_FILE}" <<TXT
include_deleted=${INCLUDE_DELETED}
blank_sub_mchid=${BLANK_SUB_MCHID}
only_unmapped=${ONLY_UNMAPPED}
total_store_count=${total_count}
exported_store_count=${exported_count}
current_mapped_count=${current_mapped_count}
template_prefilled_count=${template_prefilled_count}
template_blank_count=${template_blank_count}
invalid_sub_mchid_count=${invalid_sub_count}
template_file=${TEMPLATE_FILE}
reference_file=${REFERENCE_FILE}
warnings_file=${WARNINGS_FILE}
run_dir=${RUN_DIR}
TXT

echo "[store-mapping-template] total_store_count=${total_count}, exported_store_count=${exported_count}, current_mapped_count=${current_mapped_count}, template_blank_count=${template_blank_count}, invalid_sub_mchid_count=${invalid_sub_count}"
echo "[store-mapping-template] template=${TEMPLATE_FILE}"
echo "[store-mapping-template] reference=${REFERENCE_FILE}"
echo "[store-mapping-template] summary=${SUMMARY_FILE}"

if [[ ${total_count} -eq 0 ]]; then
  echo "提示：未查询到门店，请检查 eb_system_store 数据。"
  exit 2
fi

exit 0
