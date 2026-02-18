#!/usr/bin/env bash
set -euo pipefail

OVERLAY="${1:-dev}"
BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TARGET_DIR="${BASE_DIR}/overlays/${OVERLAY}"

bash "$(dirname "${BASH_SOURCE[0]}")/validate.sh" "${OVERLAY}"
kubectl apply -k "${TARGET_DIR}"
