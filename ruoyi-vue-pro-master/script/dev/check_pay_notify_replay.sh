#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
MYSQL_BIN="${MYSQL_BIN:-mysql}"

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-}"
DB_NAME="${DB_NAME:-ruoyi-vue-pro}"

MERCHANT_ORDER_ID=""
LOOKBACK_HOURS="${LOOKBACK_HOURS:-48}"
GRACE_MINUTES="${GRACE_MINUTES:-10}"

SQL_FILE="$ROOT_DIR/script/sql/pay_notify_replay_acceptance.sql"

usage() {
  cat <<'USAGE'
Usage:
  script/dev/check_pay_notify_replay.sh [options]

Required:
  --merchant-order-id <id>    商户单号（pay_order.merchant_order_id）

Options:
  --db-host <host>            MySQL host (default: 127.0.0.1)
  --db-port <port>            MySQL port (default: 3306)
  --db-user <user>            MySQL user (default: root)
  --db-password <password>    MySQL password (default: env DB_PASSWORD)
  --db-name <name>            Database name (default: ruoyi-vue-pro)
  --lookback-hours <hours>    观察窗口（默认 48）
  --grace-minutes <minutes>   回调任务宽限期（默认 10）
  -h, --help                  Show help

Exit Code:
  0  : 验收通过（无 BLOCK，可能有 WARN）
  2  : 存在 BLOCK 问题
  1+ : 脚本参数或执行错误
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --merchant-order-id)
      MERCHANT_ORDER_ID="$2"
      shift 2
      ;;
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
    --lookback-hours)
      LOOKBACK_HOURS="$2"
      shift 2
      ;;
    --grace-minutes)
      GRACE_MINUTES="$2"
      shift 2
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

if [[ -z "$MERCHANT_ORDER_ID" ]]; then
  echo "Missing required --merchant-order-id" >&2
  usage
  exit 1
fi

if [[ ! -f "$SQL_FILE" ]]; then
  echo "Missing SQL file: $SQL_FILE" >&2
  exit 1
fi

MYSQL_ARGS=(
  --protocol=TCP
  -h"$DB_HOST"
  -P"$DB_PORT"
  -u"$DB_USER"
  --batch
  --raw
  --skip-column-names
)
if [[ -n "$DB_PASSWORD" ]]; then
  MYSQL_ARGS+=(-p"$DB_PASSWORD")
fi

ESCAPED_ORDER_ID="${MERCHANT_ORDER_ID//\'/\'\'}"

ISSUES="$("$MYSQL_BIN" "${MYSQL_ARGS[@]}" "$DB_NAME" <<SQL
SET @merchant_order_id = '${ESCAPED_ORDER_ID}';
SET @lookback_hours = ${LOOKBACK_HOURS};
SET @grace_minutes = ${GRACE_MINUTES};
SOURCE ${SQL_FILE};
SQL
)"

echo "== Pay Notify Replay Acceptance =="
echo "db=${DB_NAME}@${DB_HOST}:${DB_PORT}"
echo "merchant_order_id=${MERCHANT_ORDER_ID}"
echo "lookback_hours=${LOOKBACK_HOURS}, grace_minutes=${GRACE_MINUTES}"
echo

if [[ -z "${ISSUES}" ]]; then
  echo "Result: PASS (no BLOCK/WARN issues)"
  exit 0
fi

echo -e "severity\tcode\tdetail"
printf "%s\n" "$ISSUES"
echo

BLOCK_COUNT="$(printf "%s\n" "$ISSUES" | awk -F'\t' '$1=="BLOCK"{c++} END{print c+0}')"
WARN_COUNT="$(printf "%s\n" "$ISSUES" | awk -F'\t' '$1=="WARN"{c++} END{print c+0}')"

echo "Summary: BLOCK=${BLOCK_COUNT}, WARN=${WARN_COUNT}"

if [[ "$BLOCK_COUNT" -gt 0 ]]; then
  echo "Result: FAIL (has BLOCK issues)"
  exit 2
fi

echo "Result: PASS_WITH_WARN"
exit 0
