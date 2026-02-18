#!/usr/bin/env bash
set -euo pipefail

# D9: 运行产物留存治理
# 默认 dry-run，仅在 --apply 时执行删除。

KEEP_DAYS="${KEEP_DAYS:-7}"
APPLY=0
RUNTIME_DIR="${RUNTIME_DIR:-}"
OUT_DIR="${OUT_DIR:-}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

usage() {
  cat <<'USAGE'
用法：
  ./shell/payment_runtime_retention.sh [--keep-days N] [--runtime-dir PATH] [--out-dir PATH] [--apply]

参数：
  --keep-days N      保留天数（默认 7）
  --runtime-dir PATH runtime 根目录（默认 <root>/runtime）
  --out-dir PATH     报告目录（默认 <runtime>/payment_retention）
  --apply            真正删除过期目录（默认 dry-run）

说明：
  1) 默认只预览，不删除。
  2) 仅治理目录：
     payment_reconcile / payment_drill / payment_ops_daily / payment_daily_report /
     payment_idempotency_regression / payment_cutover_rehearsal / payment_cutover_apply /
     payment_config_snapshot / payment_warroom / payment_go_nogo / payment_ops_morning_bundle /
     payment_ops_status / payment_ops_status_smoke / payment_ops_cron_smoke / payment_decision_chain_smoke /
     payment_contract_check / payment_contract_smoke / payment_decision_ticketize /
     payment_store_mapping / payment_reconcile_sla / payment_booking_verify_repair /
     payment_cron_healthcheck / payment_mock_replay
     payment_cutover_gate / payment_store_mapping_smoke /
     payment_store_mapping_cross_channel / payment_store_mapping_placeholder_cleanup
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --keep-days)
      KEEP_DAYS="$2"
      shift 2
      ;;
    --runtime-dir)
      RUNTIME_DIR="$2"
      shift 2
      ;;
    --out-dir)
      OUT_DIR="$2"
      shift 2
      ;;
    --apply)
      APPLY=1
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

if ! [[ "${KEEP_DAYS}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: --keep-days 必须是正整数"
  exit 1
fi

if [[ -z "${RUNTIME_DIR}" ]]; then
  RUNTIME_DIR="${ROOT_DIR}/runtime"
fi
if [[ -z "${OUT_DIR}" ]]; then
  OUT_DIR="${RUNTIME_DIR}/payment_retention"
fi

mkdir -p "${OUT_DIR}"
RUN_ID="$(date '+%Y%m%d%H%M%S')"
REPORT_FILE="${OUT_DIR}/retention-${RUN_ID}.log"

TARGETS=(
  "payment_reconcile"
  "payment_drill"
  "payment_ops_daily"
  "payment_daily_report"
  "payment_idempotency_regression"
  "payment_booking_verify_regression"
  "payment_booking_verify_repair"
  "payment_cutover_rehearsal"
  "payment_cutover_apply"
  "payment_config_snapshot"
  "payment_warroom"
  "payment_go_nogo"
  "payment_ops_morning_bundle"
  "payment_ops_status"
  "payment_ops_status_smoke"
  "payment_ops_cron_smoke"
  "payment_decision_chain_smoke"
  "payment_decision_ticketize"
  "payment_cron_healthcheck"
  "payment_mock_replay"
  "payment_cutover_gate"
  "payment_store_mapping_smoke"
  "payment_store_mapping_cross_channel"
  "payment_store_mapping_placeholder_cleanup"
  "payment_contract_check"
  "payment_contract_smoke"
  "payment_store_mapping"
  "payment_reconcile_sla"
)

{
  echo "run_id=${RUN_ID}"
  echo "run_time=$(date '+%Y-%m-%d %H:%M:%S')"
  echo "runtime_dir=${RUNTIME_DIR}"
  echo "keep_days=${KEEP_DAYS}"
  echo "mode=$([[ ${APPLY} -eq 1 ]] && echo apply || echo dry-run)"
  echo "targets=${TARGETS[*]}"
  echo
} > "${REPORT_FILE}"

candidate_count=0
removed_count=0
error_count=0

for target in "${TARGETS[@]}"; do
  base="${RUNTIME_DIR}/${target}"
  if [[ ! -d "${base}" ]]; then
    echo "[skip] not found: ${base}" | tee -a "${REPORT_FILE}"
    continue
  fi

  echo "[scan] ${base}" | tee -a "${REPORT_FILE}"
  while IFS= read -r dir; do
    [[ -d "${dir}" ]] || continue
    candidate_count=$((candidate_count + 1))
    size_h="$(du -sh "${dir}" 2>/dev/null | awk '{print $1}' || echo "-")"
    mtime="$(date -r "${dir}" '+%Y-%m-%d %H:%M:%S' 2>/dev/null || echo "-")"

    if [[ ${APPLY} -eq 1 ]]; then
      if rm -rf "${dir}"; then
        removed_count=$((removed_count + 1))
        echo "[delete] ${dir} | size=${size_h} | mtime=${mtime}" | tee -a "${REPORT_FILE}"
      else
        error_count=$((error_count + 1))
        echo "[error] failed to delete: ${dir}" | tee -a "${REPORT_FILE}"
      fi
    else
      echo "[candidate] ${dir} | size=${size_h} | mtime=${mtime}" | tee -a "${REPORT_FILE}"
    fi
  done < <(find "${base}" -mindepth 1 -maxdepth 1 -type d -mtime +"${KEEP_DAYS}" | sort)
done

{
  echo
  echo "candidate_count=${candidate_count}"
  echo "removed_count=${removed_count}"
  echo "error_count=${error_count}"
} >> "${REPORT_FILE}"

echo "[retention] report=${REPORT_FILE}"
echo "[retention] candidate_count=${candidate_count}, removed_count=${removed_count}, error_count=${error_count}"

if [[ ${APPLY} -eq 0 && ${candidate_count} -gt 0 ]]; then
  exit 2
fi
if [[ ${error_count} -gt 0 ]]; then
  exit 1
fi
exit 0
