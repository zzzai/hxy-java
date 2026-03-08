# Window C Handoff - Ops StageB Finance Release Gate

- Date: 2026-03-08
- Branch: feat/ui-four-account-reconcile-ops
- Scope: finance partial closure gate release hardening / stageB local-ci / required-check helper / workflow summary / rollout docs

## 1. Delivered

1. `check_finance_partial_closure_gate.sh` 强化为发布收口门禁（PASS/BLOCK/WARN + summary/tsv）
- 退款回调锚点：
  - `BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT (1030004012)`
  - 退款回调幂等冲突 throw 与 same-refund-id 分支
- 退款 replay 锚点：
  - controller: `/replay`、`/replay-due`、`/replay-run-log/summary`、`/replay-run-log/sync-tickets`
  - service: `replayFailedLogs`、`replayDueFailedLogs`、`getReplayRunLogSummary`、`syncReplayRunLogTickets`
  - error code: `BOOKING_ORDER_REFUND_REPLAY_RUN_ID_NOT_EXISTS (1030004016)`
  - fail-open/degrade 语义：`fail-open continue`、`refresh failed, degrade continue`
- 提成计提/冲正锚点：
  - 计提：`COMMISSION_ACCRUAL_IDEMPOTENT_CONFLICT (1030007012)`、`ACCRUAL_BIZ_TYPE`、`buildAccrualSourceBizNo`、`ensureAccrualPayloadConsistent`
  - 冲正：`COMMISSION_REVERSAL_IDEMPOTENT_CONFLICT (1030007011)`、`buildReversalSourceBizNo`、`ensureReversalPayloadConsistent`
  - mapper/冲突语义：`selectByOriginCommissionId`、`selectByBizKey`、`DuplicateKeyException`
- 冲正唯一约束锚点：`origin_commission_id` + `uk_origin_commission_id`
- 四账提成聚合锚点：
  - summary/refund-summary 字段：`tradeMinusCommissionSplitSum`、`commissionAmountSum`、`commissionDifferenceAbsSum`、`differenceAmountSum`、`unresolvedTicketCount`、`ticketSummaryDegraded`
  - service 聚合与赋值：trade-minus-commission / commission / differenceAmount / unresolvedTicketCount
  - fail-open/degrade：`syncRefundCommissionAuditTickets`、`catch (Exception ex)`、`failedOrderIds.add(...)`、`load ticket summary degrade`
- 审计增强（WARN，不阻断）：新增 replay 与 technician commission 测试锚点扫描。

2. `run_ops_stageb_p1_local_ci.sh` finance gate 失败码语义明确
- finance gate rc 处理语义增强：
  - `rc=0` => gate `PASS_OR_WARN`
  - `rc=2 && require=1` => `BLOCK`
  - `rc=2 && require=0` => `WARN_SOFT_BLOCK`
  - `rc!=0/2` => `EXEC_FAIL_(BLOCK|WARN)`
- `result.tsv` finance 事件 detail 增加 semantic 文本（`EXEC_FAIL_BLOCK` / `GATE_BLOCK` / `GATE_BLOCK_DOWNGRADED`）。
- `summary.txt` 新增 finance 摘要字段：
  - `finance_partial_closure_gate_result`
  - `finance_partial_closure_gate_decision`
  - `finance_partial_closure_gate_blocking`
  - `finance_partial_closure_gate_rc_semantics`
- `final_gate.log` 也输出 finance gate result/decision/rc_semantics，便于排障。

3. `setup_github_required_checks.sh` 增加 finance gate dry-run 场景示例
- 保持 required check context 不变：
  - `hxy-ops-stageb-p1-guard / ops-stageb-p1-guard`
- 新增输出：
  - `stageb_guard_finance_partial_dry_run_example=RUN_FINANCE_PARTIAL_CLOSURE_GATE=1 REQUIRE_FINANCE_PARTIAL_CLOSURE_GATE=0 ...run_ops_stageb_p1_local_ci.sh --skip-mysql-init --skip-tests`

4. workflow summary 增强（不改 context 名）
- `.github/workflows/ops-stageb-p1-guard.yml` 发布摘要增加 finance 门禁字段：
  - `finance_partial_closure_gate_result`
  - `finance_partial_closure_gate_decision`
  - `finance_partial_closure_gate_blocking`
  - `finance_partial_closure_gate_rc_semantics`

5. rollout 文档更新
- `docs/plans/2026-03-06-ops-stageb-required-checks-rollout.md` 已同步 replay/计提/冲正/四账聚合锚点说明。
- 文档已补 finance gate 启用、软阻断、回滚命令。

## 2. Enable / Soft-Block / Rollback

启用（默认执行）：

```bash
bash ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh --skip-mysql-init
```

软阻断（BLOCK 降 WARN，不阻断流水线）：

```bash
REQUIRE_FINANCE_PARTIAL_CLOSURE_GATE=0 \
bash ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh --skip-mysql-init
```

单次回滚（跳过 finance gate）：

```bash
bash ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh \
  --skip-mysql-init \
  --skip-finance-partial-closure-gate
```

required-check 回滚（移除 StageB context）：

```bash
bash ruoyi-vue-pro-master/script/dev/rollback_ops_stageb_required_checks.sh
```

## 3. Merge Notes For Window A

- required check context 必须保持不变：
  - `hxy-ops-stageb-p1-guard / ops-stageb-p1-guard`
- finance gate 开关保持：
  - `--skip-finance-partial-closure-gate`
  - `RUN_FINANCE_PARTIAL_CLOSURE_GATE=0`
  - `REQUIRE_FINANCE_PARTIAL_CLOSURE_GATE=0`
- 回滚命令需在发布文档与 runbook 保留：
  - `--skip-finance-partial-closure-gate`
  - `REQUIRE_FINANCE_PARTIAL_CLOSURE_GATE=0 ...run_ops_stageb_p1_local_ci.sh`
  - `rollback_ops_stageb_required_checks.sh`
