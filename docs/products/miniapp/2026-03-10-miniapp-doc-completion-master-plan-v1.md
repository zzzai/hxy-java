# MiniApp 文档补齐总收口计划 v1（2026-03-10）

## 1. 目标
- 目标：把当前 miniapp 剩余文档缺口收敛成一份可执行的总计划，明确“先文档后开发”的顺序、Owner、依赖与冻结条件。
- 适用范围：仅覆盖小程序用户侧与后端 app API 相关的产品、契约、SOP、Runbook 文档，不涉及 overlay 页面和业务代码。
- 当前原则：
  1. 03-09 `Frozen` 基线不回退。
  2. 03-10 + 03-11 文档现已全部正式落盘，当前统一处于 `Ready` 评审层；是否进入 `Frozen Candidate` 仍取决于 route/API truth 与 scope gating。
  3. 任何业务开发、联调、灰度前，必须先具备对应域的产品文档、契约文档和最小运行文档。

## 2. 当前剩余阻断判断

### 2.1 P0（当前仍阻断进入 Frozen Candidate）
1. Booking / Technician Service
   - 真实前端 route 与后端 method/path 未完全对齐，当前仍存在假 `ACTIVE` 风险。
2. Member Account & Assets
   - 文档已经补齐，但 route truth 与 `ACTIVE / PLANNED_RESERVED / 缺页能力` 边界尚未完全落到页面实现上。
3. Reserved Expansion Activation
   - checklist 与 gray runbook 已补齐，但 gift/referral/feed 仍无真实 runtime 页面与 controller。

### 2.2 P1（文档已齐，等待范围与冻结评审）
1. Content / DIY / Customer Service
   - PRD、contract、SOP、验收矩阵、告警路由已齐，当前主要剩“是否纳入冻结候选”的范围判断。
2. Brokerage / Distribution
   - PRD、contract、runbook、验收矩阵、告警路由与独立客服 SOP 已齐，当前主要剩“是否纳入冻结候选”的范围判断。
3. Product / Search / Catalog
   - `search-lite` 与 `search-canonical` 已拆清，catalog interaction 文档与独立恢复 SOP 已齐，当前主要剩 capability scope 判断。
4. Marketing Expansion
   - PRD、contract、ops playbook、验收矩阵、告警路由已齐，但 contract 明确整域当前不直接升 `ACTIVE`。

## 3. 总体执行顺序（先文档后开发）
1. A：先收口单一真值
   - `member route truth`
   - `booking route + api truth`
   - 总收口主计划
2. B/C/D：补齐 content / brokerage / catalog / marketing / reserved activation 文档包
3. A：回填 capability ledger、coverage matrix、index、freeze-review、release decision、final review
4. 先完成冻结边界评审，再进入对应域开发或灰度动作

## 4. 03-10 文档交付与状态清单

| # | Priority | Document | Owner Window | 前置依赖 | 是否阻断开发 | 当前状态 | 说明 |
|---|---|---|---|---|---|---|---|
| 1 | P0 | `docs/products/miniapp/2026-03-10-miniapp-doc-completion-master-plan-v1.md` | A | 现有 coverage matrix / capability ledger | 否 | Ready | 本文；03-10 总计划与总入口 |
| 2 | P0 | `docs/products/miniapp/2026-03-10-miniapp-member-route-truth-and-active-planned-closure-v1.md` | A | 真实 uniapp route + member docs | 否 | Ready | member route truth 已收口 |
| 3 | P0 | `docs/products/miniapp/2026-03-10-miniapp-booking-route-api-truth-review-v1.md` | A | 真实 booking FE/BE path & method | 是 | Ready | booking 真值已审，但实现仍阻断 |
| 4 | P0 | `docs/contracts/2026-03-10-miniapp-booking-user-api-alignment-v1.md` | C | #3 | 否（文档已交付；实现仍阻断） | Ready | booking canonical contract 已交付 |
| 5 | P0 | `docs/products/miniapp/2026-03-10-miniapp-content-customer-service-prd-v1.md` | B | A 主计划 | 否 | Ready | 内容/客服业务规划已交付 |
| 6 | P0 | `docs/contracts/2026-03-10-miniapp-content-customer-service-contract-v1.md` | C | #5 | 否 | Ready | 内容/客服接口真值已交付 |
| 7 | P0 | `docs/products/miniapp/2026-03-10-miniapp-content-customer-service-sop-v1.md` | D | #5 #6 | 否 | Ready | 客服与运营处置闭环已交付 |
| 8 | P0 | `docs/products/miniapp/2026-03-10-miniapp-brokerage-distribution-prd-v1.md` | B | A 主计划 | 否 | Ready | 分销业务规划已交付 |
| 9 | P0 | `docs/contracts/2026-03-10-miniapp-brokerage-domain-contract-v1.md` | C | #8 | 否 | Ready | 分销契约真值已交付 |
| 10 | P0 | `docs/plans/2026-03-10-miniapp-brokerage-domain-runbook-v1.md` | D | #8 #9 | 否 | Ready | 分销运营/财务/客服运行手册已交付 |
| 11 | P1 | `docs/products/miniapp/2026-03-10-miniapp-product-catalog-interaction-prd-v1.md` | B | A 主计划 | 否 | Ready | 商品详情/收藏/浏览历史/评论/搜索联动文档已交付 |
| 12 | P1 | `docs/contracts/2026-03-10-miniapp-product-catalog-contract-v1.md` | C | #11 | 否 | Ready | catalog canonical path/field/errorCode 已交付 |
| 13 | P1 | `docs/plans/2026-03-10-miniapp-product-catalog-kpi-and-alerting-v1.md` | D | #11 #12 | 否 | Ready | catalog 监控与告警已交付 |
| 14 | P1 | `docs/products/miniapp/2026-03-10-miniapp-marketing-expansion-prd-v1.md` | B | A 主计划 | 否 | Ready | 营销扩展业务规则已交付 |
| 15 | P1 | `docs/contracts/2026-03-10-miniapp-marketing-expansion-contract-v1.md` | C | #14 | 否 | Ready | 营销扩展契约与错误码已交付 |
| 16 | P1 | `docs/products/miniapp/2026-03-10-miniapp-marketing-expansion-ops-playbook-v1.md` | D | #14 #15 | 否（阻断灰度/发布，不阻断文档） | Ready | 上下线、灰度、库存与回滚已交付 |
| 17 | P1 | `docs/plans/2026-03-10-miniapp-reserved-expansion-activation-checklist-v1.md` | D | #14 #15 + 03-09 Reserved docs | 否（阻断激活） | Ready | Reserved -> Active 门禁已交付 |
| 18 | P1 | `docs/plans/2026-03-10-miniapp-reserved-expansion-gray-acceptance-runbook-v1.md` | D | #17 | 否（阻断激活） | Ready | Reserved 域灰度验收已交付 |
| 19 | P0 | `docs/plans/2026-03-10-miniapp-domain-release-acceptance-matrix-v1.md` | D | #5-#18 | 否 | Ready | 跨域验收矩阵已交付 |
| 20 | P0 | `docs/plans/2026-03-10-miniapp-domain-alert-owner-routing-v1.md` | D | #5-#19 | 否 | Ready | 跨域告警路由与 owner/SLA 已交付 |
| 21 | P0 | `docs/products/miniapp/2026-03-10-miniapp-doc-completion-final-review-v1.md` | A | #1-#20 | 否 | Ready | A 侧终审文档；当前结论 `Frozen Candidate = 0` |
| 22 | P1 | `docs/products/miniapp/2026-03-11-miniapp-brokerage-customer-service-sop-v1.md` | A | #8 #9 #10 #19 #20 | 否 | Ready | 补齐分销域独立客服/资金解释 SOP，关闭“申请成功=到账成功”类文档缺口 |
| 23 | P1 | `docs/products/miniapp/2026-03-11-miniapp-product-catalog-customer-recovery-sop-v1.md` | A | #11 #12 #13 #19 #20 | 否 | Ready | 补齐商品目录与互动域独立恢复 SOP，关闭 `search-lite/canonical` 混算与互动伪成功类文档缺口 |

## 5. Ready / Frozen Candidate / Frozen 判定条件

### 5.1 Ready 判定条件
满足以下条件即可记为 `Ready`：
1. 文档主体已完整，结构可执行。
2. 关键字段、页面/路由、错误码、降级语义、Owner 已明确。
3. 已知阻断项已被显式写出，而不是省略。
4. 与本域相关的 PRD + contract + 最小 SOP/runbook 已正式提交。

### 5.2 Frozen Candidate 判定条件
满足以下条件才允许由 `Ready -> Frozen Candidate`：
1. 真实 route truth 已确认，不再使用原型 alias route。
2. 真实前端 API 与后端 controller 的 `method + path` 已对齐，或 contract 已明确 canonical truth 且不存在旧 path 阻断。
3. 错误码无 `TBD_*`，且不按 message 分支。
4. fail-open / fail-close / degraded / degradeReason 边界已与 contract、SOP、runbook、acceptance matrix 对齐。
5. capability ledger、coverage matrix、freeze review、release decision 已同步。
6. 03-09 Frozen 基线未被破坏。

### 5.3 Frozen 判定条件
1. 已通过 `Frozen Candidate` 评审。
2. A 已完成索引与 freeze review 回填。
3. 发布决策包未再命中 No-Go 触发条件。

## 6. 当前阻断开发规则

### 6.1 直接阻断开发
以下情况一律阻断对应域的功能开发或放量：
1. Booking 域旧 path/method 未收口即进入联调或发布。
2. Member 域缺页能力未落地却继续按已上线页面推进。
3. Reserved Expansion 在真实页面、controller、开关、验收样本未齐前直接激活。
4. 任何域把 `PLANNED_RESERVED / ACTIVE_BE_ONLY / BLOCKED` 写成 `ACTIVE`。

### 6.2 不阻断文档评审，但阻断发布/激活
1. Content / Brokerage / Catalog / Marketing Expansion 当前可以继续做冻结边界评审，但不得直接宣布进入正式发布范围。
2. Reserved Activation checklist / gray acceptance 已齐，但 gift/referral/feed 未实现前不得把 capability 状态升到 `ACTIVE`。

## 7. A/B/C/D 窗口后续顺序建议
1. A 先完成 03-10 B/C/D pack 终审集成。
2. C / FE 后续优先关闭 booking 真值漂移。
3. A / B / C / D 再决定 content / brokerage / catalog / marketing / reserved 是否出现首批 `Frozen Candidate`。
4. 只有冻结边界评审完成后，才进入对应域开发或灰度动作。

## 8. 当前结论
1. 03-10 + 03-11 当前文档缺口已经收敛到“核心文档全部正式落盘”的状态。
2. 当前问题已从“缺文档”转为“runtime truth 与冻结边界未闭环”。
3. 真正阻断当前后续开发的是：booking 旧 path/method 未定、member 缺页能力未落地、reserved runtime 未实现。
4. 后续文档工作将以补验收样本、补运行证据为主，而不再是补 PRD/contract/SOP 基础件。
