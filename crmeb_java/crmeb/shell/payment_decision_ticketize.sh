#!/usr/bin/env bash
set -euo pipefail

# D35: 判定链路失败自动工单化
# 输入：ops_status / go_nogo / warroom / decision_chain_smoke summary
# 输出：tickets_decision.tsv + tickets_decision.md + summary.txt

REPORT_DATE="${REPORT_DATE:-}"
RUNTIME_ROOT="${RUNTIME_ROOT:-}"
OUTPUT_DIR="${OUTPUT_DIR:-}"
MAX_ROWS="${MAX_ROWS:-200}"
NO_ALERT=0
OWNER_MAP_FILE="${OWNER_MAP_FILE:-}"
OWNER_DEFAULT="${OWNER_DEFAULT:-payment-ops}"
OWNER_P1="${OWNER_P1:-payment-oncall}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ALERT_SCRIPT="${ROOT_DIR}/shell/payment_alert_notify.sh"

usage() {
  cat <<'USAGE'
用法：
  ./shell/payment_decision_ticketize.sh [--date YYYY-MM-DD] [--runtime-root PATH] [--output-dir PATH] [--max-rows N] [--owner-map-file PATH] [--owner-default NAME] [--owner-p1 NAME] [--no-alert]

参数：
  --date YYYY-MM-DD     业务日期（默认昨天）
  --runtime-root PATH   runtime 根目录（默认 <repo>/runtime）
  --output-dir PATH     输出目录（默认 runtime/payment_reconcile/<date>/tickets）
  --max-rows N          markdown 明细展示上限（默认 200）
  --owner-map-file PATH owner 规则文件（可选）
  --owner-default NAME  默认 owner（默认 payment-ops）
  --owner-p1 NAME       P1 默认 owner（默认 payment-oncall）
  --no-alert            不推送机器人告警

退出码：
  0  工单生成成功且无 P1
  2  工单生成成功但存在 P1
  1  脚本执行失败
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --date)
      REPORT_DATE="$2"
      shift 2
      ;;
    --runtime-root)
      RUNTIME_ROOT="$2"
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

if [[ -z "${REPORT_DATE}" ]]; then
  REPORT_DATE="$(date -d 'yesterday' +%F)"
fi
if ! [[ "${REPORT_DATE}" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]]; then
  echo "参数错误: --date 需要 YYYY-MM-DD"
  exit 1
fi
if [[ -z "${RUNTIME_ROOT}" ]]; then
  RUNTIME_ROOT="${ROOT_DIR}/runtime"
fi
if [[ ! -d "${RUNTIME_ROOT}" ]]; then
  echo "参数错误: --runtime-root 目录不存在 -> ${RUNTIME_ROOT}"
  exit 1
fi
if [[ -z "${OUTPUT_DIR}" ]]; then
  OUTPUT_DIR="${RUNTIME_ROOT}/payment_reconcile/${REPORT_DATE}/tickets"
fi
if ! [[ "${MAX_ROWS}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: --max-rows 必须是正整数"
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

mkdir -p "${OUTPUT_DIR}"
TSV_FILE="${OUTPUT_DIR}/tickets_decision.tsv"
MD_FILE="${OUTPUT_DIR}/tickets_decision.md"
SUMMARY_FILE="${OUTPUT_DIR}/summary_decision.txt"

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

latest_summary_by_key() {
  local base="$1"
  local key="$2"
  local expected="$3"
  if [[ ! -d "${base}" ]]; then
    printf ''
    return
  fi
  local file=""
  while IFS= read -r file; do
    [[ -f "${file}" ]] || continue
    if [[ "$(kv "${file}" "${key}")" == "${expected}" ]]; then
      printf '%s' "${file}"
      return
    fi
  done < <(find "${base}" -maxdepth 2 -type f -name 'summary.txt' -printf '%T@ %p\n' 2>/dev/null | sort -nr | cut -d' ' -f2-)
  printf ''
}

resolve_summary() {
  local base="$1"
  local key="$2"
  local expected="$3"
  local matched=""
  matched="$(latest_summary_by_key "${base}" "${key}" "${expected}")"
  if [[ -n "${matched}" ]]; then
    printf '%s' "${matched}"
  else
    latest_summary "${base}"
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
  local source="$1"
  local severity="$2"
  local owner=""
  owner="${OWNER_RULES["source:${source}"]-}"
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

escape_tsv() {
  printf '%s' "$1" | tr '\t\r\n' '   '
}

append_ticket() {
  local source="$1"
  local severity="$2"
  local reason="$3"
  local action="$4"
  local ref="$5"

  ticket_seq=$((ticket_seq + 1))
  local id
  id="$(printf 'DEC-%06d' "${ticket_seq}")"
  local sev="${severity}"
  local owner
  owner="$(resolve_owner "${source}" "${sev}")"
  if [[ "${sev}" == "P1" ]]; then
    p1_count=$((p1_count + 1))
  else
    p2_count=$((p2_count + 1))
  fi
  printf '%s\t%s\t%s\t%s\t%s\tTODO\t%s\t%s\n' \
    "${id}" \
    "$(escape_tsv "${source}")" \
    "${sev}" \
    "${REPORT_DATE}" \
    "$(escape_tsv "${reason}")" \
    "$(escape_tsv "${action}") | ref=$(escape_tsv "${ref}")" \
    "$(escape_tsv "${owner}")" >> "${TSV_FILE}"
}

if [[ -n "${OWNER_MAP_FILE}" ]]; then
  load_owner_rules "${OWNER_MAP_FILE}"
fi

ops_summary="$(resolve_summary "${RUNTIME_ROOT}/payment_ops_status" "report_date" "${REPORT_DATE}")"
gonogo_summary="$(resolve_summary "${RUNTIME_ROOT}/payment_go_nogo" "report_date" "${REPORT_DATE}")"
warroom_summary="$(resolve_summary "${RUNTIME_ROOT}/payment_warroom" "report_date" "${REPORT_DATE}")"
decision_chain_summary="$(resolve_summary "${RUNTIME_ROOT}/payment_decision_chain_smoke" "report_date" "${REPORT_DATE}")"

{
  echo -e "ticket_id\tsource\tseverity\treport_date\treason\taction_status_ref\towner"
} > "${TSV_FILE}"

ticket_seq=0
p1_count=0
p2_count=0

# ops_status 阻断/告警
ops_block_reasons="$(kv "${ops_summary}" "block_reasons")"
ops_warn_reasons="$(kv "${ops_summary}" "warn_reasons")"
if [[ -n "${ops_block_reasons}" ]]; then
  while IFS= read -r reason; do
    [[ -z "${reason}" ]] && continue
    append_ticket "ops_status" "P1" "${reason}" "按阻断项完成修复并重跑 ops_status" "${ops_summary}"
  done < <(printf '%s\n' "${ops_block_reasons}" | tr ';' '\n' | sed 's/^ *//;s/ *$//')
fi
if [[ -n "${ops_warn_reasons}" ]]; then
  while IFS= read -r reason; do
    [[ -z "${reason}" ]] && continue
    append_ticket "ops_status" "P2" "${reason}" "关注项排期修复并确认不升级为阻断" "${ops_summary}"
  done < <(printf '%s\n' "${ops_warn_reasons}" | tr ';' '\n' | sed 's/^ *//;s/ *$//')
fi

# go_nogo
gonogo_decision="$(kv "${gonogo_summary}" "decision")"
gonogo_blocker_count="$(kv "${gonogo_summary}" "blocker_count")"
if [[ "${gonogo_decision}" == "NO_GO" ]]; then
  append_ticket "go_nogo" "P1" "go_nogo=NO_GO, blocker_count=${gonogo_blocker_count:-N/A}" "按 decision.md 阻断项逐条关闭后重跑 go_nogo" "${gonogo_summary}"
fi

# warroom
warroom_overall="$(kv "${warroom_summary}" "overall")"
warroom_risk_count="$(kv "${warroom_summary}" "risk_count")"
if [[ "${warroom_overall}" == "RED" ]]; then
  append_ticket "warroom" "P1" "warroom overall=RED, risk_count=${warroom_risk_count:-N/A}" "优先处理 P1 风险并回归 warroom" "${warroom_summary}"
elif [[ "${warroom_overall}" == "YELLOW" ]]; then
  append_ticket "warroom" "P2" "warroom overall=YELLOW, risk_count=${warroom_risk_count:-N/A}" "处理关注项并回归 warroom" "${warroom_summary}"
fi

# decision_chain
dc_severity="$(kv "${decision_chain_summary}" "severity")"
dc_fail_count="$(kv "${decision_chain_summary}" "fail_count")"
if [[ -z "${decision_chain_summary}" ]]; then
  append_ticket "decision_chain_smoke" "P1" "缺少 decision_chain_smoke summary(${REPORT_DATE})" "补跑 decision_chain_smoke 并确认同日引用" "${RUNTIME_ROOT}/payment_decision_chain_smoke"
elif [[ "${dc_severity}" != "PASS" ]]; then
  append_ticket "decision_chain_smoke" "P1" "decision_chain severity=${dc_severity:-N/A}, fail_count=${dc_fail_count:-N/A}" "按 report.md 失败项修复后重跑 decision_chain_smoke" "${decision_chain_summary}"
fi

total_tickets=$((p1_count + p2_count))

{
  echo "# 判定链路工单（${REPORT_DATE}）"
  echo
  echo "- 生成时间：$(date '+%Y-%m-%d %H:%M:%S')"
  echo "- 工单总数：${total_tickets}"
  echo "- P1：${p1_count}"
  echo "- P2：${p2_count}"
  echo
  echo "## 统计来源"
  echo
  echo "- ops_status: \`${ops_summary}\`"
  echo "- go_nogo: \`${gonogo_summary}\`"
  echo "- warroom: \`${warroom_summary}\`"
  echo "- decision_chain: \`${decision_chain_summary}\`"
  echo
  echo "## 明细（最多 ${MAX_ROWS} 行）"
  echo
  echo "| ticket_id | source | severity | owner | reason |"
  echo "|---|---|---|---|---|"
  awk -F $'\t' 'NR>1 {printf("| %s | %s | %s | %s | %s |\n",$1,$2,$3,$7,$5)}' "${TSV_FILE}" | head -n "${MAX_ROWS}"
  if (( total_tickets > MAX_ROWS )); then
    echo
    echo "> 仅展示前 ${MAX_ROWS} 行，完整见 \`${TSV_FILE}\`"
  fi
} > "${MD_FILE}"

{
  echo "report_date=${REPORT_DATE}"
  echo "generated_at=$(date '+%Y-%m-%d %H:%M:%S')"
  echo "total_tickets=${total_tickets}"
  echo "p1_count=${p1_count}"
  echo "p2_count=${p2_count}"
  echo "ops_summary=${ops_summary}"
  echo "go_nogo_summary=${gonogo_summary}"
  echo "warroom_summary=${warroom_summary}"
  echo "decision_chain_summary=${decision_chain_summary}"
  echo "owner_default=${OWNER_DEFAULT}"
  echo "owner_p1=${OWNER_P1}"
  echo "owner_map_file=${OWNER_MAP_FILE}"
  echo "ticket_tsv=${TSV_FILE}"
  echo "ticket_md=${MD_FILE}"
  echo "ticket_summary=${SUMMARY_FILE}"
} > "${SUMMARY_FILE}"

echo "[decision-ticketize] summary=${SUMMARY_FILE}"
echo "[decision-ticketize] tsv=${TSV_FILE}"
echo "[decision-ticketize] md=${MD_FILE}"
echo "[decision-ticketize] total=${total_tickets}, p1=${p1_count}, p2=${p2_count}"

if (( p1_count > 0 )) && [[ ${NO_ALERT} -eq 0 ]] && [[ -x "${ALERT_SCRIPT}" ]]; then
  "${ALERT_SCRIPT}" \
    --title "支付判定链路工单告警" \
    --content "date=${REPORT_DATE}; total=${total_tickets}; p1=${p1_count}; p2=${p2_count}; md=${MD_FILE}" || true
fi

if (( p1_count > 0 )); then
  exit 2
fi
exit 0
