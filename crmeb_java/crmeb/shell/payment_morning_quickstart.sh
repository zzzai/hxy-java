#!/usr/bin/env bash
set -euo pipefail

# 明早一键联调脚本（服务商 + 子商户 / 小程序）
# 用法：
#   SP_MCHID=1900001111 SP_KEY=xxx SUB_MCHID=1900002222 \
#   APPID=wx97fb30aed3983c2c ENABLE_PAY=1 \
#   ./shell/payment_morning_quickstart.sh

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-crmeb_java}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:-}"
MYSQL_DEFAULTS_FILE="${MYSQL_DEFAULTS_FILE:-}"
APPID="${APPID:-wx97fb30aed3983c2c}"
ENABLE_PAY="${ENABLE_PAY:-0}"

SP_MCHID="${SP_MCHID:-}"
SP_KEY="${SP_KEY:-}"
SUB_MCHID="${SUB_MCHID:-}"

if [[ -z "${SP_MCHID}" || -z "${SP_KEY}" || -z "${SUB_MCHID}" ]]; then
  echo "缺少必填参数。请设置：SP_MCHID、SP_KEY、SUB_MCHID"
  exit 1
fi

sql_escape() {
  printf "%s" "$1" | sed "s/'/''/g"
}

APPID_ESC="$(sql_escape "${APPID}")"
SP_MCHID_ESC="$(sql_escape "${SP_MCHID}")"
SP_KEY_ESC="$(sql_escape "${SP_KEY}")"
SUB_MCHID_ESC="$(sql_escape "${SUB_MCHID}")"

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

PAY_SWITCH_VALUE="'0'"
if [[ "${ENABLE_PAY}" == "1" ]]; then
  PAY_SWITCH_VALUE="'1'"
fi

echo "[1/3] 写入支付配置..."
"${MYSQL_CMD[@]}" <<SQL
UPDATE eb_system_config SET value='${APPID_ESC}', update_time=NOW() WHERE name='pay_routine_sp_appid';
UPDATE eb_system_config SET value='${APPID_ESC}', update_time=NOW() WHERE name='pay_routine_sub_appid';
UPDATE eb_system_config SET value='${APPID_ESC}', update_time=NOW() WHERE name='pay_routine_appid';

UPDATE eb_system_config SET value='${SP_MCHID_ESC}', update_time=NOW() WHERE name='pay_routine_sp_mchid';
UPDATE eb_system_config SET value='${SP_KEY_ESC}', update_time=NOW() WHERE name='pay_routine_sp_key';
UPDATE eb_system_config SET value='${SUB_MCHID_ESC}', update_time=NOW() WHERE name='pay_routine_sub_mchid';

UPDATE eb_system_config SET value=${PAY_SWITCH_VALUE}, update_time=NOW() WHERE name='pay_weixin_open';
SQL

echo "[2/3] 检查关键配置..."
"${MYSQL_CMD[@]}" -N -e "
SELECT name, value
FROM eb_system_config
WHERE name IN (
  'pay_weixin_open',
  'pay_routine_sp_appid',
  'pay_routine_sp_mchid',
  'pay_routine_sp_key',
  'pay_routine_sub_appid',
  'pay_routine_sub_mchid',
  'pay_routine_appid'
)
ORDER BY name;
"

echo "[3/3] 下一步建议："
echo "1) 重启后端服务（admin/front）"
echo "2) 执行支付冒烟：下单 -> 回调 -> 查询 -> 退款"
echo "3) 如失败，优先检查商户平台授权关系与回调域名"
