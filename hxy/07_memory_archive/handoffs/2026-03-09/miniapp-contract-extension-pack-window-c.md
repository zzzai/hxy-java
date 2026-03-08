# Window C Handoff - MiniApp Contract Extension Pack

- Date: 2026-03-09
- Branch: feat/ui-four-account-reconcile-ops
- Scope: 后端契约补齐（冲突处理 / 礼品卡 / 邀请 / 技师动态）

## 1. 本批交付

新增 4 份契约文档：
1. `docs/contracts/2026-03-09-miniapp-addbook-conflict-spec-v1.md`
2. `docs/contracts/2026-03-09-miniapp-gift-card-domain-contract-v1.md`
3. `docs/contracts/2026-03-09-miniapp-referral-domain-contract-v1.md`
4. `docs/contracts/2026-03-09-miniapp-technician-feed-contract-v1.md`

每份文档均已包含：
- API 列表（路由、方法、请求、响应）
- 幂等键与冲突策略
- 错误码清单（稳定、可检索）
- fail-open / degrade 语义
- 审计字段要求：`runId/orderId/payRefundId/sourceBizNo/errorCode`
- 与现有契约兼容说明（向后兼容）

## 2. 错误码与兼容策略

- 本批未新增新的“数值错误码段”，优先复用现有稳定锚点：
  - `BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT(1030004012)`
  - `BOOKING_ORDER_REFUND_REPLAY_RUN_ID_NOT_EXISTS(1030004016)`
  - `ORDER_NOT_FOUND(1011000011)`
  - `USER_NOT_EXISTS(1004001000)`
  - `TECHNICIAN_NOT_EXISTS(1030001000)`
  - `TICKET_SYNC_DEGRADED`（warning tag）
- 对域内未来专属错误码，文档明确了“先映射后迁移”的兼容窗口策略（双写与别名映射）。

## 3. 联调建议（A/B/D）

- A（主流程编排）
  - 统一透传并落库/落日志字段：`runId/orderId/payRefundId/sourceBizNo/errorCode`。
  - 前端冲突分支可先按 `1030004012` 统一处理，后续再细分域专属冲突码。

- B（UI与交互）
  - 对新域接口默认按“可选字段”渲染：`degraded` / `degradeReason` 缺省时走正常态。
  - 对 warning tag `TICKET_SYNC_DEGRADED` 展示非阻断提示，不触发失败页。

- D（测试与门禁）
  - 回归重点：同键同参与同键异参两组用例，确保幂等命中与冲突可区分。
  - 校验日志检索字段完整性，缺失字段必须触发 `finance-log-validate` 告警。

## 4. 验证命令（本地）

1. `git diff --check`
2. `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh`
3. `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`
