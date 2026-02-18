#!/usr/bin/env bash
set -euo pipefail

# 小程序首单支付前置检查（crmeb_java/app）
# 检查项：
# 1) app/manifest.json 的 mp-weixin.appid
# 2) app/config/app.js 的 API 域名
# 3) 后端支付配置与前端 AppID 一致性

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-crmeb_java}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:-}"
MYSQL_DEFAULTS_FILE="${MYSQL_DEFAULTS_FILE:-}"

EXPECTED_APPID="${EXPECTED_APPID:-}"
STRICT=0

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_DIR="${ROOT_DIR}/../app"
APP_MANIFEST="${APP_MANIFEST:-${APP_DIR}/manifest.json}"
APP_CONFIG="${APP_CONFIG:-${APP_DIR}/config/app.js}"

PASS_COUNT=0
WARN_COUNT=0
FAIL_COUNT=0

usage() {
  cat <<'USAGE'
用法：
  ./shell/payment_mp_app_precheck.sh [--expected-appid wxxxx] [--strict]

参数：
  --expected-appid APPID     期望的小程序 AppID（可选）
  --strict                   WARN 也视为失败（退出码=2）
  -h, --help                 查看帮助

环境变量：
  DB_HOST DB_PORT DB_NAME DB_USER DB_PASS MYSQL_DEFAULTS_FILE
  APP_MANIFEST APP_CONFIG EXPECTED_APPID

退出码：
  0 通过（或仅 WARN）
  2 存在 FAIL（或 strict 下存在 WARN）
  1 脚本错误
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --expected-appid)
      EXPECTED_APPID="${2:-}"
      shift 2
      ;;
    --strict)
      STRICT=1
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

add_result() {
  local level="$1"
  local item="$2"
  local detail="$3"
  echo "[${level}] ${item}: ${detail}"
  case "${level}" in
    PASS) PASS_COUNT=$((PASS_COUNT + 1)) ;;
    WARN) WARN_COUNT=$((WARN_COUNT + 1)) ;;
    FAIL) FAIL_COUNT=$((FAIL_COUNT + 1)) ;;
  esac
}

normalize_switch() {
  local v="${1:-}"
  v="${v//\'/}"
  v="${v//\"/}"
  v="$(printf '%s' "${v}" | tr -d '[:space:]')"
  printf '%s' "${v}"
}

echo "== 小程序支付前置检查 =="
echo "APP_MANIFEST=${APP_MANIFEST}"
echo "APP_CONFIG=${APP_CONFIG}"

if [[ ! -f "${APP_MANIFEST}" ]]; then
  add_result "FAIL" "manifest 文件" "不存在: ${APP_MANIFEST}"
else
  mp_block="$(sed -n '/"mp-weixin"[[:space:]]*:[[:space:]]*{/,/^[[:space:]]*}[[:space:]]*,\?[[:space:]]*$/p' "${APP_MANIFEST}")"
  mp_appid="$(printf '%s\n' "${mp_block}" | sed -n 's/.*"appid"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -n 1)"
  if [[ -z "${mp_appid}" ]]; then
    add_result "FAIL" "mp-weixin.appid" "未解析到 appid"
  else
    add_result "PASS" "mp-weixin.appid" "${mp_appid}"
  fi
fi

if [[ ! -f "${APP_CONFIG}" ]]; then
  add_result "FAIL" "app.js 文件" "不存在: ${APP_CONFIG}"
else
  api_domain="$(sed -n "s/^[[:space:]]*let[[:space:]]\\+domain[[:space:]]*=[[:space:]]*'\\([^']*\\)'.*/\\1/p" "${APP_CONFIG}" | head -n 1)"
  if [[ -z "${api_domain}" ]]; then
    add_result "WARN" "API 域名" "未从 app.js 解析到 let domain"
  elif [[ "${api_domain}" =~ ^https:// ]]; then
    add_result "PASS" "API 域名" "${api_domain}"
  else
    add_result "WARN" "API 域名" "${api_domain}（真机支付建议 HTTPS 公网域名）"
  fi
fi

if [[ -n "${EXPECTED_APPID}" && -n "${mp_appid:-}" ]]; then
  if [[ "${EXPECTED_APPID}" == "${mp_appid}" ]]; then
    add_result "PASS" "AppID 对齐" "manifest 与期望一致"
  else
    add_result "FAIL" "AppID 对齐" "manifest=${mp_appid} != expected=${EXPECTED_APPID}"
  fi
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
if [[ -n "${DB_PASS}" && -z "${MYSQL_DEFAULTS_FILE}" ]]; then
  MYSQL_CMD+=(--password="${DB_PASS}")
fi
MYSQL_CMD+=("${DB_NAME}" --default-character-set=utf8mb4 --batch --raw --skip-column-names)

declare -A CFG
db_ok=1
set +e
db_ping="$("${MYSQL_CMD[@]}" -e "SELECT 1;" 2>/tmp/payment_mp_app_precheck_db.err)"
db_rc=$?
set -e
if [[ ${db_rc} -ne 0 || "${db_ping}" != "1" ]]; then
  db_ok=0
  add_result "WARN" "数据库连接" "连接失败，跳过后端配置比对: $(tr '\n' ' ' </tmp/payment_mp_app_precheck_db.err)"
else
  add_result "PASS" "数据库连接" "连接正常"
fi

if [[ ${db_ok} -eq 1 ]]; then
  cfg_rows="$("${MYSQL_CMD[@]}" -e "
SELECT name, IFNULL(value,'')
FROM eb_system_config
WHERE name IN (
  'pay_weixin_open',
  'pay_routine_appid',
  'pay_routine_mchid',
  'pay_routine_key',
  'api_url'
);")"
  while IFS=$'\t' read -r name value; do
    [[ -z "${name}" ]] && continue
    CFG["${name}"]="${value}"
  done <<< "${cfg_rows}"

  pay_weixin_open="$(normalize_switch "${CFG["pay_weixin_open"]-}")"
  pay_routine_appid="${CFG["pay_routine_appid"]-}"
  pay_routine_mchid="${CFG["pay_routine_mchid"]-}"
  pay_routine_key="${CFG["pay_routine_key"]-}"
  api_url="${CFG["api_url"]-}"

  if [[ "${pay_weixin_open}" == "1" ]]; then
    add_result "PASS" "支付开关" "pay_weixin_open=1"
  else
    add_result "WARN" "支付开关" "pay_weixin_open=${pay_weixin_open:-<empty>}"
  fi

  if [[ -n "${pay_routine_appid}" && -n "${pay_routine_mchid}" && -n "${pay_routine_key}" ]]; then
    add_result "PASS" "直连支付配置" "pay_routine_appid/mchid/key 已配置"
  else
    add_result "FAIL" "直连支付配置" "pay_routine_appid/mchid/key 不完整"
  fi

  if [[ -n "${mp_appid:-}" && -n "${pay_routine_appid}" ]]; then
    if [[ "${mp_appid}" == "${pay_routine_appid}" ]]; then
      add_result "PASS" "前后端 AppID 一致性" "manifest 与 eb_system_config 一致"
    else
      add_result "FAIL" "前后端 AppID 一致性" "manifest=${mp_appid} db=${pay_routine_appid}"
    fi
  fi

  if [[ -z "${api_url}" ]]; then
    add_result "FAIL" "回调域名" "api_url 为空"
  elif [[ "${api_url}" =~ ^https:// ]]; then
    add_result "PASS" "回调域名" "${api_url}"
  else
    add_result "WARN" "回调域名" "${api_url}（建议 HTTPS）"
  fi
fi

echo
echo "SUMMARY: PASS=${PASS_COUNT} WARN=${WARN_COUNT} FAIL=${FAIL_COUNT}"
if [[ ${FAIL_COUNT} -gt 0 ]]; then
  exit 2
fi
if [[ ${STRICT} -eq 1 && ${WARN_COUNT} -gt 0 ]]; then
  exit 2
fi
exit 0
