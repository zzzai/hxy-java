#!/usr/bin/env bash
set -euo pipefail

# 统一告警推送脚本（企业微信/钉钉/飞书）

TITLE="${TITLE:-支付告警}"
CONTENT="${CONTENT:-}"
WEBHOOK_URL="${ALERT_WEBHOOK_URL:-}"
WEBHOOK_TYPE="${ALERT_WEBHOOK_TYPE:-wecom}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SUPPRESS_SECONDS="${ALERT_SUPPRESS_SECONDS:-600}"
SUPPRESS_DIR="${ALERT_SUPPRESS_DIR:-${ROOT_DIR}/runtime/payment_alert_suppress}"

usage() {
  cat <<'USAGE'
用法：
  ./shell/payment_alert_notify.sh --title "标题" --content "内容" [--webhook URL] [--type wecom|dingtalk|feishu] [--suppress-seconds N]

参数：
  --title     告警标题
  --content   告警内容
  --webhook   机器人 webhook（默认读取环境变量 ALERT_WEBHOOK_URL）
  --type      告警平台类型（默认 wecom）
  --suppress-seconds N   同类告警抑制窗口（秒），默认 600，0 表示关闭抑制

说明：
  1) 未提供 webhook 时脚本会输出 skip 并返回 0。
  2) wecom/dingtalk 使用 msgtype=text；feishu 使用 msg_type=text。
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --title)
      TITLE="$2"
      shift 2
      ;;
    --content)
      CONTENT="$2"
      shift 2
      ;;
    --webhook)
      WEBHOOK_URL="$2"
      shift 2
      ;;
    --type)
      WEBHOOK_TYPE="$2"
      shift 2
      ;;
    --suppress-seconds)
      SUPPRESS_SECONDS="$2"
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

if [[ -z "${WEBHOOK_URL}" ]]; then
  echo "[alert] skip: ALERT_WEBHOOK_URL 为空"
  exit 0
fi

if [[ -z "${CONTENT}" ]]; then
  CONTENT="(empty)"
fi

WEBHOOK_TYPE="$(printf '%s' "${WEBHOOK_TYPE}" | tr '[:upper:]' '[:lower:]')"
if ! [[ "${SUPPRESS_SECONDS}" =~ ^[0-9]+$ ]]; then
  echo "[alert] 参数错误: --suppress-seconds 必须为非负整数"
  exit 1
fi

hash_text() {
  local text="$1"
  if command -v sha256sum >/dev/null 2>&1; then
    printf '%s' "${text}" | sha256sum | awk '{print $1}'
  elif command -v shasum >/dev/null 2>&1; then
    printf '%s' "${text}" | shasum -a 256 | awk '{print $1}'
  else
    printf '%s' "${text}" | cksum | awk '{print $1}'
  fi
}

SIGNATURE_KEY="$(hash_text "${WEBHOOK_TYPE}|${WEBHOOK_URL}|${TITLE}|${CONTENT}")"
STATE_FILE="${SUPPRESS_DIR}/${SIGNATURE_KEY}.state"
NOW_TS="$(date +%s)"

if (( SUPPRESS_SECONDS > 0 )); then
  mkdir -p "${SUPPRESS_DIR}"
  if [[ -f "${STATE_FILE}" ]]; then
    LAST_TS="$(cat "${STATE_FILE}" 2>/dev/null || true)"
    if [[ "${LAST_TS}" =~ ^[0-9]+$ ]]; then
      DELTA=$((NOW_TS - LAST_TS))
      if (( DELTA >= 0 && DELTA < SUPPRESS_SECONDS )); then
        echo "[alert] suppress duplicate: ${DELTA}s < ${SUPPRESS_SECONDS}s"
        exit 0
      fi
    fi
  fi
fi

# 简单 JSON 转义（满足文本告警场景）
json_escape() {
  printf '%s' "$1" | sed 's/\\/\\\\/g; s/"/\\"/g; s/\r/\\r/g; s/\n/\\n/g'
}

TEXT_CONTENT="[$TITLE] $CONTENT"
TEXT_CONTENT_ESCAPED="$(json_escape "$TEXT_CONTENT")"

case "${WEBHOOK_TYPE}" in
  wecom|dingtalk)
    PAYLOAD="{\"msgtype\":\"text\",\"text\":{\"content\":\"${TEXT_CONTENT_ESCAPED}\"}}"
    ;;
  feishu)
    PAYLOAD="{\"msg_type\":\"text\",\"content\":{\"text\":\"${TEXT_CONTENT_ESCAPED}\"}}"
    ;;
  *)
    echo "[alert] 不支持的告警类型: ${WEBHOOK_TYPE}"
    exit 1
    ;;
esac

HTTP_CODE="$(curl -sS -m 12 -o /tmp/payment_alert_resp.txt -w '%{http_code}' \
  -H 'Content-Type: application/json' \
  -X POST "${WEBHOOK_URL}" \
  -d "${PAYLOAD}" || true)"

if [[ "${HTTP_CODE}" =~ ^2[0-9][0-9]$ ]]; then
  if (( SUPPRESS_SECONDS > 0 )); then
    printf '%s' "${NOW_TS}" > "${STATE_FILE}"
  fi
  echo "[alert] push success: ${HTTP_CODE}"
  exit 0
fi

echo "[alert] push failed: http=${HTTP_CODE}"
if [[ -f /tmp/payment_alert_resp.txt ]]; then
  echo "[alert] response: $(cat /tmp/payment_alert_resp.txt)"
fi
exit 1
