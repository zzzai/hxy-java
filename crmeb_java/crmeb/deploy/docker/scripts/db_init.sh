#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
CRMEB_DIR="$(cd "${BASE_DIR}/../.." && pwd)"
ENV_FILE="${BASE_DIR}/.env"
ENV_EXAMPLE="${BASE_DIR}/.env.example"

DRY_RUN=0
SKIP_QUARTZ_FIX=0
QUARTZ_FIX_ONLY=0
DROP_LOWER_QUARTZ_DUP=0
MODE="replay"

usage() {
  cat <<'USAGE'
用法：
  ./scripts/db_init.sh [--dry-run] [--mode strict|replay] [--force] [--skip-quartz-fix] [--quartz-fix-only] [--drop-lowercase-duplicates]

参数：
  --dry-run                    仅打印将执行的 SQL，不落库
  --mode strict|replay         strict=遇错即失败（CI），replay=允许幂等重放（运维，默认）
  --force                      兼容旧参数，等价于 --mode replay
  --skip-quartz-fix            跳过 qrtz_* -> QRTZ_* 自动修复
  --quartz-fix-only            仅执行 Quartz 表名修复，不导入 SQL
  --drop-lowercase-duplicates  当大写表已存在时，删除重复小写 qrtz_* 表
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run)
      DRY_RUN=1
      shift
      ;;
    --force)
      MODE="replay"
      shift
      ;;
    --mode)
      MODE="${2:-}"
      shift 2
      ;;
    --skip-quartz-fix)
      SKIP_QUARTZ_FIX=1
      shift
      ;;
    --quartz-fix-only)
      QUARTZ_FIX_ONLY=1
      shift
      ;;
    --drop-lowercase-duplicates)
      DROP_LOWER_QUARTZ_DUP=1
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

if [[ ! -f "${ENV_FILE}" ]]; then
  cp "${ENV_EXAMPLE}" "${ENV_FILE}"
  echo "未发现 ${ENV_FILE}，已按模板创建。请先修改配置后重试。"
  exit 1
fi

load_env_file() {
  local file="$1"
  while IFS= read -r line || [[ -n "${line}" ]]; do
    [[ -z "${line}" || "${line}" =~ ^[[:space:]]*# ]] && continue
    if [[ "${line}" =~ ^[A-Za-z_][A-Za-z0-9_]*= ]]; then
      local key="${line%%=*}"
      local value="${line#*=}"
      export "${key}=${value}"
    fi
  done < "${file}"
}

load_env_file "${ENV_FILE}"

if [[ "${MODE}" != "strict" && "${MODE}" != "replay" ]]; then
  echo "无效模式: ${MODE}，仅支持 strict/replay"
  exit 1
fi

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-${MYSQL_HOST_PORT:-33306}}"
DB_NAME="${DB_NAME:-${MYSQL_DATABASE:-crmeb_java}}"
DB_USER="${DB_USER:-${MYSQL_USER:-crmeb}}"
DB_PASS="${DB_PASS:-${MYSQL_PASSWORD:-}}"

MYSQL_CMD=(mysql -h"${DB_HOST}" -P"${DB_PORT}" -u"${DB_USER}")
if [[ -n "${DB_PASS}" ]]; then
  MYSQL_CMD+=("--password=${DB_PASS}")
fi
if [[ "${MODE}" == "replay" ]]; then
  MYSQL_CMD+=(--force)
fi
MYSQL_CMD+=("${DB_NAME}")

declare -a SQL_FILES=(
  "${CRMEB_DIR}/sql/Crmeb_v1.4.sql"
  "${CRMEB_DIR}/sql/database_migration_v1.0.sql"
  "${CRMEB_DIR}/sql/database_migration_v2.0.sql"
  "${CRMEB_DIR}/sql/booking_domain.sql"
  "${CRMEB_DIR}/sql/database_architecture_fix_v2.sql"
  "${CRMEB_DIR}/sql/database_compliance_guardrails_v1.sql"
  "${CRMEB_DIR}/sql/data_governance_compliance_v1.sql"
  "${CRMEB_DIR}/sql/data_governance_rbac_v1.sql"
)

echo "目标数据库: ${DB_HOST}:${DB_PORT}/${DB_NAME} (user=${DB_USER})"
echo "导入模式: DRY_RUN=${DRY_RUN}, MODE=${MODE}, QUARTZ_FIX_ONLY=${QUARTZ_FIX_ONLY}, DROP_LOWER_QUARTZ_DUP=${DROP_LOWER_QUARTZ_DUP}"

apply_sql() {
  local file="$1"
  if [[ ! -f "${file}" ]]; then
    if [[ "${MODE}" == "strict" ]]; then
      echo "[FAIL] SQL 文件不存在: ${file}"
      exit 4
    fi
    echo "[WARN] SQL 文件不存在，跳过: ${file}"
    return 0
  fi
  if [[ "${DRY_RUN}" == "1" ]]; then
    echo "[DRY-RUN] 将执行: ${file}"
    return 0
  fi
  echo "[APPLY] ${file}"
  "${MYSQL_CMD[@]}" < "${file}"
}

quartz_fix() {
  if [[ "${SKIP_QUARTZ_FIX}" == "1" ]]; then
    echo "[SKIP] 跳过 Quartz 表名修复"
    return 0
  fi
  if [[ "${DRY_RUN}" == "1" ]]; then
    echo "[DRY-RUN] 将检查并修复 qrtz_* -> QRTZ_*"
    return 0
  fi
  mapfile -t lower_tables < <("${MYSQL_CMD[@]}" -Nse "
SELECT table_name
FROM information_schema.tables
WHERE table_schema='${DB_NAME}' AND BINARY table_name LIKE 'qrtz_%'
ORDER BY table_name;
")

  if [[ "${#lower_tables[@]}" -eq 0 ]]; then
    echo "[INFO] 未检测到 qrtz_* 小写表"
    return 0
  fi

  for lower in "${lower_tables[@]}"; do
    local upper
    local upper_exists
    upper="$(printf '%s' "${lower}" | tr '[:lower:]' '[:upper:]')"
    upper_exists="$("${MYSQL_CMD[@]}" -Nse "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${DB_NAME}' AND table_name='${upper}';")"
    if [[ "${upper_exists}" == "1" ]]; then
      if [[ "${DROP_LOWER_QUARTZ_DUP}" == "1" ]]; then
        echo "[FIX] DROP TABLE ${lower}（目标大写表已存在）"
        "${MYSQL_CMD[@]}" -e "DROP TABLE \`${lower}\`;"
      else
        echo "[WARN] 跳过重命名（目标已存在）: ${lower} -> ${upper}"
      fi
      continue
    fi
    echo "[FIX] RENAME TABLE ${lower} -> ${upper}"
    "${MYSQL_CMD[@]}" -e "RENAME TABLE \`${lower}\` TO \`${upper}\`;"
  done
}

if [[ "${QUARTZ_FIX_ONLY}" != "1" ]]; then
  for sql in "${SQL_FILES[@]}"; do
    apply_sql "${sql}"
  done
else
  echo "[SKIP] 跳过 SQL 导入，仅执行 Quartz 表名修复"
fi

quartz_fix

if [[ "${DRY_RUN}" == "1" ]]; then
  echo "执行完成（dry-run）。"
  exit 0
fi

echo "=== 导入结果校验 ==="
"${MYSQL_CMD[@]}" -Nse "
SELECT CONCAT('QRTZ_UPPER=', COUNT(*))
FROM information_schema.tables
WHERE table_schema='${DB_NAME}' AND BINARY table_name LIKE 'QRTZ_%';

SELECT CONCAT('QRTZ_LOWER=', COUNT(*))
FROM information_schema.tables
WHERE table_schema='${DB_NAME}' AND BINARY table_name LIKE 'qrtz_%';

SELECT CONCAT('GOVERNANCE_PERMS=', COUNT(*))
FROM eb_system_menu
WHERE perms LIKE 'admin:data:governance:%';
"

if [[ "$("${MYSQL_CMD[@]}" -Nse "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${DB_NAME}' AND BINARY table_name LIKE 'qrtz_%';")" != "0" ]]; then
  echo "=== 未修复的小写 Quartz 表 ==="
  "${MYSQL_CMD[@]}" -Nse "
SELECT table_name
FROM information_schema.tables
WHERE table_schema='${DB_NAME}' AND BINARY table_name LIKE 'qrtz_%'
ORDER BY table_name;
"
fi

echo "数据库初始化完成。"
