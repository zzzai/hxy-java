#!/usr/bin/env bash
set -euo pipefail

# 微信支付 v3 就绪检查（小程序主链路）

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-crmeb_java}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:-}"
MYSQL_DEFAULTS_FILE="${MYSQL_DEFAULTS_FILE:-}"

STRICT=0
OUT_DIR=""

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PLACEHOLDER_SUB_MCHID_REGEX='^99[0-9]{8}$'

usage() {
  cat <<'USAGE'
用法：
  ./shell/payment_v3_readiness_check.sh [--strict] [--out-dir PATH]

参数：
  --strict                   将 WARN 也视为失败（退出码=2）
  --out-dir PATH             报告输出目录（默认 runtime/payment_v3_readiness）

环境变量：
  DB_HOST DB_PORT DB_NAME DB_USER DB_PASS MYSQL_DEFAULTS_FILE

退出码：
  0  全部通过（或仅 WARN）
  2  存在 FAIL（或 strict 下存在 WARN）
  1  脚本执行错误
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --strict)
      STRICT=1
      shift
      ;;
    --out-dir)
      OUT_DIR="$2"
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

if [[ -n "${MYSQL_DEFAULTS_FILE}" && ! -f "${MYSQL_DEFAULTS_FILE}" ]]; then
  echo "参数错误: MYSQL_DEFAULTS_FILE 文件不存在 -> ${MYSQL_DEFAULTS_FILE}"
  exit 1
fi

if [[ -z "${OUT_DIR}" ]]; then
  OUT_DIR="${ROOT_DIR}/runtime/payment_v3_readiness"
fi
mkdir -p "${OUT_DIR}"
REPORT_FILE="${OUT_DIR}/check-$(date '+%Y%m%d%H%M%S').md"

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

PASS_COUNT=0
WARN_COUNT=0
FAIL_COUNT=0
DETAILS=""

add_result() {
  local level="$1"
  local item="$2"
  local detail="$3"
  DETAILS+="- [${level}] ${item}: ${detail}"$'\n'
  case "${level}" in
    PASS) PASS_COUNT=$((PASS_COUNT + 1)) ;;
    WARN) WARN_COUNT=$((WARN_COUNT + 1)) ;;
    FAIL) FAIL_COUNT=$((FAIL_COUNT + 1)) ;;
  esac
}

normalize_switch() {
  local v="$1"
  v="${v//\'/}"
  v="${v//\"/}"
  v="$(printf '%s' "${v}" | tr -d '[:space:]')"
  printf '%s' "${v}"
}

declare -A CFG
get_cfg_value() {
  local key="$1"
  printf '%s' "${CFG["${key}"]-}"
}

# 1) DB
set +e
db_ping="$("${MYSQL_CMD[@]}" -e "SELECT 1;" 2>/tmp/payment_v3_readiness_db.err)"
db_rc=$?
set -e
if [[ ${db_rc} -ne 0 || "${db_ping}" != "1" ]]; then
  add_result "FAIL" "数据库连接" "连接失败，详情: $(tr '\n' ' ' </tmp/payment_v3_readiness_db.err)"
else
  add_result "PASS" "数据库连接" "连接正常"
fi

# 2) 加载配置
if [[ ${db_rc} -eq 0 ]]; then
  cfg_rows="$("${MYSQL_CMD[@]}" -e "
SELECT name, IFNULL(value,'')
FROM eb_system_config
WHERE name IN (
  'pay_weixin_open',
  'api_url',
  'pay_routine_api_version',
  'pay_routine_mchid',
  'pay_routine_sp_mchid',
  'pay_routine_sub_mchid',
  'pay_routine_sp_apiv3_key',
  'pay_routine_sp_serial_no',
  'pay_routine_sp_private_key_path',
  'pay_routine_sp_platform_cert_path',
  'pay_routine_apiv3_key',
  'pay_routine_serial_no',
  'pay_routine_private_key_path',
  'pay_routine_platform_cert_path'
);")"
  while IFS=$'\t' read -r name value; do
    [[ -z "${name}" ]] && continue
    CFG["${name}"]="${value}"
  done <<< "${cfg_rows}"
fi

# 3) 基础开关
pay_switch="$(normalize_switch "$(get_cfg_value "pay_weixin_open")")"
if [[ "${pay_switch}" == "1" ]]; then
  add_result "PASS" "支付开关" "pay_weixin_open=1"
else
  add_result "WARN" "支付开关" "pay_weixin_open=${pay_switch:-<empty>}（联调前建议开启）"
fi

api_url="$(get_cfg_value "api_url")"
if [[ -z "${api_url}" ]]; then
  add_result "FAIL" "回调域名" "api_url 为空"
elif [[ "${api_url}" =~ ^https:// ]]; then
  add_result "PASS" "回调域名" "${api_url}"
else
  add_result "WARN" "回调域名" "${api_url}（建议 HTTPS）"
fi

api_version="$(normalize_switch "$(get_cfg_value "pay_routine_api_version")")"
if [[ "${api_version}" == "v3" || "${api_version}" == "V3" ]]; then
  add_result "PASS" "API版本开关" "pay_routine_api_version=${api_version}"
elif [[ -z "${api_version}" ]]; then
  add_result "WARN" "API版本开关" "pay_routine_api_version 未配置（默认会走旧链路）"
else
  add_result "WARN" "API版本开关" "pay_routine_api_version=${api_version}（当前非 v3）"
fi

# 4) 模式判定
sp_mchid="$(get_cfg_value "pay_routine_sp_mchid")"
sub_mchid="$(get_cfg_value "pay_routine_sub_mchid")"
routine_mchid="$(get_cfg_value "pay_routine_mchid")"
store_map_count="0"
if [[ ${db_rc} -eq 0 ]]; then
  store_map_count="$("${MYSQL_CMD[@]}" -e "SELECT COUNT(1) FROM eb_system_config WHERE name LIKE 'pay_routine_sub_mchid_%' AND IFNULL(value,'') <> '';" 2>/dev/null || echo "0")"
fi

MODE="unknown"
if [[ -n "${sp_mchid}" ]]; then
  MODE="service_provider"
  add_result "PASS" "支付模式" "服务商模式（pay_routine_sp_mchid 已配置）"
  if [[ -n "${sub_mchid}" || "${store_map_count}" =~ ^[1-9][0-9]*$ ]]; then
    add_result "PASS" "子商户配置" "默认子商户或门店映射已配置（store_map_count=${store_map_count:-0}）"
  else
    add_result "FAIL" "子商户配置" "服务商模式未配置 pay_routine_sub_mchid 或门店映射"
  fi
elif [[ -n "${routine_mchid}" ]]; then
  MODE="direct"
  add_result "WARN" "支付模式" "当前为直连模式（非服务商）"
else
  add_result "FAIL" "支付模式" "未识别到可用商户配置"
fi

# 4.1) 占位号检查（服务商联调硬门槛）
if [[ -n "${sub_mchid}" ]]; then
  if [[ "${sub_mchid}" =~ ${PLACEHOLDER_SUB_MCHID_REGEX} ]]; then
    add_result "FAIL" "默认子商户号占位风险" "pay_routine_sub_mchid=${sub_mchid}（禁止使用 99xxxxxxxx）"
  else
    add_result "PASS" "默认子商户号占位风险" "pay_routine_sub_mchid 非占位号"
  fi
fi

if [[ ${db_rc} -eq 0 ]]; then
  set +e
  store_placeholder_count="$("${MYSQL_CMD[@]}" -e "SELECT COUNT(1) FROM eb_system_config WHERE name LIKE 'pay_routine_sub_mchid_%' AND IFNULL(value,'') REGEXP '^99[0-9]{8}$';" 2>/tmp/payment_v3_readiness_placeholder.err)"
  placeholder_rc=$?
  set -e
  if [[ ${placeholder_rc} -ne 0 ]]; then
    add_result "WARN" "门店映射占位风险" "查询失败: $(tr '\n' ' ' </tmp/payment_v3_readiness_placeholder.err)"
  else
    if [[ "${store_placeholder_count}" =~ ^[0-9]+$ ]] && (( store_placeholder_count > 0 )); then
      sample_keys="$("${MYSQL_CMD[@]}" -e "SELECT name FROM eb_system_config WHERE name LIKE 'pay_routine_sub_mchid_%' AND IFNULL(value,'') REGEXP '^99[0-9]{8}$' ORDER BY name LIMIT 5;" 2>/dev/null | tr '\n' ',' | sed 's/,$//')"
      add_result "FAIL" "门店映射占位风险" "命中 ${store_placeholder_count} 条，占位键示例: ${sample_keys:-N/A}"
    else
      add_result "PASS" "门店映射占位风险" "未发现 pay_routine_sub_mchid_% 占位号"
    fi
  fi
fi

# 5) v3 参数检查
check_path() {
  local label="$1"
  local p="$2"
  if [[ -z "${p}" ]]; then
    add_result "FAIL" "${label}" "未配置"
  elif [[ -f "${p}" ]]; then
    add_result "PASS" "${label}" "文件存在: ${p}"
  else
    add_result "FAIL" "${label}" "文件不存在: ${p}"
  fi
}

if [[ "${MODE}" == "service_provider" ]]; then
  sp_apiv3_key="$(get_cfg_value "pay_routine_sp_apiv3_key")"
  sp_serial_no="$(get_cfg_value "pay_routine_sp_serial_no")"
  sp_private_key_path="$(get_cfg_value "pay_routine_sp_private_key_path")"
  sp_platform_cert_path="$(get_cfg_value "pay_routine_sp_platform_cert_path")"

  if [[ -n "${sp_apiv3_key}" ]]; then
    add_result "PASS" "服务商APIv3密钥" "pay_routine_sp_apiv3_key 已配置"
  else
    add_result "FAIL" "服务商APIv3密钥" "pay_routine_sp_apiv3_key 未配置"
  fi

  if [[ -n "${sp_serial_no}" ]]; then
    add_result "PASS" "服务商证书序列号" "pay_routine_sp_serial_no 已配置"
  else
    add_result "FAIL" "服务商证书序列号" "pay_routine_sp_serial_no 未配置"
  fi

  check_path "服务商私钥路径" "${sp_private_key_path}"
  check_path "服务商平台证书路径" "${sp_platform_cert_path}"
fi

if [[ "${MODE}" == "direct" ]]; then
  routine_apiv3_key="$(get_cfg_value "pay_routine_apiv3_key")"
  routine_serial_no="$(get_cfg_value "pay_routine_serial_no")"
  routine_private_key_path="$(get_cfg_value "pay_routine_private_key_path")"
  routine_platform_cert_path="$(get_cfg_value "pay_routine_platform_cert_path")"

  if [[ -n "${routine_apiv3_key}" ]]; then
    add_result "PASS" "直连APIv3密钥" "pay_routine_apiv3_key 已配置"
  else
    add_result "FAIL" "直连APIv3密钥" "pay_routine_apiv3_key 未配置"
  fi

  if [[ -n "${routine_serial_no}" ]]; then
    add_result "PASS" "直连证书序列号" "pay_routine_serial_no 已配置"
  else
    add_result "FAIL" "直连证书序列号" "pay_routine_serial_no 未配置"
  fi

  check_path "直连私钥路径" "${routine_private_key_path}"
  check_path "直连平台证书路径" "${routine_platform_cert_path}"
fi

{
  echo "# 微信支付 v3 就绪检查报告"
  echo
  echo "- 生成时间: $(date '+%F %T')"
  echo "- 数据库: ${DB_NAME}@${DB_HOST}:${DB_PORT}"
  echo "- strict: ${STRICT}"
  echo
  echo "## 检查结果"
  printf '%s' "${DETAILS}"
  echo
  echo "## 汇总"
  echo "- PASS: ${PASS_COUNT}"
  echo "- WARN: ${WARN_COUNT}"
  echo "- FAIL: ${FAIL_COUNT}"
} > "${REPORT_FILE}"

echo "[payment-v3-readiness] 报告生成: ${REPORT_FILE}"
echo "[payment-v3-readiness] PASS=${PASS_COUNT}, WARN=${WARN_COUNT}, FAIL=${FAIL_COUNT}"

if [[ ${FAIL_COUNT} -gt 0 ]]; then
  exit 2
fi
if [[ ${STRICT} -eq 1 && ${WARN_COUNT} -gt 0 ]]; then
  exit 2
fi
exit 0
