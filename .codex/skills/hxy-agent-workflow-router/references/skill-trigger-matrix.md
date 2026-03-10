# Skill Trigger Matrix

| Trigger or task signal | Primary skill | Notes |
|---|---|---|
| 能力台账, Active/Planned, route truth, release matrix | `hxy-miniapp-capability-auditor` | capability truth first, then freeze/gate as needed |
| Ready/Frozen, 封版, 索引状态, 冻结评审 | `hxy-miniapp-doc-freeze-closer` | never freeze if route/API truth still drifts |
| Go/No-Go, 放量, 回滚, degraded, reserved-disabled | `hxy-release-gate-decider` | release scope must exclude reserved items |
| booking, technician, slot, addon, cancel, refund replay | `hxy-booking-contract-alignment` | use before claiming booking active/frozen |
| member, login, sign-in, address, wallet, asset ledger | `hxy-member-domain-closer` | keep missing pages and reserved APIs out of active scope |
| 面诊, 舌诊, 体征, 健康标签, G1, G2, 诊断 | `hxy-health-data-compliance-guard` | compliance findings can block freeze and release |
| A/B/C/D, handoff, 交接, 固定格式, 联调注意点 | `hxy-window-handoff-normalizer` | normalizes prompts and reports |
| 不确定该用哪个 skill | `hxy-agent-workflow-router` | router only routes; it does not replace specialists |
