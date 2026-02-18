#!/usr/bin/env bash
set -euo pipefail

# 门店映射回滚执行脚本

SQL_FILE=""
APPLY=0

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-crmeb_java}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:-}"
MYSQL_DEFAULTS_FILE="${MYSQL_DEFAULTS_FILE:-}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUNTIME_ROOT="${ROOT_DIR}/runtime/payment_store_mapping"

usage() {
  cat <<'USAGE'
用法：
  ./shell/payment_store_mapping_rollback.sh [--sql FILE | --latest] [--apply]

参数：
  --sql FILE     指定 rollback.sql
  --latest       使用最近一次导入产物中的 rollback.sql
  --apply        真正执行（默认 dry-run）

环境变量：
  DB_HOST DB_PORT DB_NAME DB_USER DB_PASS MYSQL_DEFAULTS_FILE
USAGE
}

use_latest=0
while [[ $# -gt 0 ]]; do
  case "$1" in
    --sql)
      SQL_FILE="$2"
      shift 2
      ;;
    --latest)
      use_latest=1
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

if [[ -n "${SQL_FILE}" && ${use_latest} -eq 1 ]]; then
  echo "错误：--sql 与 --latest 只能二选一"
  exit 1
fi

if [[ ${use_latest} -eq 1 ]]; then
  shopt -s nullglob
  candidates=("${RUNTIME_ROOT}"/import-*/rollback.sql)
  shopt -u nullglob
  if [[ ${#candidates[@]} -eq 0 ]]; then
    echo "错误：未找到可用 rollback.sql"
    exit 1
  fi
  SQL_FILE="$(ls -1t "${candidates[@]}" | head -n1)"
fi

if [[ -z "${SQL_FILE}" ]]; then
  echo "错误：请通过 --sql 或 --latest 指定回滚文件"
  exit 1
fi
if [[ ! -f "${SQL_FILE}" ]]; then
  echo "错误：回滚文件不存在 -> ${SQL_FILE}"
  exit 1
fi
if [[ -n "${MYSQL_DEFAULTS_FILE}" && ! -f "${MYSQL_DEFAULTS_FILE}" ]]; then
  echo "错误：MYSQL_DEFAULTS_FILE 文件不存在 -> ${MYSQL_DEFAULTS_FILE}"
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
MYSQL_CMD+=("${DB_NAME}")
if [[ -n "${DB_PASS}" && -z "${MYSQL_DEFAULTS_FILE}" ]]; then
  MYSQL_CMD+=(--password="${DB_PASS}")
fi

echo "回滚SQL: ${SQL_FILE}"
if [[ ${APPLY} -eq 0 ]]; then
  echo "dry-run 模式，未执行。可先审阅 SQL 后加 --apply。"
  sed -n '1,120p' "${SQL_FILE}"
  exit 0
fi

"${MYSQL_CMD[@]}" < "${SQL_FILE}"
echo "回滚执行完成。"
