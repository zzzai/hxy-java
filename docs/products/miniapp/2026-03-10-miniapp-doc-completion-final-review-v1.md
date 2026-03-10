# MiniApp 文档总收口终审 v1（2026-03-10）

## 1. 目标
- 目标：对 03-10 当前分支所有已正式提交的文档做终审，给出 `Draft / Ready / Frozen Candidate / Still Blocked` 判定，并形成后续开发前的单一真值结论。
- 评审边界：
  - 03-09 `Frozen` 基线绝不回退。
  - 03-10 只吸收当前分支真实存在且已正式提交的文档；已提交文档之外的状态判断只作为历史说明。
  - 业务代码、overlay 页面、原型 alias route 不进入本次判定真值。

## 2. 状态定义

| 状态 | 判定口径 |
|---|---|
| Draft | 文档缺失，或尚未形成正式提交/固定格式回报 |
| Ready | 文档已提交并可作为单一真值输入，但 route/API/contract/runbook 尚未形成冻结候选闭环 |
| Frozen Candidate | 真实 route/API/contract/runbook 已全闭环，可进入下一轮 Frozen 评审 |
| Still Blocked | 文档虽已存在，但仍有实现真值漂移或缺页能力阻断，不能进入 Frozen Candidate |

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
| Booking | Still Blocked | `docs/products/miniapp/2026-03-09-miniapp-booking-schedule-prd-v1.md`; `docs/contracts/2026-03-09-miniapp-release-api-canonical-list-v1.md`; `docs/products/miniapp/2026-03-10-miniapp-booking-route-api-truth-review-v1.md`; `docs/contracts/2026-03-10-miniapp-booking-user-api-alignment-v1.md` | `GET /booking/technician/list-by-store`、`GET /booking/time-slot/list`、`PUT /booking/order/cancel`、`POST /booking/addon/create` 与后端真实 controller 不一致 | A/C | 前后端 method/path 真正收口，且旧 path 从联调与发布口径中移除后再重审 |
| Content / Customer Service | Ready | `docs/products/miniapp/2026-03-10-miniapp-content-customer-service-prd-v1.md`; `docs/contracts/2026-03-10-miniapp-content-customer-service-contract-v1.md`; `docs/products/miniapp/2026-03-10-miniapp-content-customer-service-sop-v1.md`; `docs/plans/2026-03-10-miniapp-domain-release-acceptance-matrix-v1.md`; `docs/plans/2026-03-10-miniapp-domain-alert-owner-routing-v1.md` | 当前 active scope 只到聊天、文章正文、FAQ 壳跳转、DIY 模板/自定义页；文章列表/分类/浏览量回写/已读回写仍是 `ACTIVE_BE_ONLY` 或 `PLANNED_RESERVED` | A/B/C/D | 若要进入 Frozen Candidate，需先确认内容域当前 scope 是否允许冻结到 Ready 集之外 |
| Brokerage | Ready | `docs/products/miniapp/2026-03-10-miniapp-brokerage-distribution-prd-v1.md`; `docs/contracts/2026-03-10-miniapp-brokerage-domain-contract-v1.md`; `docs/plans/2026-03-10-miniapp-brokerage-domain-runbook-v1.md`; `docs/plans/2026-03-10-miniapp-domain-release-acceptance-matrix-v1.md`; `docs/plans/2026-03-10-miniapp-domain-alert-owner-routing-v1.md` | 申请页、申诉页、撤回/取消提现页仍是缺页能力；当前 doc pack 只冻结真实八个页面与辅助 API | A/B/C/D | 若要进入 Frozen Candidate，需先确认分销域是否纳入当前发布范围，并保持缺页能力不被误升为 Active |
| Product / Search / Catalog | Ready | `docs/products/miniapp/2026-03-10-miniapp-product-catalog-interaction-prd-v1.md`; `docs/contracts/2026-03-10-miniapp-product-catalog-contract-v1.md`; `docs/plans/2026-03-10-miniapp-product-catalog-kpi-and-alerting-v1.md`; `docs/plans/2026-03-10-miniapp-domain-release-acceptance-matrix-v1.md`; `docs/plans/2026-03-10-miniapp-domain-alert-owner-routing-v1.md` | `search-lite` 与 `search-canonical` 已拆清，但 canonical search / catalog version guard 仍是 `BLOCKED`；评论/收藏/足迹链路保持 `PLANNED_RESERVED` | A/B/C/D | 若要进入 Frozen Candidate，需先确认现网 scope 是否只冻结 lite/browse/detail，或同步调整 capability ledger |
| Marketing Expansion | Ready | `docs/products/miniapp/2026-03-10-miniapp-marketing-expansion-prd-v1.md`; `docs/contracts/2026-03-10-miniapp-marketing-expansion-contract-v1.md`; `docs/products/miniapp/2026-03-10-miniapp-marketing-expansion-ops-playbook-v1.md`; `docs/plans/2026-03-10-miniapp-domain-release-acceptance-matrix-v1.md`; `docs/plans/2026-03-10-miniapp-domain-alert-owner-routing-v1.md` | 秒杀/拼团/满减送虽有真实页面与接口，但 contract 仍明确整域不直接升 `ACTIVE`；砍价仍是 `ACTIVE_BE_ONLY` | A/B/C/D | 若要进入 Frozen Candidate，需先确认营销扩展是否进入当前发布口径，而非继续停留在 `PLANNED_RESERVED` |
| Reserved Activation | Ready | `docs/products/miniapp/2026-03-09-miniapp-feature-priority-alignment-v1.md`; `docs/products/miniapp/2026-03-09-miniapp-gift-card-business-prd-v1.md`; `docs/products/miniapp/2026-03-09-miniapp-referral-business-prd-v1.md`; `docs/products/miniapp/2026-03-09-miniapp-technician-feed-product-policy-v1.md`; `docs/contracts/2026-03-09-miniapp-reserved-disabled-gate-spec-v1.md`; `docs/plans/2026-03-10-miniapp-reserved-expansion-activation-checklist-v1.md`; `docs/plans/2026-03-10-miniapp-reserved-expansion-gray-acceptance-runbook-v1.md`; `docs/plans/2026-03-10-miniapp-domain-release-acceptance-matrix-v1.md`; `docs/plans/2026-03-10-miniapp-domain-alert-owner-routing-v1.md` | gift/referral/feed 当前仍无真实 route/API/runtime 实现，治理文档不能替代实现闭环 | A/C/D | 仅当真实页面、真实 API、开关审批、灰度验证、误发布告警全部闭环时，才可评估下一轮 Frozen Candidate |

## 5. 本批 Frozen Candidate 判定
1. 当前结论：**03-10 本批新增 Frozen Candidate = 0**。
2. 直接原因：
   - Booking 仍有前后端 method/path 真值漂移。
   - Member 仍有缺页能力与 `PLANNED_RESERVED` API。
   - Content / Brokerage / Product-Catalog / Marketing-Expansion 虽然 doc pack 已闭环，但 contract 与 capability ledger 仍把关键能力固定在 `Ready / PLANNED_RESERVED / ACTIVE_BE_ONLY`，不直接进入 Frozen Candidate。
   - Reserved Activation 只有治理闭环，没有 runtime 闭环。

## 6. 文档级状态汇总
- 03-10 当前分支正式提交文档：`31 Ready`
- 03-10 当前分支待补正式文档：`0 Draft`
- 额外说明：
  - `docs/contracts/2026-03-09-miniapp-errorcode-canonical-register-v1.md` 已在当前分支补充 content / brokerage / catalog / marketing 相关错误码锚点。
  - 这属于 03-10 Ready 增量输入，不构成 03-09 Frozen 基线回退。

## 7. “先文档后开发”的最新执行顺序
1. A 已完成 03-10 B/C/D 文档整合集成与终审。
2. 下一步优先关闭 booking 真值漂移与 member 缺页能力边界。
3. 仅在 capability ledger、release decision 与 freeze review 重新评估后，才决定是否出现首批 `Frozen Candidate`。
4. 再之后才允许把对应域从“文档完备”推进到“开发或放量”。

## 8. 最终结论
1. 03-10 剩余文档缺口已经补齐到“全部正式落盘”的状态。
2. 当前剩余问题已经从“缺文档”转为“冻结边界和 runtime scope 管理”。
3. Booking 仍是唯一明确 `Still Blocked` 的域；其余新增域已经达到 `Ready`，但尚未进入 `Frozen Candidate`。
4. 03-09 `Frozen` 基线继续保持唯一冻结发布基线，03-10 仍停留在 `Ready` 评审层。
