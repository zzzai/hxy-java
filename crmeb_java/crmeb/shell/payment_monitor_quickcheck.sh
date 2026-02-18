#!/usr/bin/env bash
set -euo pipefail

# 支付监控快速检查（夜间值守版）
# 不依赖外部监控平台，先用日志做兜底巡检。

WINDOW_MINUTES="${WINDOW_MINUTES:-15}"
TAIL_LINES="${TAIL_LINES:-3000}"

THRESH_UNIFIEDORDER_ERR="${THRESH_UNIFIEDORDER_ERR:-5}"
THRESH_CALLBACK_ERR="${THRESH_CALLBACK_ERR:-5}"
THRESH_REFUND_ERR="${THRESH_REFUND_ERR:-3}"
THRESH_SIGN_VERIFY_ERR="${THRESH_SIGN_VERIFY_ERR:-1}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ADMIN_LOG_DIR="${ROOT_DIR}/crmeb-admin/crmeb_admin_log"
FRONT_LOG_DIR="${ROOT_DIR}/crmeb-front/crmeb_front_log"

usage() {
  cat <<'EOF'
用法：
  ./shell/payment_monitor_quickcheck.sh [--window MINUTES] [--tail LINES]

参数：
  --window MINUTES    仅扫描最近有写入的日志文件（默认 15 分钟）
  --tail LINES        每个日志文件最多读取尾部行数（默认 3000）
  -h, --help          查看帮助

阈值（可通过环境变量覆盖）：
  THRESH_UNIFIEDORDER_ERR=5
  THRESH_CALLBACK_ERR=5
  THRESH_REFUND_ERR=3
  THRESH_SIGN_VERIFY_ERR=1

退出码：
  0  正常
  2  命中告警阈值（建议立即人工介入）
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --window)
      WINDOW_MINUTES="$2"
      shift 2
      ;;
    --tail)
      TAIL_LINES="$2"
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

if ! [[ "${WINDOW_MINUTES}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: --window 必须为正整数"
  exit 1
fi
if ! [[ "${TAIL_LINES}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: --tail 必须为正整数"
  exit 1
fi

if [[ ! -d "${ADMIN_LOG_DIR}" && ! -d "${FRONT_LOG_DIR}" ]]; then
  echo "未找到日志目录: ${ADMIN_LOG_DIR} / ${FRONT_LOG_DIR}"
  exit 1
fi

mapfile -t RECENT_FILES < <(
  {
    [[ -d "${ADMIN_LOG_DIR}" ]] && find "${ADMIN_LOG_DIR}" -type f -name '*.log' -mmin -"${WINDOW_MINUTES}"
    [[ -d "${FRONT_LOG_DIR}" ]] && find "${FRONT_LOG_DIR}" -type f -name '*.log' -mmin -"${WINDOW_MINUTES}"
  } | sort -u
)

# 若最近窗口没有更新文件，回退读取最新若干日志，避免“空检查”。
if [[ ${#RECENT_FILES[@]} -eq 0 ]]; then
  mapfile -t RECENT_FILES < <(
    {
      [[ -d "${ADMIN_LOG_DIR}" ]] && find "${ADMIN_LOG_DIR}" -type f -name '*.log' | sort
      [[ -d "${FRONT_LOG_DIR}" ]] && find "${FRONT_LOG_DIR}" -type f -name '*.log' | sort
    } | tail -n 8
  )
fi

if [[ ${#RECENT_FILES[@]} -eq 0 ]]; then
  echo "未找到可读取的日志文件"
  exit 1
fi

TMP_LOG="$(mktemp)"
cleanup() {
  rm -f "${TMP_LOG}"
}
trap cleanup EXIT

for f in "${RECENT_FILES[@]}"; do
  if [[ -f "${f}" ]]; then
    tail -n "${TAIL_LINES}" "${f}" >> "${TMP_LOG}" || true
  fi
done

count_pattern() {
  local pattern="$1"
  (rg -N --no-heading -o -i "${pattern}" "${TMP_LOG}" || true) | wc -l | tr -d ' '
}

UNIFIEDORDER_ERR_COUNT="$(count_pattern '统一下单失败|unifiedorder.*(error|fail)|wechat.*pay.*(error|fail)')"
CALLBACK_ERR_COUNT="$(count_pattern '支付回调失败|支付回调异常|notify.*(error|fail)|回调.*签名.*失败|验签.*失败')"
REFUND_ERR_COUNT="$(count_pattern '退款回调失败|退款回调异常|refund.*notify.*(error|fail)|订单退款错误')"
SIGN_VERIFY_ERR_COUNT="$(count_pattern '验签失败|签名校验失败|sign.*verify.*(error|fail)')"

echo "========== 支付监控快速检查 =========="
echo "时间窗口(文件写入): ${WINDOW_MINUTES} 分钟"
echo "扫描文件数: ${#RECENT_FILES[@]}"
echo "统一下单异常数: ${UNIFIEDORDER_ERR_COUNT} (阈值 ${THRESH_UNIFIEDORDER_ERR})"
echo "支付回调异常数: ${CALLBACK_ERR_COUNT} (阈值 ${THRESH_CALLBACK_ERR})"
echo "退款回调异常数: ${REFUND_ERR_COUNT} (阈值 ${THRESH_REFUND_ERR})"
echo "验签异常数: ${SIGN_VERIFY_ERR_COUNT} (阈值 ${THRESH_SIGN_VERIFY_ERR})"

ALERT=0
if (( UNIFIEDORDER_ERR_COUNT > THRESH_UNIFIEDORDER_ERR )); then
  echo "[ALERT] 统一下单异常超阈值"
  ALERT=1
fi
if (( CALLBACK_ERR_COUNT > THRESH_CALLBACK_ERR )); then
  echo "[ALERT] 支付回调异常超阈值"
  ALERT=1
fi
if (( REFUND_ERR_COUNT > THRESH_REFUND_ERR )); then
  echo "[ALERT] 退款回调异常超阈值"
  ALERT=1
fi
if (( SIGN_VERIFY_ERR_COUNT > THRESH_SIGN_VERIFY_ERR )); then
  echo "[ALERT] 验签异常超阈值"
  ALERT=1
fi

if (( ALERT == 1 )); then
  echo "建议动作: 立刻查看 recent log 并执行支付链路冒烟（下单/回调/查单/退款）"
  exit 2
fi

echo "检查结果: 未命中阈值"
exit 0
