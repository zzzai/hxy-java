#!/usr/bin/env bash
set -euo pipefail

# 支付订单定位脚本
# 作用：按 order_id/out_trade_no/transaction_id 三种输入，在当前 MySQL 可见库中定位订单归属。

ORDER_NO="${ORDER_NO:-}"
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:-}"
MYSQL_DEFAULTS_FILE="${MYSQL_DEFAULTS_FILE:-}"

usage() {
  cat <<'USAGE'
用法：
  ./shell/payment_order_locator.sh --order-no <order_id|out_trade_no|transaction_id>

可选环境变量：
  DB_HOST DB_PORT DB_USER DB_PASS MYSQL_DEFAULTS_FILE
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --order-no)
      ORDER_NO="$2"
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

if [[ -z "${ORDER_NO}" ]]; then
  echo "缺少 --order-no"
  usage
  exit 1
fi

if ! command -v mysql >/dev/null 2>&1; then
  echo "未找到 mysql 客户端"
  exit 1
fi

MYSQL_BASE=(mysql)
if [[ -n "${MYSQL_DEFAULTS_FILE}" ]]; then
  MYSQL_BASE+=(--defaults-extra-file="${MYSQL_DEFAULTS_FILE}")
else
  MYSQL_BASE+=(-h "${DB_HOST}" -P "${DB_PORT}")
  if [[ -n "${DB_USER}" ]]; then
    MYSQL_BASE+=(-u "${DB_USER}")
  fi
  if [[ -n "${DB_PASS}" ]]; then
    MYSQL_BASE+=(--password="${DB_PASS}")
  fi
fi
MYSQL_BASE+=(--batch --raw --skip-column-names)

sql_escape() {
  printf '%s' "$1" | sed "s/'/''/g"
}

order_esc="$(sql_escape "${ORDER_NO}")"

db_list="$("${MYSQL_BASE[@]}" -e "
  SELECT DISTINCT t.table_schema
  FROM information_schema.tables t
  WHERE t.table_name IN ('eb_store_order', 'eb_wechat_pay_info')
  ORDER BY t.table_schema;
")"

if [[ -z "${db_list}" ]]; then
  echo "未发现包含 eb_store_order/eb_wechat_pay_info 的数据库。"
  exit 1
fi

found=0
echo "# payment_order_locator"
echo "input=${ORDER_NO}"
echo "host=${DB_HOST}:${DB_PORT}"
if [[ -n "${MYSQL_DEFAULTS_FILE}" ]]; then
  echo "defaults_file=${MYSQL_DEFAULTS_FILE}"
fi
echo

for db in ${db_list}; do
  store_hits="$("${MYSQL_BASE[@]}" "${db}" -e "
    SELECT COUNT(1)
    FROM eb_store_order so
    WHERE so.order_id='${order_esc}' OR IFNULL(so.out_trade_no,'')='${order_esc}';
  " 2>/dev/null || true)"
  pay_hits="$("${MYSQL_BASE[@]}" "${db}" -e "
    SELECT COUNT(1)
    FROM eb_wechat_pay_info p
    WHERE IFNULL(p.transaction_id,'')='${order_esc}' OR IFNULL(p.out_trade_no,'')='${order_esc}';
  " 2>/dev/null || true)"
  store_hits="${store_hits:-0}"
  pay_hits="${pay_hits:-0}"

  if [[ "${store_hits}" != "0" || "${pay_hits}" != "0" ]]; then
    found=1
    echo "## db=${db}"
    echo "store_hits=${store_hits}"
    echo "pay_hits=${pay_hits}"
    linked_store_hits=0
    if [[ "${pay_hits}" != "0" ]]; then
      linked_store_hits="$("${MYSQL_BASE[@]}" "${db}" -e "
        SELECT COUNT(1)
        FROM eb_store_order so
        WHERE IFNULL(so.out_trade_no,'') IN (
          SELECT IFNULL(p.out_trade_no,'')
          FROM eb_wechat_pay_info p
          WHERE IFNULL(p.transaction_id,'')='${order_esc}' OR IFNULL(p.out_trade_no,'')='${order_esc}'
        );
      " 2>/dev/null || true)"
      linked_store_hits="${linked_store_hits:-0}"
      echo "linked_store_hits=${linked_store_hits}"
    fi
    echo

    if [[ "${store_hits}" != "0" ]]; then
      echo "### eb_store_order"
      "${MYSQL_BASE[@]}" "${db}" -e "
        SELECT so.id, so.order_id, IFNULL(so.out_trade_no,''), so.pay_type, so.paid, so.pay_price, DATE_FORMAT(so.pay_time,'%Y-%m-%d %H:%i:%s')
        FROM eb_store_order so
        WHERE so.order_id='${order_esc}' OR IFNULL(so.out_trade_no,'')='${order_esc}'
        ORDER BY so.id DESC
        LIMIT 5;
      " || true
      echo
    fi

    if [[ "${pay_hits}" != "0" ]]; then
      echo "### eb_wechat_pay_info"
      "${MYSQL_BASE[@]}" "${db}" -e "
        SELECT p.id, IFNULL(p.out_trade_no,''), IFNULL(p.transaction_id,''), IFNULL(p.trade_state,''), IFNULL(p.time_end,'')
        FROM eb_wechat_pay_info p
        WHERE IFNULL(p.transaction_id,'')='${order_esc}' OR IFNULL(p.out_trade_no,'')='${order_esc}'
        ORDER BY p.id DESC
        LIMIT 10;
      " || true
      echo
    fi

    if [[ "${linked_store_hits}" != "0" ]]; then
      echo "### linked eb_store_order (via out_trade_no)"
      "${MYSQL_BASE[@]}" "${db}" -e "
        SELECT so.id, so.order_id, IFNULL(so.out_trade_no,''), so.pay_type, so.paid, so.pay_price, DATE_FORMAT(so.pay_time,'%Y-%m-%d %H:%i:%s')
        FROM eb_store_order so
        WHERE IFNULL(so.out_trade_no,'') IN (
          SELECT IFNULL(p.out_trade_no,'')
          FROM eb_wechat_pay_info p
          WHERE IFNULL(p.transaction_id,'')='${order_esc}' OR IFNULL(p.out_trade_no,'')='${order_esc}'
        )
        ORDER BY so.id DESC
        LIMIT 5;
      " || true
      echo
    fi
  fi
done

if [[ "${found}" -eq 0 ]]; then
  echo "未命中：当前 MySQL 可见库中都不存在该标识。"
  exit 2
fi

exit 0
