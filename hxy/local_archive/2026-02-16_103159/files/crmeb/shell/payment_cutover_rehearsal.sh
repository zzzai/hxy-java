#!/usr/bin/env bash
set -euo pipefail

# D14: 服务商上线切换预演（无订单号版）
# 串行执行：
# 1) preflight
# 2) ops_daily
# 3) idempotency regression
# 4) reconcile ticketize
# 5) store mapping dry-run（可选）
# 6) runtime retention dry-run

RECON_DATE="${RECON_DATE:-}"
WINDOW_HOURS="${WINDOW_HOURS:-72}"
KEEP_DAYS="${KEEP_DAYS:-7}"
STRICT_PREFLIGHT=1
NO_ALERT=0
STORE_MAPPING_CSV="${STORE_MAPPING_CSV:-}"
OUT_DIR="${OUT_DIR:-}"
SKIP_SNAPSHOT=0

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ALERT_SCRIPT="${ROOT_DIR}/shell/payment_alert_notify.sh"

usage() {
  cat <<'USAGE'
用法：
  ./shell/payment_cutover_rehearsal.sh [--date YYYY-MM-DD] [--window-hours N] [--keep-days N] [--store-mapping-csv FILE] [--skip-snapshot] [--non-strict-preflight] [--no-alert] [--out-dir PATH]

参数：
  --date YYYY-MM-DD           对账日期（默认昨天）
  --window-hours N            幂等回归窗口小时（默认 72）
  --keep-days N               运行产物保留天数（默认 7，dry-run）
  --store-mapping-csv FILE    门店映射CSV（可选，执行 dry-run 严格校验）
  --skip-snapshot             跳过预演前配置快照（默认不跳过）
  --non-strict-preflight      预检不启用 strict（默认 strict）
  --no-alert                  不推送机器人告警
  --out-dir PATH              输出目录（默认 runtime/payment_cutover_rehearsal）

说明：
  1) 本脚本不会写入支付配置和门店映射，仅做预演验证。
  2) 若要正式生效，仍需单独执行 service_provider_fill 与 mapping apply。

退出码：
  0  预演通过（可进入“拿订单号后联调”）
  2  预演未通过（有风险项）
  1  脚本执行失败
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --date)
      RECON_DATE="$2"
      shift 2
      ;;
    --window-hours)
      WINDOW_HOURS="$2"
      shift 2
      ;;
    --keep-days)
      KEEP_DAYS="$2"
      shift 2
      ;;
    --store-mapping-csv)
      STORE_MAPPING_CSV="$2"
      shift 2
      ;;
    --skip-snapshot)
      SKIP_SNAPSHOT=1
      shift
      ;;
    --non-strict-preflight)
      STRICT_PREFLIGHT=0
      shift
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

if [[ -z "${RECON_DATE}" ]]; then
  RECON_DATE="$(date -d 'yesterday' +%F)"
fi
if ! [[ "${RECON_DATE}" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]]; then
  echo "参数错误: --date 需要 YYYY-MM-DD"
  exit 1
fi
if ! [[ "${WINDOW_HOURS}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: --window-hours 必须是正整数"
  exit 1
fi
if ! [[ "${KEEP_DAYS}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: --keep-days 必须是正整数"
  exit 1
fi
if [[ -n "${STORE_MAPPING_CSV}" && ! -f "${STORE_MAPPING_CSV}" ]]; then
  echo "参数错误: --store-mapping-csv 文件不存在 -> ${STORE_MAPPING_CSV}"
  exit 1
fi

if [[ -z "${OUT_DIR}" ]]; then
  OUT_DIR="${ROOT_DIR}/runtime/payment_cutover_rehearsal"
fi
RUN_ID="$(date '+%Y%m%d%H%M%S')"
RUN_DIR="${OUT_DIR}/run-${RUN_ID}"
mkdir -p "${RUN_DIR}"

SUMMARY_FILE="${RUN_DIR}/summary.txt"
REPORT_FILE="${RUN_DIR}/report.md"

run_step() {
  local step="$1"
  shift
  local log="${RUN_DIR}/${step}.log"
  set +e
  "$@" >"${log}" 2>&1
  local rc=$?
  set -e
  printf '%s' "${rc}"
}

echo "[cutover-rehearsal] run_dir=${RUN_DIR}"
echo "[cutover-rehearsal] recon_date=${RECON_DATE}, window_hours=${WINDOW_HOURS}, keep_days=${KEEP_DAYS}"

if [[ ${SKIP_SNAPSHOT} -eq 0 ]]; then
  snapshot_raw_rc="$(run_step "00_config_snapshot" ./shell/payment_config_snapshot.sh --tag "cutover-rehearsal")"
  snapshot_rc="${snapshot_raw_rc}"
else
  snapshot_raw_rc="-"
  snapshot_rc="0"
fi

preflight_args=(./shell/payment_preflight_check.sh --out-dir "${RUN_DIR}/preflight")
if [[ ${STRICT_PREFLIGHT} -eq 1 ]]; then
  preflight_args+=(--strict)
fi
preflight_rc="$(run_step "01_preflight" "${preflight_args[@]}")"

ops_daily_rc="$(run_step "02_ops_daily" ./shell/payment_ops_daily.sh --date "${RECON_DATE}" --no-alert)"
idempotency_rc="$(run_step "03_idempotency" ./shell/payment_idempotency_regression.sh --window-hours "${WINDOW_HOURS}" --no-alert)"

ticketize_raw_rc="$(run_step "04_ticketize" ./shell/payment_reconcile_ticketize.sh --date "${RECON_DATE}")"
ticketize_rc="${ticketize_raw_rc}"
# 工单脚本 rc=2 代表“无待处理工单”，按通过处理
if [[ "${ticketize_raw_rc}" == "2" ]]; then
  ticketize_rc="0"
fi

if [[ -n "${STORE_MAPPING_CSV}" ]]; then
  mapping_raw_rc="$(run_step "05_store_mapping_dryrun" ./shell/payment_store_mapping_import.sh --csv "${STORE_MAPPING_CSV}" --strict-submchid-format --strict-submchid-unique --conflict-strategy block)"
  mapping_rc="${mapping_raw_rc}"
  # 映射 dry-run rc=2 代表冲突阻断，不通过；rc=0 才通过
else
  mapping_raw_rc="-"
  mapping_rc="0"
fi

retention_raw_rc="$(run_step "06_retention_dryrun" ./shell/payment_runtime_retention.sh --keep-days "${KEEP_DAYS}")"
retention_rc="${retention_raw_rc}"
# retention dry-run rc=2 仅表示存在过期候选，可视作通过（可人工确认后 apply）
if [[ "${retention_raw_rc}" == "2" ]]; then
  retention_rc="0"
fi

ready=1
if [[ "${snapshot_rc}" != "0" ]]; then ready=0; fi
if [[ "${preflight_rc}" != "0" ]]; then ready=0; fi
if [[ "${ops_daily_rc}" != "0" ]]; then ready=0; fi
if [[ "${idempotency_rc}" != "0" ]]; then ready=0; fi
if [[ "${ticketize_rc}" != "0" ]]; then ready=0; fi
if [[ "${mapping_rc}" != "0" ]]; then ready=0; fi
if [[ "${retention_rc}" != "0" ]]; then ready=0; fi

cat > "${SUMMARY_FILE}" <<TXT
run_id=${RUN_ID}
run_time=$(date '+%Y-%m-%d %H:%M:%S')
recon_date=${RECON_DATE}
window_hours=${WINDOW_HOURS}
keep_days=${KEEP_DAYS}
store_mapping_csv=${STORE_MAPPING_CSV}
skip_snapshot=${SKIP_SNAPSHOT}
snapshot_raw_rc=${snapshot_raw_rc}
snapshot_rc=${snapshot_rc}
preflight_rc=${preflight_rc}
ops_daily_rc=${ops_daily_rc}
idempotency_rc=${idempotency_rc}
ticketize_raw_rc=${ticketize_raw_rc}
ticketize_rc=${ticketize_rc}
mapping_raw_rc=${mapping_raw_rc}
mapping_rc=${mapping_rc}
retention_raw_rc=${retention_raw_rc}
retention_rc=${retention_rc}
ready_for_order_drill=${ready}
run_dir=${RUN_DIR}
TXT

cat > "${REPORT_FILE}" <<MD
# 支付切换预演报告（无订单号）

- run_id: \`${RUN_ID}\`
- run_time: \`$(date '+%Y-%m-%d %H:%M:%S')\`
- recon_date: \`${RECON_DATE}\`
- window_hours: \`${WINDOW_HOURS}\`
- keep_days: \`${KEEP_DAYS}\`
- store_mapping_csv: \`${STORE_MAPPING_CSV:-<none>}\`

| 步骤 | rc | 说明 |
|---|---:|---|
| config_snapshot | ${snapshot_raw_rc} | 配置快照（0=成功，-为跳过） |
| preflight | ${preflight_rc} | strict预检 |
| ops_daily | ${ops_daily_rc} | 日常巡检（预检+监控+对账） |
| idempotency | ${idempotency_rc} | 幂等一致性回归 |
| ticketize | ${ticketize_raw_rc} | 对账差异工单化（2=无工单） |
| store_mapping_dryrun | ${mapping_raw_rc} | 门店映射严格校验 dry-run |
| retention_dryrun | ${retention_raw_rc} | 运行产物留存 dry-run（2=有候选） |

## 结论

- ready_for_order_drill: **${ready}**
- summary: \`${SUMMARY_FILE}\`
MD

echo "[cutover-rehearsal] summary=${SUMMARY_FILE}"
echo "[cutover-rehearsal] ready_for_order_drill=${ready}"

if [[ "${ready}" != "1" ]]; then
  if [[ ${NO_ALERT} -eq 0 ]] && [[ -x "${ALERT_SCRIPT}" ]]; then
    "${ALERT_SCRIPT}" \
      --title "支付切换预演未通过" \
      --content "ready_for_order_drill=${ready}; snapshot=${snapshot_raw_rc}; preflight=${preflight_rc}; ops_daily=${ops_daily_rc}; idempotency=${idempotency_rc}; ticketize=${ticketize_raw_rc}; mapping=${mapping_raw_rc}; retention=${retention_raw_rc}; report=${REPORT_FILE}" || true
  fi
  exit 2
fi

exit 0
