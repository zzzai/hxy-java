#!/usr/bin/env bash
set -euo pipefail

OVERLAY="${1:-dev}"
BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TARGET_DIR="${BASE_DIR}/overlays/${OVERLAY}"

if [[ ! -d "${TARGET_DIR}" ]]; then
  echo "无效环境: ${OVERLAY}"
  exit 1
fi

kubectl delete -k "${TARGET_DIR}" --ignore-not-found
