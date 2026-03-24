# Booking Write-Chain Release Package Design

## 1. 背景
- 2026-03-24，`Booking` 写链已经完成商品来源真值收口，`create / cancel / addon` 的页面级 canonical 行为与仓内 selftest gate 已冻结。
- 当前真正阻断点已经不再是“前端路径漂移”，而是“没有一份可持续维护的发布包单一真值”，导致 release evidence、gray / rollback / sign-off、项目级 Go/No-Go 分散在多个文档中。
- 目标不是伪造真实发布证据，而是把“缺什么、现在能证明什么、还不能改口什么”收成一套固定资产，并用回归测试和 gate 保护住。

## 2. 设计目标
- 新增一份 `Booking` 写链 release evidence ledger，集中表达真实样本、allowlist、巡检日志、gray、rollback、sign-off、fallback 字段绑定的当前状态。
- 新增一份 `Booking` 写链 release package review，统一说明：
  - 现在已经完成的工程内容
  - selftest pack 能证明什么
  - selftest pack 不能证明什么
  - 为什么当前仍然是 `Doc Closed / Can Develop / Cannot Release / No-Go`
- 更新现有 runbook、closure review、项目级 Go/No-Go 包，让旧口径不再停留在“商品来源未闭环”的旧判断。
- 给现有 gate 加一层文档真值校验，避免后续只更新代码/样本、不更新发布裁决文档。

## 3. 方案比较

### 方案 A：只补文档，不加任何测试或 gate
- 优点：最快。
- 缺点：后续极易再次出现“代码已改，发布结论还停留在旧描述”或“文档被误升 release-ready”。

### 方案 B：补文档，同时把关键结论接入现有 evidence gate
- 优点：既补单一真值，又给未来变更加守边界回归；成本低，收益高。
- 缺点：需要多改一处脚本和一个测试文件。

### 方案 C：暂不补文档，继续直接做后续真实样本与灰度材料
- 优点：看起来更贴近 release。
- 缺点：当前没有真实环境与真实门店，这批证据短期拿不到；继续堆材料只会让口径更散。

## 4. 推荐方案
- 采用方案 B。
- 原因：当前最缺的不是更多模拟样本，而是“把现有真实结论收成单一真值并锁住”。这件事完成后，后续一旦有真实环境，就可以直接按 ledger 补位，不需要再重做裁决结构。

## 5. 目标文件
- 新增：`docs/plans/2026-03-24-miniapp-booking-write-chain-release-evidence-ledger-v1.md`
- 新增：`docs/products/miniapp/2026-03-24-miniapp-booking-write-chain-release-package-review-v1.md`
- 新增：`docs/plans/2026-03-24-booking-write-chain-release-package-implementation-plan.md`
- 新增：`hxy/07_memory_archive/handoffs/2026-03-24/booking-write-chain-release-package-window-a.md`
- 更新：`docs/plans/2026-03-15-miniapp-booking-runtime-release-runbook-v1.md`
- 更新：`docs/products/miniapp/2026-03-24-miniapp-booking-write-chain-closure-review-v1.md`
- 更新：`docs/products/miniapp/2026-03-24-miniapp-project-release-go-no-go-package-v1.md`
- 更新：`tests/booking-write-chain-release-evidence-gate.test.mjs`
- 更新：`ruoyi-vue-pro-master/script/dev/check_booking_write_chain_release_evidence_gate.sh`

## 6. 固定边界
- 不把 simulated/selftest 写成真实 release evidence。
- 不把 `runtime gate PASS`、`CI PASS`、`sample pack PASS` 写成 `Go`。
- 不把 `Booking` 从 `Cannot Release / No-Go` 升级。
- 不动业务代码，不回退已完成的商品来源真值。
