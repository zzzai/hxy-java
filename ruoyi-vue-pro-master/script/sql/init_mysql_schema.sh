#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
MYSQL_BIN="${MYSQL_BIN:-mysql}"

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-}"
DB_NAME="${DB_NAME:-ruoyi-vue-pro}"
MODE="${MODE:-strict}"

SKIP_BASE=0
ONLY_MODULES=0

usage() {
  cat <<'USAGE'
Usage:
  script/sql/init_mysql_schema.sh [options]

Options:
  --db-host <host>         MySQL host (default: 127.0.0.1)
  --db-port <port>         MySQL port (default: 3306)
  --db-user <user>         MySQL user (default: root)
  --db-password <password> MySQL password (default: env DB_PASSWORD)
  --db-name <name>         Database name (default: ruoyi-vue-pro)
  --mode <mode>            strict | replay (default: strict)
  --skip-base              Skip importing sql/mysql/ruoyi-vue-pro.sql
  --only-modules           Import only module schema sql
  -h, --help               Show help

Notes:
  1) Base schema:    sql/mysql/ruoyi-vue-pro.sql
  2) Module schema:  sql/mysql/ruoyi-modules-member-pay-mall.sql
  3) strict: CI friendly. Non-empty DB fails fast.
  4) replay: Ops friendly. Use mysql --force for idempotent replays.
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --db-host)
      DB_HOST="$2"
      shift 2
      ;;
    --db-port)
      DB_PORT="$2"
      shift 2
      ;;
    --db-user)
      DB_USER="$2"
      shift 2
      ;;
    --db-password)
      DB_PASSWORD="$2"
      shift 2
      ;;
    --db-name)
      DB_NAME="$2"
      shift 2
      ;;
    --mode)
      MODE="$2"
      shift 2
      ;;
    --skip-base)
      SKIP_BASE=1
      shift
      ;;
    --only-modules)
      ONLY_MODULES=1
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage
      exit 1
      ;;
  esac
done

if [[ "$MODE" != "strict" && "$MODE" != "replay" ]]; then
  echo "Invalid --mode: $MODE (expected: strict|replay)" >&2
  exit 1
fi

BASE_SQL="$ROOT_DIR/sql/mysql/ruoyi-vue-pro.sql"
MODULE_SQL="$ROOT_DIR/sql/mysql/ruoyi-modules-member-pay-mall.sql"

if [[ ! -f "$MODULE_SQL" ]]; then
  echo "Missing module SQL: $MODULE_SQL" >&2
  echo "Run: script/sql/build_mysql_module_schema.sh" >&2
  exit 1
fi

MYSQL_ARGS=(
  --protocol=TCP
  -h"$DB_HOST"
  -P"$DB_PORT"
  -u"$DB_USER"
)
if [[ -n "$DB_PASSWORD" ]]; then
  MYSQL_ARGS+=(-p"$DB_PASSWORD")
fi

echo "[1/5] Ensure database exists: $DB_NAME"
"$MYSQL_BIN" "${MYSQL_ARGS[@]}" -e "CREATE DATABASE IF NOT EXISTS \`$DB_NAME\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"

echo "[2/5] Mode: $MODE"
if [[ "$MODE" == "strict" ]]; then
  TABLE_COUNT="$("$MYSQL_BIN" "${MYSQL_ARGS[@]}" -N -B -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${DB_NAME}'")"
  if [[ "${TABLE_COUNT:-0}" -gt 0 ]]; then
    echo "STRICT mode requires empty database. table_count=$TABLE_COUNT, db=$DB_NAME" >&2
    echo "Hint: use --mode replay for idempotent replays." >&2
    exit 2
  fi
fi

import_sql() {
  local file="$1"
  if [[ "$MODE" == "replay" ]]; then
    "$MYSQL_BIN" "${MYSQL_ARGS[@]}" --force "$DB_NAME" < "$file"
  else
    "$MYSQL_BIN" "${MYSQL_ARGS[@]}" "$DB_NAME" < "$file"
  fi
}

if [[ $ONLY_MODULES -eq 0 && $SKIP_BASE -eq 0 ]]; then
  if [[ ! -f "$BASE_SQL" ]]; then
    echo "Missing base SQL: $BASE_SQL" >&2
    exit 1
  fi
  echo "[3/5] Import base schema: $BASE_SQL"
  import_sql "$BASE_SQL"
else
  echo "[3/5] Skip base schema import"
fi

echo "[4/5] Import module schema: $MODULE_SQL"
import_sql "$MODULE_SQL"

echo "[5/5] Done. Database '$DB_NAME' initialized by mode '$MODE'."
