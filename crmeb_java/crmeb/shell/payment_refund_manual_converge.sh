#!/usr/bin/env bash
set -euo pipefail

# 人工退款收敛脚本：
# 用于“渠道已退款到账，但本地订单未收敛到 refund_status=2”的兜底处理。
# 动作：
# 1) 更新订单 refund_status=2
# 2) 写审计日志 refund_manual_converge
# 3) 投递退款后置任务队列（alterOrderRefundByUser）

ORDER_NO="${ORDER_NO:-}"
ORDER_ID="${ORDER_ID:-}"
REASON="${REASON:-}"
OPERATOR="${OPERATOR:-ops}"
FORCE=0
DRY_RUN=0
SKIP_QUEUE=0

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-crmeb_java}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:-}"
MYSQL_DEFAULTS_FILE="${MYSQL_DEFAULTS_FILE:-}"

REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
REDIS_PORT="${REDIS_PORT:-6379}"
REDIS_DB="${REDIS_DB:-6}"
REDIS_PASS="${REDIS_PASS:-}"

usage() {
  cat <<'USAGE'
用法：
  ./shell/payment_refund_manual_converge.sh [--order-no NO | --order-id ID] --reason TEXT
    [--operator NAME] [--force] [--skip-queue] [--dry-run]

参数：
  --order-no NO      业务单号或商户单号（order_id / out_trade_no）
  --order-id ID      eb_store_order.id
  --reason TEXT      人工收敛原因（必填）
  --operator NAME    操作人标识（默认 ops）
  --force            跳过部分安全校验（谨慎）
  --skip-queue       仅更新状态与审计，不投递退款后置任务
  --dry-run          仅输出执行计划，不落库

环境变量（可选）：
  DB_HOST DB_PORT DB_NAME DB_USER DB_PASS MYSQL_DEFAULTS_FILE
  REDIS_HOST REDIS_PORT REDIS_DB REDIS_PASS

示例：
  DB_PORT=33306 DB_USER=crmeb DB_PASS=crmeb123 REDIS_PORT=36379 \
  ./shell/payment_refund_manual_converge.sh \
    --order-no order59022177133595738851519 \
    --reason "渠道已退款到账，回调缺失人工收敛" \
    --operator "hxy"
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --order-no)
      ORDER_NO="$2"
      shift 2
      ;;
    --order-id)
      ORDER_ID="$2"
      shift 2
      ;;
    --reason)
      REASON="$2"
      shift 2
      ;;
    --operator)
      OPERATOR="$2"
      shift 2
      ;;
    --force)
      FORCE=1
      shift
      ;;
    --skip-queue)
      SKIP_QUEUE=1
      shift
      ;;
    --dry-run)
      DRY_RUN=1
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

if [[ -n "${ORDER_NO}" && -n "${ORDER_ID}" ]]; then
  echo "参数错误: --order-no 与 --order-id 只能二选一"
  exit 1
fi
if [[ -z "${ORDER_NO}" && -z "${ORDER_ID}" ]]; then
  echo "参数错误: 必须提供 --order-no 或 --order-id"
  exit 1
fi
if [[ -z "${REASON}" ]]; then
  echo "参数错误: --reason 必填"
  exit 1
fi
if [[ -n "${MYSQL_DEFAULTS_FILE}" && ! -f "${MYSQL_DEFAULTS_FILE}" ]]; then
  echo "参数错误: MYSQL_DEFAULTS_FILE 不存在 -> ${MYSQL_DEFAULTS_FILE}"
  exit 1
fi

if ! command -v mysql >/dev/null 2>&1; then
  echo "未找到 mysql 客户端"
  exit 1
fi
if [[ "${SKIP_QUEUE}" != "1" ]] && ! command -v redis-cli >/dev/null 2>&1; then
  echo "未找到 redis-cli，无法投递退款后置队列"
  exit 1
fi

MYSQL_CMD=(mysql)
if [[ -n "${MYSQL_DEFAULTS_FILE}" ]]; then
  MYSQL_CMD+=(--defaults-extra-file="${MYSQL_DEFAULTS_FILE}")
else
  MYSQL_CMD+=(-h "${DB_HOST}" -P "${DB_PORT}")
  [[ -n "${DB_USER}" ]] && MYSQL_CMD+=(-u "${DB_USER}")
  [[ -n "${DB_PASS}" ]] && MYSQL_CMD+=(--password="${DB_PASS}")
fi
MYSQL_CMD+=("${DB_NAME}" --default-character-set=utf8mb4 --batch --raw --skip-column-names)

sql_escape() {
  printf '%s' "$1" | sed "s/'/''/g"
}

ORDER_CONDITION=""
if [[ -n "${ORDER_ID}" ]]; then
  ORDER_CONDITION="so.id=${ORDER_ID}"
else
  ORDER_NO_ESC="$(sql_escape "${ORDER_NO}")"
  ORDER_CONDITION="(so.order_id='${ORDER_NO_ESC}' OR so.out_trade_no='${ORDER_NO_ESC}')"
fi

ORDER_ROW="$("${MYSQL_CMD[@]}" -e "
SELECT so.id, so.order_id, so.out_trade_no, so.pay_type, so.paid, so.refund_status, so.refund_price, so.pay_price
FROM eb_store_order so
WHERE ${ORDER_CONDITION}
ORDER BY so.id DESC
LIMIT 1;
")"

if [[ -z "${ORDER_ROW}" ]]; then
  echo "未找到目标订单"
  exit 1
fi

IFS=$'\t' read -r SO_ID SO_ORDER_ID SO_OUT_TRADE_NO SO_PAY_TYPE SO_PAID SO_REFUND_STATUS SO_REFUND_PRICE SO_PAY_PRICE <<< "${ORDER_ROW}"

echo "命中订单: id=${SO_ID}, order_id=${SO_ORDER_ID}, out_trade_no=${SO_OUT_TRADE_NO}, pay_type=${SO_PAY_TYPE}, paid=${SO_PAID}, refund_status=${SO_REFUND_STATUS}, refund_price=${SO_REFUND_PRICE}, pay_price=${SO_PAY_PRICE}"

if [[ "${FORCE}" != "1" ]]; then
  if [[ "${SO_PAY_TYPE}" != "weixin" ]]; then
    echo "安全拦截: 非微信支付订单，拒绝收敛（可用 --force 跳过）"
    exit 1
  fi
  if [[ "${SO_PAID}" != "1" ]]; then
    echo "安全拦截: 订单未支付，拒绝收敛（可用 --force 跳过）"
    exit 1
  fi
  if [[ "${SO_REFUND_STATUS}" == "2" ]]; then
    echo "订单已是退款成功态（refund_status=2），无需处理"
    exit 0
  fi
  if [[ "${SO_REFUND_PRICE}" == "0.00" || "${SO_REFUND_PRICE}" == "0" || -z "${SO_REFUND_PRICE}" ]]; then
    echo "安全拦截: refund_price<=0，默认拒绝收敛（可用 --force 跳过）"
    exit 1
  fi
fi

MSG="人工收敛退款成功 operator=${OPERATOR}; reason=${REASON}; from_status=${SO_REFUND_STATUS}"
MSG_ESC="$(sql_escape "${MSG}")"

UPDATE_SQL="
START TRANSACTION;
UPDATE eb_store_order
SET refund_status=2, update_time=NOW()
WHERE id=${SO_ID} AND refund_status<>2;
INSERT INTO eb_store_order_status(oid, change_type, change_message, create_time)
VALUES (${SO_ID}, 'refund_manual_converge', '${MSG_ESC}', NOW());
COMMIT;
"

if [[ "${DRY_RUN}" == "1" ]]; then
  echo "[DRY-RUN] 将执行 SQL："
  echo "${UPDATE_SQL}"
  if [[ "${SKIP_QUEUE}" != "1" ]]; then
    echo "[DRY-RUN] 将执行 redis：LPUSH alterOrderRefundByUser ${SO_ID}"
  fi
  exit 0
fi

"${MYSQL_CMD[@]}" -e "${UPDATE_SQL}"

if [[ "${SKIP_QUEUE}" != "1" ]]; then
  REDIS_CMD=(redis-cli -h "${REDIS_HOST}" -p "${REDIS_PORT}" -n "${REDIS_DB}")
  [[ -n "${REDIS_PASS}" ]] && REDIS_CMD+=(-a "${REDIS_PASS}")
  "${REDIS_CMD[@]}" LPUSH alterOrderRefundByUser "${SO_ID}" >/dev/null
fi

LATEST_ROW="$("${MYSQL_CMD[@]}" -e "
SELECT so.id, so.order_id, so.out_trade_no, so.refund_status, so.refund_price, so.update_time
FROM eb_store_order so WHERE so.id=${SO_ID};
")"
echo "收敛后订单: ${LATEST_ROW}"

echo "完成: manual_refund_converged=1 order_id=${SO_ORDER_ID} out_trade_no=${SO_OUT_TRADE_NO} pushed_queue=$((1-SKIP_QUEUE))"
