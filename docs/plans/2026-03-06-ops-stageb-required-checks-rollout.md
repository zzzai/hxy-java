# Ops StageB/P1 Required Checks Rollout

## 1. 前置条件

1. 仓库与分支
   - 目标仓库可见且可访问（如 `zzzai/hxy-java`）。
   - 目标分支存在（默认 `main`）。
2. GitHub CLI 与认证
   - 本机可执行 `gh`。
   - `gh auth status` 可见有效账号。
   - Token 至少具备 `repo`，组织仓库建议补 `read:org`。
3. 脚本与 workflow 已在仓库
   - `ruoyi-vue-pro-master/script/dev/setup_github_required_checks.sh`
   - `ruoyi-vue-pro-master/script/dev/apply_ops_stageb_required_checks.sh`
   - `ruoyi-vue-pro-master/script/dev/rollback_ops_stageb_required_checks.sh`
   - `ruoyi-vue-pro-master/script/dev/check_booking_refund_notify_gate.sh`
   - `ruoyi-vue-pro-master/script/dev/check_booking_refund_audit_gate.sh`
   - `ruoyi-vue-pro-master/script/dev/check_booking_refund_replay_v2_gate.sh`
   - `ruoyi-vue-pro-master/script/dev/check_booking_refund_replay_runlog_gate.sh`
   - `ruoyi-vue-pro-master/script/dev/check_booking_refund_replay_run_summary_gate.sh`
   - `.github/workflows/ops-stageb-p1-guard.yml`

## 2. 启用（Dry Run + Apply）

### 2.1 Dry Run（只打印，不写入）

```bash
bash ruoyi-vue-pro-master/script/dev/setup_github_required_checks.sh \
  --dry-run \
  --include-stagea-checks 1 \
  --enable-ops-stageb-p1
```

说明：
- 未传 `--repo-owner/--repo-name` 时自动从 `git remote origin` 解析。
- 会输出可审计信息：`gh_dry_run_cmd`、`gh_apply_cmd`、payload 文件路径。
- 会输出运维辅助命令：`helper_setup_dry_run_cmd`、`helper_setup_apply_cmd`、`helper_rollback_cmd`。
- 检查集会显示 profile：
  - `base-only`
  - `stagea-only`
  - `stagea+stageb`

### 2.2 Apply（正式写入）

```bash
bash ruoyi-vue-pro-master/script/dev/setup_github_required_checks.sh \
  --apply \
  --include-stagea-checks 1 \
  --enable-ops-stageb-p1
```

或使用一键脚本（先 dry-run 再 apply，再打印分支保护摘要）：

```bash
bash ruoyi-vue-pro-master/script/dev/apply_ops_stageb_required_checks.sh
```

显式仓库：

```bash
bash ruoyi-vue-pro-master/script/dev/apply_ops_stageb_required_checks.sh \
  --repo-owner <owner> \
  --repo-name <repo> \
  --branch main
```

## 3. 回滚（仅移除 StageB，保留 StageA）

```bash
bash ruoyi-vue-pro-master/script/dev/rollback_ops_stageb_required_checks.sh
```

显式仓库：

```bash
bash ruoyi-vue-pro-master/script/dev/rollback_ops_stageb_required_checks.sh \
  --repo-owner <owner> \
  --repo-name <repo> \
  --branch main
```

说明：
- 回滚脚本会保留基础 checks + stageA checks，仅移除 `hxy-ops-stageb-p1-guard / ops-stageb-p1-guard`。
- 执行后会打印当前分支保护 required contexts 摘要。
- 建议回滚后立即执行 dry-run 再对比 contexts，确认 StageB checks 已移除。

## 4. 常见错误与处理

### 4.1 `gh auth status` 提示缺少 `read:org`

原因：
- 组织仓库或 SSO 场景下，token 未授权组织读取。

处理：
1. 执行 `gh auth refresh -h github.com -s read:org`。
2. 若启用 SSO，到组织授权页面完成授权。
3. 复核：

```bash
gh auth status
gh api /user/orgs
```

### 4.2 `Resource not accessible by integration` / 403

原因：
- 当前账号或 token 没有分支保护写权限。

处理：
1. 确认操作者对目标仓库有 admin 级权限。
2. 重新认证并重跑 dry-run 后再 apply。

### 4.3 `404 Not Found`（分支保护 API）

原因：
- owner/repo/branch 填写错误，或仓库可见性权限不足。

处理：

```bash
gh repo view <owner>/<repo> --json nameWithOwner,visibility,defaultBranchRef
gh api /repos/<owner>/<repo>/branches/main
```

### 4.4 运行环境缺少 `gh`

处理：
1. 安装 GitHub CLI 后重试；或
2. 在 `setup_github_required_checks.sh --apply` 场景使用 `GITHUB_TOKEN` 走 `curl` 回退路径。

### 4.5 StageB 本地门禁脚本报错（退款回调补偿 + 退款审计汇总）

若 `check_booking_refund_notify_gate.sh` 返回 `BLOCK`，优先检查以下锚点文件是否存在并包含预期片段：
- `sql/mysql/hxy/2026-03-06-hxy-booking-order-refund-notify-audit.sql`
- `ErrorCodeConstants` 中退款回调非法 ID / 幂等冲突错误码
- `AppBookingOrderController` 中 `/booking/order/update-refunded`
- `BookingOrderServiceImpl` 中退款回调幂等冲突分支

可单独执行：

```bash
bash ruoyi-vue-pro-master/script/dev/check_booking_refund_notify_gate.sh
```

若 `check_booking_refund_audit_gate.sh` 返回 `BLOCK`，优先检查以下锚点是否存在：
- `BookingRefundNotifyLogController` 中 `/booking/refund-notify-log/page` 与 `/booking/refund-notify-log/replay`
- `FourAccountReconcileController` 中 `/booking/four-account-reconcile/refund-audit-summary`
- `ErrorCodeConstants` 中 `1030004011 / 1030004013 / 1030004014`

可单独执行：

```bash
bash ruoyi-vue-pro-master/script/dev/check_booking_refund_audit_gate.sh
```

若 `check_booking_refund_replay_v2_gate.sh` 返回 `BLOCK`，优先检查以下锚点：
- replay request 支持 `ids + dryRun`（`BookingRefundNotifyLogReplayReqVO`）
- replay v2 返回体存在 `success/skip/fail` 汇总和 `details` 明细（`BookingRefundNotifyLogReplayRespVO`）
- `booking_refund_notify_log` 审计字段锚点可在 SQL/DO/Mapper 定位（如 `retry_count` / `next_retry_time`）
- `ErrorCodeConstants` 存在非法参数/状态冲突类锚点（`1030004011` / `1030004014`）

可单独执行：

```bash
bash ruoyi-vue-pro-master/script/dev/check_booking_refund_replay_v2_gate.sh
```

若 `check_booking_refund_replay_runlog_gate.sh` 返回 `BLOCK`，优先检查以下锚点：
- `/booking/refund-notify-log/replay-due` endpoint
- `/booking/refund-notify-log/replay-run-log/page|get` endpoint
- `hxy_booking_refund_replay_run_log` SQL 表及关键统计字段（`run_id/success_count/skip_count/fail_count`）
- service/job 侧 `runId + 统计字段` 锚点（自动重放链路）

可单独执行：

```bash
bash ruoyi-vue-pro-master/script/dev/check_booking_refund_replay_runlog_gate.sh
```

若 `check_booking_refund_replay_run_summary_gate.sh` 返回 `BLOCK`，优先检查以下锚点：
- `/booking/refund-notify-log/replay-run-log/summary` endpoint
- `/booking/refund-notify-log/replay-run-log/sync-tickets` endpoint
- service 层存在 replay-run summary 与 sync-tickets 方法实现
- service/test 侧可检索 fail-open / degrade 语义锚点
- 错误码/常量可检索（`BOOKING_ORDER_REFUND_REPLAY_RUN_ID_NOT_EXISTS` / `1030004016`、`TICKET_SYNC_DEGRADED`）

可单独执行：

```bash
bash ruoyi-vue-pro-master/script/dev/check_booking_refund_replay_run_summary_gate.sh
```

## 5. 快速检查命令

```bash
bash ruoyi-vue-pro-master/script/dev/setup_github_required_checks.sh --help
bash ruoyi-vue-pro-master/script/dev/setup_github_required_checks.sh --dry-run --enable-ops-stageb-p1
bash ruoyi-vue-pro-master/script/dev/check_booking_refund_audit_gate.sh
bash ruoyi-vue-pro-master/script/dev/check_booking_refund_replay_v2_gate.sh
bash ruoyi-vue-pro-master/script/dev/check_booking_refund_replay_runlog_gate.sh
bash ruoyi-vue-pro-master/script/dev/check_booking_refund_replay_run_summary_gate.sh
```

## 6. 巡检接口回归测试（退款-提成联调）

### 6.1 本地执行

`run_ops_stageb_p1_local_ci.sh` 默认已纳入退款审计门禁与巡检接口关键回归用例：
- `check_booking_refund_audit_gate.sh`（退款回调补偿 + 退款审计汇总锚点检查）
- `check_booking_refund_replay_v2_gate.sh`（退款回调补偿 V2 结构锚点检查）
- `check_booking_refund_replay_runlog_gate.sh`（退款重放运行台账 V3 锚点检查）
- `check_booking_refund_replay_run_summary_gate.sh`（退款重放汇总 + 工单同步锚点检查）
- `FourAccountReconcileServiceImplTest`
- `FourAccountReconcileControllerTest`
- `BookingOrderServiceImplTest`（退款回调一致性）
- `AppBookingOrderControllerTest`（退款回调入口与参数校验）

执行命令：

```bash
bash ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh --skip-mysql-init
```

若怀疑本地增量编译产物污染（例如偶发 `NoClassDefFoundError`、`Unresolved compilation problem`），可启用 clean 回归：

```bash
bash ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh \
  --skip-mysql-init \
  --clean-before-tests
```

推荐组合：
- 默认回归：`--skip-mysql-init`
- 疑似增量污染：`--skip-mysql-init --clean-before-tests`
- 紧急降级 replay-v2 门禁（仅短期）：`--skip-mysql-init --skip-booking-refund-replay-v2-gate`
- 紧急降级 replay-runlog 门禁（仅短期）：`--skip-mysql-init --skip-booking-refund-replay-runlog-gate`
- 紧急降级 replay-run-summary 门禁（仅短期）：`--skip-mysql-init --skip-booking-refund-replay-run-summary-gate`
- 临时软阻断 replay-run-summary 门禁（仅短期）：
  - `REQUIRE_BOOKING_REFUND_REPLAY_RUN_SUMMARY_GATE=0 bash ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh --skip-mysql-init`
- 环境变量显式关闭 replay-run-summary 门禁（单次）：
  - `RUN_BOOKING_REFUND_REPLAY_RUN_SUMMARY_GATE=0 bash ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh --skip-mysql-init`

如需临时降级退款审计门禁（不建议长期使用）：

```bash
bash ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh \
  --skip-mysql-init \
  --skip-booking-refund-audit-gate
```

如需临时降级 replay-v2 门禁（不建议长期使用）：

```bash
bash ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh \
  --skip-mysql-init \
  --skip-booking-refund-replay-v2-gate
```

如需临时降级 replay-runlog 门禁（不建议长期使用）：

```bash
bash ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh \
  --skip-mysql-init \
  --skip-booking-refund-replay-runlog-gate
```

如需临时降级 replay-run-summary 门禁（不建议长期使用）：

```bash
bash ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh \
  --skip-mysql-init \
  --skip-booking-refund-replay-run-summary-gate
```

如需临时覆盖回归测试集合，可通过环境变量：

```bash
REGRESSION_TEST_CLASSES=ProductStoreSkuControllerTest,ProductStoreServiceImplTest,AfterSaleReviewTicketServiceImplTest,BookingOrderServiceImplTest,AppBookingOrderControllerTest,FourAccountReconcileServiceImplTest,FourAccountReconcileControllerTest \
bash ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh --skip-mysql-init
```

### 6.2 CI 执行

`hxy-ops-stageb-p1-guard` workflow 在 `regression-tests` 步骤调用同一脚本与默认测试集合，确保本地与 CI 口径一致。
workflow summary 会输出 `clean_before_tests`、`booking_refund_replay_v2_gate_rc`、`booking_refund_replay_runlog_gate_rc` 与 `booking_refund_replay_run_summary_gate_rc`，用于排查 clean 口径和 replay 门禁状态。

## 7. 退款回调一致性回归用例

- 用例：`BookingOrderServiceImplTest`
- 目标：覆盖 booking 退款回调后的履约/状态一致性回归，避免“回调成功但状态未对齐”进入 StageB 发布路径。

本地单独执行示例：

```bash
mvn -f ruoyi-vue-pro-master/pom.xml \
  -pl yudao-module-mall/yudao-module-booking -am \
  -Dtest=BookingOrderServiceImplTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

CI 复现示例（与 workflow 一致的回归集合）：

```bash
REGRESSION_TEST_CLASSES=ProductStoreSkuControllerTest,ProductStoreServiceImplTest,AfterSaleReviewTicketServiceImplTest,BookingOrderServiceImplTest,AppBookingOrderControllerTest,FourAccountReconcileServiceImplTest,FourAccountReconcileControllerTest \
bash ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh --skip-mysql-init
```
