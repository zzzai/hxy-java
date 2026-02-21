#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"
# shellcheck source=script/dev/lib/http_code.sh
source "${ROOT_DIR}/script/dev/lib/http_code.sh"

BASE_URL="${BASE_URL:-http://127.0.0.1:48080}"
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-8}"
RUN_ID="${RUN_ID:-$(date +%Y%m%d_%H%M%S)_$RANDOM}"
ARTIFACT_BASE_DIR="${ARTIFACT_BASE_DIR:-${ROOT_DIR}/.tmp/ruoyi_entrypoint_diagnosis}"
REQUIRE_RUOYI="${REQUIRE_RUOYI:-1}"
REQUIRE_HEALTH="${REQUIRE_HEALTH:-1}"
PREVIEW_MAX="${PREVIEW_MAX:-200}"

usage() {
  cat <<'EOF'
Usage:
  script/dev/diagnose_ruoyi_entrypoint.sh \
    --base-url https://api.hexiaoyue.com \
    --timeout-seconds 8

Options:
  --base-url URL               服务地址（默认: http://127.0.0.1:48080）
  --timeout-seconds N          HTTP 超时秒数（默认: 8）
  --run-id ID                  指定 run id
  --artifact-base-dir DIR      产物目录根（默认: .tmp/ruoyi_entrypoint_diagnosis）
  --require-ruoyi 0|1          是否要求 /admin-api + /app-api 路由可用（默认: 1）
  --require-health 0|1         是否要求 /actuator/health=200（默认: 1）
  --preview-max N              body 预览最大字符数（默认: 200）
  -h, --help                   显示帮助
EOF
}

trim() {
  local s="$1"
  s="${s#"${s%%[![:space:]]*}"}"
  s="${s%"${s##*[![:space:]]}"}"
  printf '%s' "${s}"
}

sanitize_preview() {
  local input="$1"
  input="$(echo "${input}" | tr '\r\n\t' '   ' | tr -s ' ')"
  input="${input//|/ }"
  printf '%s' "${input}"
}

is_non_404_route() {
  local code="$1"
  if [[ "${code}" == "000" || "${code}" == "404" || "${code}" =~ ^5 ]]; then
    return 1
  fi
  return 0
}

check_path() {
  local label="$1"
  local path="$2"
  local out_code_var="$3"
  local out_type_var="$4"
  local out_preview_var="$5"
  local url="${BASE_URL%/}${path}"
  local tmp_dir
  local header_file
  local body_file
  local code
  local raw_code
  local curl_rc
  local content_type
  local body_preview

  tmp_dir="$(mktemp -d)"
  header_file="${tmp_dir}/headers.txt"
  body_file="${tmp_dir}/body.txt"
  set +e
  raw_code="$(curl -sS -m "${TIMEOUT_SECONDS}" -D "${header_file}" -o "${body_file}" -w '%{http_code}' "${url}")"
  curl_rc=$?
  set -e
  if [[ "${curl_rc}" -ne 0 ]]; then
    raw_code="${raw_code}000"
  fi
  code="$(normalize_http_code "${raw_code}")"
  content_type="$(grep -i '^content-type:' "${header_file}" | head -n 1 | cut -d':' -f2- || true)"
  content_type="$(trim "${content_type}")"
  body_preview="$(head -c "${PREVIEW_MAX}" "${body_file}" 2>/dev/null || true)"
  body_preview="$(sanitize_preview "${body_preview}")"

  printf -v "${out_code_var}" '%s' "${code}"
  printf -v "${out_type_var}" '%s' "${content_type}"
  printf -v "${out_preview_var}" '%s' "${body_preview}"

  rm -rf "${tmp_dir}"
  echo "[entrypoint-diagnose] ${label} ${path} -> ${code}"
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
    --run-id)
      RUN_ID="${2:-}"
      shift 2
      ;;
    --artifact-base-dir)
      ARTIFACT_BASE_DIR="${2:-}"
      shift 2
      ;;
    --require-ruoyi)
      REQUIRE_RUOYI="${2:-1}"
      shift 2
      ;;
    --require-health)
      REQUIRE_HEALTH="${2:-1}"
      shift 2
      ;;
    --preview-max)
      PREVIEW_MAX="${2:-200}"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "[entrypoint-diagnose] unknown argument: $1" >&2
      usage
      exit 1
      ;;
  esac
done

RUN_ID="${RUN_ID//[^a-zA-Z0-9._-]/_}"
OUT_DIR="${ARTIFACT_BASE_DIR}/${RUN_ID}"
LOG_DIR="${OUT_DIR}/logs"
mkdir -p "${LOG_DIR}"
SUMMARY_FILE="${OUT_DIR}/summary.txt"
MATRIX_FILE="${OUT_DIR}/matrix.tsv"
INDEX_FILE="${OUT_DIR}/artifact_index.md"

echo "[entrypoint-diagnose] run_id=${RUN_ID}"
echo "[entrypoint-diagnose] base_url=${BASE_URL}"
echo "[entrypoint-diagnose] timeout_seconds=${TIMEOUT_SECONDS}"
echo "[entrypoint-diagnose] out_dir=${OUT_DIR}"

health_code=""
health_type=""
health_preview=""
check_path "health" "/actuator/health" health_code health_type health_preview

admin_code=""
admin_type=""
admin_preview=""
check_path "admin-api-auth" "/admin-api/system/auth/get-permission-info" admin_code admin_type admin_preview

app_code=""
app_type=""
app_preview=""
check_path "app-api-member" "/app-api/member/user/get" app_code app_type app_preview

app_pay_code=""
app_pay_type=""
app_pay_preview=""
check_path "app-api-pay" "/app-api/pay/order/get?id=1" app_pay_code app_pay_type app_pay_preview

compat_pay_code=""
compat_pay_type=""
compat_pay_preview=""
check_path "compat-front-pay" "/api/front/pay/get/config" compat_pay_code compat_pay_type compat_pay_preview

compat_order_code=""
compat_order_type=""
compat_order_preview=""
check_path "compat-front-order-list" "/api/front/order/list" compat_order_code compat_order_type compat_order_preview

compat_admin_code=""
compat_admin_type=""
compat_admin_preview=""
check_path "compat-admin-order-list" "/api/admin/store/order/list" compat_admin_code compat_admin_type compat_admin_preview

{
  echo -e "label\tpath\thttp_code\tcontent_type\tbody_preview"
  echo -e "health\t/actuator/health\t${health_code}\t${health_type}\t${health_preview}"
  echo -e "admin-api-auth\t/admin-api/system/auth/get-permission-info\t${admin_code}\t${admin_type}\t${admin_preview}"
  echo -e "app-api-member\t/app-api/member/user/get\t${app_code}\t${app_type}\t${app_preview}"
  echo -e "app-api-pay\t/app-api/pay/order/get?id=1\t${app_pay_code}\t${app_pay_type}\t${app_pay_preview}"
  echo -e "compat-front-pay\t/api/front/pay/get/config\t${compat_pay_code}\t${compat_pay_type}\t${compat_pay_preview}"
  echo -e "compat-front-order-list\t/api/front/order/list\t${compat_order_code}\t${compat_order_type}\t${compat_order_preview}"
  echo -e "compat-admin-order-list\t/api/admin/store/order/list\t${compat_admin_code}\t${compat_admin_type}\t${compat_admin_preview}"
} > "${MATRIX_FILE}"

health_ok=0
health_warn=0
ruoyi_admin_ok=0
ruoyi_app_ok=0
ruoyi_app_pay_ok=0

if [[ "${health_code}" == "200" ]]; then
  health_ok=1
elif [[ "${health_code}" == "404" ]]; then
  # actuator 可能未启用，不直接判失败
  health_warn=1
fi
if is_non_404_route "${admin_code}"; then
  ruoyi_admin_ok=1
fi
if is_non_404_route "${app_code}"; then
  ruoyi_app_ok=1
fi
if is_non_404_route "${app_pay_code}"; then
  ruoyi_app_pay_ok=1
fi

diagnosis_status="ruoyi_stage_unknown"
stage_guess="unknown"
decision="PASS"
suggestion="保持当前配置，继续执行阶段门禁。"

if [[ "${health_ok}" -ne 1 && "${health_warn}" -ne 1 ]]; then
  diagnosis_status="service_unhealthy"
  decision="BLOCK"
  suggestion="先恢复服务健康检查（/actuator/health=200），再做灰度切换。"
elif [[ "${ruoyi_admin_ok}" -ne 1 || "${ruoyi_app_ok}" -ne 1 || "${ruoyi_app_pay_ok}" -ne 1 ]]; then
  diagnosis_status="likely_not_ruoyi_entrypoint"
  decision="BLOCK"
  suggestion="公网入口未完整命中 RuoYi。请将 upstream 切到 RuoYi 服务，并确认 /admin-api 与 /app-api 路由可访问。"
else
  if [[ "${compat_pay_code}" == "410" && "${compat_order_code}" == "410" && "${compat_admin_code}" == "410" ]]; then
    diagnosis_status="ruoyi_entrypoint_ok"
    stage_guess="disabled"
    suggestion="兼容层已全部关闭，可进入旧接口下线收口。"
  elif [[ "${compat_pay_code}" != "410" && "${compat_order_code}" == "410" && "${compat_admin_code}" == "410" ]]; then
    diagnosis_status="ruoyi_entrypoint_ok"
    stage_guess="payment-core-only"
    suggestion="当前是支付核心放行阶段，可继续观察非支付流量回落。"
  elif [[ "${compat_pay_code}" != "410" && "${compat_order_code}" != "410" && "${compat_admin_code}" != "410" ]]; then
    diagnosis_status="ruoyi_entrypoint_ok"
    stage_guess="full-compat"
    suggestion="当前为全量兼容阶段，可按计划推进到 payment-core-only。"
  else
    diagnosis_status="ruoyi_entrypoint_ok_mixed"
    stage_guess="mixed"
    decision="WARN"
    suggestion="兼容层状态混杂，建议对齐 profile 与 disabledPaths 配置后再继续切换。"
  fi
fi

if [[ "${health_warn}" -eq 1 && "${decision}" == "PASS" ]]; then
  decision="WARN"
  suggestion="RuoYi 路由正常，但 /actuator/health 未开放（404）；建议补齐或维持 admin-api 探活。"
fi

rc=0
if [[ "${REQUIRE_HEALTH}" == "1" && "${health_ok}" -ne 1 && "${health_warn}" -ne 1 ]]; then
  rc=2
fi
if [[ "${REQUIRE_RUOYI}" == "1" && ( "${ruoyi_admin_ok}" -ne 1 || "${ruoyi_app_ok}" -ne 1 || "${ruoyi_app_pay_ok}" -ne 1 ) ]]; then
  rc=2
fi

{
  echo "run_id=${RUN_ID}"
  echo "base_url=${BASE_URL}"
  echo "timeout_seconds=${TIMEOUT_SECONDS}"
  echo "health_code=${health_code}"
  echo "admin_api_code=${admin_code}"
  echo "app_api_member_code=${app_code}"
  echo "app_api_pay_code=${app_pay_code}"
  echo "compat_front_pay_code=${compat_pay_code}"
  echo "compat_front_order_code=${compat_order_code}"
  echo "compat_admin_order_code=${compat_admin_code}"
  echo "health_ok=${health_ok}"
  echo "health_warn=${health_warn}"
  echo "ruoyi_admin_ok=${ruoyi_admin_ok}"
  echo "ruoyi_app_ok=${ruoyi_app_ok}"
  echo "ruoyi_app_pay_ok=${ruoyi_app_pay_ok}"
  echo "diagnosis_status=${diagnosis_status}"
  echo "stage_guess=${stage_guess}"
  echo "decision=${decision}"
  echo "suggestion=${suggestion}"
  echo "require_ruoyi=${REQUIRE_RUOYI}"
  echo "require_health=${REQUIRE_HEALTH}"
  echo "pipeline_exit_code=${rc}"
  echo "matrix_file=${MATRIX_FILE}"
  echo "summary_file=${SUMMARY_FILE}"
} > "${SUMMARY_FILE}"

{
  echo "# RuoYi Entrypoint Diagnosis"
  echo
  echo "- run_id: \`${RUN_ID}\`"
  echo "- base_url: \`${BASE_URL}\`"
  echo "- diagnosis_status: \`${diagnosis_status}\`"
  echo "- stage_guess: \`${stage_guess}\`"
  echo "- decision: \`${decision}\`"
  echo "- suggestion: ${suggestion}"
  echo "- pipeline_exit_code: \`${rc}\`"
  echo
  echo "## Files"
  echo
  echo "- summary: \`summary.txt\`"
  echo "- matrix: \`matrix.tsv\`"
} > "${INDEX_FILE}"

if [[ "${rc}" -ne 0 ]]; then
  echo "[entrypoint-diagnose] result=FAIL rc=${rc}" >&2
  exit "${rc}"
fi

echo "[entrypoint-diagnose] result=${decision}"
echo "[entrypoint-diagnose] summary=${SUMMARY_FILE}"
