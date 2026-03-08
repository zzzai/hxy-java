# Window B Handoff - MiniApp Journey / Acceptance / Notification Pack（2026-03-09）

## 1. 交付范围
- 分支：`feat/ui-four-account-reconcile-ops`
- 本批交付：
  1. 用户旅程与服务蓝图
  2. 发布验收业务测试手册
  3. 通知触达策略
- 约束执行：仅新增文档，不改 overlay 页面、不改业务代码、不触碰 `.codex` 与历史 handoff。

## 2. 新增文件
1. `docs/products/miniapp/2026-03-09-miniapp-user-journey-service-blueprint-v1.md`
2. `docs/products/miniapp/2026-03-09-miniapp-release-acceptance-testbook-v1.md`
3. `docs/products/miniapp/2026-03-09-miniapp-notification-touchpoint-policy-v1.md`

## 3. 关键对齐点
- 旅程蓝图已覆盖：拉新->下单->预约->履约->售后->复购。
- 每个触点均标注：页面路由、关键动作、真值系统、失败降级、Owner、SLA。
- 门店侧强制动作已纳入：接单、排班、核销、客诉。
- 验收手册用业务场景组织，单条用例包含：前置条件、步骤、预期结果、错误码、降级预期、截图证据位。
- 通知策略明确：触发、频控、静默、退订、失败补发，以及“错误码 -> 提醒策略”映射。

## 4. 与A/C/D联动建议
- A（产品与前端）：严格按测试手册收集截图证据位，确保 P0 页面和高风险链路具备“成功+错误+降级”三类证据。
- C（运营与客服）：按通知策略和客服 SOP 对齐错误码分流，避免重复通知和误提醒。
- D（质量与发布）：将 AC-04/08/09/11/12/14 设为发布阻断用例，缺任一证据不允许放量。

## 5. 验证命令（本批要求）
1. `git diff --check`
2. `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh`
3. `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`
