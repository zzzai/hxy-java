#!/usr/bin/env bash
set -euo pipefail

# 门店 -> 子商户号 映射导入脚本（小程序渠道）
# CSV 格式：
#   storeId,sub_mchid
#   1,1900001001
#   2,1900001002

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUNTIME_ROOT="${ROOT_DIR}/runtime/payment_store_mapping"
RUN_ID="$(date '+%Y%m%d%H%M%S')-$$"
RUN_DIR="${RUNTIME_ROOT}/import-${RUN_ID}"
mkdir -p "${RUN_DIR}"

SUMMARY_FILE="${RUN_DIR}/summary.txt"
WARNINGS_FILE="${RUN_DIR}/warnings.txt"
ERRORS_FILE="${RUN_DIR}/errors.txt"
CONFLICTS_FILE="${RUN_DIR}/conflicts.tsv"
PLAN_FILE="${RUN_DIR}/plan.tsv"
ROLLBACK_SQL="${RUN_DIR}/rollback.sql"
NORMALIZED_FILE="${RUN_DIR}/normalized.tsv"

usage() {
  cat <<'EOF'
用法：
  ./shell/payment_store_mapping_import.sh --csv /path/store_submchid.csv [--apply] [--confirm] [--strict-submchid-unique] [--strict-submchid-format] [--strict-store-exists 0|1] [--reject-placeholder-submchid 0|1] [--conflict-strategy block|overwrite]

参数：
  --csv FILE                   映射文件，格式为 storeId,sub_mchid
  --apply                      真正写入数据库（默认 dry-run）
  --confirm                    与 --apply 配合，确认执行写入（防误操作）
  --strict-submchid-unique     强校验：同一个 sub_mchid 不能分配给多个 storeId
  --strict-submchid-format     强校验：sub_mchid 必须为 10 位数字（微信商户号格式）
  --strict-store-exists 0|1    校验 storeId 必须存在且未删除（默认 1）
  --reject-placeholder-submchid 0|1 拒绝 99xxxxxxxx 占位号（默认 1）
  --allow-placeholder-submchid 允许占位号（等价 --reject-placeholder-submchid 0）
  --conflict-strategy MODE     DB 冲突处理策略：block（默认）/overwrite
  -h, --help                   查看帮助

数据库连接（可选环境变量）：
  DB_HOST=127.0.0.1
  DB_PORT=3306
  DB_NAME=crmeb_java
  DB_USER=root
  DB_PASS=
  MYSQL_DEFAULTS_FILE=/path/to/.my.cnf

运行产物：
  runtime/payment_store_mapping/import-时间戳/
    - summary.txt
    - warnings.txt / errors.txt
    - conflicts.tsv / plan.tsv
    - rollback.sql（仅 apply 且无冲突时生成）

示例：
  ./shell/payment_store_mapping_import.sh --csv ./store_mapping.csv
  ./shell/payment_store_mapping_import.sh --csv ./store_mapping.csv --strict-submchid-unique --strict-submchid-format
  ./shell/payment_store_mapping_import.sh --csv ./store_mapping.csv --strict-store-exists 1 --reject-placeholder-submchid 1
  ./shell/payment_store_mapping_import.sh --csv ./store_mapping.csv --apply --confirm
EOF
}

trim() {
  local text="$1"
  text="${text#"${text%%[![:space:]]*}"}"
  text="${text%"${text##*[![:space:]]}"}"
  printf "%s" "$text"
}

sql_escape() {
  printf "%s" "$1" | sed "s/'/''/g"
}

is_placeholder_sub_mchid() {
  local value
  value="$(trim "$1")"
  [[ "${value}" =~ ^99[0-9]{8}$ ]]
}

CSV_FILE=""
APPLY=0
CONFIRM=0
STRICT_SUBMCHID_UNIQUE=0
STRICT_SUBMCHID_FORMAT=0
STRICT_STORE_EXISTS=1
REJECT_PLACEHOLDER_SUBMCHID=1
CONFLICT_STRATEGY="block"
CONFIG_PREFIX="pay_routine_sub_mchid_"
SUB_MCHID_REGEX='^[1-9][0-9]{9}$'
PLACEHOLDER_HIT_COUNT=0
MISSING_STORE_COUNT=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --csv)
      if [[ $# -lt 2 ]]; then
        echo "错误：--csv 缺少参数"
        exit 1
      fi
      CSV_FILE="$2"
      shift 2
      ;;
    --apply)
      APPLY=1
      shift
      ;;
    --confirm)
      CONFIRM=1
      shift
      ;;
    --strict-submchid-unique)
      STRICT_SUBMCHID_UNIQUE=1
      shift
      ;;
    --strict-submchid-format)
      STRICT_SUBMCHID_FORMAT=1
      shift
      ;;
    --strict-store-exists)
      if [[ $# -lt 2 ]]; then
        echo "错误：--strict-store-exists 缺少参数"
        exit 1
      fi
      STRICT_STORE_EXISTS="$2"
      shift 2
      ;;
    --reject-placeholder-submchid)
      if [[ $# -lt 2 ]]; then
        echo "错误：--reject-placeholder-submchid 缺少参数"
        exit 1
      fi
      REJECT_PLACEHOLDER_SUBMCHID="$2"
      shift 2
      ;;
    --allow-placeholder-submchid)
      REJECT_PLACEHOLDER_SUBMCHID=0
      shift
      ;;
    --conflict-strategy)
      if [[ $# -lt 2 ]]; then
        echo "错误：--conflict-strategy 缺少参数"
        exit 1
      fi
      CONFLICT_STRATEGY="$2"
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

if [[ "${CONFLICT_STRATEGY}" != "block" && "${CONFLICT_STRATEGY}" != "overwrite" ]]; then
  echo "错误：--conflict-strategy 仅支持 block 或 overwrite"
  exit 1
fi
if [[ "${STRICT_STORE_EXISTS}" != "0" && "${STRICT_STORE_EXISTS}" != "1" ]]; then
  echo "错误：--strict-store-exists 仅支持 0 或 1"
  exit 1
fi
if [[ "${REJECT_PLACEHOLDER_SUBMCHID}" != "0" && "${REJECT_PLACEHOLDER_SUBMCHID}" != "1" ]]; then
  echo "错误：--reject-placeholder-submchid 仅支持 0 或 1"
  exit 1
fi

if [[ ${CONFIRM} -eq 1 && ${APPLY} -eq 0 ]]; then
  echo "错误：--confirm 必须与 --apply 一起使用"
  exit 1
fi

if [[ -z "${CSV_FILE}" ]]; then
  echo "错误：必须传入 --csv 文件"
  usage
  exit 1
fi

if [[ ! -f "${CSV_FILE}" ]]; then
  echo "错误：CSV 文件不存在 -> ${CSV_FILE}"
  exit 1
fi

if ! command -v mysql >/dev/null 2>&1; then
  echo "错误：未找到 mysql 客户端"
  exit 1
fi

declare -A STORE_TO_SUB=()
declare -A STORE_LINE=()
declare -A SUB_TO_STORE=()
declare -a WARNINGS=()
declare -a ERRORS=()

write_report_files() {
  : > "${WARNINGS_FILE}"
  : > "${ERRORS_FILE}"
  : > "${CONFLICTS_FILE}"
  : > "${PLAN_FILE}"
  : > "${NORMALIZED_FILE}"

  if [[ ${#WARNINGS[@]} -gt 0 ]]; then
    for msg in "${WARNINGS[@]}"; do
      [[ -n "${msg}" ]] && printf '%s\n' "${msg}" >> "${WARNINGS_FILE}"
    done
  fi
  if [[ ${#ERRORS[@]} -gt 0 ]]; then
    for msg in "${ERRORS[@]}"; do
      [[ -n "${msg}" ]] && printf '%s\n' "${msg}" >> "${ERRORS_FILE}"
    done
  fi
}

write_summary() {
  local apply_status="$1"
  local conflict_count="$2"
  local insert_count="$3"
  local unchanged_count="$4"
  local overwrite_count="$5"
  cat > "${SUMMARY_FILE}" <<TXT
csv=${CSV_FILE}
mode=$([[ ${APPLY} -eq 1 ]] && echo "apply" || echo "dry-run")
apply_status=${apply_status}
strict_submchid_unique=${STRICT_SUBMCHID_UNIQUE}
strict_submchid_format=${STRICT_SUBMCHID_FORMAT}
strict_store_exists=${STRICT_STORE_EXISTS}
reject_placeholder_submchid=${REJECT_PLACEHOLDER_SUBMCHID}
conflict_strategy=${CONFLICT_STRATEGY}
total_store_count=${#STORE_TO_SUB[@]}
insert_count=${insert_count}
unchanged_count=${unchanged_count}
overwrite_count=${overwrite_count}
conflict_count=${conflict_count}
placeholder_hit_count=${PLACEHOLDER_HIT_COUNT}
missing_store_count=${MISSING_STORE_COUNT}
warning_count=${#WARNINGS[@]}
error_count=${#ERRORS[@]}
run_dir=${RUN_DIR}
TXT
}

line_no=0
while IFS=',' read -r raw_store raw_sub raw_extra || [[ -n "${raw_store:-}" || -n "${raw_sub:-}" || -n "${raw_extra:-}" ]]; do
  line_no=$((line_no + 1))
  store="$(trim "$(printf "%s" "${raw_store:-}" | tr -d '\r')")"
  sub_mchid="$(trim "$(printf "%s" "${raw_sub:-}" | tr -d '\r')")"
  extra="$(trim "$(printf "%s" "${raw_extra:-}" | tr -d '\r')")"

  if [[ ${line_no} -eq 1 ]]; then
    store="${store#$'\xEF\xBB\xBF'}"
  fi

  if [[ -z "${store}" && -z "${sub_mchid}" ]]; then
    continue
  fi

  if [[ "${store}" =~ ^# ]]; then
    continue
  fi

  lower_store="$(printf "%s" "${store}" | tr '[:upper:]' '[:lower:]')"
  lower_sub="$(printf "%s" "${sub_mchid}" | tr '[:upper:]' '[:lower:]')"
  if [[ ${line_no} -eq 1 && "${lower_store}" == "storeid" && ( "${lower_sub}" == "sub_mchid" || "${lower_sub}" == "submchid" ) ]]; then
    continue
  fi

  if [[ -n "${extra}" ]]; then
    ERRORS+=("第 ${line_no} 行格式错误：仅支持两列 storeId,sub_mchid")
    continue
  fi

  if [[ ! "${store}" =~ ^[1-9][0-9]*$ ]]; then
    ERRORS+=("第 ${line_no} 行 storeId 非法：${store}")
    continue
  fi

  if [[ -z "${sub_mchid}" ]]; then
    ERRORS+=("第 ${line_no} 行 sub_mchid 为空（storeId=${store}）")
    continue
  fi
  if is_placeholder_sub_mchid "${sub_mchid}"; then
    PLACEHOLDER_HIT_COUNT=$((PLACEHOLDER_HIT_COUNT + 1))
    if [[ ${REJECT_PLACEHOLDER_SUBMCHID} -eq 1 ]]; then
      ERRORS+=("第 ${line_no} 行 sub_mchid 使用占位号(99xxxxxxxx)，请替换真实商户号：${sub_mchid}")
      continue
    else
      WARNINGS+=("提示：第 ${line_no} 行 sub_mchid 为占位号(99xxxxxxxx)（storeId=${store}）")
    fi
  fi
  if [[ ${STRICT_SUBMCHID_FORMAT} -eq 1 ]]; then
    if [[ ! "${sub_mchid}" =~ ${SUB_MCHID_REGEX} ]]; then
      ERRORS+=("第 ${line_no} 行 sub_mchid 格式非法（需10位数字）：${sub_mchid}")
      continue
    fi
  else
    if [[ ! "${sub_mchid}" =~ ${SUB_MCHID_REGEX} ]]; then
      WARNINGS+=("提示：第 ${line_no} 行 sub_mchid 非标准10位数字（storeId=${store}, sub_mchid=${sub_mchid}）")
    fi
  fi

  if [[ -v STORE_TO_SUB["${store}"] ]]; then
    if [[ "${STORE_TO_SUB[${store}]}" != "${sub_mchid}" ]]; then
      ERRORS+=("storeId=${store} 在 CSV 内冲突：第 ${STORE_LINE[${store}]} 行=${STORE_TO_SUB[${store}]}, 第 ${line_no} 行=${sub_mchid}")
      continue
    fi
  else
    STORE_TO_SUB["${store}"]="${sub_mchid}"
    STORE_LINE["${store}"]="${line_no}"
  fi

  if [[ ${STRICT_SUBMCHID_UNIQUE} -eq 1 ]]; then
    if [[ -v SUB_TO_STORE["${sub_mchid}"] && "${SUB_TO_STORE[${sub_mchid}]}" != "${store}" ]]; then
      ERRORS+=("strict 模式冲突：sub_mchid=${sub_mchid} 同时映射到 storeId=${SUB_TO_STORE[${sub_mchid}]} 与 storeId=${store}")
    else
      SUB_TO_STORE["${sub_mchid}"]="${store}"
    fi
  else
    if [[ -v SUB_TO_STORE["${sub_mchid}"] && "${SUB_TO_STORE[${sub_mchid}]}" != "${store}" ]]; then
      WARNINGS+=("提示：sub_mchid=${sub_mchid} 被多个门店复用（${SUB_TO_STORE[${sub_mchid}]}, ${store}）")
    else
      SUB_TO_STORE["${sub_mchid}"]="${store}"
    fi
  fi
done < "${CSV_FILE}"

if [[ ${#STORE_TO_SUB[@]} -eq 0 ]]; then
  ERRORS+=("CSV 没有可导入的有效数据")
fi

write_report_files
printf "store_id\tsub_mchid\n" > "${NORMALIZED_FILE}"
for store_id in $(printf "%s\n" "${!STORE_TO_SUB[@]}" | sort -n); do
  printf "%s\t%s\n" "${store_id}" "${STORE_TO_SUB[${store_id}]}" >> "${NORMALIZED_FILE}"
done

if [[ ${#ERRORS[@]} -gt 0 ]]; then
  echo "发现输入错误："
  for msg in "${ERRORS[@]}"; do
    echo "  - ${msg}"
  done
  write_summary "input_error" "0" "0" "0" "0"
  echo "运行产物目录: ${RUN_DIR}"
  exit 1
fi

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-crmeb_java}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:-}"
MYSQL_DEFAULTS_FILE="${MYSQL_DEFAULTS_FILE:-}"

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
MYSQL_CMD+=("${DB_NAME}")

declare -A ACTIVE_STORE_EXISTS
set +e
active_store_rows="$("${MYSQL_CMD[@]}" -N -e "SELECT id FROM eb_system_store WHERE IFNULL(is_del,0)=0;" 2>/tmp/payment_store_mapping_import_db.err)"
active_store_rc=$?
set -e
if [[ ${active_store_rc} -ne 0 ]]; then
  ERRORS+=("查询门店失败：$(tr '\n' ' ' < /tmp/payment_store_mapping_import_db.err)")
  write_report_files
  write_summary "db_error" "0" "0" "0" "0"
  echo "运行产物目录: ${RUN_DIR}"
  exit 1
fi
while IFS=$'\t' read -r active_store_id; do
  [[ -z "${active_store_id}" ]] && continue
  ACTIVE_STORE_EXISTS["${active_store_id}"]=1
done <<< "${active_store_rows}"

for store_id in $(printf "%s\n" "${!STORE_TO_SUB[@]}" | sort -n); do
  if [[ -z "${ACTIVE_STORE_EXISTS["${store_id}"]-}" ]]; then
    MISSING_STORE_COUNT=$((MISSING_STORE_COUNT + 1))
    if [[ ${STRICT_STORE_EXISTS} -eq 1 ]]; then
      ERRORS+=("storeId=${store_id} 在 eb_system_store 不存在或已删除")
    else
      WARNINGS+=("提示：storeId=${store_id} 在 eb_system_store 不存在或已删除（已按 strict_store_exists=0 放行）")
    fi
  fi
done

write_report_files
if [[ ${#ERRORS[@]} -gt 0 ]]; then
  echo "发现输入错误："
  for msg in "${ERRORS[@]}"; do
    echo "  - ${msg}"
  done
  write_summary "input_error" "0" "0" "0" "0"
  echo "运行产物目录: ${RUN_DIR}"
  exit 1
fi

declare -a CONFIG_NAMES
for store_id in "${!STORE_TO_SUB[@]}"; do
  CONFIG_NAMES+=("'${CONFIG_PREFIX}${store_id}'")
done
config_name_sql="$(IFS=,; echo "${CONFIG_NAMES[*]}")"

declare -A DB_STORE_TO_SUB
declare -A DB_STORE_EXISTS
db_rows="$("${MYSQL_CMD[@]}" -N -e "SELECT name, IFNULL(value,'') FROM eb_system_config WHERE name IN (${config_name_sql});")"
while IFS=$'\t' read -r conf_name conf_value; do
  if [[ -z "${conf_name}" ]]; then
    continue
  fi
  store_id="${conf_name#${CONFIG_PREFIX}}"
  DB_STORE_TO_SUB["${store_id}"]="${conf_value}"
  DB_STORE_EXISTS["${store_id}"]=1
done <<< "${db_rows}"

declare -a INSERTS=()
declare -a CONFLICTS=()
declare -a UNCHANGED=()
declare -a OVERWRITES=()

for store_id in $(printf "%s\n" "${!STORE_TO_SUB[@]}" | sort -n); do
  target_sub="${STORE_TO_SUB[${store_id}]}"
  db_sub="${DB_STORE_TO_SUB[${store_id}]-}"
  if [[ ! -v DB_STORE_EXISTS["${store_id}"] ]]; then
    INSERTS+=("storeId=${store_id}: <empty> -> ${target_sub}")
  elif [[ "${db_sub}" == "${target_sub}" ]]; then
    UNCHANGED+=("storeId=${store_id}: ${target_sub} (unchanged)")
  else
    if [[ "${CONFLICT_STRATEGY}" == "overwrite" ]]; then
      OVERWRITES+=("storeId=${store_id}: DB=${db_sub} -> CSV=${target_sub}")
    else
      CONFLICTS+=("storeId=${store_id}: DB=${db_sub}, CSV=${target_sub}")
    fi
  fi
done

printf "store_id\tdb_value\tcsv_value\taction\n" > "${PLAN_FILE}"
for store_id in $(printf "%s\n" "${!STORE_TO_SUB[@]}" | sort -n); do
  target_sub="${STORE_TO_SUB[${store_id}]}"
  db_sub="${DB_STORE_TO_SUB[${store_id}]-}"
  if [[ ! -v DB_STORE_EXISTS["${store_id}"] ]]; then
    printf "%s\t%s\t%s\tinsert\n" "${store_id}" "<empty>" "${target_sub}" >> "${PLAN_FILE}"
  elif [[ "${db_sub}" == "${target_sub}" ]]; then
    printf "%s\t%s\t%s\tunchanged\n" "${store_id}" "${db_sub}" "${target_sub}" >> "${PLAN_FILE}"
  else
    if [[ "${CONFLICT_STRATEGY}" == "overwrite" ]]; then
      printf "%s\t%s\t%s\toverwrite\n" "${store_id}" "${db_sub}" "${target_sub}" >> "${PLAN_FILE}"
    else
      printf "%s\t%s\t%s\tconflict\n" "${store_id}" "${db_sub}" "${target_sub}" >> "${PLAN_FILE}"
    fi
  fi
done

printf "store_id\tdb_value\tcsv_value\n" > "${CONFLICTS_FILE}"
if [[ ${#CONFLICTS[@]} -gt 0 ]]; then
  for c in "${CONFLICTS[@]}"; do
    # c: storeId=1: DB=xxx, CSV=yyy
    store_part="$(printf '%s' "${c}" | cut -d':' -f1 | sed 's/storeId=//')"
    db_part="$(printf '%s' "${c}" | sed -n 's/.*DB=\([^,]*\), CSV=.*/\1/p')"
    csv_part="$(printf '%s' "${c}" | sed -n 's/.*CSV=\(.*\)$/\1/p')"
    printf "%s\t%s\t%s\n" "${store_part}" "${db_part}" "${csv_part}" >> "${CONFLICTS_FILE}"
  done
fi

echo "================ 导入预检结果 ================"
echo "CSV: ${CSV_FILE}"
echo "模式: $([[ ${APPLY} -eq 1 ]] && echo "apply" || echo "dry-run")"
echo "strict-submchid-unique: ${STRICT_SUBMCHID_UNIQUE}"
echo "strict-submchid-format: ${STRICT_SUBMCHID_FORMAT}"
echo "strict-store-exists: ${STRICT_STORE_EXISTS}"
echo "reject-placeholder-submchid: ${REJECT_PLACEHOLDER_SUBMCHID}"
echo "conflict-strategy: ${CONFLICT_STRATEGY}"
echo "总门店数: ${#STORE_TO_SUB[@]}"
echo "新增配置: ${#INSERTS[@]}"
echo "保持不变: ${#UNCHANGED[@]}"
echo "覆盖更新: ${#OVERWRITES[@]}"
echo "DB 冲突: ${#CONFLICTS[@]}"
echo "占位号命中: ${PLACEHOLDER_HIT_COUNT}"
echo "无效门店: ${MISSING_STORE_COUNT}"

if [[ ${#WARNINGS[@]} -gt 0 ]]; then
  echo "---- 提示 ----"
  for msg in "${WARNINGS[@]}"; do
    echo "  - ${msg}"
  done
fi

if [[ ${#CONFLICTS[@]} -gt 0 ]]; then
  echo "---- 冲突明细（需先处理） ----"
  for msg in "${CONFLICTS[@]}"; do
    echo "  - ${msg}"
  done
fi
if [[ ${#OVERWRITES[@]} -gt 0 ]]; then
  echo "---- 覆盖更新（按策略允许） ----"
  for msg in "${OVERWRITES[@]}"; do
    echo "  - ${msg}"
  done
fi

if [[ ${APPLY} -eq 0 ]]; then
  echo "dry-run 完成，未写入数据库。"
  write_summary "dry_run" "${#CONFLICTS[@]}" "${#INSERTS[@]}" "${#UNCHANGED[@]}" "${#OVERWRITES[@]}"
  echo "运行产物目录: ${RUN_DIR}"
  if [[ ${#CONFLICTS[@]} -gt 0 ]]; then
    exit 2
  fi
  exit 0
fi

if [[ ${#CONFLICTS[@]} -gt 0 ]]; then
  echo "存在冲突，拒绝 apply。请先修正 CSV 或数据库映射。"
  write_summary "apply_blocked_conflict" "${#CONFLICTS[@]}" "${#INSERTS[@]}" "${#UNCHANGED[@]}" "${#OVERWRITES[@]}"
  echo "运行产物目录: ${RUN_DIR}"
  exit 1
fi

if [[ ${CONFIRM} -ne 1 ]]; then
  confirm_flags=""
  if [[ ${STRICT_SUBMCHID_UNIQUE} -eq 1 ]]; then
    confirm_flags="${confirm_flags} --strict-submchid-unique"
  fi
  if [[ ${STRICT_SUBMCHID_FORMAT} -eq 1 ]]; then
    confirm_flags="${confirm_flags} --strict-submchid-format"
  fi
  confirm_flags="${confirm_flags} --strict-store-exists ${STRICT_STORE_EXISTS}"
  confirm_flags="${confirm_flags} --reject-placeholder-submchid ${REJECT_PLACEHOLDER_SUBMCHID}"
  echo "已生成生效前摘要，但未执行写入（缺少 --confirm）。"
  echo "请确认 plan.tsv / conflicts.tsv / warnings.txt 后，执行："
  echo "  ./shell/payment_store_mapping_import.sh --csv \"${CSV_FILE}\" --apply --confirm${confirm_flags} --conflict-strategy ${CONFLICT_STRATEGY}"
  write_summary "apply_wait_confirm" "0" "${#INSERTS[@]}" "${#UNCHANGED[@]}" "${#OVERWRITES[@]}"
  echo "运行产物目录: ${RUN_DIR}"
  exit 3
fi

echo "开始写入数据库..."
: > "${ROLLBACK_SQL}"
cat > "${ROLLBACK_SQL}" <<SQL
-- store mapping rollback generated at ${RUN_ID}
-- source csv: ${CSV_FILE}
START TRANSACTION;
SQL

for store_id in $(printf "%s\n" "${!STORE_TO_SUB[@]}" | sort -n); do
  sub_raw="${STORE_TO_SUB[${store_id}]}"
  sub_esc="$(sql_escape "${sub_raw}")"
  config_name="${CONFIG_PREFIX}${store_id}"
  title="门店${store_id}子商户号"
  title_esc="$(sql_escape "${title}")"
  config_esc="$(sql_escape "${config_name}")"
  if [[ -v DB_STORE_EXISTS["${store_id}"] ]]; then
    old_raw="${DB_STORE_TO_SUB[${store_id}]}"
    old_esc="$(sql_escape "${old_raw}")"
    cat >> "${ROLLBACK_SQL}" <<SQL
UPDATE eb_system_config
SET value='${old_esc}', update_time=NOW()
WHERE name='${config_esc}';
SQL
  else
    cat >> "${ROLLBACK_SQL}" <<SQL
DELETE FROM eb_system_config
WHERE name='${config_esc}';
SQL
  fi
  "${MYSQL_CMD[@]}" <<SQL
UPDATE eb_system_config
SET value='${sub_esc}', update_time=NOW()
WHERE name='${config_esc}';

INSERT INTO eb_system_config(name, title, form_id, value, status, create_time, update_time)
SELECT '${config_esc}', '${title_esc}', 0, '${sub_esc}', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name='${config_esc}');
SQL
done

cat >> "${ROLLBACK_SQL}" <<'SQL'
COMMIT;
SQL

echo "写入完成，回查如下："
"${MYSQL_CMD[@]}" -N -e "SELECT name, value FROM eb_system_config WHERE name IN (${config_name_sql}) ORDER BY name;"
write_summary "apply_success" "0" "${#INSERTS[@]}" "${#UNCHANGED[@]}" "${#OVERWRITES[@]}"
echo "回滚脚本: ${ROLLBACK_SQL}"
echo "运行产物目录: ${RUN_DIR}"
echo "完成。"
