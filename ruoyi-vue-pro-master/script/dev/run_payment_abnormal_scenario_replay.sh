#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

RUN_ID="${RUN_ID:-$(date +%Y%m%d_%H%M%S)_$RANDOM}"
OUT_BASE_DIR="${OUT_BASE_DIR:-${ROOT_DIR}/.tmp/payment_abnormal_replay}"
OUT_DIR=""
LOG_DIR="${OUT_DIR}/logs"
SCENARIO_FILE="${SCENARIO_FILE:-${ROOT_DIR}/script/dev/config/payment_abnormal_scenarios_v1.tsv}"
SCENARIO_TSV="${OUT_DIR}/scenario_result.tsv"
SUMMARY_FILE="${OUT_DIR}/summary.txt"
REPORT_FILE="${OUT_DIR}/report.md"
INDEX_FILE="${OUT_DIR}/artifact_index.md"

RUN_TESTS="${RUN_TESTS:-0}"
RUN_NOTIFY_SMOKE="${RUN_NOTIFY_SMOKE:-1}"
RUN_RETRY_POLICY_CHECK="${RUN_RETRY_POLICY_CHECK:-1}"
RUN_PARTNER_READINESS_CHECK="${RUN_PARTNER_READINESS_CHECK:-1}"
DEFAULT_PARTNER_READINESS_CONFIG_FILE="${ROOT_DIR}/script/dev/samples/wx_partner_channel.sample.json"
PARTNER_READINESS_CONFIG_FILE="${PARTNER_READINESS_CONFIG_FILE:-${DEFAULT_PARTNER_READINESS_CONFIG_FILE}}"
PARTNER_READINESS_CHANNEL_CODE="${PARTNER_READINESS_CHANNEL_CODE:-wx_lite}"
PARTNER_READINESS_STRICT="${PARTNER_READINESS_STRICT:-0}"
RUN_RECONCILE_CHECK="${RUN_RECONCILE_CHECK:-0}"
RECONCILE_OPTIONAL="${RECONCILE_OPTIONAL:-0}"
RECONCILE_SUMMARY_FILE="${RECONCILE_SUMMARY_FILE:-}"
RECONCILE_ISSUES_TSV="${RECONCILE_ISSUES_TSV:-}"
RECONCILE_BIZ_DATE="${RECONCILE_BIZ_DATE:-$(date -d 'yesterday' +%F)}"

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-}"
DB_NAME="${DB_NAME:-hxy_dev}"

usage() {
  cat <<'USAGE'
Usage:
  script/dev/run_payment_abnormal_scenario_replay.sh [options]

Options:
  --run-id <id>                         运行 ID（默认时间戳）
  --out-base-dir <dir>                  输出根目录（默认 .tmp/payment_abnormal_replay）
  --scenario-file <file>                场景清单（默认 script/dev/config/payment_abnormal_scenarios_v1.tsv）
  --run-tests <0|1>                     是否运行支付韧性测试（默认 0）
  --run-notify-smoke <0|1>              是否运行通知回放烟测（默认 1）
  --run-retry-policy-check <0|1>        是否运行通知重试策略检查（默认 1）
  --run-partner-readiness-check <0|1>   是否运行服务商参数检查（默认 1）
  --partner-readiness-config-file <f>   服务商参数检查配置文件（默认 sample）
  --partner-readiness-strict <0|1>      服务商参数检查是否严格模式（默认 0）
  --run-reconcile-check <0|1>           是否直接执行日对账检查（默认 0）
  --reconcile-summary-file <file>       已有 #17 对账 summary 文件（可选）
  --reconcile-issues-tsv <file>         已有 #17 对账 issues 文件（可选）
  --reconcile-biz-date <yyyy-mm-dd>     直接跑对账时的业务日期（默认昨天）
  --db-host/--db-port/--db-user/--db-password/--db-name
  -h, --help                            显示帮助

Exit Code:
  0  : 无 BLOCK
  2  : 存在 BLOCK 场景
  1+ : 执行异常
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --run-id)
      RUN_ID="$2"
      shift 2
      ;;
    --out-base-dir)
      OUT_BASE_DIR="$2"
      shift 2
      ;;
    --scenario-file)
      SCENARIO_FILE="$2"
      shift 2
      ;;
    --run-tests)
      RUN_TESTS="$2"
      shift 2
      ;;
    --run-notify-smoke)
      RUN_NOTIFY_SMOKE="$2"
      shift 2
      ;;
    --run-retry-policy-check)
      RUN_RETRY_POLICY_CHECK="$2"
      shift 2
      ;;
    --run-partner-readiness-check)
      RUN_PARTNER_READINESS_CHECK="$2"
      shift 2
      ;;
    --partner-readiness-config-file)
      PARTNER_READINESS_CONFIG_FILE="$2"
      shift 2
      ;;
    --partner-readiness-strict)
      PARTNER_READINESS_STRICT="$2"
      shift 2
      ;;
    --run-reconcile-check)
      RUN_RECONCILE_CHECK="$2"
      shift 2
      ;;
    --reconcile-summary-file)
      RECONCILE_SUMMARY_FILE="$2"
      shift 2
      ;;
    --reconcile-issues-tsv)
      RECONCILE_ISSUES_TSV="$2"
      shift 2
      ;;
    --reconcile-biz-date)
      RECONCILE_BIZ_DATE="$2"
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

for val in "${RUN_TESTS}" "${RUN_NOTIFY_SMOKE}" "${RUN_RETRY_POLICY_CHECK}" "${RUN_PARTNER_READINESS_CHECK}" "${RUN_RECONCILE_CHECK}"; do
  if ! [[ "${val}" =~ ^[01]$ ]]; then
    echo "Invalid flag value: ${val} (expect 0/1)" >&2
    exit 1
  fi
done
if ! [[ "${PARTNER_READINESS_STRICT}" =~ ^[01]$ ]]; then
  echo "Invalid PARTNER_READINESS_STRICT: ${PARTNER_READINESS_STRICT} (expect 0/1)" >&2
  exit 1
fi
if ! [[ "${RECONCILE_OPTIONAL}" =~ ^[01]$ ]]; then
  echo "Invalid RECONCILE_OPTIONAL: ${RECONCILE_OPTIONAL} (expect 0/1)" >&2
  exit 1
fi

OUT_DIR="${OUT_BASE_DIR}/${RUN_ID}"
LOG_DIR="${OUT_DIR}/logs"
SCENARIO_TSV="${OUT_DIR}/scenario_result.tsv"
SUMMARY_FILE="${OUT_DIR}/summary.txt"
REPORT_FILE="${OUT_DIR}/report.md"
INDEX_FILE="${OUT_DIR}/artifact_index.md"

if [[ ! -f "${SCENARIO_FILE}" ]]; then
  echo "Scenario file not found: ${SCENARIO_FILE}" >&2
  exit 1
fi

mkdir -p "${LOG_DIR}" "${OUT_DIR}/artifacts"

run_cmd_capture_rc() {
  local log_file="$1"
  shift
  set +e
  "$@" >"${log_file}" 2>&1
  local rc=$?
  set -e
  echo "${rc}"
}

kv() {
  local file="$1"
  local key="$2"
  if [[ ! -f "${file}" ]]; then
    printf ''
    return
  fi
  local line
  line="$(grep -E "^${key}=" "${file}" | head -n 1 || true)"
  if [[ -z "${line}" ]]; then
    printf ''
  else
    printf '%s' "${line#*=}"
  fi
}

is_sample_partner_config() {
  local config_file="$1"
  if [[ -z "${config_file}" ]]; then
    return 1
  fi

  if [[ "${config_file}" == "script/dev/samples/wx_partner_channel.sample.json" || "${config_file}" == "${DEFAULT_PARTNER_READINESS_CONFIG_FILE}" ]]; then
    return 0
  fi

  local resolved_file=""
  local resolved_default=""
  resolved_file="$(readlink -f "${config_file}" 2>/dev/null || true)"
  resolved_default="$(readlink -f "${DEFAULT_PARTNER_READINESS_CONFIG_FILE}" 2>/dev/null || true)"
  if [[ -n "${resolved_file}" && -n "${resolved_default}" && "${resolved_file}" == "${resolved_default}" ]]; then
    return 0
  fi
  return 1
}

# ---------- Evidence collection ----------

echo "[abnormal-replay] run_id=${RUN_ID}"
echo "[abnormal-replay] out_dir=${OUT_DIR}"

resilience_rc="SKIP"
if [[ "${RUN_TESTS}" == "1" ]]; then
  echo "[abnormal-replay] step=run_payment_resilience_regression"
  resilience_rc="$(run_cmd_capture_rc "${LOG_DIR}/resilience_regression.log" bash script/dev/run_payment_resilience_regression.sh)"
else
  echo "[abnormal-replay] skip run_payment_resilience_regression"
fi

notify_dup_rc="SKIP"
notify_ooo_rc="SKIP"
notify_delay_rc="SKIP"
if [[ "${RUN_NOTIFY_SMOKE}" == "1" ]]; then
  echo "[abnormal-replay] step=pay_notify_pipeline_smoke duplicate"
  notify_dup_rc="$(run_cmd_capture_rc "${LOG_DIR}/notify_smoke_duplicate.log" bash script/dev/pay_notify_pipeline_smoke.sh --out-root "${OUT_DIR}/artifacts/pay_notify_smoke" --run-id "dup_${RUN_ID}" --scenario duplicate --notify-dry-run 1)"
  echo "[abnormal-replay] step=pay_notify_pipeline_smoke out_of_order"
  notify_ooo_rc="$(run_cmd_capture_rc "${LOG_DIR}/notify_smoke_out_of_order.log" bash script/dev/pay_notify_pipeline_smoke.sh --out-root "${OUT_DIR}/artifacts/pay_notify_smoke" --run-id "ooo_${RUN_ID}" --scenario out_of_order --notify-dry-run 1)"
  echo "[abnormal-replay] step=pay_notify_pipeline_smoke delayed"
  notify_delay_rc="$(run_cmd_capture_rc "${LOG_DIR}/notify_smoke_delayed.log" bash script/dev/pay_notify_pipeline_smoke.sh --out-root "${OUT_DIR}/artifacts/pay_notify_smoke" --run-id "delay_${RUN_ID}" --scenario delayed --notify-dry-run 1)"
else
  echo "[abnormal-replay] skip pay_notify_pipeline_smoke"
fi

retry_policy_rc="SKIP"
if [[ "${RUN_RETRY_POLICY_CHECK}" == "1" ]]; then
  echo "[abnormal-replay] step=check_pay_notify_retry_policy"
  retry_policy_rc="$(run_cmd_capture_rc "${LOG_DIR}/retry_policy.log" bash script/dev/check_pay_notify_retry_policy.sh)"
else
  echo "[abnormal-replay] skip check_pay_notify_retry_policy"
fi

partner_ready_rc="SKIP"
partner_readiness_sample_config="0"
if is_sample_partner_config "${PARTNER_READINESS_CONFIG_FILE}"; then
  partner_readiness_sample_config="1"
fi
if [[ "${RUN_PARTNER_READINESS_CHECK}" == "1" ]]; then
  echo "[abnormal-replay] step=check_wx_partner_readiness config=${PARTNER_READINESS_CONFIG_FILE} strict=${PARTNER_READINESS_STRICT}"
  if [[ -z "${PARTNER_READINESS_CONFIG_FILE}" ]]; then
    {
      echo "[abnormal-replay][FAIL] partner readiness config is empty"
      echo "[abnormal-replay] strict=${PARTNER_READINESS_STRICT}"
    } > "${LOG_DIR}/partner_readiness.log"
    if [[ "${PARTNER_READINESS_STRICT}" == "1" ]]; then
      partner_ready_rc="STRICT_MISSING_CONFIG"
    else
      partner_ready_rc="MISSING_CONFIG"
    fi
  elif [[ "${PARTNER_READINESS_STRICT}" == "1" && "${partner_readiness_sample_config}" == "1" ]]; then
    {
      echo "[abnormal-replay][FAIL] strict mode requires real partner config"
      echo "[abnormal-replay] config=${PARTNER_READINESS_CONFIG_FILE}"
      echo "[abnormal-replay] sample_config=1"
    } > "${LOG_DIR}/partner_readiness.log"
    partner_ready_rc="STRICT_SAMPLE_CONFIG"
  else
    partner_ready_rc="$(run_cmd_capture_rc "${LOG_DIR}/partner_readiness.log" bash script/dev/check_wx_partner_readiness.sh --config "${PARTNER_READINESS_CONFIG_FILE}" --channel-code "${PARTNER_READINESS_CHANNEL_CODE}")"
  fi
else
  echo "[abnormal-replay] skip check_wx_partner_readiness"
fi

notify_signature_check="0"
if rg -q "parse(Order|PartnerOrder)NotifyV3Result|parse(Refund|PartnerRefund)NotifyV3Result" yudao-module-pay/src/main/java/cn/iocoder/yudao/module/pay/framework/pay/core/client/impl/weixin/AbstractWxPayClient.java; then
  notify_signature_check="1"
fi

trade_lock_guard_check="0"
if [[ -f "yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/job/order/TradeOrderAutoCancelJob.java" ]] \
  && rg -q "cancelOrderBySystem" yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/service/order/TradeOrderUpdateServiceImpl.java; then
  trade_lock_guard_check="1"
fi

refund_multi_guard_check="0"
if rg -q "AFTER_SALE_CREATE_FAIL_REFUND_PRICE_ERROR" yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/service/aftersale/AfterSaleServiceImpl.java \
  && rg -q "updateOrderItemWhenAfterSaleSuccess" yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/service/order/TradeOrderUpdateServiceImpl.java; then
  refund_multi_guard_check="1"
fi

refund_rollback_guard_check="0"
if rg -q "updateAfterSaleRefunded" yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/service/aftersale/AfterSaleServiceImpl.java \
  && rg -q "updateOrderItemWhenAfterSaleSuccess" yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/service/aftersale/AfterSaleServiceImpl.java; then
  refund_rollback_guard_check="1"
fi

ops_gate_guard_check="0"
if [[ -f "script/dev/check_payment_release_blockers.sh" && -f "script/dev/run_payment_stagea_p0_19_20.sh" ]]; then
  ops_gate_guard_check="1"
fi

reconcile_block_count=""
reconcile_warn_count=""
reconcile_result=""
reconcile_source="none"

if [[ -n "${RECONCILE_SUMMARY_FILE}" && -f "${RECONCILE_SUMMARY_FILE}" ]]; then
  reconcile_source="external-summary"
  reconcile_block_count="$(kv "${RECONCILE_SUMMARY_FILE}" issue_block_count)"
  reconcile_warn_count="$(kv "${RECONCILE_SUMMARY_FILE}" issue_warn_count)"
  reconcile_result="$(kv "${RECONCILE_SUMMARY_FILE}" reconcile_result)"
elif [[ "${RUN_RECONCILE_CHECK}" == "1" ]]; then
  reconcile_source="run-check"
  local_reconcile_summary="${OUT_DIR}/artifacts/reconcile/summary.txt"
  local_reconcile_issues="${OUT_DIR}/artifacts/reconcile/issues.tsv"
  mkdir -p "${OUT_DIR}/artifacts/reconcile"
  reconcile_rc="$(run_cmd_capture_rc "${LOG_DIR}/reconcile_check.log" bash script/dev/check_payment_reconcile_daily.sh --biz-date "${RECONCILE_BIZ_DATE}" --db-host "${DB_HOST}" --db-port "${DB_PORT}" --db-user "${DB_USER}" --db-password "${DB_PASSWORD}" --db-name "${DB_NAME}" --summary-file "${local_reconcile_summary}" --issues-tsv "${local_reconcile_issues}")"
  if [[ "${reconcile_rc}" == "0" || "${reconcile_rc}" == "2" ]]; then
    reconcile_block_count="$(kv "${local_reconcile_summary}" issue_block_count)"
    reconcile_warn_count="$(kv "${local_reconcile_summary}" issue_warn_count)"
    reconcile_result="$(kv "${local_reconcile_summary}" reconcile_result)"
    RECONCILE_SUMMARY_FILE="${local_reconcile_summary}"
    RECONCILE_ISSUES_TSV="${local_reconcile_issues}"
  else
    reconcile_result="ERROR"
  fi
fi

[[ -z "${reconcile_block_count}" ]] && reconcile_block_count="N/A"
[[ -z "${reconcile_warn_count}" ]] && reconcile_warn_count="N/A"
[[ -z "${reconcile_result}" ]] && reconcile_result="N/A"

# ---------- Scenario scoring ----------

echo -e "scenario_no\tscenario_key\tpriority\tcategory\tdescription\tstatus\tseverity\tevidence\tadvice" > "${SCENARIO_TSV}"

pass_count=0
warn_count=0
block_count=0

mark_result() {
  local scenario_no="$1"
  local scenario_key="$2"
  local priority="$3"
  local category="$4"
  local description="$5"
  local status="$6"
  local severity="$7"
  local evidence="$8"
  local advice="$9"

  if [[ "${status}" == "PASS" ]]; then
    pass_count=$((pass_count + 1))
  elif [[ "${status}" == "WARN" ]]; then
    warn_count=$((warn_count + 1))
  else
    block_count=$((block_count + 1))
  fi

  echo -e "${scenario_no}\t${scenario_key}\t${priority}\t${category}\t${description}\t${status}\t${severity}\t${evidence}\t${advice}" >> "${SCENARIO_TSV}"
}

while IFS=$'\t' read -r scenario_no scenario_key priority category description; do
  [[ "${scenario_no}" == "scenario_no" ]] && continue
  [[ -z "${scenario_no}" ]] && continue

  status="WARN"
  severity="P2-WARN"
  evidence=""
  advice=""

  case "${scenario_key}" in
    order_timeout_auto_close|front_cancel_or_interrupt|close_pay_race|duplicate_submit|refund_waiting_timeout)
      if [[ "${resilience_rc}" == "0" ]]; then
        status="PASS"; severity="INFO"; evidence="resilience_regression.log(rc=0)"; advice="维持当前回归集"
      elif [[ "${resilience_rc}" == "SKIP" ]]; then
        status="WARN"; severity="P2-WARN"; evidence="resilience tests skipped"; advice="CI 建议 RUN_TESTS=1"
      else
        status="BLOCK"; severity="P1-BLOCK"; evidence="resilience_regression.log(rc=${resilience_rc})"; advice="先修复回归失败"
      fi
      ;;
    notify_lost|channel_api_timeout_or_5xx)
      if [[ "${retry_policy_rc}" == "0" ]]; then
        status="PASS"; severity="INFO"; evidence="retry_policy.log(rc=0)"; advice="保留补偿与重试"
      elif [[ "${retry_policy_rc}" == "SKIP" ]]; then
        status="WARN"; severity="P2-WARN"; evidence="retry policy check skipped"; advice="建议开启 RUN_RETRY_POLICY_CHECK"
      else
        status="BLOCK"; severity="P1-BLOCK"; evidence="retry_policy.log(rc=${retry_policy_rc})"; advice="修复重试策略"
      fi
      ;;
    notify_delayed)
      if [[ "${notify_delay_rc}" == "0" ]]; then
        status="PASS"; severity="INFO"; evidence="notify_smoke_delayed.log(rc=0)"; advice="保持延迟回放演练"
      elif [[ "${notify_delay_rc}" == "SKIP" ]]; then
        status="WARN"; severity="P2-WARN"; evidence="notify delayed smoke skipped"; advice="建议开启 RUN_NOTIFY_SMOKE"
      else
        status="BLOCK"; severity="P1-BLOCK"; evidence="notify_smoke_delayed.log(rc=${notify_delay_rc})"; advice="修复延迟回放链路"
      fi
      ;;
    notify_duplicate)
      if [[ "${notify_dup_rc}" == "0" ]]; then
        status="PASS"; severity="INFO"; evidence="notify_smoke_duplicate.log(rc=0)"; advice="保持重复回放演练"
      elif [[ "${notify_dup_rc}" == "SKIP" ]]; then
        status="WARN"; severity="P2-WARN"; evidence="notify duplicate smoke skipped"; advice="建议开启 RUN_NOTIFY_SMOKE"
      else
        status="BLOCK"; severity="P1-BLOCK"; evidence="notify_smoke_duplicate.log(rc=${notify_dup_rc})"; advice="修复重复回放链路"
      fi
      ;;
    notify_out_of_order)
      if [[ "${notify_ooo_rc}" == "0" ]]; then
        status="PASS"; severity="INFO"; evidence="notify_smoke_out_of_order.log(rc=0)"; advice="保持乱序回放演练"
      elif [[ "${notify_ooo_rc}" == "SKIP" ]]; then
        status="WARN"; severity="P2-WARN"; evidence="notify out_of_order smoke skipped"; advice="建议开启 RUN_NOTIFY_SMOKE"
      else
        status="BLOCK"; severity="P1-BLOCK"; evidence="notify_smoke_out_of_order.log(rc=${notify_ooo_rc})"; advice="修复乱序回放链路"
      fi
      ;;
    notify_signature_invalid)
      if [[ "${notify_signature_check}" == "1" ]]; then
        status="PASS"; severity="INFO"; evidence="AbstractWxPayClient parse*NotifyV3Result"; advice="保持验签失败告警"
      else
        status="BLOCK"; severity="P1-BLOCK"; evidence="signature verify path missing"; advice="补齐回调验签路径"
      fi
      ;;
    amount_mismatch|reconcile_discrepancy)
      if [[ "${reconcile_source}" == "none" ]]; then
        if [[ "${RECONCILE_OPTIONAL}" == "1" ]]; then
          status="PASS"; severity="INFO"; evidence="reconcile optional mode"; advice="上线前切回 REQUIRE_RECONCILE=1 并接入 #17 产物"
        else
          status="WARN"; severity="P2-WARN"; evidence="reconcile summary missing"; advice="执行 #17 脚本并接入产物"
        fi
      elif [[ "${reconcile_result}" == "BLOCK" ]]; then
        status="BLOCK"; severity="P1-BLOCK"; evidence="reconcile_result=BLOCK, block_count=${reconcile_block_count}"; advice="先清空对账 BLOCK 差异"
      elif [[ "${reconcile_result}" == "PASS_WITH_WARN" || "${reconcile_result}" == "PASS" ]]; then
        status="PASS"; severity="INFO"; evidence="reconcile_result=${reconcile_result}, warn_count=${reconcile_warn_count}"; advice="继续跟踪 WARN"
      else
        status="WARN"; severity="P2-WARN"; evidence="reconcile_result=${reconcile_result}"; advice="检查对账链路"
      fi
      ;;
    mchid_relation_mismatch)
      if [[ "${partner_ready_rc}" == "0" ]]; then
        status="PASS"; severity="INFO"; evidence="partner_readiness.log(rc=0,strict=${PARTNER_READINESS_STRICT},sample=${partner_readiness_sample_config})"; advice="保持服务商参数成对校验"
      elif [[ "${partner_ready_rc}" == "SKIP" ]]; then
        status="WARN"; severity="P2-WARN"; evidence="partner readiness skipped"; advice="建议开启 RUN_PARTNER_READINESS_CHECK"
      elif [[ "${PARTNER_READINESS_STRICT}" == "0" ]]; then
        status="WARN"; severity="P2-WARN"; evidence="partner_readiness.log(rc=${partner_ready_rc},strict=0,sample=${partner_readiness_sample_config})"; advice="开发期告警允许继续，生产发布前必须切 strict=1 并使用真实配置"
      else
        status="BLOCK"; severity="P1-BLOCK"; evidence="partner_readiness.log(rc=${partner_ready_rc},strict=1,sample=${partner_readiness_sample_config})"; advice="生产期必须使用真实特约商户配置并修复 sp/sub 参数关系"
      fi
      ;;
    lock_timeout_release)
      if [[ "${trade_lock_guard_check}" == "1" ]]; then
        status="PASS"; severity="INFO"; evidence="TradeOrderAutoCancelJob + cancelOrderBySystem"; advice="后续补充端到端压测回放"
      else
        status="WARN"; severity="P2-WARN"; evidence="trade lock/release guard not found"; advice="补齐锁超时释放校验"
      fi
      ;;
    partial_and_multi_refund)
      if [[ "${refund_multi_guard_check}" == "1" ]]; then
        status="PASS"; severity="INFO"; evidence="refund amount bound + order item refund update guard"; advice="后续补充多次退款实单回放"
      else
        status="WARN"; severity="P2-WARN"; evidence="partial/multi refund guard not found"; advice="补齐可退余额防超退校验"
      fi
      ;;
    refund_success_but_biz_not_rollback)
      if [[ "${refund_rollback_guard_check}" == "1" ]]; then
        status="PASS"; severity="INFO"; evidence="updateAfterSaleRefunded + rollback update hooks"; advice="后续补充失败补偿实战演练"
      else
        status="WARN"; severity="P2-WARN"; evidence="refund rollback hook not found"; advice="补齐退款成功后的业务逆向回滚"
      fi
      ;;
    ops_anomaly_gate)
      if [[ "${ops_gate_guard_check}" == "1" ]]; then
        status="PASS"; severity="INFO"; evidence="release blocker gate scripts present"; advice="持续维护门禁规则清单"
      else
        status="WARN"; severity="P2-WARN"; evidence="release blocker gate script missing"; advice="补齐上线拦截规则脚本"
      fi
      ;;
    *)
      status="WARN"; severity="P2-WARN"; evidence="unknown scenario mapping"; advice="补充映射"
      ;;
  esac

  mark_result "${scenario_no}" "${scenario_key}" "${priority}" "${category}" "${description}" "${status}" "${severity}" "${evidence}" "${advice}"
done < "${SCENARIO_FILE}"

suite_result="PASS"
exit_code=0
if [[ "${block_count}" -gt 0 ]]; then
  suite_result="BLOCK"
  exit_code=2
elif [[ "${warn_count}" -gt 0 ]]; then
  suite_result="PASS_WITH_WARN"
fi

{
  echo "generated_at=$(date '+%Y-%m-%d %H:%M:%S')"
  echo "run_id=${RUN_ID}"
  echo "out_dir=${OUT_DIR}"
  echo "scenario_file=${SCENARIO_FILE}"
  echo "scenario_tsv=${SCENARIO_TSV}"
  echo "suite_result=${suite_result}"
  echo "scenario_pass_count=${pass_count}"
  echo "scenario_warn_count=${warn_count}"
  echo "scenario_block_count=${block_count}"
  echo "run_tests=${RUN_TESTS}"
  echo "run_notify_smoke=${RUN_NOTIFY_SMOKE}"
  echo "run_retry_policy_check=${RUN_RETRY_POLICY_CHECK}"
  echo "run_partner_readiness_check=${RUN_PARTNER_READINESS_CHECK}"
  echo "partner_readiness_config_file=${PARTNER_READINESS_CONFIG_FILE}"
  echo "partner_readiness_channel_code=${PARTNER_READINESS_CHANNEL_CODE}"
  echo "partner_readiness_strict=${PARTNER_READINESS_STRICT}"
  echo "partner_readiness_sample_config=${partner_readiness_sample_config}"
  echo "run_reconcile_check=${RUN_RECONCILE_CHECK}"
  echo "reconcile_optional=${RECONCILE_OPTIONAL}"
  echo "resilience_rc=${resilience_rc}"
  echo "notify_dup_rc=${notify_dup_rc}"
  echo "notify_ooo_rc=${notify_ooo_rc}"
  echo "notify_delay_rc=${notify_delay_rc}"
  echo "retry_policy_rc=${retry_policy_rc}"
  echo "partner_ready_rc=${partner_ready_rc}"
  echo "notify_signature_check=${notify_signature_check}"
  echo "trade_lock_guard_check=${trade_lock_guard_check}"
  echo "refund_multi_guard_check=${refund_multi_guard_check}"
  echo "refund_rollback_guard_check=${refund_rollback_guard_check}"
  echo "ops_gate_guard_check=${ops_gate_guard_check}"
  echo "reconcile_source=${reconcile_source}"
  echo "reconcile_summary_file=${RECONCILE_SUMMARY_FILE}"
  echo "reconcile_issues_tsv=${RECONCILE_ISSUES_TSV}"
  echo "reconcile_result=${reconcile_result}"
  echo "reconcile_block_count=${reconcile_block_count}"
  echo "reconcile_warn_count=${reconcile_warn_count}"
  echo "report_file=${REPORT_FILE}"
  echo "exit_code=${exit_code}"
} > "${SUMMARY_FILE}"

{
  echo "# Payment Abnormal Scenario Replay Report"
  echo
  echo "- generated_at: $(date '+%Y-%m-%d %H:%M:%S')"
  echo "- run_id: \`${RUN_ID}\`"
  echo "- suite_result: **${suite_result}**"
  echo "- pass: ${pass_count}, warn: ${warn_count}, block: ${block_count}"
  echo
  echo "## Scenario Results"
  echo
  echo "| # | key | priority | category | status | severity | evidence | advice |"
  echo "|---|---|---|---|---|---|---|---|"
  awk -F'\t' 'NR>1 {
    gsub(/\|/, "\\|", $8);
    gsub(/\|/, "\\|", $9);
    printf "| %s | %s | %s | %s | %s | %s | %s | %s |\\n", $1,$2,$3,$4,$6,$7,$8,$9;
  }' "${SCENARIO_TSV}"
  echo
  echo "## Artifacts"
  echo
  echo "- summary: \`${SUMMARY_FILE}\`"
  echo "- scenario_tsv: \`${SCENARIO_TSV}\`"
  echo "- logs: \`${LOG_DIR}\`"
} > "${REPORT_FILE}"

{
  echo "# Payment Abnormal Replay Artifact Index"
  echo
  echo "- summary: \`summary.txt\`"
  echo "- report: \`report.md\`"
  echo "- scenario_tsv: \`scenario_result.tsv\`"
  echo "- logs: \`logs/\`"
  echo
  echo "## Result"
  echo
  echo "- suite_result: \`${suite_result}\`"
  echo "- pass: \`${pass_count}\`"
  echo "- warn: \`${warn_count}\`"
  echo "- block: \`${block_count}\`"
} > "${INDEX_FILE}"

echo "[abnormal-replay] summary=${SUMMARY_FILE}"
echo "[abnormal-replay] report=${REPORT_FILE}"
echo "[abnormal-replay] result=${suite_result}"

exit "${exit_code}"
