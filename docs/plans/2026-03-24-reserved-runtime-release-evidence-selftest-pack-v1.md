# Reserved Runtime Release Evidence Selftest Pack v1 (2026-03-24)

## 1. 目标
- 给 `gift-card / referral / technician-feed` 增加一套仓内 simulated selftest pack 与 evidence gate。
- 先冻结三域运行样本结构、switch 默认关闭态、gray / rollback / sign-off 口径。
- 本批不伪造真实发布样本，不把 `Reserved` 从 `Can Develop / Cannot Release` 提升为可灰度。

## 2. 样本包组成
目录：`tests/fixtures/reserved-runtime-release-evidence-simulated/`

- `gift-card-template-page.json`
- `gift-card-order-create.json`
- `referral-overview.json`
- `referral-bind-inviter.json`
- `technician-feed-page.json`
- `technician-feed-comment-create.json`
- `switch-snapshot.json`
- `gray-stage.json`
- `rollback-drill.json`
- `signoff.json`

## 3. 固定边界
- `switch-snapshot.json` 三个开关都固定为 `off`。
- `gray-stage.json` 固定 `decision=NO_RELEASE`。
- `rollback-drill.json` 固定 `rollbackMode=ALL_SWITCH_OFF`。
- `signoff.json` 固定 `decision=SELFTEST_ONLY_NO_RELEASE`。
- 所有样本只证明仓内 evidence structure 可校验，不证明真实 gray / rollback / sign-off 已闭环。

## 4. 关联资产
- 门禁脚本：`ruoyi-vue-pro-master/script/dev/check_reserved_runtime_release_evidence_gate.sh`
- 自测用例：`tests/reserved-runtime-release-evidence-gate.test.mjs`
- 既有 readiness register：`docs/plans/2026-03-11-miniapp-reserved-runtime-readiness-register-v1.md`

## 5. 当前结论
- 该 selftest pack 只证明 `Reserved` 三域的最小运行样本结构与发布门禁字段已锁定。
- 它不能替代真实运行样本、真实开关审批、真实灰度 / 回滚 / sign-off 证据。
- `Reserved` 当前结论继续固定为：`runtime implemented / Can Develop / Cannot Release / No-Go`。
