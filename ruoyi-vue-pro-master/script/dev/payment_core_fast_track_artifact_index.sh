#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
用法:
  script/dev/payment_core_fast_track_artifact_index.sh --out-dir <dir> [options]

选项:
  --out-dir <dir>            产物目录（必填）
  --summary-file <file>      summary.txt（默认 <out-dir>/summary.txt）
  --run-log <file>           主运行日志（默认 <out-dir>/run.log）
  --final-gate-log <file>    最终拦截日志（默认 <out-dir>/logs/final_gate.log）
  --notify-smoke-dir <dir>   notify smoke 目录（可选）
  --surefire-dir <dir>       surefire 报告目录（默认 <out-dir>/surefire-reports）
  -h, --help                 显示帮助
EOF
}

OUT_DIR=""
SUMMARY_FILE=""
RUN_LOG=""
FINAL_GATE_LOG=""
NOTIFY_SMOKE_DIR=""
SUREFIRE_DIR=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --out-dir)
      OUT_DIR="${2:-}"
      shift 2
      ;;
    --summary-file)
      SUMMARY_FILE="${2:-}"
      shift 2
      ;;
    --run-log)
      RUN_LOG="${2:-}"
      shift 2
      ;;
    --final-gate-log)
      FINAL_GATE_LOG="${2:-}"
      shift 2
      ;;
    --notify-smoke-dir)
      NOTIFY_SMOKE_DIR="${2:-}"
      shift 2
      ;;
    --surefire-dir)
      SUREFIRE_DIR="${2:-}"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "未知参数: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

if [[ -z "${OUT_DIR}" ]]; then
  echo "--out-dir 必填" >&2
  usage >&2
  exit 1
fi

mkdir -p "${OUT_DIR}"
if [[ -z "${SUMMARY_FILE}" ]]; then
  SUMMARY_FILE="${OUT_DIR}/summary.txt"
fi
if [[ -z "${RUN_LOG}" ]]; then
  RUN_LOG="${OUT_DIR}/run.log"
fi
if [[ -z "${FINAL_GATE_LOG}" ]]; then
  FINAL_GATE_LOG="${OUT_DIR}/logs/final_gate.log"
fi
if [[ -z "${SUREFIRE_DIR}" ]]; then
  SUREFIRE_DIR="${OUT_DIR}/surefire-reports"
fi

kv() {
  local key="$1"
  if [[ ! -f "${SUMMARY_FILE}" ]]; then
    return 0
  fi
  local line
  line="$(grep -E "^${key}=" "${SUMMARY_FILE}" | tail -n 1 || true)"
  if [[ -z "${line}" ]]; then
    return 0
  fi
  printf '%s' "${line#*=}"
}

to_rel() {
  local path="$1"
  if [[ "${path}" == "${OUT_DIR}" ]]; then
    printf '.'
  elif [[ "${path}" == "${OUT_DIR}/"* ]]; then
    printf '%s' "${path#${OUT_DIR}/}"
  else
    printf '%s' "${path}"
  fi
}

file_state() {
  local path="$1"
  if [[ -f "${path}" ]]; then
    printf 'present'
  else
    printf 'missing'
  fi
}

upsert_kv() {
  local file="$1"
  local key="$2"
  local value="$3"
  local tmp
  tmp="$(mktemp)"
  if [[ -f "${file}" ]]; then
    awk -v key="${key}" -v value="${value}" -F= '
      BEGIN { done = 0 }
      $1 == key { print key "=" value; done = 1; next }
      { print }
      END { if (done == 0) print key "=" value }
    ' "${file}" > "${tmp}"
  else
    printf '%s=%s\n' "${key}" "${value}" > "${tmp}"
  fi
  mv "${tmp}" "${file}"
}

if [[ -z "${NOTIFY_SMOKE_DIR}" ]]; then
  notify_from_summary="$(kv pay_notify_smoke_artifact_dir)"
  if [[ -n "${notify_from_summary}" ]]; then
    NOTIFY_SMOKE_DIR="${notify_from_summary}"
  fi
fi

INDEX_FILE="${OUT_DIR}/artifact_index.md"
mkdir -p "${OUT_DIR}/logs"
mkdir -p "${SUREFIRE_DIR}"

surefire_count="$(find "${SUREFIRE_DIR}" -type f | wc -l | tr -d '[:space:]')"
mapfile -t surefire_samples < <(find "${SUREFIRE_DIR}" -type f | sort | head -n 20)

notify_summary=""
notify_report=""
notify_artifact_index=""
if [[ -n "${NOTIFY_SMOKE_DIR}" ]]; then
  notify_summary="${NOTIFY_SMOKE_DIR}/summary.txt"
  notify_report="${NOTIFY_SMOKE_DIR}/report.md"
  notify_artifact_index="${NOTIFY_SMOKE_DIR}/artifact/artifact_index.md"
fi

generated_at="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
pipeline_exit_code="$(kv pipeline_exit_code)"
run_notify_smoke="$(kv run_notify_smoke)"
run_resilience_regression="$(kv run_resilience_regression)"
run_clean="$(kv run_clean)"

{
  echo "# Payment Core Fast Track Artifact Index"
  echo
  echo "- generated_at: \`${generated_at}\`"
  echo "- pipeline_exit_code: \`${pipeline_exit_code:-unknown}\`"
  echo "- run_notify_smoke: \`${run_notify_smoke:-unknown}\`"
  echo "- run_resilience_regression: \`${run_resilience_regression:-unknown}\`"
  echo "- run_clean: \`${run_clean:-unknown}\`"
  echo
  echo "## Core Logs"
  echo
  echo "| File | Status |"
  echo "|---|---|"
  echo "| \`$(to_rel "${SUMMARY_FILE}")\` | $(file_state "${SUMMARY_FILE}") |"
  echo "| \`$(to_rel "${RUN_LOG}")\` | $(file_state "${RUN_LOG}") |"
  echo "| \`$(to_rel "${FINAL_GATE_LOG}")\` | $(file_state "${FINAL_GATE_LOG}") |"
  echo
  echo "## Notify Smoke"
  echo
  if [[ -n "${NOTIFY_SMOKE_DIR}" ]]; then
    echo "- smoke_dir: \`$(to_rel "${NOTIFY_SMOKE_DIR}")\`"
    echo "- summary: \`$(to_rel "${notify_summary}")\` ($(file_state "${notify_summary}"))"
    echo "- report: \`$(to_rel "${notify_report}")\` ($(file_state "${notify_report}"))"
    echo "- artifact_index: \`$(to_rel "${notify_artifact_index}")\` ($(file_state "${notify_artifact_index}"))"
  else
    echo "- skipped"
  fi
  echo
  echo "## Surefire Reports"
  echo
  echo "- total_files: \`${surefire_count}\`"
  if [[ "${#surefire_samples[@]}" -gt 0 ]]; then
    echo "- samples:"
    for file in "${surefire_samples[@]}"; do
      echo "  - \`$(to_rel "${file}")\`"
    done
  fi
} > "${INDEX_FILE}"

upsert_kv "${SUMMARY_FILE}" "artifact_index" "${INDEX_FILE}"
upsert_kv "${SUMMARY_FILE}" "final_gate_log" "${FINAL_GATE_LOG}"
upsert_kv "${SUMMARY_FILE}" "surefire_report_count" "${surefire_count}"
if [[ -n "${NOTIFY_SMOKE_DIR}" ]]; then
  upsert_kv "${SUMMARY_FILE}" "pay_notify_smoke_artifact_dir" "${NOTIFY_SMOKE_DIR}"
fi

echo "artifact_index=${INDEX_FILE}"
echo "summary_file=${SUMMARY_FILE}"
