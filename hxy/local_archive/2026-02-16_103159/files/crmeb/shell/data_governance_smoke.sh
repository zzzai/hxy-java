#!/usr/bin/env bash
set -euo pipefail

# 用户数据治理接口冒烟脚本
# 用法：
#   FRONT_TOKEN=xxx ./shell/data_governance_smoke.sh
#   FRONT_TOKEN=xxx ADMIN_TOKEN=yyy TARGET_USER_ID=123 ./shell/data_governance_smoke.sh
#
# 可选参数：
#   FRONT_BASE_URL=http://127.0.0.1:8081
#   ADMIN_BASE_URL=http://127.0.0.1:8080
#   TARGET_USER_ID=1
#   PAGE=1 LIMIT=10

FRONT_BASE_URL="${FRONT_BASE_URL:-http://127.0.0.1:8081}"
ADMIN_BASE_URL="${ADMIN_BASE_URL:-http://127.0.0.1:8080}"
FRONT_TOKEN="${FRONT_TOKEN:-}"
ADMIN_TOKEN="${ADMIN_TOKEN:-}"
TARGET_USER_ID="${TARGET_USER_ID:-1}"
PAGE="${PAGE:-1}"
LIMIT="${LIMIT:-10}"

if [[ -z "${FRONT_TOKEN}" ]]; then
  echo "缺少 FRONT_TOKEN"
  echo "示例：FRONT_TOKEN=xxx ./shell/data_governance_smoke.sh"
  exit 1
fi

request_json() {
  local method="$1"
  local url="$2"
  local token="$3"
  local body="${4:-}"
  if [[ -n "${body}" ]]; then
    curl -fsS -X "${method}" "${url}" \
      -H "Authorization: Bearer ${token}" \
      -H "Content-Type: application/json" \
      -d "${body}"
  else
    curl -fsS -X "${method}" "${url}" \
      -H "Authorization: Bearer ${token}" \
      -H "Content-Type: application/json"
  fi
}

extract_code() {
  local resp="$1"
  printf "%s" "${resp}" | tr -d '\n' | grep -oE '"code"[[:space:]]*:[[:space:]]*[0-9]+' | head -n1 | grep -oE '[0-9]+'
}

extract_first_id() {
  local resp="$1"
  printf "%s" "${resp}" | tr -d '\n' | grep -oE '"id"[[:space:]]*:[[:space:]]*[0-9]+' | head -n1 | grep -oE '[0-9]+'
}

must_ok() {
  local name="$1"
  local resp="$2"
  local code
  code="$(extract_code "${resp}")"
  if [[ -z "${code}" ]]; then
    echo "[FAIL] ${name}: 返回体缺少 code 字段"
    echo "${resp}"
    exit 2
  fi
  if [[ "${code}" != "200" ]]; then
    echo "[FAIL] ${name}: code=${code}"
    echo "${resp}"
    exit 2
  fi
  echo "[OK] ${name}"
}

echo "========== FRONT 隐私中心冒烟 =========="

grant_body='{
  "scenarioCode":"HEALTH_BASIC",
  "policyVersion":"v1.0.0",
  "consentTextHash":"a94a8fe5ccb19ba61c4c0873d391e987982fbbd3f1d5d36f0a8cd824f8f2f21d",
  "dataScopeJson":"{\"fields\":[\"complaint_type\",\"discomfort_score_before\"]}",
  "purposeCodesJson":"[\"SERVICE_PLAN\",\"EFFECT_EVAL\"]",
  "sourceChannel":"miniapp",
  "storeId":1
}'
grant_resp="$(request_json "POST" "${FRONT_BASE_URL}/api/front/privacy/consent/grant" "${FRONT_TOKEN}" "${grant_body}")"
must_ok "consent/grant" "${grant_resp}"
consent_id="$(extract_first_id "${grant_resp}")"

list_resp="$(request_json "GET" "${FRONT_BASE_URL}/api/front/privacy/consent/list?page=${PAGE}&limit=${LIMIT}" "${FRONT_TOKEN}")"
must_ok "consent/list" "${list_resp}"

if [[ -n "${consent_id}" ]]; then
  withdraw_body="{\"consentId\":${consent_id}}"
  withdraw_resp="$(request_json "POST" "${FRONT_BASE_URL}/api/front/privacy/consent/withdraw" "${FRONT_TOKEN}" "${withdraw_body}")"
  must_ok "consent/withdraw" "${withdraw_resp}"
else
  echo "[WARN] consent/grant 未解析出 consentId，跳过 withdraw"
fi

deletion_body='{
  "scopeCode":"HEALTH_DATA",
  "scopeJson":"{\"deleteFields\":[\"tongue_photo_url\"]}"
}'
deletion_resp="$(request_json "POST" "${FRONT_BASE_URL}/api/front/privacy/deletion/request" "${FRONT_TOKEN}" "${deletion_body}")"
must_ok "deletion/request" "${deletion_resp}"
deletion_ticket_id="$(extract_first_id "${deletion_resp}")"

my_deletion_resp="$(request_json "GET" "${FRONT_BASE_URL}/api/front/privacy/deletion/list?page=${PAGE}&limit=${LIMIT}" "${FRONT_TOKEN}")"
must_ok "deletion/list" "${my_deletion_resp}"

if [[ -n "${deletion_ticket_id}" ]]; then
  cancel_body="{\"ticketId\":${deletion_ticket_id}}"
  cancel_resp="$(request_json "POST" "${FRONT_BASE_URL}/api/front/privacy/deletion/cancel" "${FRONT_TOKEN}" "${cancel_body}")"
  must_ok "deletion/cancel" "${cancel_resp}"
else
  echo "[WARN] deletion/request 未解析出 ticketId，跳过 cancel"
fi

if [[ -z "${ADMIN_TOKEN}" ]]; then
  echo "========== ADMIN 治理中心冒烟（跳过） =========="
  echo "未提供 ADMIN_TOKEN，仅完成 front 冒烟。"
  exit 0
fi

echo "========== ADMIN 治理中心冒烟 =========="

field_list_resp="$(request_json "GET" "${ADMIN_BASE_URL}/api/admin/data/governance/field/list?necessityLevel=1&page=${PAGE}&limit=${LIMIT}" "${ADMIN_TOKEN}")"
must_ok "field/list" "${field_list_resp}"

consent_list_resp="$(request_json "GET" "${ADMIN_BASE_URL}/api/admin/data/governance/consent/list?scenarioCode=HEALTH_BASIC&status=1&page=${PAGE}&limit=${LIMIT}" "${ADMIN_TOKEN}")"
must_ok "consent/list(admin)" "${consent_list_resp}"

create_ticket_body="{
  \"userId\": ${TARGET_USER_ID},
  \"applicantRole\": 4,
  \"dataLevel\": 3,
  \"dataFieldsJson\": \"[\\\"blood_pressure\\\"]\",
  \"purposeCode\": \"SERVICE_QA_INTERVENTION\",
  \"reason\": \"smoke test\"
}"
create_ticket_resp="$(request_json "POST" "${ADMIN_BASE_URL}/api/admin/data/governance/access-ticket/create" "${ADMIN_TOKEN}" "${create_ticket_body}")"
must_ok "access-ticket/create" "${create_ticket_resp}"
access_ticket_id="$(extract_first_id "${create_ticket_resp}")"

if [[ -n "${access_ticket_id}" ]]; then
  approve_body="{\"ticketId\":${access_ticket_id}}"
  approve_resp="$(request_json "POST" "${ADMIN_BASE_URL}/api/admin/data/governance/access-ticket/approve" "${ADMIN_TOKEN}" "${approve_body}")"
  must_ok "access-ticket/approve" "${approve_resp}"

  close_body="{\"ticketId\":${access_ticket_id}}"
  close_resp="$(request_json "POST" "${ADMIN_BASE_URL}/api/admin/data/governance/access-ticket/close" "${ADMIN_TOKEN}" "${close_body}")"
  must_ok "access-ticket/close" "${close_resp}"
else
  echo "[WARN] access-ticket/create 未解析出 ticketId，跳过 approve/close"
fi

access_list_resp="$(request_json "GET" "${ADMIN_BASE_URL}/api/admin/data/governance/access-ticket/list?status=5&page=${PAGE}&limit=${LIMIT}" "${ADMIN_TOKEN}")"
must_ok "access-ticket/list" "${access_list_resp}"

deletion_admin_resp="$(request_json "GET" "${ADMIN_BASE_URL}/api/admin/data/governance/deletion/list?status=1&page=${PAGE}&limit=${LIMIT}" "${ADMIN_TOKEN}")"
must_ok "deletion/list(admin)" "${deletion_admin_resp}"

label_policy_resp="$(request_json "GET" "${ADMIN_BASE_URL}/api/admin/data/governance/label-policy/list?riskLevel=2&page=${PAGE}&limit=${LIMIT}" "${ADMIN_TOKEN}")"
must_ok "label-policy/list" "${label_policy_resp}"

echo "全部冒烟完成。"
