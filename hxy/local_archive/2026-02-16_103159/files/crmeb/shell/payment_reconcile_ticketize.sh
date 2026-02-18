#!/usr/bin/env bash
set -euo pipefail

# D8: 对账差异工单化
# 输入：payment_reconcile 的 summary/main_diff/orphan 文件
# 输出：tickets.tsv + tickets.md

RECON_DATE="${RECON_DATE:-}"
RUN_DIR="${RUN_DIR:-}"
SUMMARY_FILE="${SUMMARY_FILE:-}"
OUTPUT_DIR="${OUTPUT_DIR:-}"
MAX_ROWS="${MAX_ROWS:-200}"
NO_ALERT=0
SLA_SUMMARY_FILE="${SLA_SUMMARY_FILE:-}"
SLA_DETAIL_FILE="${SLA_DETAIL_FILE:-}"
AMOUNT_P1_THRESHOLD_CENT="${AMOUNT_P1_THRESHOLD_CENT:-100000}"
P1_SLA_HOURS="${P1_SLA_HOURS:-4}"
P2_SLA_HOURS="${P2_SLA_HOURS:-24}"
OWNER_MAP_FILE="${OWNER_MAP_FILE:-}"
OWNER_DEFAULT="${OWNER_DEFAULT:-payment-ops}"
OWNER_P1="${OWNER_P1:-payment-oncall}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ALERT_SCRIPT="${ROOT_DIR}/shell/payment_alert_notify.sh"

usage() {
  cat <<'USAGE'
用法：
  ./shell/payment_reconcile_ticketize.sh [--date YYYY-MM-DD] [--run-dir PATH] [--summary PATH] [--output-dir PATH] [--max-rows N] [--sla-summary PATH] [--sla-detail PATH] [--amount-p1-threshold-cent N] [--p1-sla-hours N] [--p2-sla-hours N] [--owner-map-file PATH] [--owner-default NAME] [--owner-p1 NAME] [--no-alert]

参数：
  --date YYYY-MM-DD   对账日期（默认昨天）
  --run-dir PATH      对账运行目录（默认 runtime/payment_reconcile/<date>）
  --summary PATH      summary 文件（默认 <run-dir>/summary.txt）
  --output-dir PATH   工单输出目录（默认 <run-dir>/tickets）
  --max-rows N        markdown 明细最多展示行数（默认 200）
  --sla-summary PATH  SLA summary 文件（默认自动取 runtime/payment_reconcile_sla/latest）
  --sla-detail PATH   SLA detail 文件（默认从 sla-summary 的 detail_file 读取）
  --amount-p1-threshold-cent N  金额升级 P1 阈值（分，默认 100000）
  --p1-sla-hours N    P1 工单默认处理时限（小时，默认 4）
  --p2-sla-hours N    P2 工单默认处理时限（小时，默认 24）
  --owner-map-file PATH owner 规则文件（可选）
  --owner-default NAME 默认 owner（默认 payment-ops）
  --owner-p1 NAME      P1 默认 owner（默认 payment-oncall）
  --no-alert          生成工单但不推送机器人

退出码：
  0  工单生成成功（无 P1）
  2  无差异单（生成空工单）或存在 P1 工单
  1  脚本执行失败
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --date)
      RECON_DATE="$2"
      shift 2
      ;;
    --run-dir)
      RUN_DIR="$2"
      shift 2
      ;;
    --summary)
      SUMMARY_FILE="$2"
      shift 2
      ;;
    --output-dir)
      OUTPUT_DIR="$2"
      shift 2
      ;;
    --max-rows)
      MAX_ROWS="$2"
      shift 2
      ;;
    --sla-summary)
      SLA_SUMMARY_FILE="$2"
      shift 2
      ;;
    --sla-detail)
      SLA_DETAIL_FILE="$2"
      shift 2
      ;;
    --amount-p1-threshold-cent)
      AMOUNT_P1_THRESHOLD_CENT="$2"
      shift 2
      ;;
    --p1-sla-hours)
      P1_SLA_HOURS="$2"
      shift 2
      ;;
    --p2-sla-hours)
      P2_SLA_HOURS="$2"
      shift 2
      ;;
    --owner-map-file)
      OWNER_MAP_FILE="$2"
      shift 2
      ;;
    --owner-default)
      OWNER_DEFAULT="$2"
      shift 2
      ;;
    --owner-p1)
      OWNER_P1="$2"
      shift 2
      ;;
    --no-alert)
      NO_ALERT=1
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

if [[ -z "${RECON_DATE}" ]]; then
  RECON_DATE="$(date -d 'yesterday' +%F)"
fi
if ! [[ "${RECON_DATE}" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]]; then
  echo "参数错误: --date 需要 YYYY-MM-DD"
  exit 1
fi
if ! [[ "${MAX_ROWS}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: --max-rows 必须为正整数"
  exit 1
fi
if ! [[ "${AMOUNT_P1_THRESHOLD_CENT}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: --amount-p1-threshold-cent 必须为正整数"
  exit 1
fi
if ! [[ "${P1_SLA_HOURS}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: --p1-sla-hours 必须为正整数"
  exit 1
fi
if ! [[ "${P2_SLA_HOURS}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: --p2-sla-hours 必须为正整数"
  exit 1
fi
if [[ -z "${OWNER_DEFAULT}" ]]; then
  echo "参数错误: --owner-default 不能为空"
  exit 1
fi
if [[ -z "${OWNER_P1}" ]]; then
  echo "参数错误: --owner-p1 不能为空"
  exit 1
fi
if [[ -n "${OWNER_MAP_FILE}" && ! -f "${OWNER_MAP_FILE}" ]]; then
  echo "参数错误: --owner-map-file 不存在 -> ${OWNER_MAP_FILE}"
  exit 1
fi

if [[ -z "${RUN_DIR}" ]]; then
  RUN_DIR="${ROOT_DIR}/runtime/payment_reconcile/${RECON_DATE}"
fi
if [[ -z "${SUMMARY_FILE}" ]]; then
  SUMMARY_FILE="${RUN_DIR}/summary.txt"
fi
if [[ -z "${OUTPUT_DIR}" ]]; then
  OUTPUT_DIR="${RUN_DIR}/tickets"
fi

if [[ ! -f "${SUMMARY_FILE}" ]]; then
  echo "未找到 summary 文件: ${SUMMARY_FILE}"
  exit 1
fi

kv() {
  local file="$1"
  local key="$2"
  local line
  line="$(grep -E "^${key}=" "${file}" | head -n 1 || true)"
  if [[ -z "${line}" ]]; then
    printf ''
  else
    printf '%s' "${line#*=}"
  fi
}

trim() {
  local text="$1"
  text="${text#"${text%%[![:space:]]*}"}"
  text="${text%"${text##*[![:space:]]}"}"
  printf '%s' "${text}"
}

declare -A OWNER_RULES=()

load_owner_rules() {
  local file="$1"
  [[ -f "${file}" ]] || return
  local line c1 c2 c3 c4 key owner
  while IFS= read -r line || [[ -n "${line}" ]]; do
    line="$(trim "${line//$'\r'/}")"
    [[ -z "${line}" ]] && continue
    [[ "${line}" =~ ^# ]] && continue

    c1=""; c2=""; c3=""; c4=""
    IFS=$'\t' read -r c1 c2 c3 c4 <<< "${line}"
    if [[ -z "${c2}" ]]; then
      IFS=',' read -r c1 c2 c3 c4 <<< "${line}"
    fi
    c1="$(trim "${c1}")"
    c2="$(trim "${c2}")"
    c3="$(trim "${c3}")"
    c4="$(trim "${c4}")"

    if [[ "${c1}" == "key" && "${c2}" == "owner" ]]; then
      continue
    fi
    if [[ "${c1}" == "match_type" && "${c2}" == "match_value" ]]; then
      continue
    fi

    if [[ -n "${c3}" ]]; then
      key="${c1}:${c2}"
      owner="${c3}"
    else
      key="${c1}"
      owner="${c2}"
    fi
    key="$(trim "${key}")"
    owner="$(trim "${owner}")"
    [[ -z "${key}" || -z "${owner}" ]] && continue
    OWNER_RULES["${key}"]="${owner}"
  done < "${file}"
}

resolve_owner() {
  local severity="$1"
  local source="$2"
  local diff_type="$3"
  local store_id="$4"
  local owner=""

  if [[ -n "${store_id}" ]]; then
    owner="${OWNER_RULES["store:${store_id}"]-}"
  fi
  if [[ -z "${owner}" && -n "${diff_type}" ]]; then
    owner="${OWNER_RULES["diff_type:${diff_type}"]-}"
  fi
  if [[ -z "${owner}" && -n "${diff_type}" ]]; then
    local key prefix best_owner=""
    local best_len=0
    for key in "${!OWNER_RULES[@]}"; do
      if [[ "${key}" == diff_prefix:* ]]; then
        prefix="${key#diff_prefix:}"
        if [[ -n "${prefix}" && "${diff_type}" == "${prefix}"* ]]; then
          if (( ${#prefix} > best_len )); then
            best_len=${#prefix}
            best_owner="${OWNER_RULES["${key}"]}"
          fi
        fi
      fi
    done
    if [[ -n "${best_owner}" ]]; then
      owner="${best_owner}"
    fi
  fi
  if [[ -z "${owner}" ]]; then
    owner="${OWNER_RULES["source:${source}"]-}"
  fi
  if [[ -z "${owner}" ]]; then
    owner="${OWNER_RULES["severity:${severity}"]-}"
  fi
  if [[ -z "${owner}" && "${severity}" == "P1" ]]; then
    owner="${OWNER_P1}"
  fi
  if [[ -z "${owner}" ]]; then
    owner="${OWNER_DEFAULT}"
  fi
  printf '%s' "${owner}"
}

latest_summary() {
  local base="$1"
  if [[ ! -d "${base}" ]]; then
    printf ''
    return
  fi
  find "${base}" -maxdepth 2 -type f -name 'summary.txt' -printf '%T@ %p\n' 2>/dev/null \
    | sort -n \
    | tail -n 1 \
    | cut -d' ' -f2- || true
}

MAIN_DIFF_FILE="$(kv "${SUMMARY_FILE}" "main_diff_file")"
ORPHAN_FILE="$(kv "${SUMMARY_FILE}" "orphan_wx_file")"
RAW_DIFF_COUNT="$(kv "${SUMMARY_FILE}" "main_raw_diff_count")"
AUTO_CLEARED_COUNT="$(kv "${SUMMARY_FILE}" "main_cleared_by_refund_count")"
UNRESOLVED_DIFF_COUNT="$(kv "${SUMMARY_FILE}" "main_diff_count")"
ORPHAN_COUNT="$(kv "${SUMMARY_FILE}" "orphan_wx_count")"

if [[ -z "${MAIN_DIFF_FILE}" ]]; then
  MAIN_DIFF_FILE="${RUN_DIR}/main_diff.tsv"
fi
if [[ -z "${ORPHAN_FILE}" ]]; then
  ORPHAN_FILE="${RUN_DIR}/orphan_wx.tsv"
fi

if [[ -z "${SLA_SUMMARY_FILE}" ]]; then
  SLA_SUMMARY_FILE="$(latest_summary "${ROOT_DIR}/runtime/payment_reconcile_sla")"
fi
if [[ -z "${SLA_DETAIL_FILE}" && -n "${SLA_SUMMARY_FILE}" && -f "${SLA_SUMMARY_FILE}" ]]; then
  SLA_DETAIL_FILE="$(kv "${SLA_SUMMARY_FILE}" "detail_file")"
fi

SLA_STATUS="UNKNOWN"
SLA_AGE_DAYS=""
if [[ -n "${SLA_DETAIL_FILE}" && -f "${SLA_DETAIL_FILE}" ]]; then
  sla_line="$(awk -F $'\t' -v d="${RECON_DATE}" 'NR>1 && $1==d {print; exit}' "${SLA_DETAIL_FILE}" || true)"
  if [[ -n "${sla_line}" ]]; then
    SLA_AGE_DAYS="$(printf '%s' "${sla_line}" | awk -F $'\t' '{print $2}')"
    SLA_STATUS="$(printf '%s' "${sla_line}" | awk -F $'\t' '{print $6}')"
  fi
fi

if [[ ! -f "${MAIN_DIFF_FILE}" ]]; then
  echo "未找到差异文件: ${MAIN_DIFF_FILE}"
  exit 1
fi
if [[ ! -f "${ORPHAN_FILE}" ]]; then
  echo "未找到孤儿流水文件: ${ORPHAN_FILE}"
  exit 1
fi

if [[ -n "${OWNER_MAP_FILE}" ]]; then
  load_owner_rules "${OWNER_MAP_FILE}"
fi

mkdir -p "${OUTPUT_DIR}"
TSV_FILE="${OUTPUT_DIR}/tickets.tsv"
MD_FILE="${OUTPUT_DIR}/tickets.md"
TICKET_SUMMARY_FILE="${OUTPUT_DIR}/summary.txt"
ESCALATION_TSV_FILE="${OUTPUT_DIR}/escalation.tsv"
ESCALATION_MD_FILE="${OUTPUT_DIR}/escalation.md"
TYPE_SUMMARY_TMP="${OUTPUT_DIR}/.type_summary.tmp"
DETAIL_TMP="${OUTPUT_DIR}/.detail.tmp"

cat > "${TSV_FILE}" <<'TSV'
ticket_id	source	diff_type	base_severity	severity	escalation_reason	out_trade_no	order_id	store_id	amount_cent	amount_yuan	wx_trade_state	suggest_action	status
TSV

awk -F $'\t' -v out="${TSV_FILE}" -v sla_status="${SLA_STATUS}" -v amount_p1_threshold="${AMOUNT_P1_THRESHOLD_CENT}" '
BEGIN {
  idx_order_id = idx_out_trade_no = idx_store_id = idx_order_total_fee = idx_wx_trade_state = idx_diff_type = 0;
  seq = 0;
}
NR == 1 {
  for (i = 1; i <= NF; i++) {
    if ($i == "order_id") idx_order_id = i;
    else if ($i == "out_trade_no") idx_out_trade_no = i;
    else if ($i == "store_id") idx_store_id = i;
    else if ($i == "order_total_fee") idx_order_total_fee = i;
    else if ($i == "wx_trade_state") idx_wx_trade_state = i;
    else if ($i == "diff_type") idx_diff_type = i;
  }
  next;
}
{
  diff_type = (idx_diff_type > 0 ? $idx_diff_type : "");
  if (diff_type == "" || diff_type == "OK") next;

  out_trade_no = (idx_out_trade_no > 0 ? $idx_out_trade_no : "");
  order_id = (idx_order_id > 0 ? $idx_order_id : "");
  store_id = (idx_store_id > 0 ? $idx_store_id : "");
  amount_cent = (idx_order_total_fee > 0 ? $idx_order_total_fee : 0);
  wx_trade_state = (idx_wx_trade_state > 0 ? $idx_wx_trade_state : "");

  base_severity = "P2";
  suggest_action = "人工复核订单与渠道流水";
  if (diff_type ~ /^A1_/) {
    base_severity = "P1";
    suggest_action = "补查微信流水与回调日志，必要时执行补单";
  } else if (diff_type ~ /^A2_/) {
    base_severity = "P1";
    suggest_action = "核对金额差异与优惠退款，必要时差额退款";
  } else if (diff_type ~ /^A3_/) {
    base_severity = "P1";
    suggest_action = "执行查单补偿并校准支付/退款状态";
  } else if (diff_type ~ /^A4_/) {
    base_severity = "P1";
    suggest_action = "核对微信SUCCESS原因，补记支付或退款关单";
  }

  severity = base_severity;
  escalation_reason = "-";
  if (severity != "P1" && amount_cent + 0 >= amount_p1_threshold + 0) {
    severity = "P1";
    escalation_reason = "amount_threshold";
  }
  if (severity != "P1" && sla_status == "BREACH") {
    severity = "P1";
    if (escalation_reason == "-") {
      escalation_reason = "sla_breach";
    } else {
      escalation_reason = escalation_reason "+sla_breach";
    }
  }

  seq++;
  amount_yuan = sprintf("%.2f", amount_cent / 100.0);
  ticket_id = sprintf("MAIN-%06d", seq);
  printf "%s\tmain\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\tTODO\n", \
    ticket_id, diff_type, base_severity, severity, escalation_reason, out_trade_no, order_id, store_id, amount_cent, amount_yuan, wx_trade_state, suggest_action >> out;
}
' "${MAIN_DIFF_FILE}"

awk -F $'\t' -v out="${TSV_FILE}" '
BEGIN {
  idx_out_trade_no = idx_total_fee = idx_trade_state = 0;
  seq = 0;
}
NR == 1 {
  for (i = 1; i <= NF; i++) {
    if ($i == "out_trade_no") idx_out_trade_no = i;
    else if ($i == "total_fee") idx_total_fee = i;
    else if ($i == "trade_state") idx_trade_state = i;
  }
  next;
}
{
  out_trade_no = (idx_out_trade_no > 0 ? $idx_out_trade_no : "");
  amount_cent = (idx_total_fee > 0 ? $idx_total_fee : 0);
  wx_trade_state = (idx_trade_state > 0 ? $idx_trade_state : "");
  seq++;
  amount_yuan = sprintf("%.2f", amount_cent / 100.0);
  ticket_id = sprintf("ORPHAN-%06d", seq);
  diff_type = "O1_微信SUCCESS_系统无订单";
  base_severity = "P1";
  severity = "P1";
  escalation_reason = "-";
  suggest_action = "核对商户号与路由配置，确认是否漏单并补账";
  printf "%s\torphan\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\tTODO\n", \
    ticket_id, diff_type, base_severity, severity, escalation_reason, out_trade_no, "", "", amount_cent, amount_yuan, wx_trade_state, suggest_action >> out;
}
' "${ORPHAN_FILE}"

total_tickets="$(awk 'END{print NR>0?NR-1:0}' "${TSV_FILE}")"

if (( total_tickets == 0 )); then
  cat > "${ESCALATION_TSV_FILE}" <<'TSV'
ticket_id	severity	due_time	owner	status	out_trade_no	order_id	store_id	amount_yuan	escalation_reason	suggest_action
TSV
  cat > "${ESCALATION_MD_FILE}" <<MD
# 支付差异升级看板（${RECON_DATE}）

- 生成时间：$(date '+%Y-%m-%d %H:%M:%S')
- 工单总数：0
- 结论：无待升级工单。
MD
  cat > "${MD_FILE}" <<MD
# 支付差异工单（${RECON_DATE}）

- 生成时间：$(date '+%Y-%m-%d %H:%M:%S')
- summary：\`${SUMMARY_FILE}\`
- 原始差异：${RAW_DIFF_COUNT:-0}
- 自动消差：${AUTO_CLEARED_COUNT:-0}
- 最终待处理差异：${UNRESOLVED_DIFF_COUNT:-0}
- 孤儿流水：${ORPHAN_COUNT:-0}
- 工单数：0
- SLA状态：${SLA_STATUS}

结论：当日无待处理工单。
MD
  cat > "${TICKET_SUMMARY_FILE}" <<TXT
recon_date=${RECON_DATE}
total_tickets=0
p1_count=0
p2_count=0
escalated_count=0
sla_status=${SLA_STATUS}
sla_age_days=${SLA_AGE_DAYS}
amount_p1_threshold_cent=${AMOUNT_P1_THRESHOLD_CENT}
p1_sla_hours=${P1_SLA_HOURS}
p2_sla_hours=${P2_SLA_HOURS}
ticket_tsv=${TSV_FILE}
ticket_md=${MD_FILE}
escalation_tsv=${ESCALATION_TSV_FILE}
escalation_md=${ESCALATION_MD_FILE}
ticket_summary=${TICKET_SUMMARY_FILE}
TXT
  echo "[ticketize] 无待处理工单: ${MD_FILE}"
  exit 2
fi

p1_count="$(awk -F $'\t' 'NR>1 && $5=="P1" {c++} END{print c+0}' "${TSV_FILE}")"
p2_count="$(awk -F $'\t' 'NR>1 && $5=="P2" {c++} END{print c+0}' "${TSV_FILE}")"
escalated_count="$(awk -F $'\t' 'NR>1 && $4!=$5 {c++} END{print c+0}' "${TSV_FILE}")"

awk -F $'\t' '
NR == 1 { next; }
{
  key = $3;
  cnt[key]++;
  sum[key] += ($10 + 0);
  sev[key] = $5;
  action[key] = $13;
}
END {
  for (k in cnt) {
    sev_rank = (sev[k] == "P1" ? 1 : 2);
    printf "%d\t%s\t%s\t%d\t%.2f\t%s\n", sev_rank, sev[k], k, cnt[k], sum[k] / 100.0, action[k];
  }
}
' "${TSV_FILE}" | sort -t$'\t' -k1,1n -k4,4nr > "${TYPE_SUMMARY_TMP}"

tail -n +2 "${TSV_FILE}" | sort -t$'\t' -k5,5 -k10,10nr | head -n "${MAX_ROWS}" > "${DETAIL_TMP}"

{
  cat <<MD
# 支付差异工单（${RECON_DATE}）

- 生成时间：$(date '+%Y-%m-%d %H:%M:%S')
- summary：\`${SUMMARY_FILE}\`
- 原始差异：${RAW_DIFF_COUNT:-0}
- 自动消差：${AUTO_CLEARED_COUNT:-0}
- 最终待处理差异：${UNRESOLVED_DIFF_COUNT:-0}
- 孤儿流水：${ORPHAN_COUNT:-0}
- 工单总数：${total_tickets}
- P1工单：${p1_count}
- P2工单：${p2_count}
- 自动升级工单：${escalated_count}
- SLA状态：${SLA_STATUS}
- SLA账龄：${SLA_AGE_DAYS:-N/A}

## 一、按差异类型汇总

| 严重级别 | 差异类型 | 工单数 | 涉及金额(元) | 建议动作 |
|---|---|---:|---:|---|
MD

  while IFS=$'\t' read -r _sev_rank sev diff_type cnt sum_yuan action; do
    [[ -z "${diff_type}" ]] && continue
    printf '| %s | %s | %s | %s | %s |\n' "${sev}" "${diff_type}" "${cnt}" "${sum_yuan}" "${action}"
  done < "${TYPE_SUMMARY_TMP}"

  cat <<'MD'

## 二、待处理明细（Top）

| ticket_id | source | diff_type | base_severity | severity | escalation_reason | out_trade_no | order_id | store_id | amount_yuan | wx_trade_state | suggest_action | status |
|---|---|---|---|---|---|---|---|---|---:|---|---|---|
MD

  while IFS=$'\t' read -r ticket_id source diff_type base_severity severity escalation_reason out_trade_no order_id store_id _amount_cent amount_yuan wx_trade_state suggest_action status; do
    printf '| %s | %s | %s | %s | %s | %s | %s | %s | %s | %s | %s | %s | %s |\n' \
      "${ticket_id}" "${source}" "${diff_type}" "${base_severity}" "${severity}" "${escalation_reason}" "${out_trade_no}" "${order_id}" "${store_id}" "${amount_yuan}" "${wx_trade_state}" "${suggest_action}" "${status}"
  done < "${DETAIL_TMP}"
} > "${MD_FILE}"

rm -f "${TYPE_SUMMARY_TMP}" "${DETAIL_TMP}"

cat > "${ESCALATION_TSV_FILE}" <<'TSV'
ticket_id	severity	due_time	owner	status	out_trade_no	order_id	store_id	amount_yuan	escalation_reason	suggest_action
TSV

p1_due_time="$(date -d "+${P1_SLA_HOURS} hour" '+%Y-%m-%d %H:%M:%S')"
p2_due_time="$(date -d "+${P2_SLA_HOURS} hour" '+%Y-%m-%d %H:%M:%S')"

while IFS=$'\t' read -r ticket_id source diff_type base_severity severity escalation_reason out_trade_no order_id store_id _amount_cent amount_yuan wx_trade_state suggest_action status; do
  [[ -z "${ticket_id}" ]] && continue
  owner="$(resolve_owner "${severity}" "${source}" "${diff_type}" "${store_id}")"
  due_time="${p2_due_time}"
  if [[ "${severity}" == "P1" ]]; then
    due_time="${p1_due_time}"
  fi
  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
    "${ticket_id}" "${severity}" "${due_time}" "${owner}" "OPEN" "${out_trade_no}" "${order_id}" "${store_id}" "${amount_yuan}" "${escalation_reason}" "${suggest_action}" \
    >> "${ESCALATION_TSV_FILE}"
done < <(tail -n +2 "${TSV_FILE}")

{
  cat <<MD
# 支付差异升级看板（${RECON_DATE}）

- 生成时间：$(date '+%Y-%m-%d %H:%M:%S')
- P1时限：${P1_SLA_HOURS}小时
- P2时限：${P2_SLA_HOURS}小时
- 工单总数：${total_tickets}
- P1工单：${p1_count}
- P2工单：${p2_count}

| ticket_id | severity | due_time | owner | status | out_trade_no | order_id | store_id | amount_yuan | escalation_reason | suggest_action |
|---|---|---|---|---|---|---|---|---:|---|---|
MD
  tail -n +2 "${ESCALATION_TSV_FILE}" | head -n "${MAX_ROWS}" | while IFS=$'\t' read -r ticket_id severity due_time owner status out_trade_no order_id store_id amount_yuan escalation_reason suggest_action; do
    printf '| %s | %s | %s | %s | %s | %s | %s | %s | %s | %s | %s |\n' \
      "${ticket_id}" "${severity}" "${due_time}" "${owner}" "${status}" "${out_trade_no}" "${order_id}" "${store_id}" "${amount_yuan}" "${escalation_reason}" "${suggest_action}"
  done
} > "${ESCALATION_MD_FILE}"

cat > "${TICKET_SUMMARY_FILE}" <<TXT
recon_date=${RECON_DATE}
total_tickets=${total_tickets}
p1_count=${p1_count}
p2_count=${p2_count}
escalated_count=${escalated_count}
sla_status=${SLA_STATUS}
sla_age_days=${SLA_AGE_DAYS}
amount_p1_threshold_cent=${AMOUNT_P1_THRESHOLD_CENT}
p1_sla_hours=${P1_SLA_HOURS}
p2_sla_hours=${P2_SLA_HOURS}
owner_default=${OWNER_DEFAULT}
owner_p1=${OWNER_P1}
owner_map_file=${OWNER_MAP_FILE}
sla_summary_file=${SLA_SUMMARY_FILE}
sla_detail_file=${SLA_DETAIL_FILE}
ticket_tsv=${TSV_FILE}
ticket_md=${MD_FILE}
escalation_tsv=${ESCALATION_TSV_FILE}
escalation_md=${ESCALATION_MD_FILE}
ticket_summary=${TICKET_SUMMARY_FILE}
TXT

if [[ ${NO_ALERT} -eq 0 && -x "${ALERT_SCRIPT}" ]]; then
  title="支付对账工单提醒"
  if (( p1_count > 0 )); then
    title="支付对账工单告警(P1)"
  fi
  "${ALERT_SCRIPT}" \
    --title "${title}" \
    --content "date=${RECON_DATE}; tickets=${total_tickets}; p1=${p1_count}; p2=${p2_count}; escalated=${escalated_count}; sla_status=${SLA_STATUS}; sla_age_days=${SLA_AGE_DAYS:-N/A}; md=${MD_FILE}; escalation=${ESCALATION_MD_FILE}" || true
fi

echo "[ticketize] 生成成功:"
echo "- ${TSV_FILE}"
echo "- ${MD_FILE}"
echo "- ${ESCALATION_TSV_FILE}"
echo "- ${ESCALATION_MD_FILE}"
echo "- ${TICKET_SUMMARY_FILE}"
echo "[ticketize] total_tickets=${total_tickets}, p1_count=${p1_count}, p2_count=${p2_count}, escalated_count=${escalated_count}, sla_status=${SLA_STATUS}"

if (( p1_count > 0 )); then
  exit 2
fi
exit 0
