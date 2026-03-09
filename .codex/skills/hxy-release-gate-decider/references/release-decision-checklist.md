# Release Decision Checklist

Required inputs:
- capability ledger
- doc coverage matrix
- release decision pack
- consistency audit
- degraded-pool governance
- release-gate KPI runbook
- reserved-disabled gate spec

Decision rules:
- `Go`: no P0 blockers, no fake active, no reserved leakage, gates healthy
- `Go with Gate`: release possible but with explicit watchpoints and rollback threshold
- `No-Go`: unresolved truth drift, KPI gate failure, reserved leakage, or frozen boundary violation
