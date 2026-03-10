# MiniApp 文档总收口终审 v1（2026-03-10）

## 1. 目标
- 目标：对 03-10 当前分支已提交或已明确落盘的文档做最终终审，给出 `Draft / Ready / Frozen Candidate / Still Blocked` 判定，并形成后续开发前的单一真值结论。
- 评审边界：
  - 03-09 `Frozen` 基线绝不回退。
  - 03-10 只吸收当前分支真实存在的文档。
  - 未正式提交的窗口产出只能记为 `Draft` 或 `Pending formal window output`。
  - 业务代码、overlay 页面、原型 alias route 不进入本次判定真值。

## 2. 状态定义

| 状态 | 判定口径 |
|---|---|
| Draft | 文档缺失，或文件仅存在于工作树但未形成正式提交/固定格式回报 |
| Ready | 文档已提交并可作为单一真值输入，但 route/API/contract/runbook 尚未形成冻结候选闭环 |
| Frozen Candidate | 真实 route/API/contract/runbook 已全闭环，可进入下一轮 Frozen 评审 |
| Still Blocked | 已有部分文档或部分提交，但仍存在阻断项，不能进入 Frozen Candidate |

## 3. 03-09 Frozen Baseline（不纳入本批候选判定）

| 业务域 | 状态 | 说明 |
|---|---|---|
| Trade & Pay | Frozen | 03-09 已冻结，继续作为支付、订单、下单结算主链路基线 |
| After-sale & Refund | Frozen | 03-09 已冻结，继续作为售后/退款/回寄/退款进度主链路基线 |
| Coupon / Point / Home-Growth Core | Frozen | 03-09 已冻结，继续作为券、积分商城、首页增长主链路基线 |
| Gift / Referral / Technician Feed 规划包 | Frozen（规划基线） | 03-09 已冻结为 `P2/RB3-P2` 规划基线，不等于 runtime `ACTIVE` |

## 4. 03-10 终审结论（按业务域）

| 业务域 | 当前状态 | 最终单一真值引用 | 当前阻断项 | 责任窗口 | 解除条件 |
|---|---|---|---|---|---|
| Member | Ready | `docs/products/miniapp/2026-03-10-miniapp-member-domain-prd-v1.md`; `docs/products/miniapp/2026-03-10-miniapp-member-page-api-field-dictionary-v1.md`; `docs/products/miniapp/2026-03-10-miniapp-member-user-facing-errorcopy-v1.md`; `docs/contracts/2026-03-10-miniapp-member-domain-contract-v1.md`; `docs/contracts/2026-03-10-miniapp-active-vs-planned-api-matrix-v1.md`; `docs/plans/2026-03-10-miniapp-member-domain-kpi-and-alerts-v1.md`; `docs/plans/2026-03-10-miniapp-active-planned-gate-runbook-v1.md`; `docs/plans/2026-03-10-miniapp-member-domain-sla-routing-v1.md`; `docs/products/miniapp/2026-03-10-miniapp-member-route-truth-and-active-planned-closure-v1.md` | `/pages/user/level`、`/pages/profile/assets`、`/pages/user/tag` 仍无真实页面；`/member/asset-ledger/page` 仍是 `PLANNED_RESERVED` | A/B/C/D | 页面真实落地，route truth 全部回填到 PRD/contract，且资产总账接口形成真实 controller + 前端承接 |
| Booking | Still Blocked | `docs/products/miniapp/2026-03-09-miniapp-booking-schedule-prd-v1.md`; `docs/contracts/2026-03-09-miniapp-release-api-canonical-list-v1.md`; `docs/products/miniapp/2026-03-10-miniapp-booking-route-api-truth-review-v1.md` | `GET /booking/technician/list-by-store`、`GET /booking/time-slot/list`、`PUT /booking/order/cancel`、`POST /booking/addon/create` 与后端真实 controller 不一致 | A/C | C 正式提交 `docs/contracts/2026-03-10-miniapp-booking-user-api-alignment-v1.md`，且前后端 method/path 统一后再重审 |
| Content / Customer Service | Still Blocked | `docs/products/miniapp/2026-03-10-miniapp-content-customer-service-sop-v1.md`; `docs/products/miniapp/2026-03-10-miniapp-doc-completion-master-plan-v1.md`; `docs/contracts/2026-03-10-miniapp-content-customer-service-contract-v1.md`（Pending formal window output） | B 侧 PRD缺失；C 侧 contract 虽已落盘到工作树但未正式提交；错误码/降级/发布口径仍未形成正式闭环 | B/C/D | B 提交 content PRD，C 正式提交 contract，并与 SOP、errorcode、degrade 口径对齐 |
| Brokerage | Still Blocked | `docs/plans/2026-03-10-miniapp-brokerage-domain-runbook-v1.md`; `docs/products/miniapp/2026-03-10-miniapp-doc-completion-master-plan-v1.md` | 仅有 D 侧 runbook；B 侧 PRD与 C 侧 contract 均缺正式提交 | B/C/D | B 提交分销 PRD，C 提交 contract，runbook 回填字段/错误码/降级引用 |
| Product / Search / Catalog | Still Blocked | `docs/products/miniapp/2026-03-09-miniapp-search-discovery-prd-v1.md`; `docs/plans/2026-03-10-miniapp-product-catalog-kpi-and-alerting-v1.md`; `docs/products/miniapp/2026-03-10-miniapp-doc-completion-master-plan-v1.md` | `search-lite` 与 `search-canonical` 仍未被独立 PRD/contract 收口；商品详情/收藏/浏览历史/评论缺产品文档 | B/C/D | B 提交 catalog interaction PRD，C 提交 contract，并把 `search-lite` / `search-canonical` 切成独立真值 |
| Marketing Expansion | Still Blocked | `docs/products/miniapp/2026-03-10-miniapp-marketing-expansion-ops-playbook-v1.md`; `docs/products/miniapp/2026-03-10-miniapp-doc-completion-master-plan-v1.md` | 只有 D 侧 ops playbook 已提交；B PRD、C contract 均未正式交付 | B/C/D | B 提交 PRD，C 提交 contract，活动上下线/灰度/库存/回滚口径与 ops playbook 对齐 |
| Reserved Activation | Ready | `docs/products/miniapp/2026-03-09-miniapp-feature-priority-alignment-v1.md`; `docs/products/miniapp/2026-03-09-miniapp-gift-card-business-prd-v1.md`; `docs/products/miniapp/2026-03-09-miniapp-referral-business-prd-v1.md`; `docs/products/miniapp/2026-03-09-miniapp-technician-feed-product-policy-v1.md`; `docs/contracts/2026-03-09-miniapp-reserved-disabled-gate-spec-v1.md`; `docs/plans/2026-03-10-miniapp-reserved-expansion-activation-checklist-v1.md`; `docs/plans/2026-03-10-miniapp-reserved-expansion-gray-acceptance-runbook-v1.md` | 激活治理文档已落盘，但 gift/referral/feed 当前仍无真实 route/API 实现，不能把治理 Ready 误判为 runtime Active | A/C/D | 仅当真实页面、真实 API、开关审批、灰度验证、误发布告警全部闭环时，才可评估下一轮 Frozen Candidate |

## 5. 本批 Frozen Candidate 判定
1. 当前结论：**03-10 本批新增 Frozen Candidate = 0**。
2. 原因：
   - Member 仍有缺页能力与 `PLANNED_RESERVED` API。
   - Booking 仍有前后端 method/path 真值漂移。
   - Content / Brokerage / Catalog / Marketing Expansion 至少缺一侧正式 PRD 或 contract。
   - Reserved Activation 当前只是治理文档 `Ready`，并不代表 gift/referral/feed runtime 已闭环。

## 6. 文档级 Draft / Ready 明细

| 文档 | 当前状态 | 说明 |
|---|---|---|
| `docs/contracts/2026-03-10-miniapp-booking-user-api-alignment-v1.md` | Draft | 文件已存在于工作树，但未形成正式提交与窗口回报 |
| `docs/contracts/2026-03-10-miniapp-content-customer-service-contract-v1.md` | Draft | 文件已存在于工作树，但未形成正式提交与窗口回报 |
| `docs/products/miniapp/2026-03-10-miniapp-content-customer-service-sop-v1.md` | Ready | 已由 D 正式提交，可作为终审输入 |
| `docs/plans/2026-03-10-miniapp-brokerage-domain-runbook-v1.md` | Ready | 已由 D 正式提交，可作为终审输入 |
| `docs/plans/2026-03-10-miniapp-product-catalog-kpi-and-alerting-v1.md` | Ready | 已由 D 正式提交，可作为终审输入 |
| `docs/products/miniapp/2026-03-10-miniapp-marketing-expansion-ops-playbook-v1.md` | Ready | 已由 D 正式提交，可作为终审输入 |
| `docs/plans/2026-03-10-miniapp-reserved-expansion-activation-checklist-v1.md` | Ready | 已由 D 正式提交，可作为终审输入 |
| `docs/plans/2026-03-10-miniapp-reserved-expansion-gray-acceptance-runbook-v1.md` | Ready | 已由 D 正式提交，可作为终审输入 |

## 7. “先文档后开发”的最终执行顺序
1. C 正式提交 booking user API alignment contract。
2. B 正式提交 content / customer-service PRD。
3. C 正式提交 content / customer-service contract。
4. B 正式提交 brokerage PRD。
5. C 正式提交 brokerage contract。
6. B 正式提交 product-catalog interaction PRD。
7. C 正式提交 product-catalog contract。
8. B 正式提交 marketing-expansion PRD。
9. C 正式提交 marketing-expansion contract。
10. A 统一回填 index、freeze review、ledger、coverage、release decision，并重新判定是否出现首批 `Frozen Candidate`。
11. 只有文档闭环后，才允许进入对应业务域开发或扩量动作。

## 8. 最终结论
1. 03-09 `Frozen` 基线保持不变，仍是当前唯一冻结发布基线。
2. 03-10 已有大量 `Ready` 输入，但本批没有新增 `Frozen Candidate`。
3. Member 与 Reserved Activation 是最接近下一轮冻结评审的两个方向，但仍不能越过当前真实实现边界。
4. Booking / Content / Brokerage / Catalog / Marketing Expansion 仍属于“文档先行但未闭环”的阻断域。
