# Window C Handoff - MiniApp Contract Gate Pack

- Date: 2026-03-09
- Branch: feat/ui-four-account-reconcile-ops
- Scope: 契约层从“可读”升级到“可执行门禁”（API canonical + reserved-disabled gate + errorcode register增强）

## 1. 本批交付

1. 新增：`docs/contracts/2026-03-09-miniapp-release-api-canonical-list-v1.md`
- 将页面能力映射到明确 endpoint（无通配）。
- 每条 API 含：`method/path/请求关键字段/响应关键字段/错误码/降级语义`。
- 覆盖 P0 + P1/P2（含 reserved 场景）。

2. 新增：`docs/contracts/2026-03-09-miniapp-reserved-disabled-gate-spec-v1.md`
- 定义 `RESERVED_DISABLED` 码的开关、灰度、回滚规范。
- 定义“误返回即 P1”处置流程（SLA、RACI、审计字段）。

3. 更新：`docs/contracts/2026-03-09-miniapp-errorcode-canonical-register-v1.md`
- 新增字段：`severity/retryClass/userAction/opsAction`。
- 每个错误码显式标记 `failureMode`：`FAIL_OPEN` 或 `FAIL_CLOSE`。
- reserved-disabled 条目补全“生效条件/禁用态”。

## 2. 关键执行口径
- 冲突类/资金类统一 `FAIL_CLOSE`，禁止自动重试。
- 下游依赖类统一 `FAIL_OPEN`，主链路继续 + `warning/degraded` + 后台补偿。
- `RESERVED_DISABLED` 码在禁用态返回，一律视为配置异常并按 P1 处理。

## 3. 对 A/B/D 联调提示
- A（集成与门禁）：
  - 发布门禁应直接读取 canonical API 清单，禁止 wildcard 路径。
  - 审计字段固定：`runId/orderId/payRefundId/sourceBizNo/errorCode`。
- B（前端与交互）：
  - 严格按 `failureMode + retryClass` 驱动行为。
  - `FAIL_CLOSE` 不自动重试；`FAIL_OPEN` 必须展示 warning 且页面保活。
- D（监控与运营）：
  - 增加 reserved-disabled 误返回告警看板。
  - 误返回触发 P1 时，按 gate spec 执行 5/10/30 分钟处置时序。

## 4. 验证命令
1. `git diff --check`
2. `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh`
3. `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`
