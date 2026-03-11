# MiniApp Reserved Runtime Readiness Register v1 (2026-03-11)

## 1. 目标与适用范围
- 目标：把 gift-card / referral / technician-feed 当前仍处于 `PLANNED_RESERVED` 的原因、缺失证据和解锁条件固化成单一登记表，避免治理文档被误当作 runtime 已闭环依据。
- 适用范围：
  - gift-card
  - referral
  - technician-feed
- 对齐基线：
  - `docs/contracts/2026-03-09-miniapp-reserved-disabled-gate-spec-v1.md`
  - `docs/plans/2026-03-10-miniapp-reserved-expansion-activation-checklist-v1.md`
  - `docs/plans/2026-03-10-miniapp-reserved-expansion-gray-acceptance-runbook-v1.md`
  - `docs/products/miniapp/2026-03-10-miniapp-capability-status-ledger-v1.md`
  - `docs/products/miniapp/2026-03-09-miniapp-release-decision-pack-v1.md`

## 2. 当前统一结论
- 当前三个 reserved capability 全部维持：`NO_GO / PLANNED_RESERVED`
- 当前原因不是文档缺失，而是 runtime 证据缺失：
  - 无真实页面
  - 无真实 app controller
  - 无真实样本包
  - 开关默认 `off`

## 3. 运行证据登记表

| capability | 当前页面状态 | 当前 controller 状态 | 当前 switch | 关键错误码 | 当前结论 |
|---|---|---|---|---|---|
| gift-card | 无真实 `/pages/gift-card/*` | 无真实 `/promotion/gift-card/*` app controller | `miniapp.gift-card=off` | `1011009901`、`1011009902` | 持续 `NO_GO` |
| referral | 无真实 `/pages/referral/*` | 无真实 `/promotion/referral/*` app controller | `miniapp.referral=off` | `1013009901`、`1013009902` | 持续 `NO_GO` |
| technician-feed | 无真实 `/pages/technician/feed` | 无真实 `/booking/technician/feed/*` app controller | `miniapp.technician-feed.audit=off` | `1030009901` | 持续 `NO_GO` |

## 4. 必须补齐的 runtime 证据

### 4.1 页面与入口
- [ ] 真实页面文件存在
- [ ] 进入 `pages.json`
- [ ] 有明确入口，不依赖原型 alias 或临时深链
- [ ] 页面展示与 capability 定义一致

### 4.2 后端与契约
- [ ] 真实 app controller 存在
- [ ] canonical contract 更新到真实 path / field / errorCode
- [ ] 未再使用占位路径或 wildcard
- [ ] `RESERVED_DISABLED` 开关关闭态命中规则明确

### 4.3 验收与观察
- [ ] 最小样本包存在
- [ ] 五键日志可检索：`runId/orderId/payRefundId/sourceBizNo/errorCode`
- [ ] `degraded_pool` 与主口径隔离
- [ ] 回滚路径和人工接管入口已演练

## 5. 不得误判的情况
- 只完成 activation checklist，不等于 runtime 可激活。
- 只完成 gray runbook，不等于 capability 可以进 G1。
- 只完成产品 PRD 或优先级文档，不等于 capability 已实现。
- 任一关闭态命中 `RESERVED_DISABLED`，都不是 warning，而是 mis-release / No-Go。

## 6. 阶段转换规则

| 当前阶段 | 可进入阶段 | 前置条件 |
|---|---|---|
| `NO_GO / PLANNED_RESERVED` | `BLOCKED_NO_IMPL` 解除中 | 页面、controller、contract 三件套存在 |
| `BLOCKED_NO_IMPL` 解除中 | `G1 5%` | activation checklist 全通过、样本包可回放、误返回计数 0 |
| `G1/G2/G3` | 下一阶段灰度 | 达到样本数、无回滚条件、五键日志完整 |
| `G4 100%` | `ACTIVE` | 连续观察通过、误返回 0、A 窗口回写 capability ledger 与 release decision |

## 7. A/B/C/D 同步要求
- A：只有在本登记表所有 runtime 缺项关闭后，才允许把 reserved capability 从 `PLANNED_RESERVED` 改到下一阶段。
- B：任何产品文档都不得越过本登记表，把未实现能力写成“已进入灰度”。
- C：switch、errorCode、controller path 必须与本登记表一致，尤其是 `RESERVED_DISABLED`。
- D：所有观察项继续按 gray runbook 执行，关闭态命中一律回滚。

## 8. 退出条件
只有同时满足以下条件，reserved capability 才允许离开当前状态：
1. 页面存在。
2. app controller 存在。
3. contract 完成并进入 allowlist。
4. activation checklist 全通过。
5. gray runbook 样本和观察项全部具备。
6. A 窗口重新签发后，才允许推进下一阶段。
