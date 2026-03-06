# Window C Handoff - Refund Commission Audit Gate Coverage

- Date: 2026-03-06
- Branch: `feat/ui-four-account-reconcile-ops`
- Scope: StageB/P1 门禁与本地 CI 纳入“退款-提成联调巡检”回归覆盖（仅脚本/文档治理，同步事实）

## Changes

1. `run_ops_stageb_p1_local_ci.sh`
- 新增默认回归测试集合变量 `REGRESSION_TEST_CLASSES`。
- 默认集合补齐 `FourAccountReconcileServiceImplTest`，并保留 `FourAccountReconcileControllerTest`。
- `regression-tests` 步骤改为读取 `-Dtest="${REGRESSION_TEST_CLASSES}"`，不改变既有 stagea/stageb 门禁开关与流程。
- summary 新增 `regression_test_classes` 字段，便于构件审计。

2. `FourAccountReconcileServiceImplTest`（测试修复，不涉及业务逻辑）
- 修复 `argThat` 泛型匹配写法，避免在当前编译链路下测试编译失败。
- 修正 `sourceBizNo` 断言值为 `REFUND_COMMISSION_AUDIT:2001`，与现有实现一致。

3. 发布门禁文档
- 更新 `docs/plans/2026-03-06-ops-stageb-required-checks-rollout.md`：
  - 新增“巡检接口回归测试（退款-提成联调）”章节。
  - 说明本地命令与 CI 同口径执行方式。

4. 治理文档同步
- `HXY-项目事实基线` 追加本批事实（StageB/P1 回归纳入巡检服务层 + 控制层）。
- `HXY-架构决策记录-ADR` 新增 ADR-089，明确“双用例基线”门禁决策。
- `HXY-执行状态看板` 新增 Done 条目，标记该门禁收口完成。

## Verification

- `git diff --check` => PASS
- `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh` => PASS
- `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh` => PASS
- `bash ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh --skip-mysql-init` => PASS
