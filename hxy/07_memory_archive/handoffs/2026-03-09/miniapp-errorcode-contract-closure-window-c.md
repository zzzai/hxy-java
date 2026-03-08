# Window C Handoff - MiniApp ErrorCode & Contract Closure

- Date: 2026-03-09
- Branch: feat/ui-four-account-reconcile-ops
- Scope: 错误码去占位符收口 + P1/P2 契约补完 + recovery matrix 补映射

## 1. 本批交付

1. 新增：`docs/contracts/2026-03-09-miniapp-errorcode-canonical-register-v1.md`
- 提供全量注册表字段：`code/name/domain/触发条件/前端动作/客服动作/重试策略`
- 补充 `status/生效条件/禁用态`，覆盖 ACTIVE 与 RESERVED_DISABLED
- 明确 fail-open / fail-close 判定规则
- 消除占位错误码，给出预留码段与启用条件

2. 新增：`docs/contracts/2026-03-09-miniapp-p1p2-contract-tbd-closure-v1.md`
- 覆盖 9 个场景：首页、项目页、加购冲突、预约排期、资产账本、搜索、礼品卡、邀请、技师 feed
- 每项包含：请求字段、响应字段、错误码、降级语义、向后兼容说明
- 对占位错误码完成映射到 canonical register 中的命名与编码

3. 更新：`docs/contracts/2026-03-08-miniapp-errorcode-recovery-matrix-v1.md`
- 增补 P1/P2 场景错误码恢复动作（含 reserved-disabled 条目）
- 对齐 `degrade-retry-playbook` 的重试与降级语义

## 2. 关键口径
- 冲突类（幂等/库存/时段）统一 fail-close，不自动重试。
- 下游依赖类（ticket sync / pay aggregate）按 fail-open，主链路成功 + warning/degraded。
- reserved-disabled 错误码默认禁用；在禁用态返回视为配置异常，按 P1 处理。

## 3. 对 A/B/D 联调提示
- A：链路编排与事件落库必须包含 `runId/orderId/payRefundId/sourceBizNo/errorCode`；reserved-disabled 返回时直接触发配置异常流程。
- B：前端仅按错误码/键处理，不按 message；`PAY_ORDER_NOT_FOUND` 必须 `WAITING + warning`，冲突码禁止自动重试。
- D：监控看板增加 reserved-disabled 错误码告警维度；重点盯 `1030004012`、`TICKET_SYNC_DEGRADED`、`1008009006` 趋势。

## 4. 验证命令
1. `git diff --check`
2. `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh`
3. `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`
