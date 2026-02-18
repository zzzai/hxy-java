#!/usr/bin/env bash
set -euo pipefail

# 用户数据治理基线初始化脚本
# 用法：
#   DB_USER=root DB_PASS=xxx ./shell/data_governance_bootstrap.sh

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-crmeb_java}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:-}"
MYSQL_DEFAULTS_FILE="${MYSQL_DEFAULTS_FILE:-}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SQL_FILE="${ROOT_DIR}/sql/data_governance_compliance_v1.sql"

if [[ ! -f "${SQL_FILE}" ]]; then
  echo "SQL文件不存在: ${SQL_FILE}"
  exit 1
fi

MYSQL_CMD=(mysql)
if [[ -n "${MYSQL_DEFAULTS_FILE}" ]]; then
  MYSQL_CMD+=(--defaults-extra-file="${MYSQL_DEFAULTS_FILE}")
fi
MYSQL_CMD+=(-h "${DB_HOST}" -P "${DB_PORT}")
if [[ -n "${DB_USER}" ]]; then
  MYSQL_CMD+=(-u "${DB_USER}")
fi
if [[ -n "${DB_PASS}" && -z "${MYSQL_DEFAULTS_FILE}" ]]; then
  MYSQL_CMD+=(--password="${DB_PASS}")
fi
MYSQL_CMD+=("${DB_NAME}")

echo "[1/2] 执行数据治理SQL补丁..."
"${MYSQL_CMD[@]}" < "${SQL_FILE}"

echo "[2/2] 校验核心表..."
"${MYSQL_CMD[@]}" -N -e "
SELECT table_name
FROM information_schema.tables
WHERE table_schema='${DB_NAME}'
  AND table_name IN (
    'eb_field_governance_catalog',
    'eb_user_consent_record',
    'eb_data_access_ticket',
    'eb_data_deletion_ticket',
    'eb_label_policy'
  )
ORDER BY table_name;
"

echo "完成。建议下一步："
echo "1) 重启admin/front服务"
echo "2) 在管理端联调 /api/admin/data/governance/*"
echo "3) 在前台联调 /api/front/privacy/*"
