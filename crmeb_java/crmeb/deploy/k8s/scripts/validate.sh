#!/usr/bin/env bash
set -euo pipefail

OVERLAY="${1:-dev}"
BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TARGET_DIR="${BASE_DIR}/overlays/${OVERLAY}"

if [[ ! -d "${TARGET_DIR}" ]]; then
  echo "无效环境: ${OVERLAY}"
  exit 1
fi

if ! command -v kubectl >/dev/null 2>&1; then
  echo "缺少 kubectl"
  exit 2
fi

if [[ ! -f "${BASE_DIR}/base/secret.yaml" ]]; then
  echo "缺少 ${BASE_DIR}/base/secret.yaml"
  echo "请先执行: cp ${BASE_DIR}/base/secret.example.yaml ${BASE_DIR}/base/secret.yaml"
  exit 3
fi

kubectl kustomize "${TARGET_DIR}" >/dev/null
echo "K8s 清单校验通过: ${TARGET_DIR}"
