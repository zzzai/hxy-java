#!/usr/bin/env bash
set -euo pipefail

# D17: 对账差异 SLA 守卫
# 目标：检查历史对账差异是否在 SLA 窗口内消化，避免“差异长期挂账”。

LOOKBACK_DAYS="${LOOKBACK_DAYS:-14}"
SLA_DAYS="${SLA_DAYS:-1}"
NO_ALERT=0
BASE_DIR="${BASE_DIR:-}"
OUT_DIR="${OUT_DIR:-}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ALERT_SCRIPT="${ROOT_DIR}/shell/payment_alert_notify.sh"

usage() {
  cat <<'USAGE'
用法：
  ./shell/payment_reconcile_sla_guard.sh [--lookback-days N] [--sla-days N] [--base-dir PATH] [--no-alert] [--out-dir PATH]

参数：
  --lookback-days N   回看天数（默认 14）
  --sla-days N        差异允许存续天数（默认 1；超过即 BREACH）
  --base-dir PATH     对账目录（默认 runtime/payment_reconcile）
  --no-alert          不推送告警
  --out-dir PATH      输出目录（默认 runtime/payment_reconcile_sla）

退出码：
  0  OK（无差异挂账）
  2  WARN/BREACH（存在挂账，需处理）
  1  脚本执行失败
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --lookback-days)
      LOOKBACK_DAYS="$2"
      shift 2
      ;;
    --sla-days)
      SLA_DAYS="$2"
      shift 2
      ;;
    --base-dir)
      BASE_DIR="$2"
      shift 2
      ;;
    --no-alert)
      NO_ALERT=1
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

if ! [[ "${LOOKBACK_DAYS}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: --lookback-days 必须是正整数"
  exit 1
fi
if ! [[ "${SLA_DAYS}" =~ ^[0-9]+$ ]]; then
  echo "参数错误: --sla-days 必须是非负整数"
  exit 1
fi

if [[ -z "${BASE_DIR}" ]]; then
  BASE_DIR="${ROOT_DIR}/runtime/payment_reconcile"
fi
if [[ -z "${OUT_DIR}" ]]; then
  OUT_DIR="${ROOT_DIR}/runtime/payment_reconcile_sla"
fi

RUN_ID="$(date '+%Y%m%d%H%M%S')-$$"
RUN_DIR="${OUT_DIR}/run-${RUN_ID}"
mkdir -p "${RUN_DIR}"
SUMMARY_FILE="${RUN_DIR}/summary.txt"
REPORT_FILE="${RUN_DIR}/report.md"
DETAIL_FILE="${RUN_DIR}/detail.tsv"

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

days_between() {
  local date_from="$1"
  local date_to="$2"
  local ts_from ts_to
  ts_from="$(date -d "${date_from} 00:00:00" +%s 2>/dev/null || echo "")"
  ts_to="$(date -d "${date_to} 00:00:00" +%s 2>/dev/null || echo "")"
  if [[ -z "${ts_from}" || -z "${ts_to}" ]]; then
    printf -- "-1"
    return
  fi
  printf '%s' "$(( (ts_to - ts_from) / 86400 ))"
}

echo "[reconcile-sla] run_dir=${RUN_DIR}"
echo "[reconcile-sla] base_dir=${BASE_DIR}, lookback_days=${LOOKBACK_DAYS}, sla_days=${SLA_DAYS}"

if [[ ! -d "${BASE_DIR}" ]]; then
  echo "未找到对账目录: ${BASE_DIR}"
  exit 1
fi

today="$(date +%F)"
lookback_start="$(date -d "${today} -${LOOKBACK_DAYS} day" +%F)"

printf "recon_date\tage_days\tunresolved\torphan\ttotal\tstatus\tsummary_path\n" > "${DETAIL_FILE}"

scan_count=0
issue_days=0
pending_days=0
breach_days=0
total_unresolved=0
total_orphan=0

while IFS= read -r summary; do
  [[ -f "${summary}" ]] || continue
  recon_date="$(kv "${summary}" "recon_date")"
  unresolved="$(kv "${summary}" "main_diff_count")"
  orphan="$(kv "${summary}" "orphan_wx_count")"

  if [[ -z "${recon_date}" || ! "${recon_date}" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]]; then
    continue
  fi
  if [[ "${recon_date}" < "${lookback_start}" || "${recon_date}" > "${today}" ]]; then
    continue
  fi
  if ! [[ "${unresolved:-}" =~ ^[0-9]+$ ]]; then unresolved=0; fi
  if ! [[ "${orphan:-}" =~ ^[0-9]+$ ]]; then orphan=0; fi

  total=$(( unresolved + orphan ))
  age_days="$(days_between "${recon_date}" "${today}")"
  if ! [[ "${age_days}" =~ ^-?[0-9]+$ ]]; then
    age_days=-1
  fi

  status="OK"
  scan_count=$((scan_count + 1))
  total_unresolved=$((total_unresolved + unresolved))
  total_orphan=$((total_orphan + orphan))
  if (( total > 0 )); then
    issue_days=$((issue_days + 1))
    if (( age_days > SLA_DAYS )); then
      status="BREACH"
      breach_days=$((breach_days + 1))
    else
      status="PENDING"
      pending_days=$((pending_days + 1))
    fi
  fi

  printf "%s\t%s\t%s\t%s\t%s\t%s\t%s\n" "${recon_date}" "${age_days}" "${unresolved}" "${orphan}" "${total}" "${status}" "${summary}" >> "${DETAIL_FILE}"
done < <(find "${BASE_DIR}" -maxdepth 2 -type f -name 'summary.txt' | sort)

severity="OK"
exit_code=0
if (( breach_days > 0 )); then
  severity="BREACH"
  exit_code=2
elif (( pending_days > 0 )); then
  severity="WARN"
  exit_code=2
fi

cat > "${SUMMARY_FILE}" <<TXT
run_id=${RUN_ID}
run_time=$(date '+%Y-%m-%d %H:%M:%S')
lookback_days=${LOOKBACK_DAYS}
sla_days=${SLA_DAYS}
lookback_start=${lookback_start}
today=${today}
severity=${severity}
scan_count=${scan_count}
issue_days=${issue_days}
pending_days=${pending_days}
breach_days=${breach_days}
total_unresolved=${total_unresolved}
total_orphan=${total_orphan}
detail_file=${DETAIL_FILE}
run_dir=${RUN_DIR}
TXT

{
  echo "# 对账差异 SLA 守卫"
  echo
  echo "- run_id: \`${RUN_ID}\`"
  echo "- run_time: \`$(date '+%Y-%m-%d %H:%M:%S')\`"
  echo "- lookback_days: \`${LOOKBACK_DAYS}\`"
  echo "- sla_days: \`${SLA_DAYS}\`"
  echo "- severity: **${severity}**"
  echo
  echo "## 汇总"
  echo
  echo "| 指标 | 数值 |"
  echo "|---|---:|"
  echo "| scan_count | ${scan_count} |"
  echo "| issue_days | ${issue_days} |"
  echo "| pending_days | ${pending_days} |"
  echo "| breach_days | ${breach_days} |"
  echo "| total_unresolved | ${total_unresolved} |"
  echo "| total_orphan | ${total_orphan} |"
  echo
  echo "## 明细（最近 ${LOOKBACK_DAYS} 天）"
  echo
  echo "| recon_date | age_days | unresolved | orphan | total | status |"
  echo "|---|---:|---:|---:|---:|---|"
  tail -n +2 "${DETAIL_FILE}" | sort -t$'\t' -k1,1r | while IFS=$'\t' read -r recon_date age_days unresolved orphan total status _summary; do
    printf '| %s | %s | %s | %s | %s | %s |\n' "${recon_date}" "${age_days}" "${unresolved}" "${orphan}" "${total}" "${status}"
  done
  echo
  echo "detail_file: \`${DETAIL_FILE}\`"
  echo "summary_file: \`${SUMMARY_FILE}\`"
} > "${REPORT_FILE}"

echo "[reconcile-sla] report=${REPORT_FILE}"
echo "[reconcile-sla] summary=${SUMMARY_FILE}"
echo "[reconcile-sla] severity=${severity}, issue_days=${issue_days}, pending_days=${pending_days}, breach_days=${breach_days}"

if (( exit_code != 0 && NO_ALERT == 0 )) && [[ -x "${ALERT_SCRIPT}" ]]; then
  "${ALERT_SCRIPT}" \
    --title "支付对账SLA告警" \
    --content "severity=${severity}; issue_days=${issue_days}; pending_days=${pending_days}; breach_days=${breach_days}; report=${REPORT_FILE}" || true
fi

exit "${exit_code}"
