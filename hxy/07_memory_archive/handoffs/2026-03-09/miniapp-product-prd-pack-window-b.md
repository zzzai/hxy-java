# Window B Handoff - MiniApp Product PRD Pack（2026-03-09）

## 1. 交付范围
- 分支：`feat/ui-four-account-reconcile-ops`
- 本批交付：补齐 4 份产品 PRD（仅文档变更，不改 overlay、不改代码）。
- 对齐基线：
  - `docs/contracts/2026-03-08-miniapp-contract-freeze-v1.md`
  - `docs/contracts/2026-03-08-miniapp-unified-state-machine-v1.md`
  - `docs/contracts/2026-03-08-miniapp-errorcode-recovery-matrix-v1.md`

## 2. 新增文档清单
1. `docs/products/miniapp/2026-03-09-miniapp-home-growth-prd-v1.md`
2. `docs/products/miniapp/2026-03-09-miniapp-booking-schedule-prd-v1.md`
3. `docs/products/miniapp/2026-03-09-miniapp-member-asset-ledger-prd-v1.md`
4. `docs/products/miniapp/2026-03-09-miniapp-search-discovery-prd-v1.md`

## 3. 内容覆盖核对（四份 PRD 全部具备）
- 场景目标与非目标
- 页面信息架构与关键流程
- 状态机映射（显式引用统一状态机）
- 错误码与降级语义（沿用冻结语义，不反向改码）
- 埋点事件最小集
- 验收清单（happy path / 业务错误 / 降级路径）

## 4. 关键对齐点
- 错误码锚点语义保持不变：`1011000011/1011000100/1011000125/1011003004/1030004012/1030004016`。
- 降级统一 fail-open：`PAY_ORDER_NOT_FOUND`、`TICKET_SYNC_DEGRADED` 均按“主链路可用”执行。
- 事件口径沿用 miniapp analytics 基线（`page_view/order_submit/pay_result_view/coupon_take/point_activity_view`），按场景补最小扩展事件。

## 5. 后续建议（给窗口A）
1. 先以预约排期和会员资产台账 PRD 为开发优先级（直接对应 UI 审查缺口和客服成本）。
2. 首页增长与搜索发现先落埋点再做灰度优化，避免先做视觉而无可观测闭环。
3. 联调验收统一按“错误码优先、降级优先、文案次之”执行。
