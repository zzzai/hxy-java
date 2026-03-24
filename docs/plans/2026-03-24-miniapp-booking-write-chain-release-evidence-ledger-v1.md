# MiniApp Booking Write-Chain Release Evidence Ledger v1 (2026-03-24)

## 1. 目标
- 把 `Booking create / cancel / addon` 当前进入 release 评估所需的真实证据、模拟证据、缺口和责任面统一落盘。
- 本文只描述“当前有没有证据、缺什么证据、缺证据时为什么仍 `No-Go`”，不把 simulated/selftest 伪造为真实发布证据。

## 2. 当前固定边界
- 商品来源真值已闭环：`technician-detail -> service-select -> order-confirm` 是 create 唯一路径，`addonType=1/2/3` 商品来源规则已冻结。
- 仓内 simulated selftest pack 已存在：`tests/fixtures/booking-write-chain-release-evidence-simulated/`。
- gate 已存在：`ruoyi-vue-pro-master/script/dev/check_booking_write_chain_release_evidence_gate.sh`。
- 当前最终口径仍固定为：`Doc Closed / Can Develop / Cannot Release / No-Go`。

## 3. release evidence ledger

| 证据项 | 最低要求 | 当前状态 | 当前真值 | 责任面 | 结论 |
|---|---|---|---|---|---|
| create success 样本 | 真实 request / response / detail readback | 未核出 | 仅有 simulated `create-success.json` | A + D | blocker |
| create failure 样本 | 真实冲突/失败 request / response，且只按 `errorCode` 判定 | 未核出 | 仅有 simulated `create-conflict.json` | A + D | blocker |
| cancel success 样本 | 真实取消 request / response / postStatus=`CANCELLED` readback | 未核出 | 仅有 simulated `cancel-success.json` | A + D | blocker |
| addon success 样本 | 真实 add-on request / response / detail readback | 未核出 | 仅有 simulated `addon-success.json` | A + D | blocker |
| addon failure 样本 | 真实失败 request / response，且只按 `errorCode` 判定 | 未核出 | 仅有 simulated `addon-conflict.json` | A + D | blocker |
| technician / slot 上游样本 | 真实 technician list / slot list-by-technician success 样本 | 未核出 | 仅有 simulated list/slot 成功样本 | A + D | blocker |
| 旧 path / method 清零 | allowlist、巡检日志、回放日志命中数全为 `0` | 未核出 | 当前只有仓内 runtime gate / CI PASS | A + D | blocker |
| gray batch 记录 | 真实 gray 批次、批次 runId、owner、放量范围 | 未核出 | 仅有 simulated `gray-stage.json`，`decision=NO_RELEASE` | A + D | blocker |
| rollback drill | 真实 rollback owner、动作、回执、恢复窗口 | 未核出 | 仅有 simulated `rollback-drill.json` | A + D | blocker |
| sign-off | 产品 / 技术 / 运营真实签字材料 | 未核出 | 仅有 simulated `signoff.json`，`decision=SELFTEST_ONLY_NO_RELEASE` | A + D | blocker |
| technician fallback 字段 | `title / specialties / status` 真实后端绑定或明确删除 fallback | 部分 | 页面仍有 fallback，尚未后端真值化 | B + C | blocker |

## 4. 当前能证明什么
- 仓内可以持续校验样本结构是否齐全。
- 负向样本已经冻结为 `errorCode-only` 判定边界。
- `gray / rollback / sign-off` 字段口径已经收口，且当前明确只允许写成 `NO_RELEASE` / `SELFTEST_ONLY_NO_RELEASE`。

## 5. 当前不能证明什么
- 不能证明真实环境已有 create / cancel / addon 的 success/failure 样本。
- 不能证明旧 path / 旧 method 已在真实发布链路中清零。
- 不能证明已经完成真实 gray / rollback / sign-off。
- 不能证明 `Booking` 已经具备 release-ready 分母。

## 6. 当前结论
1. `selftest pack` 只用于结构校验，不是发布放量证据。
2. 只要上表任一 blocker 未解除，`Booking` 继续保持 `Cannot Release / No-Go`。
3. 当前最值得继续推进的不是重做商品来源，而是拿真实样本、真实日志、真实灰度和真实签字材料。
