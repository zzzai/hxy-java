# MiniApp Reserved Expansion Activation Checklist v1 (2026-03-10)

## 1. 目标
- 为 gift-card、referral、technician-feed 从 `PLANNED_RESERVED` 进入 `ACTIVE` 提供统一门禁清单。
- 原则：先完成契约、页面、接口、监控、SOP、灰度演练，再考虑激活；不得越级上线。
- 对齐基线：
  - `docs/products/miniapp/2026-03-10-miniapp-capability-status-ledger-v1.md`
  - `docs/contracts/2026-03-09-miniapp-reserved-disabled-gate-spec-v1.md`
  - `docs/plans/2026-03-10-miniapp-domain-release-acceptance-matrix-v1.md`
  - `docs/plans/2026-03-10-miniapp-domain-alert-owner-routing-v1.md`

## 2. 适用能力

| capability | 当前状态 | 开关键 | 关键错误码 |
|---|---|---|---|
| gift-card | `PLANNED_RESERVED` | `miniapp.gift-card` | `1011009901`、`1011009902` |
| referral | `PLANNED_RESERVED` | `miniapp.referral` | `1013009901`、`1013009902` |
| technician-feed | `PLANNED_RESERVED` | `miniapp.technician-feed.audit` | `1030009901` |

## 2.1 当前批次默认结论
- 按 capability ledger 当前真值：
  - gift-card：前端无页面、后端无 app controller
  - referral：前端无页面、后端无 app controller
  - technician-feed：前端无页面、后端无 feed controller
- 因此本批默认结论是 `NO_GO`；只有当真实页面、controller、台账、灰度 evidence 全部补齐后，本文才进入“可执行激活”状态。

## 3. 激活前门禁条件

| 类别 | 必须满足 |
|---|---|
| 前端承接 | 真实页面存在，且与 capability ledger 对齐 |
| 后端接口 | app controller 存在且方法/路径冻结 |
| 契约 | API 字段、错误码、降级语义冻结 |
| 测试 | Happy path + 失败路径 + 降级路径均可执行 |
| 监控 | 成功率、错误率、降级率、恢复率、`RESERVED_DISABLED` 计数上线 |
| SOP | 客服、运营、回滚 runbook 已冻结并演练 |
| 灰度 | 白名单、比例、样本、回滚阈值明确 |

- 统一附加规则：
  - `degraded=true` 只进入 `degraded_pool`，不得计入主成功率 / 主 ROI。
  - 五键固定为 `runId/orderId/payRefundId/sourceBizNo/errorCode`；无值时填 `"0"`。

## 4. 逐域激活清单

### 4.1 Gift Card
- [ ] 页面存在：`/pages/gift-card/*`
- [ ] 接口存在：`/promotion/gift-card/*`
- [ ] 发卡、核销、退款主链路可回归
- [ ] `1011009901/1011009902` 已接入错误码与客服口径
- [ ] 发卡失败 fail-open 与退款补偿路径演练完成

### 4.2 Referral
- [ ] 页面存在：`/pages/referral/*`
- [ ] 接口存在：`/promotion/referral/*`
- [ ] 绑定、奖励到账、台账查询、补偿重试可回归
- [ ] `1013009901/1013009902` 已接入错误码与客服口径
- [ ] 奖励账本和申诉流程演练完成

### 4.3 Technician Feed
- [ ] 页面存在：`/pages/technician/feed`
- [ ] 接口存在：`/booking/technician/feed/*`
- [ ] 浏览、点赞、评论、审核回写可回归
- [ ] `1030009901` 已接入错误码与客服口径
- [ ] 审核超时降级和内容拦截路径演练完成

## 5. 不得越级上线规则
1. 不得从 `0%` 直接到 `50%` 或 `100%`。
2. 不得在前端页面缺失或 app controller 缺失时把状态改为 `ACTIVE`。
3. 不得在客服 SOP 未冻结时开放灰度。
4. 不得在 `RESERVED_DISABLED` 误返回未清零时继续推进。

## 6. `RESERVED_DISABLED` 命中即回滚规则

| 条件 | 判断 | 动作 |
|---|---|---|
| 开关关闭态命中保留错误码 | 误返回 | 立即回滚开关并锁定发布 |
| 未命中灰度范围却命中保留错误码 | 误返回 | 清空灰度名单并回滚 |
| 灰度开启但错误码占比异常升高 | 高风险 | 暂停放量并进入人工复核 |

## 7. 激活决策

| 结果 | 条件 |
|---|---|
| `ALLOW_GRAY` | 所有门禁项通过，允许进入 5% 灰度 |
| `GO_WITH_WATCH` | 非阻断项有轻微风险，但不影响主链路 |
| `NO_GO` | 任一阻断项失败或 `RESERVED_DISABLED` 误返回 |

- 当前批次补充判断：
  - 若 capability ledger 仍显示“未实现页面 / 未实现 controller / switch=off”，即使文档存在也固定判定为 `NO_GO`。

## 8. 审计字段
- 激活决策必须记录：
  - `releaseId`
  - `capability`
  - `switchKey`
  - `switchValue`
  - `grayScope`
  - `runId/orderId/payRefundId/sourceBizNo/errorCode`
- 字段补位规则：
  - 激活巡检 / 台账扫描：`orderId=0`、`payRefundId=0`
  - 误发布扫描：`sourceBizNo` 优先取灰度规则或真实来源号，无值填 `0`
  - 纯门禁判断无单一错误码时，`errorCode=0`

## 9. 验收标准
1. gift/referral/feed 的门禁条件逐项明确。
2. 灰度前置条件和不得越级上线规则明确。
3. `RESERVED_DISABLED` 命中即回滚的判断条件可直接执行。
4. 激活决策可审计、可复盘。
5. capability 未落地时会保持 `NO_GO`，不会因文档完备而被误判上线。
