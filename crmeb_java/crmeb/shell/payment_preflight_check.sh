#!/usr/bin/env bash
set -euo pipefail

# D7: 上线前一键预检（配置/域名/证书/cron/脚本）

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-crmeb_java}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:-}"
MYSQL_DEFAULTS_FILE="${MYSQL_DEFAULTS_FILE:-}"

STRICT=0
OUT_DIR=""

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

usage() {
  cat <<'USAGE'
用法：
  ./shell/payment_preflight_check.sh [--strict] [--out-dir PATH]

参数：
  --strict                   将 WARN 也视为失败（退出码=2）
  --out-dir PATH             报告输出目录（默认 runtime/payment_preflight）

环境变量：
  DB_HOST DB_PORT DB_NAME DB_USER DB_PASS MYSQL_DEFAULTS_FILE

退出码：
  0  全部通过（或仅有 WARN）
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
  OUT_DIR="${ROOT_DIR}/runtime/payment_preflight"
fi
mkdir -p "${OUT_DIR}"
REPORT_FILE="${OUT_DIR}/preflight-$(date '+%Y%m%d%H%M%S').md"

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
MYSQL_CMD+=("${DB_NAME}" --default-character-set=utf8mb4 --batch --raw --skip-column-names)
if [[ -n "${DB_PASS}" && -z "${MYSQL_DEFAULTS_FILE}" ]]; then
  MYSQL_CMD+=(--password="${DB_PASS}")
fi

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

is_placeholder_sub_mchid() {
  local v
  v="$(normalize_switch "$1")"
  [[ "${v}" =~ ^99[0-9]{8}$ ]]
}

get_cfg_value() {
  local key="$1"
  printf '%s' "${CFG["${key}"]-}"
}

declare -A CFG

# 1) 数据库可用性
set +e
db_ping="$("${MYSQL_CMD[@]}" -e "SELECT 1;" 2>/tmp/payment_preflight_db.err)"
db_rc=$?
set -e
if [[ ${db_rc} -ne 0 || "${db_ping}" != "1" ]]; then
  add_result "FAIL" "数据库连接" "连接失败，详情: $(tr '\n' ' ' </tmp/payment_preflight_db.err)"
else
  add_result "PASS" "数据库连接" "连接正常"
fi

# 2) 加载关键配置
if [[ ${db_rc} -eq 0 ]]; then
  cfg_rows="$("${MYSQL_CMD[@]}" -e "
SELECT name, IFNULL(value,'')
FROM eb_system_config
WHERE name IN (
  'pay_weixin_open','api_url',
  'pay_routine_appid','pay_routine_mchid','pay_routine_key',
  'pay_routine_sp_appid','pay_routine_sp_mchid','pay_routine_sp_key',
  'pay_routine_sub_mchid','pay_routine_sub_appid',
  'pay_routine_sp_certificate_path'
);")"
  while IFS=$'\t' read -r name value; do
    [[ -z "${name}" ]] && continue
    CFG["${name}"]="${value}"
  done <<< "${cfg_rows}"
fi

# 3) 脚本完整性
required_scripts=(
  "shell/payment_morning_quickstart.sh"
  "shell/payment_reconcile_daily.sh"
  "shell/payment_reconcile_ticketize.sh"
  "shell/payment_decision_ticketize.sh"
  "shell/payment_cutover_gate.sh"
  "shell/payment_cron_healthcheck.sh"
  "shell/payment_store_mapping_audit.sh"
  "shell/payment_mock_replay.sh"
  "shell/payment_fullchain_drill.sh"
  "shell/payment_monitor_quickcheck.sh"
  "shell/payment_monitor_alert.sh"
  "shell/payment_ops_cron.sh"
  "shell/payment_ops_morning_bundle.sh"
  "shell/payment_ops_status.sh"
  "shell/payment_incident_bundle.sh"
  "shell/payment_ops_status_smoke.sh"
  "shell/payment_ops_cron_smoke.sh"
  "shell/payment_decision_chain_smoke.sh"
  "shell/payment_summary_contract_check.sh"
  "shell/payment_summary_contract_smoke.sh"
  "shell/payment_exception_acceptance.sh"
  "shell/payment_reconcile_sla_guard.sh"
  "shell/payment_booking_verify_regression.sh"
  "shell/payment_booking_verify_repair.sh"
  "shell/payment_store_mapping_template_export.sh"
  "shell/payment_store_mapping_import.sh"
  "shell/payment_store_mapping_pipeline_smoke.sh"
  "shell/payment_store_mapping_csv_generate.sh"
  "shell/payment_store_mapping_cross_channel_audit.sh"
  "shell/payment_store_mapping_placeholder_cleanup.sh"
  "shell/payment_store_mapping_cutover.sh"
  "shell/payment_go_nogo_decision.sh"
  "shell/payment_v3_readiness_check.sh"
  "shell/payment_v3_fill.sh"
)
for rel in "${required_scripts[@]}"; do
  abs="${ROOT_DIR}/${rel}"
  if [[ -x "${abs}" ]]; then
    add_result "PASS" "脚本存在" "${rel}"
  elif [[ -f "${abs}" ]]; then
    add_result "WARN" "脚本权限" "${rel} 存在但不可执行"
  else
    add_result "FAIL" "脚本缺失" "${rel}"
  fi
done

# 4) 支付开关与模式
pay_switch="$(normalize_switch "$(get_cfg_value "pay_weixin_open")")"
if [[ "${pay_switch}" == "1" ]]; then
  add_result "PASS" "支付开关" "pay_weixin_open=1"
else
  add_result "WARN" "支付开关" "pay_weixin_open=${pay_switch:-<empty>}（当前未开启）"
fi

routine_appid="$(get_cfg_value "pay_routine_appid")"
routine_mchid="$(get_cfg_value "pay_routine_mchid")"
routine_key="$(get_cfg_value "pay_routine_key")"
sp_appid="$(get_cfg_value "pay_routine_sp_appid")"
sp_mchid="$(get_cfg_value "pay_routine_sp_mchid")"
sp_key="$(get_cfg_value "pay_routine_sp_key")"
sub_mchid="$(get_cfg_value "pay_routine_sub_mchid")"
sub_appid="$(get_cfg_value "pay_routine_sub_appid")"
sp_cert_path="$(get_cfg_value "pay_routine_sp_certificate_path")"

if [[ -n "${sp_mchid}" && -n "${sp_key}" ]]; then
  add_result "PASS" "支付模式" "服务商模式（pay_routine_sp_mchid 已配置）"
else
  if [[ -n "${routine_appid}" && -n "${routine_mchid}" && -n "${routine_key}" ]]; then
    add_result "WARN" "支付模式" "当前为直连模式（未配置服务商号）"
  else
    add_result "FAIL" "支付模式" "服务商与直连配置均不完整"
  fi
fi

if [[ -n "${sp_mchid}" && -n "${sp_key}" ]]; then
  store_map_count="$("${MYSQL_CMD[@]}" -e "SELECT COUNT(1) FROM eb_system_config WHERE name LIKE 'pay_routine_sub_mchid_%' AND IFNULL(value,'') <> '';" 2>/dev/null || echo "0")"
  if [[ -n "${sub_mchid}" || "${store_map_count}" =~ ^[1-9][0-9]*$ ]]; then
    add_result "PASS" "子商户配置" "默认子商户或门店映射已配置（store_map_count=${store_map_count:-0}）"
  else
    add_result "FAIL" "子商户配置" "服务商模式下未配置 pay_routine_sub_mchid 或门店映射"
  fi
  if [[ -n "${sub_appid}" || -n "${sp_appid}" ]]; then
    add_result "PASS" "子商户AppID" "sub_appid/sp_appid 已配置"
  else
    add_result "WARN" "子商户AppID" "sub_appid/sp_appid 均为空，可能影响特定场景"
  fi

fi

placeholder_hits=()
if is_placeholder_sub_mchid "${sub_mchid}"; then
  placeholder_hits+=("pay_routine_sub_mchid=$(normalize_switch "${sub_mchid}")")
fi
map_rows="$("${MYSQL_CMD[@]}" -e "
SELECT name, IFNULL(value,'')
FROM eb_system_config
WHERE name LIKE 'pay_routine_sub_mchid_%'
  AND IFNULL(value,'') <> '';
" 2>/dev/null || true)"
while IFS=$'\t' read -r map_key map_value; do
  [[ -z "${map_key}" ]] && continue
  if is_placeholder_sub_mchid "${map_value}"; then
    placeholder_hits+=("${map_key}=$(normalize_switch "${map_value}")")
  fi
done <<< "${map_rows}"

if (( ${#placeholder_hits[@]} > 0 )); then
  add_result "FAIL" "子商户占位号" "检测到占位 sub_mchid(99xxxxxxxx): ${placeholder_hits[*]}"
else
  add_result "PASS" "子商户占位号" "未发现 99xxxxxxxx 占位号"
fi

# 5) 回调域名
api_url="$(get_cfg_value "api_url")"
if [[ -z "${api_url}" ]]; then
  add_result "FAIL" "回调域名" "api_url 未配置"
else
  if [[ "${api_url}" =~ ^https:// ]]; then
    add_result "PASS" "回调域名" "api_url=${api_url}"
  else
    add_result "WARN" "回调域名" "api_url=${api_url}（建议 HTTPS）"
  fi
fi

# 6) 证书路径（退款相关）
if [[ -n "${sp_cert_path}" ]]; then
  if [[ -f "${sp_cert_path}" ]]; then
    add_result "PASS" "退款证书" "证书文件存在: ${sp_cert_path}"
  else
    add_result "FAIL" "退款证书" "证书路径不存在: ${sp_cert_path}"
  fi
else
  add_result "WARN" "退款证书" "pay_routine_sp_certificate_path 未配置（退款联调会失败）"
fi

# 7) cron 托管任务
cron_all="$(crontab -l 2>/dev/null || true)"
if [[ "${cron_all}" == *"# >>> payment ops managed >>>"* && "${cron_all}" == *"# <<< payment ops managed <<<"* ]]; then
  add_result "PASS" "cron托管块" "存在 payment 托管块"
  cron_block="$(printf '%s\n' "${cron_all}" | awk '
    $0=="# >>> payment ops managed >>>" {show=1; next}
    $0=="# <<< payment ops managed <<<" {show=0; next}
    show==1 {print}
  ')"
  line_count="$(printf '%s\n' "${cron_block}" | sed '/^[[:space:]]*$/d' | wc -l | tr -d ' ')"
  if [[ "${line_count}" -ge 3 ]]; then
    add_result "PASS" "cron任务数" "托管任务=${line_count}"
  else
    add_result "FAIL" "cron任务数" "托管任务不足（${line_count}）"
  fi
  if printf '%s\n' "${cron_block}" | grep -q "DB_PASS='"; then
    add_result "WARN" "cron凭据安全" "检测到明文 DB_PASS，建议改用 MYSQL_DEFAULTS_FILE"
  fi
  if printf '%s\n' "${cron_block}" | grep -q "MYSQL_DEFAULTS_FILE='[^']\\+'"; then
    add_result "PASS" "cron凭据安全" "已配置 MYSQL_DEFAULTS_FILE"
  else
    add_result "WARN" "cron凭据安全" "未检测到 MYSQL_DEFAULTS_FILE"
  fi

  if printf '%s\n' "${cron_block}" | grep -q "payment_reconcile_ticketize.sh"; then
    add_result "PASS" "cron工单任务" "已配置 payment_reconcile_ticketize 定时任务"
  else
    add_result "WARN" "cron工单任务" "未检测到 payment_reconcile_ticketize 定时任务"
  fi

  if printf '%s\n' "${cron_block}" | grep -q "payment_decision_ticketize.sh"; then
    add_result "PASS" "cron判定工单任务" "已配置 payment_decision_ticketize 定时任务"
  else
    add_result "WARN" "cron判定工单任务" "未检测到 payment_decision_ticketize 定时任务"
  fi

  if printf '%s\n' "${cron_block}" | grep -q "payment_decision_chain_smoke.sh"; then
    add_result "PASS" "cron判定链路自测" "已配置 payment_decision_chain_smoke 定时任务"
  else
    add_result "WARN" "cron判定链路自测" "未检测到 payment_decision_chain_smoke 定时任务"
  fi

  if printf '%s\n' "${cron_block}" | grep -q "payment_cron_healthcheck.sh"; then
    add_result "PASS" "cron自监控任务" "已配置 payment_cron_healthcheck 定时任务"
  else
    add_result "WARN" "cron自监控任务" "未检测到 payment_cron_healthcheck 定时任务"
  fi

  if printf '%s\n' "${cron_block}" | grep -q "payment_store_mapping_audit.sh"; then
    add_result "PASS" "cron映射审计任务" "已配置 payment_store_mapping_audit 定时任务"
  else
    add_result "WARN" "cron映射审计任务" "未检测到 payment_store_mapping_audit 定时任务"
  fi

  if printf '%s\n' "${cron_block}" | grep -q "payment_store_mapping_pipeline_smoke.sh"; then
    add_result "PASS" "cron映射smoke任务" "已配置 payment_store_mapping_pipeline_smoke 定时任务"
  else
    add_result "WARN" "cron映射smoke任务" "未检测到 payment_store_mapping_pipeline_smoke 定时任务"
  fi

  if printf '%s\n' "${cron_block}" | grep -q "payment_cutover_gate.sh"; then
    add_result "PASS" "cron切换上线拦截规则任务" "已配置 payment_cutover_gate 定时任务"
  else
    add_result "WARN" "cron切换上线拦截规则任务" "未检测到 payment_cutover_gate 定时任务"
  fi

  status_line="$(printf '%s\n' "${cron_block}" | grep -F "payment_ops_status.sh" | head -n 1 || true)"
  if [[ -n "${status_line}" ]]; then
    if [[ "${status_line}" == *"--refresh"* ]]; then
      add_result "PASS" "cron值守刷新" "payment_ops_status 已启用 --refresh"
    else
      add_result "WARN" "cron值守刷新" "payment_ops_status 未启用 --refresh，可能读取旧产物"
    fi
    if [[ "${status_line}" == *"--max-summary-age-minutes"* && "${status_line}" == *"--max-recon-age-days"* && "${status_line}" == *"--max-daily-report-age-days"* ]]; then
      add_result "PASS" "cron新鲜度守卫" "payment_ops_status 已启用新鲜度阈值参数"
    else
      add_result "WARN" "cron新鲜度守卫" "payment_ops_status 未完整配置新鲜度阈值参数"
    fi
    if [[ "${status_line}" == *"--require-decision-chain-pass"* ]]; then
      add_result "PASS" "cron判定链路门槛" "payment_ops_status 已配置 decision_chain 门槛参数"
    else
      add_result "WARN" "cron判定链路门槛" "payment_ops_status 未配置 decision_chain 门槛参数"
    fi
  else
    add_result "WARN" "cron值守任务" "未检测到 payment_ops_status 任务"
  fi
else
  add_result "WARN" "cron托管块" "未安装 payment 托管任务"
fi

run_time="$(date '+%Y-%m-%d %H:%M:%S')"
cat > "${REPORT_FILE}" <<REPORT
# 支付上线预检报告

- 生成时间: ${run_time}
- 数据库: ${DB_NAME}@${DB_HOST}:${DB_PORT}
- strict: ${STRICT}

## 检查结果
${DETAILS}

## 汇总
- PASS: ${PASS_COUNT}
- WARN: ${WARN_COUNT}
- FAIL: ${FAIL_COUNT}
REPORT

echo "[preflight] 报告生成: ${REPORT_FILE}"
echo "[preflight] PASS=${PASS_COUNT}, WARN=${WARN_COUNT}, FAIL=${FAIL_COUNT}"

if [[ ${FAIL_COUNT} -gt 0 ]]; then
  exit 2
fi
if [[ ${STRICT} -eq 1 && ${WARN_COUNT} -gt 0 ]]; then
  exit 2
fi
exit 0
