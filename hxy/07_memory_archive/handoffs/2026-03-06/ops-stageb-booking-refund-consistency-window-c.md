# Window C Handoff - StageB Booking Refund Consistency CI Coverage

- Date: 2026-03-06
- Branch: `feat/ui-four-account-reconcile-ops`
- Scope: StageB/P1 本地 CI 与 workflow 回归增强（覆盖 booking 退款回调一致性）

## Changes

1. `run_ops_stageb_p1_local_ci.sh`
- 默认 `REGRESSION_TEST_CLASSES` 新增 `BookingOrderServiceImplTest`。
- 仍保留 `REGRESSION_TEST_CLASSES` 环境变量覆盖能力，未改变现有 StageA/StageB 门禁开关行为。

2. `.github/workflows/ops-stageb-p1-guard.yml`
- `Run ops stageB p1 guard` 步骤新增 `regression_test_classes` 常量并透传到脚本环境变量 `REGRESSION_TEST_CLASSES`。
- 回归集合与本地脚本保持同口径，新增 `BookingOrderServiceImplTest`。

3. `docs/plans/2026-03-06-ops-stageb-required-checks-rollout.md`
- 补充 `BookingOrderServiceImplTest`（退款回调一致性）说明。
- 新增“退款回调一致性回归用例”章节，含本地单测命令与 CI 等价执行样例。

## Verification

- `git diff --check` => PASS
- `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh` => PASS (`checked_files=204`)
- `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh` => PASS (`checked_files=209 core_domains=0`)
- `bash ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh --skip-mysql-init` => PASS (`result=PASS`)
