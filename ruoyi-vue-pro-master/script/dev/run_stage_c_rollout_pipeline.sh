#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

BASE_URL="${BASE_URL:-http://127.0.0.1:48080}"
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-8}"
STAGES="${STAGES:-full-compat}"
MODE="${MODE:-verify-only}" # verify-only | switch-and-verify
SWITCH_DEPLOY_MODE="${SWITCH_DEPLOY_MODE:-verify-only}" # verify-only | docker-compose
SWITCH_APPLY="${SWITCH_APPLY:-0}"
SWITCH_RESTART_SERVER="${SWITCH_RESTART_SERVER:-0}"
SWITCH_DOCKER_ENV_FILE="${SWITCH_DOCKER_ENV_FILE:-}"
SWITCH_COMPOSE_FILE="${SWITCH_COMPOSE_FILE:-}"
SWITCH_HEALTH_URL="${SWITCH_HEALTH_URL:-}"
SWITCH_HEALTH_TIMEOUT_SECONDS="${SWITCH_HEALTH_TIMEOUT_SECONDS:-120}"
RUN_ID="${RUN_ID:-$(date +%Y%m%d_%H%M%S)_$RANDOM}"
ARTIFACT_BASE_DIR="${ARTIFACT_BASE_DIR:-${ROOT_DIR}/.tmp/stage_c_rollout_pipeline}"
VERIFY_RUOYI_BASELINE="${VERIFY_RUOYI_BASELINE:-1}"
DIAGNOSE_ENTRYPOINT="${DIAGNOSE_ENTRYPOINT:-1}"
RUN_NON_PAYMENT_BASELINE="${RUN_NON_PAYMENT_BASELINE:-1}"
RUN_NON_PAYMENT_RUNTIME_VERIFY="${RUN_NON_PAYMENT_RUNTIME_VERIFY:-1}"
NON_PAYMENT_RUN_CLEAN="${NON_PAYMENT_RUN_CLEAN:-0}"
NON_PAYMENT_EXTRA_MVN_ARGS="${NON_PAYMENT_EXTRA_MVN_ARGS:-}"

usage() {
  cat <<'EOF'
Usage:
  script/dev/run_stage_c_rollout_pipeline.sh \
    --base-url https://api.hexiaoyue.com \
    --stages full-compat,payment-core-only,disabled

Options:
  --base-url URL                    服务地址（默认: http://127.0.0.1:48080）
  --timeout-seconds N               HTTP 超时秒数（默认: 8）
  --stages CSV                      灰度阶段列表（默认: full-compat）
  --mode VALUE                      verify-only|switch-and-verify（默认: verify-only）
  --switch-deploy-mode VALUE        verify-only|docker-compose（默认: verify-only）
  --switch-apply 0|1                switch 模式下是否应用配置（默认: 0）
  --switch-restart-server 0|1       switch 模式下是否重启 server（默认: 0）
  --switch-docker-env-file PATH     switch 模式 docker env 文件（可空）
  --switch-compose-file PATH        switch 模式 compose 文件（可空）
  --switch-health-url URL           switch 模式健康检查 URL（可空）
  --switch-health-timeout-seconds N switch 模式健康检查超时（默认: 120）
  --run-id ID                       指定 run id
  --artifact-base-dir DIR           产物目录根（默认: .tmp/stage_c_rollout_pipeline）
  --verify-ruoyi-baseline 0|1       是否校验 RuoYi 基线（默认: 1）
  --diagnose-entrypoint 0|1         是否执行入口诊断（默认: 1）
  --run-non-payment-baseline 0|1    是否执行非支付二开基线（默认: 1）
  --run-non-payment-runtime-verify 0|1
                                   非支付基线是否带运行时校验（默认: 1）
  --non-payment-run-clean 0|1       非支付基线是否 clean test（默认: 0）
  --non-payment-extra-mvn-args STR  非支付基线额外 Maven 参数
  -h, --help                        显示帮助
EOF
}

trim() {
  local s="$1"
  s="${s#"${s%%[![:space:]]*}"}"
  s="${s%"${s##*[![:space:]]}"}"
  printf '%s' "${s}"
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --base-url)
      BASE_URL="${2:-}"
      shift 2
      ;;
    --timeout-seconds)
      TIMEOUT_SECONDS="${2:-8}"
      shift 2
      ;;
    --stages)
      STAGES="${2:-full-compat}"
      shift 2
      ;;
    --mode)
      MODE="${2:-verify-only}"
      shift 2
      ;;
    --switch-deploy-mode)
      SWITCH_DEPLOY_MODE="${2:-verify-only}"
      shift 2
      ;;
    --switch-apply)
      SWITCH_APPLY="${2:-0}"
      shift 2
      ;;
    --switch-restart-server)
      SWITCH_RESTART_SERVER="${2:-0}"
      shift 2
      ;;
    --switch-docker-env-file)
      SWITCH_DOCKER_ENV_FILE="${2:-}"
      shift 2
      ;;
    --switch-compose-file)
      SWITCH_COMPOSE_FILE="${2:-}"
      shift 2
      ;;
    --switch-health-url)
      SWITCH_HEALTH_URL="${2:-}"
      shift 2
      ;;
    --switch-health-timeout-seconds)
      SWITCH_HEALTH_TIMEOUT_SECONDS="${2:-120}"
      shift 2
      ;;
    --run-id)
      RUN_ID="${2:-}"
      shift 2
      ;;
    --artifact-base-dir)
      ARTIFACT_BASE_DIR="${2:-}"
      shift 2
      ;;
    --verify-ruoyi-baseline)
      VERIFY_RUOYI_BASELINE="${2:-1}"
      shift 2
      ;;
    --diagnose-entrypoint)
      DIAGNOSE_ENTRYPOINT="${2:-1}"
      shift 2
      ;;
    --run-non-payment-baseline)
      RUN_NON_PAYMENT_BASELINE="${2:-1}"
      shift 2
      ;;
    --run-non-payment-runtime-verify)
      RUN_NON_PAYMENT_RUNTIME_VERIFY="${2:-1}"
      shift 2
      ;;
    --non-payment-run-clean)
      NON_PAYMENT_RUN_CLEAN="${2:-0}"
      shift 2
      ;;
    --non-payment-extra-mvn-args)
      NON_PAYMENT_EXTRA_MVN_ARGS="${2:-}"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "[stage-c-pipeline] unknown argument: $1" >&2
      usage
      exit 1
      ;;
  esac
done

case "${MODE}" in
  verify-only|switch-and-verify)
    ;;
  *)
    echo "[stage-c-pipeline] invalid mode: ${MODE}" >&2
    exit 1
    ;;
esac

case "${SWITCH_DEPLOY_MODE}" in
  verify-only|docker-compose)
    ;;
  *)
    echo "[stage-c-pipeline] invalid switch deploy mode: ${SWITCH_DEPLOY_MODE}" >&2
    exit 1
    ;;
esac

RUN_ID="${RUN_ID//[^a-zA-Z0-9._-]/_}"
OUT_DIR="${ARTIFACT_BASE_DIR}/${RUN_ID}"
LOG_DIR="${OUT_DIR}/logs"
mkdir -p "${LOG_DIR}"
SUMMARY_FILE="${OUT_DIR}/summary.txt"
INDEX_FILE="${OUT_DIR}/artifact_index.md"
RUN_LOG="${LOG_DIR}/run.log"
STAGE_RESULT_FILE="${OUT_DIR}/gray_stage_results.tsv"

exec > >(tee -a "${RUN_LOG}") 2>&1

echo "[stage-c-pipeline] run_id=${RUN_ID}"
echo "[stage-c-pipeline] base_url=${BASE_URL}"
echo "[stage-c-pipeline] timeout_seconds=${TIMEOUT_SECONDS}"
echo "[stage-c-pipeline] stages=${STAGES}"
echo "[stage-c-pipeline] mode=${MODE}"
echo "[stage-c-pipeline] switch_deploy_mode=${SWITCH_DEPLOY_MODE}"
echo "[stage-c-pipeline] switch_apply=${SWITCH_APPLY}"
echo "[stage-c-pipeline] switch_restart_server=${SWITCH_RESTART_SERVER}"
echo "[stage-c-pipeline] verify_ruoyi_baseline=${VERIFY_RUOYI_BASELINE}"
echo "[stage-c-pipeline] diagnose_entrypoint=${DIAGNOSE_ENTRYPOINT}"
echo "[stage-c-pipeline] run_non_payment_baseline=${RUN_NON_PAYMENT_BASELINE}"
echo "[stage-c-pipeline] run_non_payment_runtime_verify=${RUN_NON_PAYMENT_RUNTIME_VERIFY}"

echo -e "stage\trc\tsummary_file" > "${STAGE_RESULT_FILE}"

overall_rc=0
last_stage=""

IFS=',' read -r -a stage_items <<< "${STAGES}"
for raw_stage in "${stage_items[@]}"; do
  stage="$(trim "${raw_stage}")"
  [[ -z "${stage}" ]] && continue
  case "${stage}" in
    full-compat|payment-core-only|disabled)
      ;;
    *)
      echo "[stage-c-pipeline] invalid stage in --stages: ${stage}" >&2
      exit 1
      ;;
  esac
  last_stage="${stage}"

  gray_run_id="gray_${stage}_${RUN_ID}"
  stage_summary=""
  rc=0
  if [[ "${MODE}" == "verify-only" ]]; then
    stage_summary="${OUT_DIR}/gray/${gray_run_id}/summary.txt"
    echo "[stage-c-pipeline] step=gray-gate stage=${stage}"
    set +e
    RUN_ID="${gray_run_id}" \
    ARTIFACT_BASE_DIR="${OUT_DIR}/gray" \
    STAGE="${stage}" \
    BASE_URL="${BASE_URL}" \
    TIMEOUT_SECONDS="${TIMEOUT_SECONDS}" \
    VERIFY_RUOYI_BASELINE="${VERIFY_RUOYI_BASELINE}" \
    DIAGNOSE_ENTRYPOINT="${DIAGNOSE_ENTRYPOINT}" \
    bash script/dev/run_crmeb_gray_stage_gate.sh
    rc=$?
    set -e
  else
    switch_run_id="switch_${stage}_${RUN_ID}"
    stage_summary="${OUT_DIR}/switch/${switch_run_id}/summary.txt"
    echo "[stage-c-pipeline] step=stage-switch stage=${stage}"
    switch_cmd=(
      bash script/dev/switch_crmeb_stage.sh
      --stage "${stage}"
      --base-url "${BASE_URL}"
      --timeout-seconds "${TIMEOUT_SECONDS}"
      --deploy-mode "${SWITCH_DEPLOY_MODE}"
      --verify-ruoyi-baseline "${VERIFY_RUOYI_BASELINE}"
      --run-id "${switch_run_id}"
      --artifact-base-dir "${OUT_DIR}/switch"
      --health-timeout-seconds "${SWITCH_HEALTH_TIMEOUT_SECONDS}"
    )
    if [[ -n "${SWITCH_DOCKER_ENV_FILE}" ]]; then
      switch_cmd+=(--docker-env-file "${SWITCH_DOCKER_ENV_FILE}")
    fi
    if [[ -n "${SWITCH_COMPOSE_FILE}" ]]; then
      switch_cmd+=(--compose-file "${SWITCH_COMPOSE_FILE}")
    fi
    if [[ -n "${SWITCH_HEALTH_URL}" ]]; then
      switch_cmd+=(--health-url "${SWITCH_HEALTH_URL}")
    fi
    if [[ "${SWITCH_APPLY}" == "1" ]]; then
      switch_cmd+=(--apply)
    fi
    if [[ "${SWITCH_RESTART_SERVER}" == "1" ]]; then
      switch_cmd+=(--restart-server)
    fi
    set +e
    "${switch_cmd[@]}"
    rc=$?
    set -e
  fi
  echo -e "${stage}\t${rc}\t${stage_summary}" >> "${STAGE_RESULT_FILE}"
  if [[ "${rc}" -ne 0 && "${overall_rc}" -eq 0 ]]; then
    overall_rc="${rc}"
  fi
done

non_payment_rc=0
non_payment_summary=""
if [[ "${RUN_NON_PAYMENT_BASELINE}" == "1" ]]; then
  if [[ -z "${last_stage}" ]]; then
    last_stage="full-compat"
  fi
  non_payment_run_id="non_payment_${RUN_ID}"
  non_payment_summary="${OUT_DIR}/non-payment/${non_payment_run_id}/summary.txt"
  echo "[stage-c-pipeline] step=non-payment-baseline verify_stage=${last_stage}"
  set +e
  RUN_ID="${non_payment_run_id}" \
  ARTIFACT_BASE_DIR="${OUT_DIR}/non-payment" \
  RUN_CLEAN="${NON_PAYMENT_RUN_CLEAN}" \
  BYTE_BUDDY_EXPERIMENTAL=1 \
  EXTRA_MVN_ARGS_RAW="${NON_PAYMENT_EXTRA_MVN_ARGS}" \
  VERIFY_RUNTIME="${RUN_NON_PAYMENT_RUNTIME_VERIFY}" \
  VERIFY_STAGE="${last_stage}" \
  VERIFY_BASE_URL="${BASE_URL}" \
  VERIFY_TIMEOUT_SECONDS="${TIMEOUT_SECONDS}" \
  bash script/dev/run_non_payment_secondary_baseline.sh
  non_payment_rc=$?
  set -e
  if [[ "${non_payment_rc}" -ne 0 && "${overall_rc}" -eq 0 ]]; then
    overall_rc="${non_payment_rc}"
  fi
else
  echo "[stage-c-pipeline] skip non-payment baseline"
fi

{
  echo "run_id=${RUN_ID}"
  echo "base_url=${BASE_URL}"
  echo "timeout_seconds=${TIMEOUT_SECONDS}"
  echo "stages=${STAGES}"
  echo "mode=${MODE}"
  echo "switch_deploy_mode=${SWITCH_DEPLOY_MODE}"
  echo "switch_apply=${SWITCH_APPLY}"
  echo "switch_restart_server=${SWITCH_RESTART_SERVER}"
  echo "switch_docker_env_file=${SWITCH_DOCKER_ENV_FILE}"
  echo "switch_compose_file=${SWITCH_COMPOSE_FILE}"
  echo "switch_health_url=${SWITCH_HEALTH_URL}"
  echo "switch_health_timeout_seconds=${SWITCH_HEALTH_TIMEOUT_SECONDS}"
  echo "verify_ruoyi_baseline=${VERIFY_RUOYI_BASELINE}"
  echo "diagnose_entrypoint=${DIAGNOSE_ENTRYPOINT}"
  echo "run_non_payment_baseline=${RUN_NON_PAYMENT_BASELINE}"
  echo "run_non_payment_runtime_verify=${RUN_NON_PAYMENT_RUNTIME_VERIFY}"
  echo "non_payment_run_clean=${NON_PAYMENT_RUN_CLEAN}"
  echo "non_payment_rc=${non_payment_rc}"
  echo "non_payment_summary=${non_payment_summary}"
  echo "overall_rc=${overall_rc}"
  echo "stage_result_file=${STAGE_RESULT_FILE}"
  echo "run_log=${RUN_LOG}"
} > "${SUMMARY_FILE}"

{
  echo "# Stage C Rollout Pipeline"
  echo
  echo "- run_id: \`${RUN_ID}\`"
  echo "- base_url: \`${BASE_URL}\`"
  echo "- stages: \`${STAGES}\`"
  echo "- mode: \`${MODE}\`"
  echo "- overall_rc: \`${overall_rc}\`"
  echo
  echo "## Files"
  echo
  echo "- summary: \`summary.txt\`"
  echo "- stage results: \`gray_stage_results.tsv\`"
  echo "- run log: \`logs/run.log\`"
  if [[ "${MODE}" == "verify-only" ]]; then
    echo "- gray artifacts: \`gray/\`"
  else
    echo "- switch artifacts: \`switch/\`"
  fi
  echo "- non-payment artifacts: \`non-payment/\`"
} > "${INDEX_FILE}"

if [[ "${overall_rc}" -ne 0 ]]; then
  echo "[stage-c-pipeline] result=FAIL rc=${overall_rc}" >&2
  exit "${overall_rc}"
fi

echo "[stage-c-pipeline] result=PASS"
echo "[stage-c-pipeline] summary=${SUMMARY_FILE}"
