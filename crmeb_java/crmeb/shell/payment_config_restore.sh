#!/usr/bin/env bash
set -euo pipefail

# 支付配置回滚执行脚本

SQL_FILE=""
LATEST=0
APPLY=0

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-crmeb_java}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:-}"
MYSQL_DEFAULTS_FILE="${MYSQL_DEFAULTS_FILE:-}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SNAPSHOT_ROOT="${ROOT_DIR}/runtime/payment_config_snapshot"

usage() {
  cat <<'USAGE'
用法：
  ./shell/payment_config_restore.sh [--sql FILE | --latest] [--apply]

参数：
  --sql FILE   指定 restore.sql
  --latest     使用最近一次快照的 restore.sql
  --apply      真正执行回滚（默认仅预览）

环境变量：
  DB_HOST DB_PORT DB_NAME DB_USER DB_PASS MYSQL_DEFAULTS_FILE

退出码：
  0  成功
  1  失败
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --sql)
      SQL_FILE="$2"
      shift 2
      ;;
    --latest)
      LATEST=1
      shift
      ;;
    --apply)
      APPLY=1
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

if [[ ${LATEST} -eq 1 && -n "${SQL_FILE}" ]]; then
  echo "错误：--latest 与 --sql 不能同时使用"
  exit 1
fi

if [[ ${LATEST} -eq 1 ]]; then
  mapfile -t candidates < <(find "${SNAPSHOT_ROOT}" -type f -name 'restore.sql' -printf '%T@ %p\n' | sort -n | awk '{print $2}')
  if [[ ${#candidates[@]} -eq 0 ]]; then
    echo "错误：未找到可用 restore.sql"
    exit 1
  fi
  SQL_FILE="${candidates[$(( ${#candidates[@]} - 1 ))]}"
fi

if [[ -z "${SQL_FILE}" ]]; then
  echo "错误：请通过 --sql 或 --latest 指定 restore.sql"
  exit 1
fi
if [[ ! -f "${SQL_FILE}" ]]; then
  echo "错误：restore.sql 不存在 -> ${SQL_FILE}"
  exit 1
fi
if [[ -n "${MYSQL_DEFAULTS_FILE}" && ! -f "${MYSQL_DEFAULTS_FILE}" ]]; then
  echo "参数错误: MYSQL_DEFAULTS_FILE 文件不存在 -> ${MYSQL_DEFAULTS_FILE}"
  exit 1
fi

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
MYSQL_CMD+=("${DB_NAME}" --default-character-set=utf8mb4)
if [[ -n "${DB_PASS}" && -z "${MYSQL_DEFAULTS_FILE}" ]]; then
  MYSQL_CMD+=(--password="${DB_PASS}")
fi

echo "[config-restore] restore_sql=${SQL_FILE}"
if [[ ${APPLY} -eq 0 ]]; then
  echo "[config-restore] dry-run preview (未执行):"
  sed -n '1,80p' "${SQL_FILE}"
  exit 0
fi

"${MYSQL_CMD[@]}" < "${SQL_FILE}"
echo "[config-restore] 回滚执行完成"

exit 0
