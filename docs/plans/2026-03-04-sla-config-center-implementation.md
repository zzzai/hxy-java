# SLA Config Center Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a table-driven SLA ticket-rule center for booking/trade with admin CRUD, preview matching, and runtime replacement of hardcoded routing.

**Architecture:** Introduce one shared SLA rule table in trade domain and expose match capability through trade-api so booking can reuse the same rule engine. Runtime matching follows strict precedence and falls back to hardcoded-safe defaults when no active rule exists.

**Tech Stack:** Spring Boot, MyBatis-Plus, Lombok, JUnit5/Mockito, MySQL migration SQL.

---

### Task 1: Define shared API contract and enums

**Files:**
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade-api/src/main/java/cn/iocoder/yudao/module/trade/api/ticketsla/TradeTicketSlaRuleApi.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade-api/src/main/java/cn/iocoder/yudao/module/trade/api/ticketsla/dto/TradeTicketSlaRuleMatchReqDTO.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade-api/src/main/java/cn/iocoder/yudao/module/trade/api/ticketsla/dto/TradeTicketSlaRuleMatchRespDTO.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade-api/src/main/java/cn/iocoder/yudao/module/trade/enums/ticketsla/TicketSlaRuleScopeTypeEnum.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade-api/src/main/java/cn/iocoder/yudao/module/trade/enums/ticketsla/TicketSlaRuleMatchLevelEnum.java`

**Steps:**
1. Add DTOs with required request and resolved output fields.
2. Add scope/match-level enums and utility methods.
3. Keep DTO neutral for both booking and trade use.

### Task 2: Add trade-domain persistence + service + controller

**Files:**
- Create DO/Mapper/Service/Impl/Convert/VO/Controller under trade `aftersale` package
- Modify error-code constants in trade-api

**Steps:**
1. Add rule DO with audit and matching fields.
2. Add mapper page/select/conflict helper methods.
3. Add service methods: page/create/update/updateStatus/previewMatch/matchRule.
4. Implement conflict validation by normalized scope key.
5. Add controller endpoints and converter.
6. Add API implementation bean for booking to call.

### Task 3: Replace hardcoded trade routing and connect booking

**Files:**
- Modify `AfterSaleReviewTicketServiceImpl`
- Modify `AfterSaleReviewTicketCreateReqBO` (if needed)
- Modify `TechnicianCommissionSettlementServiceImpl`

**Steps:**
1. Inject rule API/service into trade review ticket service.
2. Replace hardcoded `buildRoute` with DB resolution + fallback defaults.
3. Inject trade rule API into booking settlement service.
4. Use resolved defaults for submit/warn/escalate parameters, keep constants as final fallback.

### Task 4: SQL migration and seeds

**Files:**
- Create: `ruoyi-vue-pro-master/sql/mysql/hxy/2026-03-04-hxy-sla-config-center.sql`

**Steps:**
1. Create table and indexes idempotently.
2. Seed `GLOBAL_DEFAULT` + representative booking/trade defaults.
3. Ensure repeated execution does not break or duplicate.

### Task 5: Tests

**Files:**
- Modify/create trade service/controller tests
- Modify booking settlement service tests

**Steps:**
1. Add service tests for precedence `RULE > TYPE_SEVERITY > TYPE_DEFAULT > GLOBAL_DEFAULT`.
2. Add tests for conflict validation and enable/disable effect.
3. Add controller tests for page/create/update/status/preview.
4. Add booking tests proving rule-center override and fallback.

### Task 6: Governance + handoff

**Files:**
- Modify: `hxy/00_governance/HXY-项目事实基线-v1-2026-03-01.md`
- Modify: `hxy/00_governance/HXY-架构决策记录-ADR-v1.md`
- Modify: `hxy/06_roadmap/HXY-执行状态看板-v1-2026-03-01.md`
- Create: `hxy/07_memory_archive/handoffs/2026-03-04/sla-config-center-window-b.md`

**Steps:**
1. Record scope, decisions, and rollout status.
2. Add handoff sections required by memory governance.

### Task 7: Verification

**Steps:**
1. Run `git diff --check`.
2. Run naming guard command.
3. Run memory guard command.
4. Run requested Maven tests and capture summary.
