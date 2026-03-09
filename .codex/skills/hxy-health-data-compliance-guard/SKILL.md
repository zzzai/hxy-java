---
name: hxy-health-data-compliance-guard
description: Use when reviewing HXY face diagnosis, tongue diagnosis, health tags, biometric sensing, or any flow involving sensitive health-data collection, storage, labeling, recommendation, or marketing use.
---

# HXY Health Data Compliance Guard

## Overview
Guard the project against privacy, audit, and misuse failures in health-sensing and inferred health-tag workflows.

## Required Input
- Data-flow docs, event schemas, label rules, and audit requirements
- Any proposed fields, tags, prompts, storage paths, or API contracts touching health data
- Current HXY governance baseline for G0/G1/G2 and abstract-tag-only policy

## Workflow
1. Check whether the flow touches raw physiological signals, images, biometric traits, or inferred health tags.
2. Enforce the allowed boundary: abstract recommendation only, no raw parameter or image circulation.
3. Verify `userId` decoupling, encryption, purpose/consent, and audit-field coverage.
4. Reject G2-style diagnosis, discrimination, pricing, refusal, or hidden downgrade logic.
5. Require manual-review and gate logic for all G1 labels.
6. Emit compliance findings with risk level, blocked fields, and required remediation.

## Quick Run
```bash
bash .codex/skills/hxy-health-data-compliance-guard/scripts/scan_health_compliance.sh .
```

## Deliverables
- Compliance finding list by G0/G1/G2 level
- Blocked-field table
- Audit and storage remediation checklist

## Quality Gates
- No raw physiological data or images are approved for central plaintext storage.
- Marketing flows only receive abstract recommendations, never raw health signals.
- Any G1 use without consent, purpose, audit, and review is blocked.

## References
- `references/health-data-red-lines.md`
- `references/health-audit-template.md`
