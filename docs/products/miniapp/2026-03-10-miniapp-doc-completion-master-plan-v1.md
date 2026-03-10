# MiniApp 文档补齐总收口计划 v1（2026-03-10）

## 1. 目标
- 目标：把当前 miniapp 剩余文档缺口收敛成一份可执行的总计划，明确“先文档后开发”的顺序、Owner、依赖与冻结条件。
- 适用范围：仅覆盖小程序用户侧与后端 app API 相关的产品、契约、SOP、Runbook 文档，不涉及 overlay 页面和业务代码。
- 当前原则：
  1. 03-09 `Frozen` 基线不回退。
  2. 03-10 新增文档先收口到 `Draft / Ready`，待 route/API truth 和跨窗口文档闭环后再决定是否进入 `Frozen`。
  3. 任何业务开发、联调、灰度前，必须先具备对应域的产品文档、契约文档和最小运行文档。

## 2. 当前剩余缺口判断

### 2.1 P0（先补再开发）
1. Booking / Technician Service
   - 真实前端 route 与后端 method/path 未完全对齐，当前存在假 `ACTIVE` 风险。
2. Member Account & Assets
   - 文档已经补齐，但 route truth 与 `ACTIVE / PLANNED_RESERVED / 缺页能力` 边界尚未收口。
3. Content / DIY / Customer Service
   - 代码与页面存在，但缺 PRD、contract、error/degrade、runbook 闭环。
4. Brokerage / Distribution
   - 页面与后端能力存在，但整域缺产品、契约、错误码和运行文档。

### 2.2 P1（P0 完成后补）
1. Product / Search / Catalog
   - `search-lite` 与 canonical search 仍未拆清；商品详情/收藏/浏览历史/评论没有完整产品文档包。
2. Marketing Expansion
   - 拼团、秒杀、砍价、满减送等活动仍缺业务规则、contract、运营 playbook。
3. Reserved Expansion Activation
   - gift-card / referral / technician-feed 文档已齐，但缺从 `PLANNED_RESERVED -> ACTIVE` 的切换 checklist 与灰度验收 runbook。

## 3. 总体执行顺序（先文档后开发）
1. A：先收口单一真值
   - `member route truth`
   - `booking route + api truth`
   - 总收口主计划
2. C：补 contract / canonical path / errorCode 真值
3. B：补业务 PRD / 字段 / 用户恢复动作
4. D：补 SOP / Runbook / KPI / 灰度与回滚治理
5. A：汇总 B/C/D 输出，回填 capability ledger、coverage matrix、index、freeze-review
6. 仅当文档达到对应 `Ready/Frozen` 条件后，才允许进入开发任务。

## 4. 剩余文档缺口清单

| # | Priority | Document | Owner Window | 前置依赖 | 是否阻断开发 | 当前状态 | 说明 |
|---|---|---|---|---|---|---|---|
| 1 | P0 | `docs/products/miniapp/2026-03-10-miniapp-doc-completion-master-plan-v1.md` | A | 现有 coverage matrix / capability ledger | 是 | Ready | 本文；所有后续文档补齐的主计划 |
| 2 | P0 | `docs/products/miniapp/2026-03-10-miniapp-member-route-truth-and-active-planned-closure-v1.md` | A | 真实 uniapp route + member docs | 是 | Ready | 先定 member route truth，避免继续误写 alias route |
| 3 | P0 | `docs/products/miniapp/2026-03-10-miniapp-booking-route-api-truth-review-v1.md` | A | 真实 booking FE/BE path & method | 是 | Ready | 先确认 booking 真值，后续 contract 才能冻结 |
| 4 | P0 | `docs/contracts/2026-03-10-miniapp-booking-user-api-alignment-v1.md` | C | #3 | 是 | Draft | Pending window output；booking canonical contract |
| 5 | P0 | `docs/products/miniapp/2026-03-10-miniapp-content-customer-service-prd-v1.md` | B | A 主计划 | 是 | Draft | Pending window output；内容/客服业务规划 |
| 6 | P0 | `docs/contracts/2026-03-10-miniapp-content-customer-service-contract-v1.md` | C | #5 | 是 | Draft | Pending window output；内容/客服接口真值 |
| 7 | P0 | `docs/products/miniapp/2026-03-10-miniapp-content-customer-service-sop-v1.md` | D | #5 #6 | 是 | Draft | Pending window output；客服与运营处置闭环 |
| 8 | P0 | `docs/products/miniapp/2026-03-10-miniapp-brokerage-distribution-prd-v1.md` | B | A 主计划 | 是 | Draft | Pending window output；分销业务规划 |
| 9 | P0 | `docs/contracts/2026-03-10-miniapp-brokerage-domain-contract-v1.md` | C | #8 | 是 | Draft | Pending window output；分销契约真值 |
| 10 | P0 | `docs/plans/2026-03-10-miniapp-brokerage-domain-runbook-v1.md` | D | #8 #9 | 是 | Draft | Pending window output；分销运营/财务/客服运行手册 |
| 11 | P1 | `docs/products/miniapp/2026-03-10-miniapp-product-catalog-interaction-prd-v1.md` | B | A 主计划 | 是（对应域） | Draft | Pending window output；商品详情/收藏/浏览历史/评论/搜索联动 |
| 12 | P1 | `docs/contracts/2026-03-10-miniapp-product-catalog-contract-v1.md` | C | #11 | 是（对应域） | Draft | Pending window output；catalog canonical path/field/errorCode |
| 13 | P1 | `docs/plans/2026-03-10-miniapp-product-catalog-kpi-and-alerting-v1.md` | D | #11 #12 | 否（阻断发布，不阻断纯文档讨论） | Draft | Pending window output；catalog 监控与告警 |
| 14 | P1 | `docs/products/miniapp/2026-03-10-miniapp-marketing-expansion-prd-v1.md` | B | A 主计划 | 是（对应域） | Draft | Pending window output；营销扩展业务规则 |
| 15 | P1 | `docs/contracts/2026-03-10-miniapp-marketing-expansion-contract-v1.md` | C | #14 | 是（对应域） | Draft | Pending window output；营销扩展契约与错误码 |
| 16 | P1 | `docs/products/miniapp/2026-03-10-miniapp-marketing-expansion-ops-playbook-v1.md` | D | #14 #15 | 否（阻断灰度/发布） | Draft | Pending window output；上下线、灰度、库存与回滚 |
| 17 | P1 | `docs/plans/2026-03-10-miniapp-reserved-expansion-activation-checklist-v1.md` | D | #14 #15 + 03-09 Reserved docs | 否（阻断激活） | Draft | Pending window output；Reserved -> Active 门禁 |
| 18 | P1 | `docs/plans/2026-03-10-miniapp-reserved-expansion-gray-acceptance-runbook-v1.md` | D | #17 | 否（阻断激活） | Draft | Pending window output；Reserved 域灰度验收 |

## 5. Ready / Frozen 判定条件

### 5.1 Ready 判定条件
满足以下条件即可记为 `Ready`：
1. 文档主体已完整，结构可执行。
2. 关键字段、页面/路由、错误码、降级语义、Owner 已明确。
3. 已知阻断项已被显式写出，而不是省略。
4. 对于尚未交付的跨窗口依赖，已在索引中占位并标记 `Pending window output`。

### 5.2 Frozen 判定条件
满足以下条件才允许由 `Ready -> Frozen`：
1. 真实 route truth 已确认，不再使用原型 alias route。
2. 真实前端 API 与后端 controller 的 `method + path` 已对齐或 contract 已明确 canonical truth。
3. 错误码无 `TBD_*`，且不按 message 分支。
4. fail-open / fail-close / degraded / degradeReason 边界已与 contract、SOP、runbook 对齐。
5. 索引、capability ledger、coverage matrix、freeze review 已同步。
6. 03-09 Frozen 基线未被破坏。

## 6. 当前阻断开发规则

### 6.1 直接阻断开发
以下情况一律阻断对应域开发：
1. Booking 域真值未收口即进入接口改造或联调。
2. Member 域 route truth 未收口即继续写页面/验收口径。
3. Content / Brokerage 域在没有 PRD + contract 前进入业务开发。
4. Catalog / Marketing 扩展域在没有 PRD + contract 前进入功能开发。

### 6.2 不阻断文档补齐，但阻断发布/激活
1. KPI / alerting / playbook / runbook 缺失时，可继续补文档，但不得发布。
2. Reserved Activation checklist / gray acceptance 缺失时，不得把 gift/referral/feed 从 `PLANNED_RESERVED` 升到 `ACTIVE`。

## 7. A/B/C/D 窗口交付顺序建议
1. A 先交付 #1/#2/#3。
2. C 紧接 #4，给 booking 生成可执行 contract 真值。
3. B/C/D 并行完成 content / brokerage / catalog / marketing / reserved expansion 文档包。
4. A 最后做索引、台账、freeze review 二次收口。

## 8. 当前结论
1. 03-10 本批不能直接追求 Frozen，先把单一真值与剩余缺口盘清楚。
2. 真正阻断当前后续开发的是：booking 真值未定、member route truth 未定、content/brokerage 无完整文档包。
3. 本计划是后续所有窗口文档任务的总入口；未在本计划登记的文档，不应直接进入开发排期。
