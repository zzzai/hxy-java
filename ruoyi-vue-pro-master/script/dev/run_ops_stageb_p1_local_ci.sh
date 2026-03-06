#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

RUN_ID="${RUN_ID:-local_$(date +%Y%m%d_%H%M%S)_$RANDOM}"
ARTIFACT_BASE_DIR="${ARTIFACT_BASE_DIR:-${ROOT_DIR}/.tmp/ops_stageb_p1_local_ci}"
ARTIFACT_DIR="${ARTIFACT_BASE_DIR}/${RUN_ID}"
LOG_DIR="${ARTIFACT_DIR}/logs"
SUMMARY_FILE="${ARTIFACT_DIR}/summary.txt"
RESULT_TSV="${ARTIFACT_DIR}/result.tsv"

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-root}"
DB_NAME="${DB_NAME:-hxy_dev}"

GIT_DIFF_RANGE="${GIT_DIFF_RANGE:-}"
NAMING_GIT_DIFF_RANGE="${NAMING_GIT_DIFF_RANGE:-}"

SKIP_MYSQL_INIT="${SKIP_MYSQL_INIT:-0}"
RUN_NAMING_GUARD="${RUN_NAMING_GUARD:-1}"
RUN_MEMORY_GUARD="${RUN_MEMORY_GUARD:-1}"
RUN_STORE_SKU_STOCK_GATE="${RUN_STORE_SKU_STOCK_GATE:-1}"
RUN_STORE_LIFECYCLE_GATE="${RUN_STORE_LIFECYCLE_GATE:-1}"
RUN_TESTS="${RUN_TESTS:-1}"

REQUIRE_NAMING_GUARD="${REQUIRE_NAMING_GUARD:-1}"
REQUIRE_MEMORY_GUARD="${REQUIRE_MEMORY_GUARD:-1}"
REQUIRE_STORE_SKU_STOCK_GATE="${REQUIRE_STORE_SKU_STOCK_GATE:-0}"
REQUIRE_STORE_LIFECYCLE_GATE="${REQUIRE_STORE_LIFECYCLE_GATE:-0}"

STOCK_REQUIRE_OVERDUE_ZERO="${STOCK_REQUIRE_OVERDUE_ZERO:-${REQUIRE_STORE_SKU_STOCK_GATE}}"
STOCK_REQUIRE_APPROVAL_OVERDUE_ZERO="${STOCK_REQUIRE_APPROVAL_OVERDUE_ZERO:-${REQUIRE_STORE_SKU_STOCK_GATE}}"
PENDING_APPROVAL_TIMEOUT_MINUTES="${PENDING_APPROVAL_TIMEOUT_MINUTES:-120}"

LIFECYCLE_REQUIRE_OVERDUE_ZERO="${LIFECYCLE_REQUIRE_OVERDUE_ZERO:-${REQUIRE_STORE_LIFECYCLE_GATE}}"
LIFECYCLE_REQUIRE_EXPIRE_ABNORMAL_ZERO="${LIFECYCLE_REQUIRE_EXPIRE_ABNORMAL_ZERO:-${REQUIRE_STORE_LIFECYCLE_GATE}}"
LIFECYCLE_EXPIRE_ACTION_CODE="${LIFECYCLE_EXPIRE_ACTION_CODE:-EXPIRE}"
LIFECYCLE_EXPIRE_REMARK="${LIFECYCLE_EXPIRE_REMARK:-SYSTEM_SLA_EXPIRED}"
REGRESSION_TEST_CLASSES="${REGRESSION_TEST_CLASSES:-ProductStoreSkuControllerTest,ProductStoreServiceImplTest,AfterSaleReviewTicketServiceImplTest,BookingOrderServiceImplTest,FourAccountReconcileServiceImplTest,FourAccountReconcileControllerTest}"

usage() {
  cat <<'USAGE'
Usage:
  script/dev/run_ops_stageb_p1_local_ci.sh [options]

Options:
  --run-id <id>                              执行 ID（默认 local_时间戳）
  --artifact-base-dir <dir>                  产物目录根路径（默认 .tmp/ops_stageb_p1_local_ci）
  --db-host <host>                           数据库地址（默认 127.0.0.1）
  --db-port <port>                           数据库端口（默认 3306）
  --db-user <user>                           数据库用户名（默认 root）
  --db-password <password>                   数据库密码（默认 root）
  --db-name <name>                           数据库名（默认 hxy_dev）
  --git-diff-range <base...head>             记忆门禁差异范围（可选）
  --naming-git-diff-range <base...head>      命名门禁差异范围（可选）

  --skip-mysql-init                          跳过 MySQL schema 初始化
  --skip-naming-guard                        跳过命名门禁
  --skip-memory-guard                        跳过记忆门禁
  --skip-stock-gate                          跳过库存门禁
  --skip-lifecycle-gate                      跳过生命周期审批门禁
  --skip-tests                               跳过回归测试（product/trade/booking）

  --require-naming-guard <0|1>               命名门禁失败是否阻断（默认 1）
  --require-memory-guard <0|1>               记忆门禁失败是否阻断（默认 1）
  --require-store-sku-stock-gate <0|1>       库存门禁失败是否阻断（默认 0）
  --require-store-lifecycle-gate <0|1>       生命周期门禁失败是否阻断（默认 0）

  --pending-approval-timeout-minutes <n>     库存审批单待审超时阈值（默认 120）
  -h, --help                                 显示帮助

Env:
  REGRESSION_TEST_CLASSES                    覆盖默认回归测试集合（逗号分隔）

Exit Code:
  0: PASS
  2: BLOCK（门禁阻断）
  1: 执行异常
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --run-id)
      RUN_ID="$2"
      shift 2
      ;;
    --artifact-base-dir)
      ARTIFACT_BASE_DIR="$2"
      shift 2
      ;;
    --db-host)
      DB_HOST="$2"
      shift 2
      ;;
    --db-port)
      DB_PORT="$2"
      shift 2
      ;;
    --db-user)
      DB_USER="$2"
      shift 2
      ;;
    --db-password)
      DB_PASSWORD="$2"
      shift 2
      ;;
    --db-name)
      DB_NAME="$2"
      shift 2
      ;;
    --git-diff-range)
      GIT_DIFF_RANGE="$2"
      shift 2
      ;;
    --naming-git-diff-range)
      NAMING_GIT_DIFF_RANGE="$2"
      shift 2
      ;;
    --skip-mysql-init)
      SKIP_MYSQL_INIT=1
      shift
      ;;
    --skip-naming-guard)
      RUN_NAMING_GUARD=0
      shift
      ;;
    --skip-memory-guard)
      RUN_MEMORY_GUARD=0
      shift
      ;;
    --skip-stock-gate)
      RUN_STORE_SKU_STOCK_GATE=0
      shift
      ;;
    --skip-lifecycle-gate)
      RUN_STORE_LIFECYCLE_GATE=0
      shift
      ;;
    --skip-tests)
      RUN_TESTS=0
      shift
      ;;
    --require-naming-guard)
      REQUIRE_NAMING_GUARD="$2"
      shift 2
      ;;
    --require-memory-guard)
      REQUIRE_MEMORY_GUARD="$2"
      shift 2
      ;;
    --require-store-sku-stock-gate)
      REQUIRE_STORE_SKU_STOCK_GATE="$2"
      STOCK_REQUIRE_OVERDUE_ZERO="$2"
      STOCK_REQUIRE_APPROVAL_OVERDUE_ZERO="$2"
      shift 2
      ;;
    --require-store-lifecycle-gate)
      REQUIRE_STORE_LIFECYCLE_GATE="$2"
      LIFECYCLE_REQUIRE_OVERDUE_ZERO="$2"
      LIFECYCLE_REQUIRE_EXPIRE_ABNORMAL_ZERO="$2"
      shift 2
      ;;
    --pending-approval-timeout-minutes)
      PENDING_APPROVAL_TIMEOUT_MINUTES="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage
      exit 1
      ;;
  esac
done

ARTIFACT_DIR="${ARTIFACT_BASE_DIR}/${RUN_ID}"
LOG_DIR="${ARTIFACT_DIR}/logs"
SUMMARY_FILE="${ARTIFACT_DIR}/summary.txt"
RESULT_TSV="${ARTIFACT_DIR}/result.tsv"

for flag in \
  "${SKIP_MYSQL_INIT}" \
  "${RUN_NAMING_GUARD}" \
  "${RUN_MEMORY_GUARD}" \
  "${RUN_STORE_SKU_STOCK_GATE}" \
  "${RUN_STORE_LIFECYCLE_GATE}" \
  "${RUN_TESTS}" \
  "${REQUIRE_NAMING_GUARD}" \
  "${REQUIRE_MEMORY_GUARD}" \
  "${REQUIRE_STORE_SKU_STOCK_GATE}" \
  "${REQUIRE_STORE_LIFECYCLE_GATE}" \
  "${STOCK_REQUIRE_OVERDUE_ZERO}" \
  "${STOCK_REQUIRE_APPROVAL_OVERDUE_ZERO}" \
  "${LIFECYCLE_REQUIRE_OVERDUE_ZERO}" \
  "${LIFECYCLE_REQUIRE_EXPIRE_ABNORMAL_ZERO}"; do
  if ! [[ "${flag}" =~ ^[01]$ ]]; then
    echo "Invalid 0|1 flag value: ${flag}" >&2
    exit 1
  fi
done

if ! [[ "${PENDING_APPROVAL_TIMEOUT_MINUTES}" =~ ^[1-9][0-9]*$ ]]; then
  echo "Invalid --pending-approval-timeout-minutes: ${PENDING_APPROVAL_TIMEOUT_MINUTES}" >&2
  exit 1
fi

RUN_LOG="${LOG_DIR}/run.log"
FINAL_GATE_LOG="${LOG_DIR}/final_gate.log"
MYSQL_INIT_LOG="${LOG_DIR}/mysql_init.log"
NAMING_GUARD_LOG="${LOG_DIR}/naming_guard.log"
MEMORY_GUARD_LOG="${LOG_DIR}/memory_guard.log"
STORE_SKU_STOCK_GATE_LOG="${LOG_DIR}/store_sku_stock_gate.log"
STORE_LIFECYCLE_GATE_LOG="${LOG_DIR}/store_lifecycle_change_order_gate.log"
TEST_LOG="${LOG_DIR}/regression_tests.log"

STORE_SKU_STOCK_GATE_DIR="${ARTIFACT_DIR}/store_sku_stock_gate"
STORE_SKU_STOCK_GATE_SUMMARY="${STORE_SKU_STOCK_GATE_DIR}/summary.txt"
STORE_SKU_STOCK_GATE_TSV="${STORE_SKU_STOCK_GATE_DIR}/result.tsv"

STORE_LIFECYCLE_GATE_DIR="${ARTIFACT_DIR}/store_lifecycle_change_order_gate"
STORE_LIFECYCLE_GATE_SUMMARY="${STORE_LIFECYCLE_GATE_DIR}/summary.txt"
STORE_LIFECYCLE_GATE_TSV="${STORE_LIFECYCLE_GATE_DIR}/result.tsv"

mkdir -p "${LOG_DIR}" "${STORE_SKU_STOCK_GATE_DIR}" "${STORE_LIFECYCLE_GATE_DIR}"
echo -e "stage\tseverity\tcode\tdetail" > "${RESULT_TSV}"
exec > >(tee -a "${RUN_LOG}") 2>&1

mysql_init_rc="SKIP"
naming_guard_rc="SKIP"
memory_guard_rc="SKIP"
store_sku_stock_gate_rc="SKIP"
store_lifecycle_gate_rc="SKIP"
tests_rc="SKIP"

add_issue() {
  local stage="$1"
  local severity="$2"
  local code="$3"
  local detail="$4"
  echo -e "${stage}\t${severity}\t${code}\t${detail}" >> "${RESULT_TSV}"
}

append_gate_tsv() {
  local stage="$1"
  local source_tsv="$2"
  if [[ ! -f "${source_tsv}" ]]; then
    return
  fi
  awk -F $'\t' -v stage="${stage}" 'NR > 1 && NF >= 3 {print stage "\t" $1 "\t" $2 "\t" $3}' "${source_tsv}" >> "${RESULT_TSV}"
}

finalize() {
  local rc=$?
  local pipeline_rc="${PIPELINE_EXIT_CODE:-$rc}"

  {
    echo "run_id=${RUN_ID}"
    echo "pipeline_exit_code=${pipeline_rc}"
    echo "db_host=${DB_HOST}"
    echo "db_port=${DB_PORT}"
    echo "db_name=${DB_NAME}"
    echo "skip_mysql_init=${SKIP_MYSQL_INIT}"
    echo "run_naming_guard=${RUN_NAMING_GUARD}"
    echo "run_memory_guard=${RUN_MEMORY_GUARD}"
    echo "run_store_sku_stock_gate=${RUN_STORE_SKU_STOCK_GATE}"
    echo "run_store_lifecycle_gate=${RUN_STORE_LIFECYCLE_GATE}"
    echo "run_tests=${RUN_TESTS}"
    echo "regression_test_classes=${REGRESSION_TEST_CLASSES}"
    echo "require_naming_guard=${REQUIRE_NAMING_GUARD}"
    echo "require_memory_guard=${REQUIRE_MEMORY_GUARD}"
    echo "require_store_sku_stock_gate=${REQUIRE_STORE_SKU_STOCK_GATE}"
    echo "require_store_lifecycle_gate=${REQUIRE_STORE_LIFECYCLE_GATE}"
    echo "mysql_init_rc=${mysql_init_rc}"
    echo "naming_guard_rc=${naming_guard_rc}"
    echo "memory_guard_rc=${memory_guard_rc}"
    echo "store_sku_stock_gate_rc=${store_sku_stock_gate_rc}"
    echo "store_lifecycle_change_order_gate_rc=${store_lifecycle_gate_rc}"
    echo "tests_rc=${tests_rc}"
    echo "result_tsv=${RESULT_TSV}"
    echo "run_log=${RUN_LOG}"
    echo "final_gate_log=${FINAL_GATE_LOG}"
    echo "mysql_init_log=${MYSQL_INIT_LOG}"
    echo "naming_guard_log=${NAMING_GUARD_LOG}"
    echo "memory_guard_log=${MEMORY_GUARD_LOG}"
    echo "store_sku_stock_gate_log=${STORE_SKU_STOCK_GATE_LOG}"
    echo "store_lifecycle_change_order_gate_log=${STORE_LIFECYCLE_GATE_LOG}"
    echo "store_sku_stock_gate_summary=${STORE_SKU_STOCK_GATE_SUMMARY}"
    echo "store_sku_stock_gate_tsv=${STORE_SKU_STOCK_GATE_TSV}"
    echo "store_lifecycle_change_order_gate_summary=${STORE_LIFECYCLE_GATE_SUMMARY}"
    echo "store_lifecycle_change_order_gate_tsv=${STORE_LIFECYCLE_GATE_TSV}"
    echo "test_log=${TEST_LOG}"
    echo "summary_file=${SUMMARY_FILE}"
  } > "${SUMMARY_FILE}"

  {
    echo "ops_stageb_p1_local_ci"
    echo "generated_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    echo "pipeline_exit_code=${pipeline_rc}"
    if [[ "${pipeline_rc}" == "0" ]]; then
      echo "decision=PASS"
    else
      echo "decision=BLOCK"
    fi
    echo "summary=${SUMMARY_FILE}"
    echo "result_tsv=${RESULT_TSV}"
  } > "${FINAL_GATE_LOG}"

  echo "[ops-stageb-p1-local-ci] artifact_dir=${ARTIFACT_DIR}"
  echo "[ops-stageb-p1-local-ci] summary=${SUMMARY_FILE}"
  return "${pipeline_rc}"
}
trap finalize EXIT

mysql_cmd=(mysql -h"${DB_HOST}" -P"${DB_PORT}" -u"${DB_USER}")
if [[ -n "${DB_PASSWORD}" ]]; then
  mysql_cmd+=("-p${DB_PASSWORD}")
fi

if [[ "${SKIP_MYSQL_INIT}" == "0" ]]; then
  echo "[ops-stageb-p1-local-ci] step=mysql-init"
  set +e
  {
    "${mysql_cmd[@]}" "${DB_NAME}" -e "SELECT 1;" >/dev/null

    bootstrap_sql_files=(
      "sql/mysql/ruoyi-modules-member-pay-mall.sql"
      "sql/mysql/hxy/2026-02-28-hxy-store-product-mapping.sql"
      "sql/mysql/hxy/2026-03-01-hxy-store-sku-stock-flow.sql"
      "sql/mysql/hxy/2026-03-05-hxy-store-lifecycle-change-order.sql"
      "sql/mysql/hxy/2026-03-05-hxy-store-lifecycle-change-order-sla.sql"
      "sql/mysql/hxy/2026-03-05-hxy-store-sku-stock-adjust-order.sql"
      "sql/mysql/hxy/2026-03-06-hxy-store-sku-transfer-order-and-stocktake-audit-config.sql"
    )

    for sql_file in "${bootstrap_sql_files[@]}"; do
      if [[ ! -f "${ROOT_DIR}/${sql_file}" ]]; then
        echo "missing bootstrap sql: ${sql_file}" >&2
        exit 1
      fi
      echo "[mysql-init] apply ${sql_file}"
      "${mysql_cmd[@]}" "${DB_NAME}" < "${ROOT_DIR}/${sql_file}"
    done
  } > "${MYSQL_INIT_LOG}" 2>&1
  mysql_init_rc=$?
  set -e
  if [[ "${mysql_init_rc}" != "0" ]]; then
    PIPELINE_EXIT_CODE="${mysql_init_rc}"
    add_issue "mysql-init" "BLOCK" "OPS01_MYSQL_INIT_FAILED" "mysql_init_rc=${mysql_init_rc}"
    exit "${mysql_init_rc}"
  fi
fi

if [[ "${RUN_NAMING_GUARD}" == "1" ]]; then
  echo "[ops-stageb-p1-local-ci] step=naming-guard"
  set +e
  if [[ -n "${NAMING_GIT_DIFF_RANGE}" ]]; then
    CHECK_STAGED=0 \
    CHECK_UNSTAGED=0 \
    CHECK_UNTRACKED=0 \
    GIT_DIFF_RANGE="${NAMING_GIT_DIFF_RANGE}" \
    bash script/dev/check_hxy_naming_guard.sh > "${NAMING_GUARD_LOG}" 2>&1
  else
    bash script/dev/check_hxy_naming_guard.sh > "${NAMING_GUARD_LOG}" 2>&1
  fi
  naming_guard_rc=$?
  set -e
  if [[ "${naming_guard_rc}" != "0" ]]; then
    if [[ "${REQUIRE_NAMING_GUARD}" == "1" ]]; then
      add_issue "naming-guard" "BLOCK" "OPS02_NAMING_GUARD_FAIL" "naming_guard_rc=${naming_guard_rc}"
      PIPELINE_EXIT_CODE=2
      exit 2
    fi
    add_issue "naming-guard" "WARN" "OPS02_NAMING_GUARD_FAIL" "naming_guard_rc=${naming_guard_rc}"
  fi
fi

if [[ "${RUN_MEMORY_GUARD}" == "1" ]]; then
  echo "[ops-stageb-p1-local-ci] step=memory-guard"
  set +e
  if [[ -n "${GIT_DIFF_RANGE}" ]]; then
    CHECK_STAGED=0 \
    CHECK_UNSTAGED=0 \
    CHECK_UNTRACKED=0 \
    GIT_DIFF_RANGE="${GIT_DIFF_RANGE}" \
    bash script/dev/check_hxy_memory_guard.sh > "${MEMORY_GUARD_LOG}" 2>&1
  else
    CHECK_STAGED=1 \
    CHECK_UNSTAGED=0 \
    CHECK_UNTRACKED=0 \
    bash script/dev/check_hxy_memory_guard.sh > "${MEMORY_GUARD_LOG}" 2>&1
  fi
  memory_guard_rc=$?
  set -e
  if [[ "${memory_guard_rc}" != "0" ]]; then
    if [[ "${REQUIRE_MEMORY_GUARD}" == "1" ]]; then
      add_issue "memory-guard" "BLOCK" "OPS03_MEMORY_GUARD_FAIL" "memory_guard_rc=${memory_guard_rc}"
      PIPELINE_EXIT_CODE=2
      exit 2
    fi
    add_issue "memory-guard" "WARN" "OPS03_MEMORY_GUARD_FAIL" "memory_guard_rc=${memory_guard_rc}"
  fi
fi

if [[ "${RUN_STORE_SKU_STOCK_GATE}" == "1" ]]; then
  echo "[ops-stageb-p1-local-ci] step=store-sku-stock-gate"
  set +e
  bash script/dev/check_store_sku_stock_gate.sh \
    --db-host "${DB_HOST}" \
    --db-port "${DB_PORT}" \
    --db-user "${DB_USER}" \
    --db-password "${DB_PASSWORD}" \
    --db-name "${DB_NAME}" \
    --require-overdue-zero "${STOCK_REQUIRE_OVERDUE_ZERO}" \
    --require-approval-overdue-zero "${STOCK_REQUIRE_APPROVAL_OVERDUE_ZERO}" \
    --pending-approval-timeout-minutes "${PENDING_APPROVAL_TIMEOUT_MINUTES}" \
    --summary-file "${STORE_SKU_STOCK_GATE_SUMMARY}" \
    --output-tsv "${STORE_SKU_STOCK_GATE_TSV}" > "${STORE_SKU_STOCK_GATE_LOG}" 2>&1
  store_sku_stock_gate_rc=$?
  set -e
  append_gate_tsv "store-sku-stock-gate" "${STORE_SKU_STOCK_GATE_TSV}"

  if [[ "${store_sku_stock_gate_rc}" != "0" && "${store_sku_stock_gate_rc}" != "2" ]]; then
    if [[ "${REQUIRE_STORE_SKU_STOCK_GATE}" == "1" ]]; then
      add_issue "store-sku-stock-gate" "BLOCK" "OPS04_STOCK_GATE_EXEC_FAIL" "store_sku_stock_gate_rc=${store_sku_stock_gate_rc}"
      PIPELINE_EXIT_CODE=2
      exit 2
    fi
    add_issue "store-sku-stock-gate" "WARN" "OPS04_STOCK_GATE_EXEC_FAIL" "store_sku_stock_gate_rc=${store_sku_stock_gate_rc}"
  elif [[ "${store_sku_stock_gate_rc}" == "2" && "${REQUIRE_STORE_SKU_STOCK_GATE}" == "1" ]]; then
    PIPELINE_EXIT_CODE=2
    exit 2
  fi
fi

if [[ "${RUN_STORE_LIFECYCLE_GATE}" == "1" ]]; then
  echo "[ops-stageb-p1-local-ci] step=store-lifecycle-change-order-gate"
  set +e
  bash script/dev/check_store_lifecycle_change_order_gate.sh \
    --db-host "${DB_HOST}" \
    --db-port "${DB_PORT}" \
    --db-user "${DB_USER}" \
    --db-password "${DB_PASSWORD}" \
    --db-name "${DB_NAME}" \
    --require-overdue-zero "${LIFECYCLE_REQUIRE_OVERDUE_ZERO}" \
    --require-expire-abnormal-zero "${LIFECYCLE_REQUIRE_EXPIRE_ABNORMAL_ZERO}" \
    --expire-action-code "${LIFECYCLE_EXPIRE_ACTION_CODE}" \
    --expire-remark "${LIFECYCLE_EXPIRE_REMARK}" \
    --summary-file "${STORE_LIFECYCLE_GATE_SUMMARY}" \
    --output-tsv "${STORE_LIFECYCLE_GATE_TSV}" > "${STORE_LIFECYCLE_GATE_LOG}" 2>&1
  store_lifecycle_gate_rc=$?
  set -e
  append_gate_tsv "store-lifecycle-change-order-gate" "${STORE_LIFECYCLE_GATE_TSV}"

  if [[ "${store_lifecycle_gate_rc}" != "0" && "${store_lifecycle_gate_rc}" != "2" ]]; then
    if [[ "${REQUIRE_STORE_LIFECYCLE_GATE}" == "1" ]]; then
      add_issue "store-lifecycle-change-order-gate" "BLOCK" "OPS05_LIFECYCLE_GATE_EXEC_FAIL" "store_lifecycle_gate_rc=${store_lifecycle_gate_rc}"
      PIPELINE_EXIT_CODE=2
      exit 2
    fi
    add_issue "store-lifecycle-change-order-gate" "WARN" "OPS05_LIFECYCLE_GATE_EXEC_FAIL" "store_lifecycle_gate_rc=${store_lifecycle_gate_rc}"
  elif [[ "${store_lifecycle_gate_rc}" == "2" && "${REQUIRE_STORE_LIFECYCLE_GATE}" == "1" ]]; then
    PIPELINE_EXIT_CODE=2
    exit 2
  fi
fi

if [[ "${RUN_TESTS}" == "1" ]]; then
  echo "[ops-stageb-p1-local-ci] step=regression-tests"
  set +e
  mvn -f pom.xml \
    -pl yudao-module-mall/yudao-module-product,yudao-module-mall/yudao-module-booking,yudao-module-mall/yudao-module-trade -am \
    -Dtest="${REGRESSION_TEST_CLASSES}" \
    -Dsurefire.failIfNoSpecifiedTests=false test > "${TEST_LOG}" 2>&1
  tests_rc=$?
  set -e
  if [[ "${tests_rc}" != "0" ]]; then
    add_issue "tests" "BLOCK" "OPS06_REGRESSION_TEST_FAIL" "tests_rc=${tests_rc}"
    PIPELINE_EXIT_CODE="${tests_rc}"
    exit "${tests_rc}"
  fi
fi

echo "[ops-stageb-p1-local-ci] result=PASS"
PIPELINE_EXIT_CODE=0
exit 0
