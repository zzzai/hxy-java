#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"
# shellcheck source=script/dev/lib/http_code.sh
source "${ROOT_DIR}/script/dev/lib/http_code.sh"

STAGE="full-compat"
BASE_URL=""
TIMEOUT_SECONDS=8
DEPLOY_MODE="verify-only" # verify-only | docker-compose
APPLY=0
RESTART_SERVER=0
DOCKER_ENV_FILE="${ROOT_DIR}/script/docker/docker.env"
COMPOSE_FILE="${ROOT_DIR}/script/docker/docker-compose.yml"
HEALTH_URL=""
HEALTH_TIMEOUT_SECONDS=120
VERIFY_RUOYI_BASELINE=1
RUN_ID="${RUN_ID:-$(date +%Y%m%d_%H%M%S)_$RANDOM}"
ARTIFACT_BASE_DIR="${ROOT_DIR}/.tmp/crmeb_stage_switch"

usage() {
  cat <<'EOF'
Usage:
  script/dev/switch_crmeb_stage.sh \
    --stage payment-core-only \
    --base-url https://api.hexiaoyue.com \
    --deploy-mode docker-compose \
    --apply \
    --restart-server

Options:
  --stage VALUE                full-compat | payment-core-only | disabled
  --base-url URL               对外服务地址（用于阶段门禁校验）
  --timeout-seconds N          阶段门禁 HTTP 超时（默认: 8）
  --deploy-mode VALUE          verify-only | docker-compose（默认: verify-only）
  --apply                      应用配置变更（默认仅 dry-run）
  --restart-server             在 docker-compose 模式下重启 server 容器
  --docker-env-file PATH       docker env 文件（默认: script/docker/docker.env）
  --compose-file PATH          docker compose 文件（默认: script/docker/docker-compose.yml）
  --health-url URL             健康检查地址（默认: <base-url>/actuator/health）
  --health-timeout-seconds N   健康检查最大等待秒数（默认: 120）
  --verify-ruoyi-baseline 0|1  阶段门禁前是否校验 RuoYi 运行基线（默认: 1）
  --run-id ID                  指定 run id
  --artifact-base-dir DIR      产物目录（默认: .tmp/crmeb_stage_switch）
  -h, --help                   显示帮助
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --stage)
      STAGE="${2:-}"
      shift 2
      ;;
    --base-url)
      BASE_URL="${2:-}"
      shift 2
      ;;
    --timeout-seconds)
      TIMEOUT_SECONDS="${2:-8}"
      shift 2
      ;;
    --deploy-mode)
      DEPLOY_MODE="${2:-verify-only}"
      shift 2
      ;;
    --apply)
      APPLY=1
      shift
      ;;
    --restart-server)
      RESTART_SERVER=1
      shift
      ;;
    --docker-env-file)
      DOCKER_ENV_FILE="${2:-}"
      shift 2
      ;;
    --compose-file)
      COMPOSE_FILE="${2:-}"
      shift 2
      ;;
    --health-url)
      HEALTH_URL="${2:-}"
      shift 2
      ;;
    --health-timeout-seconds)
      HEALTH_TIMEOUT_SECONDS="${2:-120}"
      shift 2
      ;;
    --verify-ruoyi-baseline)
      VERIFY_RUOYI_BASELINE="${2:-1}"
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
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "[stage-switch] unknown argument: $1" >&2
      usage
      exit 1
      ;;
  esac
done

case "${STAGE}" in
  full-compat)
    STAGE_PROFILE="local,crmeb-gray"
    ;;
  payment-core-only)
    STAGE_PROFILE="local,crmeb-fast-track"
    ;;
  disabled)
    STAGE_PROFILE="local,crmeb-disabled"
    ;;
  *)
    echo "[stage-switch] invalid --stage: ${STAGE}" >&2
    usage
    exit 1
    ;;
esac

case "${DEPLOY_MODE}" in
  verify-only|docker-compose)
    ;;
  *)
    echo "[stage-switch] invalid --deploy-mode: ${DEPLOY_MODE}" >&2
    usage
    exit 1
    ;;
esac

RUN_ID="${RUN_ID//[^a-zA-Z0-9._-]/_}"
OUT_DIR="${ARTIFACT_BASE_DIR}/${RUN_ID}"
LOG_DIR="${OUT_DIR}/logs"
mkdir -p "${LOG_DIR}"
LOG_FILE="${LOG_DIR}/switch.log"
SUMMARY_FILE="${OUT_DIR}/summary.txt"
INDEX_FILE="${OUT_DIR}/artifact_index.md"
BACKUP_ENV_FILE="${OUT_DIR}/docker.env.bak"

exec > >(tee -a "${LOG_FILE}") 2>&1

echo "[stage-switch] run_id=${RUN_ID}"
echo "[stage-switch] stage=${STAGE}"
echo "[stage-switch] stage_profile=${STAGE_PROFILE}"
echo "[stage-switch] deploy_mode=${DEPLOY_MODE}"
echo "[stage-switch] apply=${APPLY}"
echo "[stage-switch] restart_server=${RESTART_SERVER}"
echo "[stage-switch] verify_ruoyi_baseline=${VERIFY_RUOYI_BASELINE}"
echo "[stage-switch] base_url=${BASE_URL:-<none>}"

if [[ -z "${HEALTH_URL}" && -n "${BASE_URL}" ]]; then
  HEALTH_URL="${BASE_URL%/}/actuator/health"
fi
echo "[stage-switch] health_url=${HEALTH_URL:-<none>}"

upsert_env_kv() {
  local file="$1"
  local key="$2"
  local value="$3"
  local tmp
  tmp="$(mktemp)"
  awk -F= -v k="$key" -v v="$value" '
    BEGIN { updated=0 }
    $1 == k {
      if (updated == 0) {
        print k "=" v
        updated=1
      }
      next
    }
    { print $0 }
    END {
      if (updated == 0) {
        print k "=" v
      }
    }
  ' "$file" > "$tmp"
  mv "$tmp" "$file"
}

is_existing_route_code() {
  local code="$1"
  if [[ "${code}" == "000" || "${code}" == "404" || "${code}" =~ ^5 ]]; then
    return 1
  fi
  return 0
}

deploy_rc=0
health_rc=0
gate_rc=0
gate_summary_file=""

if [[ "${DEPLOY_MODE}" == "docker-compose" ]]; then
  if [[ ! -f "${DOCKER_ENV_FILE}" ]]; then
    echo "[stage-switch] missing docker env file: ${DOCKER_ENV_FILE}" >&2
    exit 1
  fi
  if [[ ! -f "${COMPOSE_FILE}" ]]; then
    echo "[stage-switch] missing compose file: ${COMPOSE_FILE}" >&2
    exit 1
  fi

  if [[ "${APPLY}" -eq 1 ]]; then
    cp "${DOCKER_ENV_FILE}" "${BACKUP_ENV_FILE}"
    upsert_env_kv "${DOCKER_ENV_FILE}" "CRMEB_COMPAT_STAGE" "${STAGE}"
    upsert_env_kv "${DOCKER_ENV_FILE}" "SPRING_PROFILES_ACTIVE" "${STAGE_PROFILE}"
    echo "[stage-switch] docker env updated: ${DOCKER_ENV_FILE}"
    echo "[stage-switch] backup: ${BACKUP_ENV_FILE}"
  else
    echo "[stage-switch] dry-run: would update ${DOCKER_ENV_FILE}"
    echo "[stage-switch] dry-run value: CRMEB_COMPAT_STAGE=${STAGE}"
    echo "[stage-switch] dry-run value: SPRING_PROFILES_ACTIVE=${STAGE_PROFILE}"
  fi

  if [[ "${APPLY}" -eq 1 && "${RESTART_SERVER}" -eq 1 ]]; then
    jar_file="${ROOT_DIR}/yudao-server/target/yudao-server.jar"
    has_local_jar=0
    has_server_image=0
    if [[ -f "${jar_file}" ]]; then
      has_local_jar=1
    fi
    if docker image inspect yudao-server:latest >/dev/null 2>&1; then
      has_server_image=1
    fi
    if [[ "${has_local_jar}" -eq 0 && "${has_server_image}" -eq 0 ]]; then
      deploy_rc=3
      echo "[stage-switch] missing build prerequisite: ${jar_file}" >&2
      echo "[stage-switch] run: mvn -pl yudao-server -am -DskipTests package" >&2
      echo "[stage-switch] or prepare image: yudao-server:latest" >&2
    else
      set +e
      docker compose --env-file "${DOCKER_ENV_FILE}" -f "${COMPOSE_FILE}" up -d --force-recreate server
      deploy_rc=$?
      set -e
    fi
    if [[ "${deploy_rc}" -ne 0 ]]; then
      echo "[stage-switch] docker compose restart failed: rc=${deploy_rc}" >&2
    else
      echo "[stage-switch] docker compose restart done"
    fi

    if [[ "${deploy_rc}" -eq 0 && -n "${HEALTH_URL}" ]]; then
      set +e
      ok=0
      for ((i = 0; i < HEALTH_TIMEOUT_SECONDS; i++)); do
        code="$(curl_http_code "5" "${HEALTH_URL}")"
        if [[ "${code}" == "200" ]]; then
          ok=1
          break
        fi
        # 兼容 actuator 未暴露场景：health_url 为 /actuator/health 且返回 404 时，降级检查 admin-api 路由存在性
        if [[ "${code}" == "404" && "${HEALTH_URL}" == */actuator/health ]]; then
          fallback_url="${BASE_URL%/}/admin-api/system/auth/get-permission-info"
          fallback_code="$(curl_http_code "5" "${fallback_url}")"
          if is_existing_route_code "${fallback_code}"; then
            ok=1
            echo "[stage-switch] actuator missing, fallback health passed: ${fallback_url} -> ${fallback_code}"
            break
          fi
        fi
        sleep 1
      done
      set -e
      if [[ "${ok}" -eq 1 ]]; then
        echo "[stage-switch] health check passed: ${HEALTH_URL}"
      else
        health_rc=2
        echo "[stage-switch] health check failed: ${HEALTH_URL}" >&2
      fi
    fi
  fi
fi

if [[ -n "${BASE_URL}" ]]; then
  set +e
  GATE_RUN_ID="gate_${RUN_ID}"
  RUN_ID="${GATE_RUN_ID}" \
  ARTIFACT_BASE_DIR="${OUT_DIR}/gate" \
  STAGE="${STAGE}" \
  BASE_URL="${BASE_URL}" \
  TIMEOUT_SECONDS="${TIMEOUT_SECONDS}" \
  VERIFY_RUOYI_BASELINE="${VERIFY_RUOYI_BASELINE}" \
  bash script/dev/run_crmeb_gray_stage_gate.sh
  gate_rc=$?
  set -e
  gate_summary_file="${OUT_DIR}/gate/${GATE_RUN_ID}/summary.txt"
fi

overall_rc=0
if [[ "${deploy_rc}" -ne 0 ]]; then
  overall_rc="${deploy_rc}"
elif [[ "${health_rc}" -ne 0 ]]; then
  overall_rc="${health_rc}"
elif [[ "${gate_rc}" -ne 0 ]]; then
  overall_rc="${gate_rc}"
fi

{
  echo "run_id=${RUN_ID}"
  echo "stage=${STAGE}"
  echo "stage_profile=${STAGE_PROFILE}"
  echo "deploy_mode=${DEPLOY_MODE}"
  echo "apply=${APPLY}"
  echo "restart_server=${RESTART_SERVER}"
  echo "docker_env_file=${DOCKER_ENV_FILE}"
  echo "compose_file=${COMPOSE_FILE}"
  echo "base_url=${BASE_URL}"
  echo "verify_ruoyi_baseline=${VERIFY_RUOYI_BASELINE}"
  echo "health_url=${HEALTH_URL}"
  echo "deploy_rc=${deploy_rc}"
  echo "health_rc=${health_rc}"
  echo "gate_rc=${gate_rc}"
  echo "overall_rc=${overall_rc}"
  echo "log_file=${LOG_FILE}"
  echo "gate_summary_file=${gate_summary_file}"
  echo "backup_env_file=${BACKUP_ENV_FILE}"
} > "${SUMMARY_FILE}"

{
  echo "# CRMEB Stage Switch"
  echo
  echo "- run_id: \`${RUN_ID}\`"
  echo "- stage: \`${STAGE}\`"
  echo "- stage_profile: \`${STAGE_PROFILE}\`"
  echo "- deploy_mode: \`${DEPLOY_MODE}\`"
  echo "- apply: \`${APPLY}\`"
  echo "- restart_server: \`${RESTART_SERVER}\`"
  echo "- overall_rc: \`${overall_rc}\`"
  echo
  echo "## Files"
  echo
  echo "- summary: \`summary.txt\`"
  echo "- log: \`logs/switch.log\`"
  if [[ -n "${gate_summary_file}" ]]; then
    echo "- gate summary: \`${gate_summary_file}\`"
  fi
  if [[ -f "${BACKUP_ENV_FILE}" ]]; then
    echo "- docker env backup: \`${BACKUP_ENV_FILE}\`"
  fi
} > "${INDEX_FILE}"

if [[ "${overall_rc}" -ne 0 ]]; then
  echo "[stage-switch] result=FAIL rc=${overall_rc}" >&2
  exit "${overall_rc}"
fi

echo "[stage-switch] result=PASS"
echo "[stage-switch] summary=${SUMMARY_FILE}"
