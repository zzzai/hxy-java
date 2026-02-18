#!/usr/bin/env bash
set -euo pipefail

# D46: 支付离线 smoke 产物索引
# 目标：把各 smoke 脚本最新 summary/report 聚合成单页索引，便于 CI 下载后快速审计。

RUNTIME_ROOT="${RUNTIME_ROOT:-}"
OUT_FILE="${OUT_FILE:-}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

usage() {
  cat <<'USAGE'
用法：
  ./shell/payment_smoke_artifact_index.sh [--runtime-root PATH] [--out FILE]

参数：
  --runtime-root PATH  runtime 根目录（默认 <repo>/runtime）
  --out FILE           输出 markdown 文件（默认 <runtime>/payment_ops_smoke_artifact_index.md）

退出码：
  0  生成成功
  1  脚本执行失败
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --runtime-root)
      RUNTIME_ROOT="$2"
      shift 2
      ;;
    --out)
      OUT_FILE="$2"
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

if [[ -z "${RUNTIME_ROOT}" ]]; then
  RUNTIME_ROOT="${ROOT_DIR}/runtime"
fi
if [[ -z "${OUT_FILE}" ]]; then
  OUT_FILE="${RUNTIME_ROOT}/payment_ops_smoke_artifact_index.md"
fi

mkdir -p "$(dirname "${OUT_FILE}")"

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

latest_run_dir() {
  local base="$1"
  if [[ ! -d "${base}" ]]; then
    printf ''
    return
  fi
  find "${base}" -maxdepth 1 -mindepth 1 -type d -name 'run-*' -printf '%T@ %p\n' 2>/dev/null \
    | sort -n \
    | tail -n 1 \
    | cut -d' ' -f2- || true
}

summary_status() {
  local summary="$1"
  if [[ -z "${summary}" || ! -f "${summary}" ]]; then
    printf 'MISSING'
    return
  fi
  local severity overall gate_decision fail_count warn_count block_count
  severity="$(kv "${summary}" "severity")"
  overall="$(kv "${summary}" "overall")"
  gate_decision="$(kv "${summary}" "gate_decision")"
  fail_count="$(kv "${summary}" "fail_count")"
  warn_count="$(kv "${summary}" "warn_count")"
  block_count="$(kv "${summary}" "block_count")"

  local status_parts=()
  if [[ -n "${severity}" ]]; then
    status_parts+=("severity=${severity}")
  fi
  if [[ -n "${overall}" ]]; then
    status_parts+=("overall=${overall}")
  fi
  if [[ -n "${gate_decision}" ]]; then
    status_parts+=("gate=${gate_decision}")
  fi
  if [[ -n "${fail_count}" ]]; then
    status_parts+=("fail=${fail_count}")
  fi
  if [[ -n "${warn_count}" ]]; then
    status_parts+=("warn=${warn_count}")
  fi
  if [[ -n "${block_count}" ]]; then
    status_parts+=("block=${block_count}")
  fi

  if (( ${#status_parts[@]} == 0 )); then
    printf 'OK'
  else
    local out=""
    local item=""
    for item in "${status_parts[@]}"; do
      if [[ -n "${out}" ]]; then
        out="${out}, ${item}"
      else
        out="${item}"
      fi
    done
    printf '%s' "${out}"
  fi
}

declare -a SUITES=(
  "contract_smoke:payment_contract_smoke"
  "ops_status_smoke:payment_ops_status_smoke"
  "decision_chain_smoke:payment_decision_chain_smoke"
  "cutover_gate_smoke:payment_cutover_gate_smoke"
  "ops_cron_smoke:payment_ops_cron_smoke"
  "mock_replay:payment_mock_replay"
)

{
  echo "# Payment Ops Smoke Artifact Index"
  echo
  echo "- generated_at: $(date '+%Y-%m-%d %H:%M:%S')"
  echo "- runtime_root: \`${RUNTIME_ROOT}\`"
  echo
  echo "| suite | latest_run_dir | summary | report | status |"
  echo "|---|---|---|---|---|"

  row=""
  for row in "${SUITES[@]}"; do
    suite="${row%%:*}"
    rel_dir="${row##*:}"
    base="${RUNTIME_ROOT}/${rel_dir}"
    latest_run="$(latest_run_dir "${base}")"
    summary="${latest_run}/summary.txt"
    report="${latest_run}/report.md"

    run_disp="-"
    summary_disp="-"
    report_disp="-"
    status_disp="MISSING"

    if [[ -n "${latest_run}" && -d "${latest_run}" ]]; then
      run_disp="\`${latest_run}\`"
      if [[ -f "${summary}" ]]; then
        summary_disp="\`${summary}\`"
      fi
      if [[ -f "${report}" ]]; then
        report_disp="\`${report}\`"
      fi
      status_disp="$(summary_status "${summary}")"
    fi

    echo "| ${suite} | ${run_disp} | ${summary_disp} | ${report_disp} | ${status_disp} |"
  done
} > "${OUT_FILE}"

echo "[smoke-artifact-index] out=${OUT_FILE}"

