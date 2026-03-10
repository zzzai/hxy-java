#!/usr/bin/env bash
set -euo pipefail
ROOT="${1:-.}"

SKILLS=(
  hxy-agent-workflow-router
  hxy-window-handoff-normalizer
  hxy-miniapp-capability-auditor
  hxy-miniapp-doc-freeze-closer
  hxy-release-gate-decider
  hxy-booking-contract-alignment
  hxy-member-domain-closer
  hxy-health-data-compliance-guard
)

DOCS=(
  "$ROOT/docs/plans/2026-03-10-hxy-agent-operating-system-design.md"
  "$ROOT/docs/plans/2026-03-10-hxy-agent-operating-system-implementation.md"
  "$ROOT/hxy/05_engineering/HXY-AI代理操作系统与项目技能编排-v1-2026-03-10.md"
)

printf '%s\n' '[skills]'
for skill in "${SKILLS[@]}"; do
  base="$ROOT/.codex/skills/$skill"
  if [[ -f "$base/SKILL.md" && -f "$base/agents/openai.yaml" ]]; then
    echo "[ok] $skill"
  else
    echo "[missing] $skill"
  fi
done

printf '\n%s\n' '[docs]'
for doc in "${DOCS[@]}"; do
  if [[ -f "$doc" ]]; then
    echo "[ok] $doc"
  else
    echo "[missing] $doc"
  fi
done

printf '\n%s\n' '[readme-link]'
rg -n 'HXY-AI代理操作系统与项目技能编排-v1-2026-03-10.md' "$ROOT/hxy/README.md" || true
