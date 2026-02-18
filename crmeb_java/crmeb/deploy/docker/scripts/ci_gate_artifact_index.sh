#!/usr/bin/env bash
set -euo pipefail

# 生成 ci-gate 统一 artifact 入口页：
# - 汇总 gate 日志文件
# - 透传 payment-ops-smoke 索引页位置

OUT_DIR="${OUT_DIR:-}"
LOG_DIR="${LOG_DIR:-}"
SMOKE_INDEX="${SMOKE_INDEX:-}"
GATE_STATUS="${GATE_STATUS:-unknown}"

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

参数：
  --out-dir PATH        输出目录（默认 <repo>/runtime/ci_gate_artifacts）
  --log-dir PATH        日志目录（默认 <out-dir>/logs）
  --smoke-index FILE    payment-ops-smoke 索引文件（默认 <out-dir>/smoke/payment_ops_smoke_artifact_index.md）
  --gate-status STATUS  gate job 状态（success|failure|cancelled|unknown）
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
  echo "index_md=${INDEX_MD}"
} > "${SUMMARY_TXT}"

{
  echo "# CI Gate Artifact Index"
  echo
  echo "- generated_at: $(date '+%Y-%m-%d %H:%M:%S')"
  echo "- gate_status: **${GATE_STATUS}**"
  echo "- log_dir: \`${LOG_DIR}\`"
  echo "- smoke_index: \`${SMOKE_INDEX}\` (exists=${smoke_index_exists})"
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

  # 附加展示其他日志文件
  while IFS= read -r extra; do
    [[ -n "${extra}" ]] || continue
    base="$(basename "${extra}")"
    case "${base}" in
      ci_gate.log|ps.log|admin.log|front.log) continue ;;
    esac
    echo "| \`${extra}\` | yes | $(file_size "${extra}") | $(file_lines "${extra}") |"
  done < <(find "${LOG_DIR}" -maxdepth 1 -type f 2>/dev/null | sort)

  echo
  echo "## 3) Summary"
  echo
  echo "- \`${SUMMARY_TXT}\`"
} > "${INDEX_MD}"

echo "[ci-gate-artifact-index] index=${INDEX_MD}"
echo "[ci-gate-artifact-index] summary=${SUMMARY_TXT}"

