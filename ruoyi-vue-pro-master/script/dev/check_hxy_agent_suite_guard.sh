#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
PROJECT_ROOT="${REPO_ROOT}"

if [[ -f "${REPO_ROOT}/.codex/skills/hxy-agent-workflow-router/scripts/check_hxy_agent_suite.sh" ]]; then
  PROJECT_ROOT="${REPO_ROOT}"
elif [[ -f "${REPO_ROOT}/../.codex/skills/hxy-agent-workflow-router/scripts/check_hxy_agent_suite.sh" ]]; then
  PROJECT_ROOT="$(cd "${REPO_ROOT}/.." && pwd)"
else
  echo "[hxy-agent-suite-guard][FAIL] missing suite checker under ${REPO_ROOT} or parent" >&2
  exit 1
fi

bash "${PROJECT_ROOT}/.codex/skills/hxy-agent-workflow-router/scripts/check_hxy_agent_suite.sh" "${PROJECT_ROOT}"
