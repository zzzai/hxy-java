#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:48080/admin-api/pay/notify}"
SCENARIO="duplicate"
BIZ_TYPE="order"
CHANNEL_ID=""
FIRST_BODY_FILE=""
SECOND_BODY_FILE=""
HEADER_FILE=""
QUERY_FILE=""
REPEAT_TIMES=2
DELAY_SECONDS=120
CONTENT_TYPE="application/json"
INSECURE=0
DRY_RUN=0
OUT_DIR="${OUT_DIR:-/tmp/pay_notify_replay_$(date +%Y%m%d_%H%M%S)}"

usage() {
  cat <<'USAGE'
Usage:
  script/dev/replay_pay_notify.sh [options]

Required:
  --channel-id <id>          支付渠道 ID（回调地址中的 channelId）
  --first-body-file <file>   第一条回调请求体（原样 body）

Options:
  --base-url <url>           回调地址前缀（default: http://127.0.0.1:48080/admin-api/pay/notify）
  --biz-type <type>          order | refund | transfer（default: order）
  --scenario <type>          duplicate | out_of_order | delayed（default: duplicate）
  --second-body-file <file>  乱序场景第二条请求体（out_of_order 必填）
  --header-file <file>       请求头文件（每行一个 Header，格式 Key: Value）
  --query-file <file>        Query 参数文件（每行 key=value）
  --repeat-times <n>         duplicate 场景发送次数（default: 2）
  --delay-seconds <n>        delayed 场景间隔秒数（default: 120）
  --content-type <type>      请求 Content-Type（default: application/json）
  --insecure                 curl 跳过 HTTPS 证书校验（-k）
  --dry-run                  仅打印请求，不实际发送
  --out-dir <dir>            输出目录（default: /tmp/pay_notify_replay_时间戳）
  -h, --help                 Show help

Examples:
  # 重复回调
  script/dev/replay_pay_notify.sh \
    --channel-id 18 \
    --first-body-file /tmp/pay_notify_success.json \
    --header-file /tmp/pay_notify_headers.txt \
    --scenario duplicate \
    --repeat-times 3

  # 乱序回调（先成功再关闭，或反向）
  script/dev/replay_pay_notify.sh \
    --channel-id 18 \
    --first-body-file /tmp/pay_notify_success.json \
    --second-body-file /tmp/pay_notify_closed.json \
    --header-file /tmp/pay_notify_headers.txt \
    --scenario out_of_order
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --base-url)
      BASE_URL="$2"
      shift 2
      ;;
    --biz-type)
      BIZ_TYPE="$2"
      shift 2
      ;;
    --scenario)
      SCENARIO="$2"
      shift 2
      ;;
    --channel-id)
      CHANNEL_ID="$2"
      shift 2
      ;;
    --first-body-file)
      FIRST_BODY_FILE="$2"
      shift 2
      ;;
    --second-body-file)
      SECOND_BODY_FILE="$2"
      shift 2
      ;;
    --header-file)
      HEADER_FILE="$2"
      shift 2
      ;;
    --query-file)
      QUERY_FILE="$2"
      shift 2
      ;;
    --repeat-times)
      REPEAT_TIMES="$2"
      shift 2
      ;;
    --delay-seconds)
      DELAY_SECONDS="$2"
      shift 2
      ;;
    --content-type)
      CONTENT_TYPE="$2"
      shift 2
      ;;
    --insecure)
      INSECURE=1
      shift
      ;;
    --dry-run)
      DRY_RUN=1
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
      echo "Unknown option: $1" >&2
      usage
      exit 1
      ;;
  esac
done

if [[ -z "$CHANNEL_ID" ]]; then
  echo "Missing required --channel-id" >&2
  exit 1
fi
if [[ -z "$FIRST_BODY_FILE" || ! -f "$FIRST_BODY_FILE" ]]; then
  echo "Missing or invalid --first-body-file: $FIRST_BODY_FILE" >&2
  exit 1
fi
if [[ "$BIZ_TYPE" != "order" && "$BIZ_TYPE" != "refund" && "$BIZ_TYPE" != "transfer" ]]; then
  echo "Invalid --biz-type: $BIZ_TYPE" >&2
  exit 1
fi
if [[ "$SCENARIO" != "duplicate" && "$SCENARIO" != "out_of_order" && "$SCENARIO" != "delayed" ]]; then
  echo "Invalid --scenario: $SCENARIO" >&2
  exit 1
fi
if [[ "$SCENARIO" == "out_of_order" && ( -z "$SECOND_BODY_FILE" || ! -f "$SECOND_BODY_FILE" ) ]]; then
  echo "out_of_order requires valid --second-body-file" >&2
  exit 1
fi

mkdir -p "$OUT_DIR"
SUMMARY_FILE="$OUT_DIR/summary.tsv"
touch "$SUMMARY_FILE"

build_query_string() {
  local qs=""
  if [[ -n "$QUERY_FILE" ]]; then
    if [[ ! -f "$QUERY_FILE" ]]; then
      echo "query file not found: $QUERY_FILE" >&2
      exit 1
    fi
    while IFS= read -r line; do
      [[ -z "${line// }" ]] && continue
      [[ "$line" =~ ^# ]] && continue
      if [[ "$line" != *"="* ]]; then
        echo "Invalid query line (expected key=value): $line" >&2
        exit 1
      fi
      if [[ -n "$qs" ]]; then
        qs+="&"
      fi
      qs+="$line"
    done < "$QUERY_FILE"
  fi
  echo "$qs"
}

build_header_args() {
  local -n out_arr=$1
  out_arr=(-H "Content-Type: ${CONTENT_TYPE}")
  if [[ -n "$HEADER_FILE" ]]; then
    if [[ ! -f "$HEADER_FILE" ]]; then
      echo "header file not found: $HEADER_FILE" >&2
      exit 1
    fi
    while IFS= read -r line; do
      [[ -z "${line// }" ]] && continue
      [[ "$line" =~ ^# ]] && continue
      out_arr+=(-H "$line")
    done < "$HEADER_FILE"
  fi
}

QUERY_STRING="$(build_query_string)"
TARGET_URL="${BASE_URL%/}/${BIZ_TYPE}/${CHANNEL_ID}"
if [[ -n "$QUERY_STRING" ]]; then
  TARGET_URL="${TARGET_URL}?${QUERY_STRING}"
fi

declare -a HEADER_ARGS=()
build_header_args HEADER_ARGS

send_once() {
  local body_file="$1"
  local label="$2"
  local ts
  ts="$(date '+%Y-%m-%d %H:%M:%S')"
  local resp_file="$OUT_DIR/response_${label}_$(date +%s).txt"

  if [[ "$DRY_RUN" -eq 1 ]]; then
    echo "[$ts] DRY_RUN label=${label} url=${TARGET_URL} body=${body_file}"
    return 0
  fi

  local -a curl_args=(
    -sS
    -X POST
    "$TARGET_URL"
    "${HEADER_ARGS[@]}"
    --data-binary "@${body_file}"
    -o "$resp_file"
    -w "%{http_code}"
  )
  if [[ "$INSECURE" -eq 1 ]]; then
    curl_args=(-k "${curl_args[@]}")
  fi

  local http_code
  http_code="$(curl "${curl_args[@]}")"
  echo -e "${ts}\t${label}\t${http_code}\t${resp_file}" >> "$SUMMARY_FILE"
  echo "[$ts] label=${label} http=${http_code} resp=${resp_file}"
}

echo "Target URL: ${TARGET_URL}"
echo "Scenario  : ${SCENARIO}"
echo "Out Dir   : ${OUT_DIR}"
echo

case "$SCENARIO" in
  duplicate)
    for i in $(seq 1 "$REPEAT_TIMES"); do
      send_once "$FIRST_BODY_FILE" "duplicate_${i}"
    done
    ;;
  out_of_order)
    send_once "$FIRST_BODY_FILE" "out_of_order_1"
    send_once "$SECOND_BODY_FILE" "out_of_order_2"
    ;;
  delayed)
    send_once "$FIRST_BODY_FILE" "delayed_1"
    echo "Sleep ${DELAY_SECONDS}s before replay..."
    sleep "$DELAY_SECONDS"
    send_once "$FIRST_BODY_FILE" "delayed_2"
    ;;
esac

if [[ "$DRY_RUN" -eq 0 ]]; then
  echo
  echo "== Replay Summary =="
  echo -e "time\tlabel\thttp_code\tresponse_file"
  cat "$SUMMARY_FILE"
  echo
  echo "HTTP code stats:"
  awk -F'\t' '{c[$3]++} END{for (k in c) printf("  %s -> %d\n", k, c[k]);}' "$SUMMARY_FILE" | sort
else
  echo
  echo "DRY_RUN completed."
fi
