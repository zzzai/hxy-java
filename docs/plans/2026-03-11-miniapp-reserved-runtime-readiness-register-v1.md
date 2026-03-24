# MiniApp Reserved Runtime Readiness Register v1 (2026-03-11)

## 1. 目标与适用范围
- 目标：把 gift-card / referral / technician-feed 从“runtime 未实现”推进到“runtime 已实现、release evidence 待补”后的剩余缺口固化成单一登记表，避免工程闭环被误当作可放量依据。
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
- 当前三个 reserved capability 已统一升级为：`Can Develop / Cannot Release`
- 当前原因不是文档、页面或 controller 缺失，而是发布级证据缺失：
  - 真实运行样本包未闭环
  - 开关审批 / 灰度 / 回滚样本未闭环
  - sign-off 与误发布告警演练未闭环
  - 开关默认仍为 `off`
- 03-24 已新增仓内 simulated selftest pack 与 `check_reserved_runtime_release_evidence_gate.sh`，只能证明 evidence structure 与 switch / gray / rollback / sign-off 字段口径可校验，不能替代真实发布证据。

## 3. 运行证据登记表

| capability | 当前页面状态 | 当前 controller 状态 | 当前 switch | 关键错误码 | 当前结论 |
|---|---|---|---|---|---|
| gift-card | 已存在 `/pages/gift-card/list`、`/pages/gift-card/order-detail`、`/pages/gift-card/redeem`、`/pages/gift-card/refund`，且 `/pages/profile/assets` 已补真实入口 | 已存在 `/promotion/gift-card/template/page`、`/promotion/gift-card/order/create`、`/promotion/gift-card/order/get`、`/promotion/gift-card/redeem`、`/promotion/gift-card/refund/apply` | `miniapp.gift-card=off` | `1011009901`、`1011009902` | `runtime implemented / cannot release` |
| referral | 已存在 `/pages/referral/index` | 已存在 `/promotion/referral/bind-inviter`、`/promotion/referral/overview`、`/promotion/referral/reward-ledger/page` | `miniapp.referral=off` | `1013009901`、`1013009902` | `runtime implemented / cannot release` |
| technician-feed | 已存在 `/pages/technician/feed`，且技师详情页已补真实入口 | 已存在 `/booking/technician/feed/page`、`/booking/technician/feed/like`、`/booking/technician/feed/comment/create` | `miniapp.technician-feed.audit=off` | `1030009901` | `runtime implemented / cannot release` |

## 4. 当前证据状态

### 4.1 已补齐的页面与入口证据
- [x] 真实页面文件存在
- [x] 已进入 `pages.json`
- [x] 已有明确入口，不依赖原型 alias 或临时深链
- [x] 页面展示与 capability 定义一致

### 4.2 已补齐的后端与契约证据
- [x] 真实 app controller 存在
- [x] canonical contract 已更新到真实 path / field / errorCode
- [x] 未再使用占位路径或 wildcard 充当 runtime 真值
- [x] `RESERVED_DISABLED` 开关关闭态命中规则明确

### 4.3 仍缺失的发布级证据
- [x] 仓内 simulated selftest pack 已存在，但只用于固定 evidence structure
- [ ] 真实最小运行样本包已归档
- [ ] 开关审批 / 灰度名单 / 回滚样本已归档
- [ ] 误发布告警与 `RESERVED_DISABLED` 关闭态演练已完成
- [ ] sign-off 与人工接管入口已演练

## 5. 不得误判的情况
- 只完成 activation checklist，不等于 runtime 可激活。
- 只完成 gray runbook，不等于 capability 可以进 G1。
- 只完成页面、controller、SQL、测试，不等于 capability 可以放量。
- 任一关闭态命中 `RESERVED_DISABLED`，都不是 warning，而是 mis-release / No-Go。

## 6. 阶段转换规则

| 当前阶段 | 可进入阶段 | 前置条件 |
|---|---|---|
| `Can Develop / Cannot Release` | `G1 5%` | activation checklist 全通过、样本包可回放、误返回计数 0、灰度 / 回滚证据齐备 |
| `G1/G2/G3` | 下一阶段灰度 | 达到样本数、无回滚条件、五键日志完整 |
| `G4 100%` | `ACTIVE` | 连续观察通过、误返回 0、A 窗口回写 capability ledger 与 release decision |

## 7. A/B/C/D 同步要求
- A：只有在本登记表所有发布级缺项关闭后，才允许把 reserved capability 从 `Can Develop / Cannot Release` 推进到灰度阶段。
- B：任何产品文档都不得越过本登记表，把页面/controller/测试存在写成“已进入灰度”。
- C：switch、errorCode、controller path 必须与本登记表一致，尤其是 `RESERVED_DISABLED`。
- D：所有观察项继续按 gray runbook 执行，关闭态命中一律回滚。

## 8. 退出条件
只有同时满足以下条件，reserved capability 才允许离开当前状态：
1. 页面、入口、app controller 与 contract 真值持续一致。
2. activation checklist 全通过。
3. gray runbook 样本和观察项全部具备。
4. 开关审批、回滚路径与误发布告警演练已归档。
5. A 窗口重新签发后，才允许推进下一阶段。
