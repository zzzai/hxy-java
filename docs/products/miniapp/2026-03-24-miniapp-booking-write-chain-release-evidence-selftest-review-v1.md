# MiniApp Booking Write-Chain Release Evidence Selftest Review v1 (2026-03-24)

## 1. 评审目标
- 只评审仓内新增的 simulated selftest pack 与 evidence gate。
- 不把本轮输出写成真实发布证据。

## 2. 本轮新增资产
- 样本包：`tests/fixtures/booking-write-chain-release-evidence-simulated/`
- 门禁脚本：`ruoyi-vue-pro-master/script/dev/check_booking_write_chain_release_evidence_gate.sh`
- 测试：`tests/booking-write-chain-release-evidence-gate.test.mjs`
- 说明文档：`docs/plans/2026-03-24-booking-write-chain-release-evidence-selftest-pack-v1.md`

## 3. 当前能证明什么
- `technician list -> slot list -> create / cancel / addon` 的样本结构已经在仓内冻结。
- `create-conflict`、`addon-conflict` 的负向样本都被固定成 `errorCode-only` 判定，避免 message 漂移误判。
- `gray / rollback / sign-off` 的字段口径已经收口，并被固定为 `NO_RELEASE` / `SELFTEST_ONLY_NO_RELEASE`。
- 可持续执行的 evidence gate 已存在，后续真实样本替换时有明确校验入口。

## 4. 当前不能证明什么
- 不能证明真实环境已有 create / cancel / addon 发布级 success/failure 样本。
- 不能证明 allowlist、巡检日志、回放日志中的旧 path / 旧 method 已真实清零。
- 不能证明真实 gray / rollback / sign-off 已闭环。
- 不能据此把 `Booking` 升级为 `Can Release=Yes`。

## 5. 当前结论
- `Booking` 新增了“仓内可执行的 write-chain release evidence selftest pack”。
- `Booking` 当前真实状态仍然只能写成：`Doc Closed / Can Develop / Cannot Release / No-Go`。
