# SLA Config Center Design (Window B)

## Context
- Task scope is limited to booking/trade domains.
- Existing SLA routing is hardcoded in trade review ticket service (`buildRoute`).
- Booking settlement workflow still uses hardcoded SLA defaults.
- Goal is centralized, table-driven SLA rule resolution with management APIs and repeatable SQL migration.

## Requirements Digest
- Rule dimensions: ticket type + severity + store scope.
- Rule controls: enabled status + priority.
- Match precedence: `RULE > TYPE_SEVERITY > TYPE_DEFAULT > GLOBAL_DEFAULT`.
- Runtime behavior: "config first + default fallback".
- Governance: update baseline/ADR/roadmap + handoff.

## Option Comparison
1. Keep separate rule tables in booking and trade.
- Pros: local autonomy.
- Cons: duplicated logic, inconsistent precedence.

2. Unified table + unified resolver service in trade module, exposed to booking via trade-api (Recommended).
- Pros: single source of truth, strict precedence consistency, minimum cross-module coupling.
- Cons: booking adds dependency on trade-api contract usage.

3. Pure YAML with admin sync job.
- Pros: low DB complexity.
- Cons: weak auditability/conflict control, poor runtime previewability.

## Chosen Design
- Add table `trade_ticket_sla_rule` for both booking/trade.
- Implement resolver service in trade module:
  - Filters only enabled rules under tenant + scope (store/global).
  - Computes match level and sorts by:
    - level rank: `RULE(1) < TYPE_SEVERITY(2) < TYPE_DEFAULT(3) < GLOBAL_DEFAULT(4)`
    - scope precision: store-specific before global
    - priority desc
    - id desc
- Provide admin APIs:
  - page
  - create
  - update
  - update status (enable/disable)
  - match preview
- Add conflict validation: same scope key cannot duplicate.

## Rule Model
- Core keys:
  - `ticket_type`
  - `rule_code` (optional)
  - `severity` (optional)
  - `scope_type` (`GLOBAL` / `STORE`)
  - `scope_store_id`
- Rule outputs:
  - `escalate_to`
  - `sla_minutes`
  - `warn_lead_minutes`
  - `escalate_delay_minutes`
  - `priority`
  - `enabled`
- Audit fields:
  - `creator`, `updater`, `last_action`, `last_action_at`

## Runtime Integration
- Trade `AfterSaleReviewTicketServiceImpl`
  - Replace hardcoded `buildRoute` with resolver.
  - Preserve safety fallback if no DB match.
- Booking `TechnicianCommissionSettlementServiceImpl`
  - Resolve submit/warn/escalate defaults from rule center by `ticket_type=BOOKING_SETTLEMENT`.
  - Keep current constants as final fallback.

## Testing Strategy
- Service tests (trade): precedence, conflict validation, status effect.
- Controller tests (trade): page/create/update/status/preview.
- Booking service tests: verify rule-center override and fallback behavior.

## SQL Strategy
- `CREATE TABLE IF NOT EXISTS`.
- `ALTER TABLE ... ADD COLUMN/INDEX IF NOT EXISTS` where needed.
- Seed `GLOBAL_DEFAULT` plus booking/trade defaults.
- Script must be idempotent.
