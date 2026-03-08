# Window C Handoff - Ops StageB MiniApp P0 Contract Gate

- Date: 2026-03-08
- Branch: feat/ui-four-account-reconcile-ops
- Scope: 小程序 P0 页面补齐 + 契约冻结纳入 StageB 门禁；支持启用/降级/回滚

## 1. 本批完成项

1. MiniApp P0 契约门禁脚本（已纳入 StageB）
- 路径：`ruoyi-vue-pro-master/script/dev/check_miniapp_p0_contract_gate.sh`
- 检查点：
  - `docs/contracts/2026-03-08-miniapp-contract-freeze-v1.md` 存在
  - `P0 页面清单` 锚点存在
  - P0 页面锚点存在：`支付结果/售后申请/售后列表/售后详情/退款进度/异常兜底`
  - 关键错误码/语义锚点存在：
    - `BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT`（`1030004012`，幂等冲突）
    - `BOOKING_ORDER_REFUND_REPLAY_RUN_ID_NOT_EXISTS`（`1030004016`，runId 不存在）
    - `TICKET_SYNC_DEGRADED`（降级语义）

2. StageB 本地 CI 与 workflow 集成
- 本地 CI：`ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh`
  - 开关保留：
    - `--skip-miniapp-p0-contract-gate`
    - `RUN_MINIAPP_P0_CONTRACT_GATE=0`
    - `REQUIRE_MINIAPP_P0_CONTRACT_GATE=0`
- Workflow：`.github/workflows/ops-stageb-p1-guard.yml`
  - `pull_request.paths` 已包含 miniapp gate 脚本与契约文档
  - `workflow_dispatch` 已包含 `require_miniapp_p0_contract_gate`
  - summary 输出已包含 `run_miniapp_p0_contract_gate` / `require_miniapp_p0_contract_gate` / `miniapp_p0_contract_gate_rc`

3. required checks 脚本同步（context 名保持不变）
- 脚本：`ruoyi-vue-pro-master/script/dev/setup_github_required_checks.sh`
- StageB context 维持：`hxy-ops-stageb-p1-guard / ops-stageb-p1-guard`
- 脚本输出包含 miniapp gate 的 skip / soft-block 示例与 required checks 回滚命令提示。

4. 契约文档补齐
- 文件：`docs/contracts/2026-03-08-miniapp-contract-freeze-v1.md`
- 本次补齐：
  - 新增章节锚点：`## 2. P0 页面清单（页面契约矩阵）`
  - 新增页面行锚点：`/pages/common/exception`（`异常兜底`）

## 2. 启用 / 降级 / 回滚命令

启用（默认执行 miniapp gate）：

```bash
bash ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh --skip-mysql-init
```

降级（soft-block，BLOCK -> WARN）：

```bash
REQUIRE_MINIAPP_P0_CONTRACT_GATE=0 \
bash ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh --skip-mysql-init
```

回滚（单次跳过 miniapp gate）：

```bash
RUN_MINIAPP_P0_CONTRACT_GATE=0 \
bash ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh --skip-mysql-init

# 或
bash ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh \
  --skip-mysql-init \
  --skip-miniapp-p0-contract-gate
```

required checks 回滚（移除 StageB context）：

```bash
bash ruoyi-vue-pro-master/script/dev/rollback_ops_stageb_required_checks.sh
```

## 3. 本地验证结果（Window C）

1. `git diff --check` -> PASS
2. `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh` -> PASS
3. `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh` -> PASS
4. `bash ruoyi-vue-pro-master/script/dev/check_miniapp_p0_contract_gate.sh` -> PASS
5. `bash ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh --skip-mysql-init` -> PASS

## 4. 对 A 合入注意点

- 必须保持 required check context 名不变：`hxy-ops-stageb-p1-guard / ops-stageb-p1-guard`
- 必须保留 miniapp gate 三个开关：
  - `--skip-miniapp-p0-contract-gate`
  - `RUN_MINIAPP_P0_CONTRACT_GATE=0`
  - `REQUIRE_MINIAPP_P0_CONTRACT_GATE=0`
- 回滚命令必须保留在 runbook/handoff：
  - `--skip-miniapp-p0-contract-gate`
  - `RUN_MINIAPP_P0_CONTRACT_GATE=0 ...run_ops_stageb_p1_local_ci.sh`
  - `rollback_ops_stageb_required_checks.sh`
