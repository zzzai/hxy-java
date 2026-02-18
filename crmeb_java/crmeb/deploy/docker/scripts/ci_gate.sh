#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
CRMEB_DIR="$(cd "${BASE_DIR}/../.." && pwd)"

FULL_MODE=0
INIT_DB=0
DB_MODE="strict"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --full)
      FULL_MODE=1
      shift
      ;;
    --init-db)
      INIT_DB=1
      shift
      ;;
    --db-mode)
      DB_MODE="${2:-}"
      shift 2
      ;;
    *)
      echo "未知参数: $1"
      echo "用法: ./scripts/ci_gate.sh [--full] [--init-db] [--db-mode strict|replay]"
      exit 1
      ;;
  esac
done

if [[ "${DB_MODE}" != "strict" && "${DB_MODE}" != "replay" ]]; then
  echo "无效 --db-mode: ${DB_MODE}"
  exit 1
fi

echo "[1/4] bash 语法校验..."
bash -n "${SCRIPT_DIR}"/*.sh
bash -n "${CRMEB_DIR}/shell/data_governance_smoke.sh"
echo "[OK] 语法校验通过"

echo "[2/4] 运行 preflight..."
bash "${SCRIPT_DIR}/preflight.sh"

if [[ "${INIT_DB}" == "1" ]]; then
  echo "[3/5] 初始化数据库（mode=${DB_MODE}）..."
  bash "${SCRIPT_DIR}/db_init.sh" --mode "${DB_MODE}" --drop-lowercase-duplicates
fi

echo "[4/5] 运行容器发布联调..."
if [[ "${FULL_MODE}" == "1" ]]; then
  bash "${SCRIPT_DIR}/release_smoke.sh" --skip-preflight
else
  bash "${SCRIPT_DIR}/release_smoke.sh" --skip-build --skip-preflight
fi

echo "[5/5] 输出容器状态..."
bash "${SCRIPT_DIR}/ps.sh"

echo "CI 上线拦截规则执行完成。"
