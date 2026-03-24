# MiniApp Reserved Runtime Release Evidence Selftest Review v1 (2026-03-24)

## 1. 评审目标
- 只评审仓内新增的 `Reserved` simulated selftest pack 与 evidence gate。
- 不把本轮输出写成真实灰度或真实放量证据。

## 2. 本轮新增资产
- 样本包：`tests/fixtures/reserved-runtime-release-evidence-simulated/`
- 门禁脚本：`ruoyi-vue-pro-master/script/dev/check_reserved_runtime_release_evidence_gate.sh`
- 测试：`tests/reserved-runtime-release-evidence-gate.test.mjs`
- 说明文档：`docs/plans/2026-03-24-reserved-runtime-release-evidence-selftest-pack-v1.md`

## 3. 当前能证明什么
- `gift-card / referral / technician-feed` 三域的最小运行样本结构已经在仓内冻结。
- 三个 reserved switch 的默认关闭态已经通过样本和门禁脚本固定。
- `gray / rollback / sign-off` 的字段口径已经收口，并明确保持 `NO_RELEASE` / `SELFTEST_ONLY_NO_RELEASE`。
- 后续真实样本替换时，有统一的 evidence gate 可以持续校验。

## 4. 当前不能证明什么
- 不能证明真实环境已经拿到三域真实请求/响应/回读样本。
- 不能证明真实开关审批、真实灰度记录、真实回滚回执与真实 sign-off 已归档。
- 不能把 `Reserved` 提升为“已可灰度”或“已可放量”。

## 5. 当前结论
- `Reserved` 新增了一套合并版 release evidence selftest pack。
- `Reserved` 当前真实状态仍然只能写成：`runtime implemented / Can Develop / Cannot Release / No-Go`。
