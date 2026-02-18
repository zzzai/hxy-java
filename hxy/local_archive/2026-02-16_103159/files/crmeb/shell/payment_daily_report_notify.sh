#!/usr/bin/env bash
set -euo pipefail

# D12: 日报生成 + 机器人推送（可选）

REPORT_DATE="${REPORT_DATE:-}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DAILY_REPORT_SCRIPT="${ROOT_DIR}/shell/payment_daily_report.sh"
ALERT_SCRIPT="${ROOT_DIR}/shell/payment_alert_notify.sh"

usage() {
  cat <<'USAGE'
用法：
  ./shell/payment_daily_report_notify.sh [--date YYYY-MM-DD]

参数：
  --date YYYY-MM-DD   报告日期（默认昨天）

说明：
  1) 会先调用 payment_daily_report.sh 生成日报。
  2) 然后提取简版摘要并通过 payment_alert_notify.sh 推送。
  3) 未配置 webhook 时会自动 skip（返回 0）。
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --date)
      REPORT_DATE="$2"
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

if [[ -z "${REPORT_DATE}" ]]; then
  REPORT_DATE="$(date -d 'yesterday' +%F)"
fi
if ! [[ "${REPORT_DATE}" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]]; then
  echo "参数错误: --date 需要 YYYY-MM-DD"
  exit 1
fi

set +e
gen_out="$("${DAILY_REPORT_SCRIPT}" --date "${REPORT_DATE}" 2>&1)"
gen_rc=$?
set -e
printf '%s\n' "${gen_out}"
if [[ ${gen_rc} -ne 0 ]]; then
  exit "${gen_rc}"
fi

report_file="$(printf '%s\n' "${gen_out}" | sed -n 's/^\[daily-report\] 生成成功: //p' | tail -n 1)"
if [[ -z "${report_file}" || ! -f "${report_file}" ]]; then
  echo "[daily-report-notify] 无法定位 report 文件"
  exit 1
fi

brief="$(awk '
  /^```/ { in_block = !in_block; next; }
  in_block == 1 { print; }
' "${report_file}" | tr '\n' '; ' | sed 's/[[:space:]]\+/ /g; s/; $//')"

if [[ -z "${brief}" ]]; then
  brief="支付日报 ${REPORT_DATE} 已生成，详见 ${report_file}"
else
  brief="${brief}; report=${report_file}"
fi

"${ALERT_SCRIPT}" --title "支付日报" --content "${brief}"

exit 0
