# Member Release Evidence Selftest Pack v1 (2026-03-24)

## 1. 目标
- 给 `level / assets / tag` 三页增加一套仓内 simulated selftest pack 与 evidence gate。
- 先冻结页面样本结构、`asset-ledger` 的默认 `degraded=false` 边界、gray / rollback / sign-off 口径。
- 本批不伪造真实发布样本，也不补造不存在的独立 feature flag。

## 2. 样本包组成
目录：`tests/fixtures/member-release-evidence-simulated/`

- `level-page-sample.json`
- `level-experience-page.json`
- `asset-ledger-page.json`
- `tag-page-sample.json`
- `switch-snapshot.json`
- `gray-stage.json`
- `rollback-drill.json`
- `signoff.json`

## 3. 固定边界
- `switch-snapshot.json` 固定 `hasDedicatedFeatureFlag=false`。
- `gateStrategy` 固定为 `A_WINDOW_RELEASE_SIGNOFF_REQUIRED`。
- `asset-ledger-page.json` 中的 `degraded=false` 只表示默认字段输出，不表示真实降级链路已验证。
- `gray-stage.json` 固定 `decision=NO_RELEASE`。
- `signoff.json` 固定 `decision=SELFTEST_ONLY_NO_RELEASE`。

## 4. 关联资产
- 门禁脚本：`ruoyi-vue-pro-master/script/dev/check_member_release_evidence_gate.sh`
- 自测用例：`tests/member-release-evidence-gate.test.mjs`
- 现有工程闭环 review：`docs/products/miniapp/2026-03-24-miniapp-member-missing-page-closure-review-v1.md`

## 5. 当前结论
- 该 selftest pack 只证明 `Member` 三页的 evidence structure 和非发布边界已锁定。
- 它不能替代真实运行样本、灰度 / 回滚 / 客服演练与 release sign-off。
- `Member` 当前结论继续固定为：`Doc Closed / Can Develop / Cannot Release`。
