# Window C Handoff - Ops StageB MiniApp P0 Contract Gate

- Date: 2026-03-08
- Branch: feat/ui-four-account-reconcile-ops
- Scope: miniapp P0 page anchors + contract freeze gate, StageB local-ci/workflow integration, required-check helper/docs sync

## 1. Delivered

1. 新增门禁脚本：`check_miniapp_p0_contract_gate.sh`
- 路径：`ruoyi-vue-pro-master/script/dev/check_miniapp_p0_contract_gate.sh`
- 语义：`0=PASS, 2=BLOCK, 1=SCRIPT_ERROR`
- 检查范围：
  - `docs/contracts/2026-03-08-miniapp-contract-freeze-v1.md` 存在
  - `P0 页面清单` 锚点及 6 个页面：`支付结果/售后申请/售后列表/售后详情/退款进度/异常兜底`
  - `关键错误码锚点`：`BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT(1030004012)`、`BOOKING_ORDER_REFUND_REPLAY_RUN_ID_NOT_EXISTS(1030004016)`、`TICKET_SYNC_DEGRADED`（降级语义）

2. 新增契约冻结文档
- 路径：`docs/contracts/2026-03-08-miniapp-contract-freeze-v1.md`
- 作用：为 StageB 门禁提供稳定锚点，确保小程序 P0 页面与关键错误码/降级语义可被自动校验。

3. StageB 本地 CI 接入
- 路径：`ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh`
- 新增开关：
  - `--skip-miniapp-p0-contract-gate`
  - `RUN_MINIAPP_P0_CONTRACT_GATE=0`
  - `REQUIRE_MINIAPP_P0_CONTRACT_GATE=0`
- 新增 summary/log/tsv 字段：
  - `run_miniapp_p0_contract_gate`
  - `require_miniapp_p0_contract_gate`
  - `miniapp_p0_contract_gate_rc`
  - `miniapp_p0_contract_gate_log`
  - `miniapp_p0_contract_gate_summary`
  - `miniapp_p0_contract_gate_tsv`

4. workflow 接入（context 名保持不变）
- 路径：`.github/workflows/ops-stageb-p1-guard.yml`
- 变更：
  - `pull_request.paths` 增加新 gate 脚本与契约文档路径
  - `workflow_dispatch` 增加 `require_miniapp_p0_contract_gate` 输入
  - 调用 local-ci 时透传 `REQUIRE_MINIAPP_P0_CONTRACT_GATE`
  - step summary 增加 `miniapp_p0_contract_gate_rc` 与 run/require 展示字段

5. required checks helper 与 rollout 文档同步
- `ruoyi-vue-pro-master/script/dev/setup_github_required_checks.sh`
  - 保持 context 名不变：`hxy-ops-stageb-p1-guard / ops-stageb-p1-guard`
  - 新增 miniapp gate 的启用/降级（soft-block）/回滚（skip）示例输出。
- `docs/plans/2026-03-06-ops-stageb-required-checks-rollout.md`
  - 增补 miniapp gate 的单独排障锚点、快速检查命令、local-ci 启用/降级/回滚命令、workflow summary 字段说明。

## 2. Enable / Degrade / Rollback

启用（默认执行）：

```bash
bash ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh --skip-mysql-init
```

降级（soft-block，BLOCK 降 WARN）：

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

## 3. Merge Notes For Window A

- required check context 必须保持不变：
  - `hxy-ops-stageb-p1-guard / ops-stageb-p1-guard`
- miniapp gate 开关必须保留：
  - `--skip-miniapp-p0-contract-gate`
  - `RUN_MINIAPP_P0_CONTRACT_GATE=0`
  - `REQUIRE_MINIAPP_P0_CONTRACT_GATE=0`
- 回滚命令需在 runbook/rollout 文档保留：
  - `--skip-miniapp-p0-contract-gate`
  - `RUN_MINIAPP_P0_CONTRACT_GATE=0 ...run_ops_stageb_p1_local_ci.sh`
  - `rollback_ops_stageb_required_checks.sh`
