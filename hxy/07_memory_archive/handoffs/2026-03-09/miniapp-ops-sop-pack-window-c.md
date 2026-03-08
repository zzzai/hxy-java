# Window C Handoff - MiniApp Ops SOP Pack

- Date: 2026-03-09
- Branch: feat/ui-four-account-reconcile-ops
- Scope: 客服升级处置 SOP + 运营配置执行 Playbook

## 1. 本批交付

1. `docs/products/miniapp/2026-03-09-miniapp-cs-sop-and-escalation-v1.md`
- 含错误码分级（P0/P1/P2）
- 含处理时限（首响/处置/升级/闭环）
- 含升级路径（L1/L2/on-call/值班负责人）
- 含回访闭环标准
- 动作表每条均含：触发条件、执行人、审计字段、验证标准

2. `docs/products/miniapp/2026-03-09-miniapp-operation-config-playbook-v1.md`
- 覆盖：活动上下架、库存阈值、降级开关、灰度策略、回滚步骤
- 对齐降级语义：pay 聚合降级、退款进度回退、ticket sync degrade、券领取超时保护、目录强制刷新
- 动作表每条均含：触发条件、执行人、审计字段、验证标准

## 2. 对齐说明
- 错误码口径严格对齐：`docs/contracts/2026-03-08-miniapp-errorcode-recovery-matrix-v1.md`
- 降级语义严格对齐：`docs/plans/2026-03-08-miniapp-degrade-retry-playbook-v1.md`
- 本批仅文档改动，不涉及业务代码与 overlay 页面改动。

## 3. 联调提示（A/B/D）
- A：统一审计字段透传与落库，确保 `runId/orderId/payRefundId/sourceBizNo/errorCode` 全链路可检索。
- B：前端严格按错误码和 `degraded/degradeReason` 驱动展示，避免按 message 分支。
- D：监控与巡检按 P0/P1/P2 维度建立看板，重点关注 `1030004012` 与 `TICKET_SYNC_DEGRADED` 趋势。

## 4. 验证命令
1. `git diff --check`
2. `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh`
3. `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`
