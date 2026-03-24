# Booking Write-Chain Release Evidence Selftest Pack v1 (2026-03-24)

## 1. 目标
- 给 `Booking create / cancel / addon` 新增一套仓内 simulated selftest pack 与 evidence gate。
- 先锁定样本结构、errorCode-only 判定边界、gray / rollback / sign-off 口径。
- 本批不伪造真实发布样本，不把 `Booking` 从 `Cannot Release` 提升为 `release-ready`。

## 2. 样本包组成
目录：`tests/fixtures/booking-write-chain-release-evidence-simulated/`

- `technician-list-success.json`
- `slot-list-success.json`
- `create-success.json`
- `create-conflict.json`
- `cancel-success.json`
- `addon-success.json`
- `addon-conflict.json`
- `gray-stage.json`
- `rollback-drill.json`
- `signoff.json`

## 3. 固定边界
- `create-conflict` 只认 `1030003001` + `ERROR_CODE_ONLY`。
- `addon-conflict` 只认 `1030004001` + `ERROR_CODE_ONLY`。
- `cancel-success` 必须带 `postStatus=CANCELLED` 与 `refreshed=true`。
- `create-success` 与 `addon-success` 只证明 helper 成功后跳详情，不证明真实环境已放量。
- `signoff.json` 的 `decision` 固定为 `SELFTEST_ONLY_NO_RELEASE`。

## 4. 关联资产
- 门禁脚本：`ruoyi-vue-pro-master/script/dev/check_booking_write_chain_release_evidence_gate.sh`
- 自测用例：`tests/booking-write-chain-release-evidence-gate.test.mjs`
- 现有静态边界：`ruoyi-vue-pro-master/script/dev/check_booking_miniapp_runtime_gate.sh`

## 5. 当前结论
- 该 selftest pack 只证明仓内 evidence structure 可校验。
- 它不能替代真实 request / response / readback 样本、allowlist / 巡检日志、gray / rollback / sign-off 证据。
- `Booking` 当前结论继续固定为：`Doc Closed / Can Develop / Cannot Release / No-Go`。
