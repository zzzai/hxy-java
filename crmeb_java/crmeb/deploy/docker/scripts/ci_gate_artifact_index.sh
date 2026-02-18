#!/usr/bin/env bash
set -euo pipefail

# 生成 ci-gate 统一 artifact 入口页：
# - 汇总 gate 日志文件
# - 透传 payment-ops-smoke 索引页位置
# - 补充支付运行产物 latest summary 与失败高亮

OUT_DIR="${OUT_DIR:-}"
LOG_DIR="${LOG_DIR:-}"
SMOKE_INDEX="${SMOKE_INDEX:-}"
GATE_STATUS="${GATE_STATUS:-unknown}"
PAY_RUNTIME_ROOT="${PAY_RUNTIME_ROOT:-}"
MAX_FAIL_LINES="${MAX_FAIL_LINES:-30}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CRMEB_DIR="$(cd "${SCRIPT_DIR}/../../.." && pwd)"

usage() {
  cat <<'USAGE'
用法：
  ./deploy/docker/scripts/ci_gate_artifact_index.sh
    [--out-dir PATH]
    [--log-dir PATH]
    [--smoke-index FILE]
    [--gate-status STATUS]
    [--pay-runtime-root PATH]
    [--max-fail-lines N]

参数：
  --out-dir PATH          输出目录（默认 <repo>/runtime/ci_gate_artifacts）
  --log-dir PATH          日志目录（默认 <out-dir>/logs）
  --smoke-index FILE      payment-ops-smoke 索引文件（默认 <out-dir>/smoke/payment_ops_smoke_artifact_index.md）
  --gate-status STATUS    gate job 状态（success|failure|cancelled|unknown）
  --pay-runtime-root PATH 支付 runtime 根目录（默认 <repo>/runtime）
  --max-fail-lines N      失败高亮最大行数（默认 30）
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --out-dir)
      OUT_DIR="$2"
      shift 2
      ;;
    --log-dir)
      LOG_DIR="$2"
      shift 2
      ;;
    --smoke-index)
      SMOKE_INDEX="$2"
      shift 2
      ;;
    --gate-status)
      GATE_STATUS="$2"
      shift 2
      ;;
    --pay-runtime-root)
      PAY_RUNTIME_ROOT="$2"
      shift 2
      ;;
    --max-fail-lines)
      MAX_FAIL_LINES="$2"
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

if [[ -z "${OUT_DIR}" ]]; then
  OUT_DIR="${CRMEB_DIR}/runtime/ci_gate_artifacts"
fi
if [[ -z "${LOG_DIR}" ]]; then
  LOG_DIR="${OUT_DIR}/logs"
fi
if [[ -z "${SMOKE_INDEX}" ]]; then
  SMOKE_INDEX="${OUT_DIR}/smoke/payment_ops_smoke_artifact_index.md"
fi
if [[ -z "${PAY_RUNTIME_ROOT}" ]]; then
  PAY_RUNTIME_ROOT="${CRMEB_DIR}/runtime"
fi
if ! [[ "${MAX_FAIL_LINES}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: --max-fail-lines 必须是正整数"
  exit 1
fi

mkdir -p "${OUT_DIR}" "${LOG_DIR}"

INDEX_MD="${OUT_DIR}/artifact_index.md"
SUMMARY_TXT="${OUT_DIR}/summary.txt"

file_exists() {
  [[ -f "$1" ]] && echo "yes" || echo "no"
}

file_size() {
  if [[ -f "$1" ]]; then
    stat -c %s "$1"
  else
    echo "0"
  fi
}

file_lines() {
  if [[ -f "$1" ]]; then
    wc -l < "$1" | tr -d ' '
  else
    echo "0"
  fi
}

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
  find "${base}" -maxdepth 3 -type f -name 'summary.txt' -printf '%T@ %p\n' 2>/dev/null \
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
  local severity overall gate_decision gate_result fail_count warn_count block_count
  severity="$(kv "${summary}" "severity")"
  overall="$(kv "${summary}" "overall")"
  gate_decision="$(kv "${summary}" "gate_decision")"
  gate_result="$(kv "${summary}" "gate_result")"
  fail_count="$(kv "${summary}" "fail_count")"
  warn_count="$(kv "${summary}" "warn_count")"
  block_count="$(kv "${summary}" "block_count")"

  local parts=()
  [[ -n "${severity}" ]] && parts+=("severity=${severity}")
  [[ -n "${overall}" ]] && parts+=("overall=${overall}")
  [[ -n "${gate_decision}" ]] && parts+=("gate=${gate_decision}")
  [[ -n "${gate_result}" ]] && parts+=("gate_result=${gate_result}")
  [[ -n "${fail_count}" ]] && parts+=("fail=${fail_count}")
  [[ -n "${warn_count}" ]] && parts+=("warn=${warn_count}")
  [[ -n "${block_count}" ]] && parts+=("block=${block_count}")

  if (( ${#parts[@]} == 0 )); then
    printf 'OK'
    return
  fi
  local out=""
  local item=""
  for item in "${parts[@]}"; do
    if [[ -n "${out}" ]]; then
      out="${out}, ${item}"
    else
      out="${item}"
    fi
  done
  printf '%s' "${out}"
}

declare -a PAYMENT_SUMMARIES=(
  "cutover_gate:payment_cutover_gate"
  "go_nogo:payment_go_nogo"
  "ops_status:payment_ops_status"
  "mock_replay:payment_mock_replay"
  "refund_convergence:payment_refund_convergence"
  "preflight:payment_preflight"
)

cutover_summary=""
go_nogo_summary=""
ops_status_summary=""
mock_replay_summary=""
refund_convergence_summary=""
preflight_summary=""

for row in "${PAYMENT_SUMMARIES[@]}"; do
  suite="${row%%:*}"
  rel_dir="${row##*:}"
  summary="$(latest_summary "${PAY_RUNTIME_ROOT}/${rel_dir}")"
  case "${suite}" in
    cutover_gate) cutover_summary="${summary}" ;;
    go_nogo) go_nogo_summary="${summary}" ;;
    ops_status) ops_status_summary="${summary}" ;;
    mock_replay) mock_replay_summary="${summary}" ;;
    refund_convergence) refund_convergence_summary="${summary}" ;;
    preflight) preflight_summary="${summary}" ;;
  esac
done

log_count="$(find "${LOG_DIR}" -maxdepth 1 -type f 2>/dev/null | wc -l | tr -d ' ')"
smoke_index_exists="$(file_exists "${SMOKE_INDEX}")"

{
  echo "generated_at=$(date '+%Y-%m-%d %H:%M:%S')"
  echo "gate_status=${GATE_STATUS}"
  echo "out_dir=${OUT_DIR}"
  echo "log_dir=${LOG_DIR}"
  echo "log_count=${log_count}"
  echo "smoke_index=${SMOKE_INDEX}"
  echo "smoke_index_exists=${smoke_index_exists}"
  echo "pay_runtime_root=${PAY_RUNTIME_ROOT}"
  echo "max_fail_lines=${MAX_FAIL_LINES}"
  echo "latest_cutover_summary=${cutover_summary}"
  echo "latest_go_nogo_summary=${go_nogo_summary}"
  echo "latest_ops_status_summary=${ops_status_summary}"
  echo "latest_mock_replay_summary=${mock_replay_summary}"
  echo "latest_refund_convergence_summary=${refund_convergence_summary}"
  echo "latest_preflight_summary=${preflight_summary}"
  echo "index_md=${INDEX_MD}"
} > "${SUMMARY_TXT}"

{
  echo "# CI Gate Artifact Index"
  echo
  echo "- generated_at: $(date '+%Y-%m-%d %H:%M:%S')"
  echo "- gate_status: **${GATE_STATUS}**"
  echo "- log_dir: \`${LOG_DIR}\`"
  echo "- smoke_index: \`${SMOKE_INDEX}\` (exists=${smoke_index_exists})"
  echo "- pay_runtime_root: \`${PAY_RUNTIME_ROOT}\`"
  echo
  echo "## 1) Payment Ops Smoke Entry"
  echo
  if [[ "${smoke_index_exists}" == "yes" ]]; then
    echo "- \`${SMOKE_INDEX}\`"
  else
    echo "- missing"
  fi
  echo
  echo "## 2) CI Gate Logs"
  echo
  echo "| file | exists | size_bytes | lines |"
  echo "|---|---|---:|---:|"
  for file in ci_gate.log ps.log admin.log front.log; do
    path="${LOG_DIR}/${file}"
    echo "| \`${path}\` | $(file_exists "${path}") | $(file_size "${path}") | $(file_lines "${path}") |"
  done
  while IFS= read -r extra; do
    [[ -n "${extra}" ]] || continue
    base="$(basename "${extra}")"
    case "${base}" in
      ci_gate.log|ps.log|admin.log|front.log) continue ;;
    esac
    echo "| \`${extra}\` | yes | $(file_size "${extra}") | $(file_lines "${extra}") |"
  done < <(find "${LOG_DIR}" -maxdepth 1 -type f 2>/dev/null | sort)
  echo
  echo "## 3) Payment Runtime Latest Summary"
  echo
  echo "| suite | summary | status |"
  echo "|---|---|---|"
  for row in "${PAYMENT_SUMMARIES[@]}"; do
    suite="${row%%:*}"
    rel_dir="${row##*:}"
    summary="$(latest_summary "${PAY_RUNTIME_ROOT}/${rel_dir}")"
    if [[ -n "${summary}" ]]; then
      echo "| ${suite} | \`${summary}\` | $(summary_status "${summary}") |"
    else
      echo "| ${suite} | - | MISSING |"
    fi
  done
  echo
  echo "## 4) Failure Highlights"
  echo
  has_highlight=0
  while IFS= read -r f; do
    [[ -n "${f}" ]] || continue
    highlights="$(grep -Eain 'NO_GO|FAIL|ERROR|BLOCK|EXCEPTION|Traceback|panic|denied|timed out' "${f}" 2>/dev/null | tail -n "${MAX_FAIL_LINES}" || true)"
    if [[ -z "${highlights}" ]]; then
      continue
    fi
    has_highlight=1
    echo "### $(basename "${f}")"
    echo
    echo "- file: \`${f}\`"
    echo
    echo '```text'
    echo "${highlights}"
    echo '```'
    echo
  done < <(find "${LOG_DIR}" -maxdepth 1 -type f 2>/dev/null | sort)
  if [[ "${has_highlight}" -eq 0 ]]; then
    echo "- no failure highlights in current log set"
    echo
  fi
  echo "## 5) Summary"
  echo
  echo "- \`${SUMMARY_TXT}\`"
} > "${INDEX_MD}"

echo "[ci-gate-artifact-index] index=${INDEX_MD}"
echo "[ci-gate-artifact-index] summary=${SUMMARY_TXT}"
