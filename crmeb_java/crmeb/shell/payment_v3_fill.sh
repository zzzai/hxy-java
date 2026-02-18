#!/usr/bin/env bash
set -euo pipefail

# 微信支付 v3 参数填充脚本（支持 dry-run/apply）

MODE="${MODE:-service-provider}"   # service-provider | direct
API_VERSION="${API_VERSION:-v3}"   # v2 | v3

SP_MCHID="${SP_MCHID:-}"
SP_APPID="${SP_APPID:-}"
SUB_MCHID="${SUB_MCHID:-}"
SUB_APPID="${SUB_APPID:-}"

SP_APIV3_KEY="${SP_APIV3_KEY:-}"
SP_SERIAL_NO="${SP_SERIAL_NO:-}"
SP_PRIVATE_KEY_PATH="${SP_PRIVATE_KEY_PATH:-}"
SP_PLATFORM_CERT_PATH="${SP_PLATFORM_CERT_PATH:-}"

ROUTINE_APIV3_KEY="${ROUTINE_APIV3_KEY:-}"
ROUTINE_SERIAL_NO="${ROUTINE_SERIAL_NO:-}"
ROUTINE_PRIVATE_KEY_PATH="${ROUTINE_PRIVATE_KEY_PATH:-}"
ROUTINE_PLATFORM_CERT_PATH="${ROUTINE_PLATFORM_CERT_PATH:-}"

ENABLE_PAY="${ENABLE_PAY:-0}"
API_URL="${API_URL:-}"

APPLY=0
STRICT_PATH=0

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-crmeb_java}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:-}"
MYSQL_DEFAULTS_FILE="${MYSQL_DEFAULTS_FILE:-}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUN_DIR="${ROOT_DIR}/runtime/payment_onboarding/v3-fill-$(date '+%Y%m%d%H%M%S')"
mkdir -p "${RUN_DIR}"
SQL_FILE="${RUN_DIR}/apply.sql"
SUMMARY_FILE="${RUN_DIR}/summary.txt"

usage() {
  cat <<'USAGE'
用法：
  ./shell/payment_v3_fill.sh [--apply] [--strict-path] [--mode service-provider|direct] [--api-version v2|v3]
    [--enable-pay 0|1] [--api-url https://api.example.com]
    --sp-mchid 19xxxxxxxx --sp-apiv3-key xxx --sp-serial-no xxx --sp-private-key-path /path/key.pem --sp-platform-cert-path /path/cert.pem

示例（服务商）：
  ./shell/payment_v3_fill.sh \
    --mode service-provider --api-version v3 \
    --sp-mchid '1739xxxxxx' \
    --sp-apiv3-key '***' \
    --sp-serial-no '***' \
    --sp-private-key-path '/data/wechatpay/sp_key.pem' \
    --sp-platform-cert-path '/data/wechatpay/platform_cert.pem' \
    --sub-mchid '1721xxxxxx'

示例（直连）：
  ./shell/payment_v3_fill.sh \
    --mode direct --api-version v3 \
    --routine-apiv3-key '***' \
    --routine-serial-no '***' \
    --routine-private-key-path '/data/wechatpay/mch_key.pem' \
    --routine-platform-cert-path '/data/wechatpay/platform_cert.pem'

参数：
  --mode                          service-provider | direct
  --api-version                   v2 | v3
  --enable-pay                    0 | 1
  --api-url                       回调域名根地址
  --strict-path                   私钥/平台证书路径不存在时直接失败
  --apply                         写库（默认 dry-run）

  --sp-mchid                      服务商商户号（service-provider 模式必填）
  --sp-appid                      服务商 appid（可选，默认使用原小程序 appid）
  --sub-mchid                     默认子商户号（可选，门店未配置映射时兜底）
  --sub-appid                     默认子商户 appid（可选）

  --sp-apiv3-key                  服务商 APIv3 密钥
  --sp-serial-no                  服务商证书序列号
  --sp-private-key-path           服务商私钥路径（pem）
  --sp-platform-cert-path         服务商平台证书路径

  --routine-apiv3-key             直连 APIv3 密钥
  --routine-serial-no             直连证书序列号
  --routine-private-key-path      直连私钥路径（pem）
  --routine-platform-cert-path    直连平台证书路径
USAGE
}

sql_escape() {
  printf "%s" "$1" | sed "s/'/''/g"
}

append_upsert_sql() {
  local key="$1"
  local title="$2"
  local value="$3"
  local value_esc
  value_esc="$(sql_escape "${value}")"
  cat >> "${SQL_FILE}" <<SQL
UPDATE eb_system_config SET value='${value_esc}', update_time=NOW() WHERE name='${key}';
INSERT INTO eb_system_config(name, title, form_id, value, status, create_time, update_time)
SELECT '${key}', '${title}', 0, '${value_esc}', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name='${key}');
SQL
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --mode) MODE="$2"; shift 2 ;;
    --api-version) API_VERSION="$2"; shift 2 ;;
    --enable-pay) ENABLE_PAY="$2"; shift 2 ;;
    --api-url) API_URL="$2"; shift 2 ;;
    --strict-path) STRICT_PATH=1; shift ;;
    --apply) APPLY=1; shift ;;

    --sp-mchid) SP_MCHID="$2"; shift 2 ;;
    --sp-appid) SP_APPID="$2"; shift 2 ;;
    --sub-mchid) SUB_MCHID="$2"; shift 2 ;;
    --sub-appid) SUB_APPID="$2"; shift 2 ;;

    --sp-apiv3-key) SP_APIV3_KEY="$2"; shift 2 ;;
    --sp-serial-no) SP_SERIAL_NO="$2"; shift 2 ;;
    --sp-private-key-path) SP_PRIVATE_KEY_PATH="$2"; shift 2 ;;
    --sp-platform-cert-path) SP_PLATFORM_CERT_PATH="$2"; shift 2 ;;

    --routine-apiv3-key) ROUTINE_APIV3_KEY="$2"; shift 2 ;;
    --routine-serial-no) ROUTINE_SERIAL_NO="$2"; shift 2 ;;
    --routine-private-key-path) ROUTINE_PRIVATE_KEY_PATH="$2"; shift 2 ;;
    --routine-platform-cert-path) ROUTINE_PLATFORM_CERT_PATH="$2"; shift 2 ;;

    -h|--help) usage; exit 0 ;;
    *) echo "未知参数: $1"; usage; exit 1 ;;
  esac
done

if [[ "${MODE}" != "service-provider" && "${MODE}" != "direct" ]]; then
  echo "错误：--mode 仅支持 service-provider|direct"
  exit 1
fi
if [[ "${API_VERSION}" != "v2" && "${API_VERSION}" != "v3" ]]; then
  echo "错误：--api-version 仅支持 v2|v3"
  exit 1
fi
if [[ "${ENABLE_PAY}" != "0" && "${ENABLE_PAY}" != "1" ]]; then
  echo "错误：--enable-pay 仅支持 0|1"
  exit 1
fi
if [[ -n "${MYSQL_DEFAULTS_FILE}" && ! -f "${MYSQL_DEFAULTS_FILE}" ]]; then
  echo "错误：MYSQL_DEFAULTS_FILE 文件不存在 -> ${MYSQL_DEFAULTS_FILE}"
  exit 1
fi

if [[ "${MODE}" == "service-provider" ]]; then
  if [[ -z "${SP_MCHID}" || -z "${SP_APIV3_KEY}" || -z "${SP_SERIAL_NO}" || -z "${SP_PRIVATE_KEY_PATH}" || -z "${SP_PLATFORM_CERT_PATH}" ]]; then
    echo "错误：service-provider 模式缺少必填参数"
    exit 1
  fi
fi
if [[ "${MODE}" == "direct" ]]; then
  if [[ -z "${ROUTINE_APIV3_KEY}" || -z "${ROUTINE_SERIAL_NO}" || -z "${ROUTINE_PRIVATE_KEY_PATH}" || -z "${ROUTINE_PLATFORM_CERT_PATH}" ]]; then
    echo "错误：direct 模式缺少必填参数"
    exit 1
  fi
fi

if [[ ${STRICT_PATH} -eq 1 ]]; then
  if [[ "${MODE}" == "service-provider" ]]; then
    [[ -f "${SP_PRIVATE_KEY_PATH}" ]] || { echo "错误：服务商私钥路径不存在 -> ${SP_PRIVATE_KEY_PATH}"; exit 1; }
    [[ -f "${SP_PLATFORM_CERT_PATH}" ]] || { echo "错误：服务商平台证书路径不存在 -> ${SP_PLATFORM_CERT_PATH}"; exit 1; }
  fi
  if [[ "${MODE}" == "direct" ]]; then
    [[ -f "${ROUTINE_PRIVATE_KEY_PATH}" ]] || { echo "错误：直连私钥路径不存在 -> ${ROUTINE_PRIVATE_KEY_PATH}"; exit 1; }
    [[ -f "${ROUTINE_PLATFORM_CERT_PATH}" ]] || { echo "错误：直连平台证书路径不存在 -> ${ROUTINE_PLATFORM_CERT_PATH}"; exit 1; }
  fi
fi

cat > "${SQL_FILE}" <<'SQL'
-- generated by payment_v3_fill.sh
START TRANSACTION;
SQL

append_upsert_sql "pay_routine_api_version" "小程序支付API版本(v2/v3)" "${API_VERSION}"
append_upsert_sql "pay_weixin_open" "微信支付开关" "${ENABLE_PAY}"

if [[ -n "${API_URL}" ]]; then
  append_upsert_sql "api_url" "后台api地址" "${API_URL}"
fi

if [[ "${MODE}" == "service-provider" ]]; then
  append_upsert_sql "pay_routine_sp_mchid" "小程序服务商商户号" "${SP_MCHID}"
  if [[ -n "${SP_APPID}" ]]; then
    append_upsert_sql "pay_routine_sp_appid" "小程序服务商AppID" "${SP_APPID}"
  fi
  if [[ -n "${SUB_MCHID}" ]]; then
    append_upsert_sql "pay_routine_sub_mchid" "小程序默认子商户号" "${SUB_MCHID}"
  fi
  if [[ -n "${SUB_APPID}" ]]; then
    append_upsert_sql "pay_routine_sub_appid" "小程序默认子商户AppID" "${SUB_APPID}"
  fi

  append_upsert_sql "pay_routine_sp_apiv3_key" "小程序服务商APIv3密钥" "${SP_APIV3_KEY}"
  append_upsert_sql "pay_routine_sp_serial_no" "小程序服务商商户证书序列号" "${SP_SERIAL_NO}"
  append_upsert_sql "pay_routine_sp_private_key_path" "小程序服务商私钥文件路径(pem)" "${SP_PRIVATE_KEY_PATH}"
  append_upsert_sql "pay_routine_sp_platform_cert_path" "小程序服务商微信支付平台证书路径" "${SP_PLATFORM_CERT_PATH}"
fi

if [[ "${MODE}" == "direct" ]]; then
  append_upsert_sql "pay_routine_apiv3_key" "小程序直连APIv3密钥" "${ROUTINE_APIV3_KEY}"
  append_upsert_sql "pay_routine_serial_no" "小程序直连商户证书序列号" "${ROUTINE_SERIAL_NO}"
  append_upsert_sql "pay_routine_private_key_path" "小程序直连私钥文件路径(pem)" "${ROUTINE_PRIVATE_KEY_PATH}"
  append_upsert_sql "pay_routine_platform_cert_path" "小程序直连微信支付平台证书路径" "${ROUTINE_PLATFORM_CERT_PATH}"
fi

cat >> "${SQL_FILE}" <<'SQL'
COMMIT;
SQL

{
  echo "run_dir=${RUN_DIR}"
  echo "sql_file=${SQL_FILE}"
  echo "mode=${MODE}"
  echo "api_version=${API_VERSION}"
  echo "apply=${APPLY}"
  echo "strict_path=${STRICT_PATH}"
  echo "enable_pay=${ENABLE_PAY}"
  echo "api_url=${API_URL:-<unchanged>}"
  if [[ "${MODE}" == "service-provider" ]]; then
    echo "sp_mchid=${SP_MCHID}"
    echo "sp_appid=${SP_APPID:-<inherit_base_appid>}"
    echo "sub_mchid=${SUB_MCHID:-<store_mapping_only>}"
    echo "sub_appid=${SUB_APPID:-<inherit_base_appid>}"
    echo "sp_serial_no=${SP_SERIAL_NO}"
    echo "sp_private_key_path=${SP_PRIVATE_KEY_PATH}"
    echo "sp_platform_cert_path=${SP_PLATFORM_CERT_PATH}"
  else
    echo "routine_serial_no=${ROUTINE_SERIAL_NO}"
    echo "routine_private_key_path=${ROUTINE_PRIVATE_KEY_PATH}"
    echo "routine_platform_cert_path=${ROUTINE_PLATFORM_CERT_PATH}"
  fi
} > "${SUMMARY_FILE}"

if [[ ${APPLY} -eq 0 ]]; then
  echo "dry-run 完成，未写库。"
  echo "产物目录: ${RUN_DIR}"
  echo "SQL 文件: ${SQL_FILE}"
  exit 0
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
MYSQL_CMD+=("${DB_NAME}" --default-character-set=utf8mb4)

"${MYSQL_CMD[@]}" < "${SQL_FILE}"
echo "写库完成。产物目录: ${RUN_DIR}"
