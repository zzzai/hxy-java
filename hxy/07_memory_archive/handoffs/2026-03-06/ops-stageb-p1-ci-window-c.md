# Window C Handoff - Ops StageB/P1 CI Guard

- Date: 2026-03-06
- Branch: `wip/window-c-ops-ci`
- Scope: 发布门禁/CI 收口（库存、生命周期、四账运营链路）

## Delivered

1. 扩展库存门禁脚本 `check_store_sku_stock_gate.sh`
- 新增 `hxy_store_sku_stock_adjust_order` 待审批超时检查。
- 新增 `hxy_store_sku_transfer_order` 待审批超时检查。
- 保持 `summary + tsv` 输出，错误码扩展到 `SS01~SS15`。

2. 新增生命周期审批门禁脚本 `check_store_lifecycle_change_order_gate.sh`
- 检查 `hxy_store_lifecycle_change_order` 待审批超时。
- 检查超时单收口一致性（`EXPIRE + SYSTEM_SLA_EXPIRED`）。
- 输出 `summary + tsv`，错误码 `LC01~LC07`。

3. 新增本地一键验收脚本 `run_ops_stageb_p1_local_ci.sh`
- 串联命名门禁、记忆门禁、库存门禁、生命周期门禁、关键回归测试。
- 支持 DB 参数：`DB_HOST/DB_PORT/DB_USER/DB_PASSWORD/DB_NAME`。
- 支持分段跳过：`--skip-mysql-init`、`--skip-tests`、`--skip-naming-guard`、`--skip-memory-guard`、`--skip-stock-gate`、`--skip-lifecycle-gate`。

4. 新增 workflow `.github/workflows/ops-stageb-p1-guard.yml`
- 触发：`workflow_dispatch` + `pull_request` 关键路径变更。
- 执行：`run_ops_stageb_p1_local_ci.sh`。
- 上传：`summary.txt` 与 `result.tsv` 构件。

5. required checks 脚本接入
- `setup_github_required_checks.sh` 新增 `INCLUDE_OPS_STAGEB_CHECKS=1` / `--include-ops-stageb-checks`。
- 默认不变更 stagea checks。

6. 治理文档同步
- `HXY-项目事实基线` 增补 110~113。
- `HXY-架构决策记录-ADR` 增补 ADR-085。
- `HXY-执行状态看板` 增补 94~97。

## Verification Snapshot

- `git diff --check`: PASS
- `check_hxy_naming_guard.sh`（`CHECK_UNSTAGED=1 CHECK_UNTRACKED=1`）: PASS
- `check_hxy_memory_guard.sh`（`CHECK_UNSTAGED=1 CHECK_UNTRACKED=1`）: PASS
- `run_ops_stageb_p1_local_ci.sh --skip-mysql-init`: FAIL（回归测试阶段失败）
- 指定 Maven 回归命令: FAIL（trade 模块测试编译失败）

## Known Failure (Non-window-C scope)

- `yudao-module-trade` 测试编译报错：
  - file: `yudao-module-mall/yudao-module-trade/src/test/java/cn/iocoder/yudao/module/trade/service/aftersale/AfterSaleServiceImplBundleRefundValidationTest.java`
  - symptom: `updateById` 调用二义性（`updateById(T)` vs `updateById(Collection<T>)`）

