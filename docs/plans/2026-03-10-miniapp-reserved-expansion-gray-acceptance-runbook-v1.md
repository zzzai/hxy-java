# MiniApp Reserved Expansion Gray Acceptance Runbook v1 (2026-03-10)

## 1. 目标
- 规范 gift-card、referral、technician-feed 的灰度验收流程，确保从 `PLANNED_RESERVED` 到 `ACTIVE` 的过程可观测、可回滚、不可越级。
- 对齐基线：
  - `docs/plans/2026-03-10-miniapp-reserved-expansion-activation-checklist-v1.md`
  - `docs/plans/2026-03-10-miniapp-domain-release-acceptance-matrix-v1.md`
  - `docs/plans/2026-03-10-miniapp-domain-alert-owner-routing-v1.md`

## 1.1 当前批次执行前提
- capability ledger 当前仍记录：
  - gift-card / referral / technician-feed 页面未实现
  - 对应 app controller 未实现
  - 开关默认 `off`
- 因此当前默认结论仍是 `NO_GO`。本 runbook 现在是“灰度执行模板”，只有当 activation checklist 全量通过后才允许启动阶段 G1。

## 2. 灰度比例

| 阶段 | 比例 | 最小观察时长 | 是否可跳过 |
|---|---|---|---|
| G1 | 5% | 30 分钟 | 不可 |
| G2 | 20% | 30 分钟 | 不可 |
| G3 | 50% | 60 分钟 | 不可 |
| G4 | 100% | 120 分钟 | 仅前三阶段全部通过后允许 |

## 3. 验收样本

| 能力 | 最小样本 | 必测样本 |
|---|---|---|
| gift-card | 30 个创建/支付样本，10 个退款样本，10 个核销样本 | 成功、库存不足、冲突、降级补偿 |
| referral | 30 个绑定样本，30 个奖励到账样本，10 个申诉样本 | 成功、重复绑定、账本延迟、冲正 |
| technician-feed | 50 个浏览样本，20 个点赞样本，20 个评论样本 | 成功、审核阻断、计数降级、内容拦截 |

## 4. 放量前检查
1. 开关快照正确。
2. 白名单正确。
3. `RESERVED_DISABLED` 最近 24 小时误返回计数 = 0。
4. 客服/运营话术已切换到灰度版本。
5. 五键日志可检索。

## 5. 验收观察项

| 观察项 | 通过标准 |
|---|---|
| 主成功率 | 不低于基线（`degraded_pool` 不计入） |
| 错误率 | 不高于基线 + 1 个百分点 |
| 降级率 | `<=3%`（只统计 `degraded_pool`） |
| 恢复率 | `>=85%` |
| 客服投诉量 | 不超过基线 2 倍 |
| `RESERVED_DISABLED` 误返回 | 必须为 0 |

## 6. 回滚阈值

| 触发条件 | 回滚动作 |
|---|---|
| 任一 `RESERVED_DISABLED` 在开关关闭态或越权范围返回 | 立即回滚开关，锁定发布 |
| 主成功率低于基线 2 个窗口 | 暂停灰度，回退到上一阶段 |
| 错误率高于基线 2 个百分点 | 暂停灰度，回滚上一稳定配置 |
| 降级率 `>5%` | 暂停灰度，复核依赖链路 |
| 客服工单量 15 分钟内爆发 3 倍 | 立即回滚并发布公告 |

## 7. 不得越级上线规则
1. G1 未完成不得进入 G2。
2. 任一阶段有 `NO_GO` 结果，不得继续推进。
3. 任一阶段未完成样本量，不得判定通过。
4. 灰度阶段未完成回滚演练，不得推进下一阶段。

## 7.1 五键记录规则
- 每个阶段都必须记录：`runId/orderId/payRefundId/sourceBizNo/errorCode`。
- 不适用字段固定填 `"0"`：
  - 灰度台账巡检：`orderId=0`、`payRefundId=0`
  - 样本命中真实业务对象时，优先透传真实 `orderId/payRefundId/sourceBizNo`
  - 无单一错误码的阶段性观察可填 `errorCode=0`

## 8. `RESERVED_DISABLED` 命中即回滚判断

| 能力 | 错误码 | 判定 |
|---|---|---|
| gift-card | `1011009901`、`1011009902` | 开关关闭态或非灰度命中即回滚 |
| referral | `1013009901`、`1013009902` | 开关关闭态或非灰度命中即回滚 |
| technician-feed | `1030009901` | 开关关闭态或非灰度命中即回滚 |

## 9. 分阶段验收步骤

### 9.1 G1 5%
1. 只对内部账号/白名单门店放量。
2. 完成最小样本 30%。
3. 观察 30 分钟，若稳定进入 G2。

### 9.2 G2 20%
1. 扩到低风险门店/用户组。
2. 完成最小样本 60%。
3. 观察 30 分钟，若稳定进入 G3。

### 9.3 G3 50%
1. 扩到中等风险用户组。
2. 完成 100% 样本。
3. 观察 60 分钟，确认客服和运营负载可控。

### 9.4 G4 100%
1. 全量放开但保留高敏告警阈值 24 小时。
2. 连续 120 分钟稳定后，才能将 capability 状态改为 `ACTIVE`。

## 10. 结论模板

| 结论 | 条件 | 后续动作 |
|---|---|---|
| `PASS` | 样本达标、指标达标、无误返回 | 推进下一阶段或转 `ACTIVE` |
| `PASS_WITH_WATCH` | 非阻断项轻微异常 | 保持当前阶段继续观察 |
| `FAIL_ROLLBACK` | 触发任一回滚条件 | 立即回滚并复盘 |
| `BLOCKED_NO_IMPL` | 页面 / controller / checklist 任一未满足 | 保持 `NO_GO`，不得启动灰度 |

## 11. 审计字段
- 每个阶段都必须记录：
  - `releaseId`
  - `capability`
  - `stage`
  - `sampleCount`
  - `passCount`
  - `failCount`
  - `switchSnapshot`
  - `runId/orderId/payRefundId/sourceBizNo/errorCode`

## 12. 验收标准
1. 灰度比例、样本、观察项、回滚阈值、升级规则完整。
2. gift/referral/feed 三个能力都有明确的 `RESERVED_DISABLED` 回滚条件。
3. 状态从 `PLANNED_RESERVED` 到 `ACTIVE` 的变更有唯一准入路径。
4. 整个灰度过程支持审计与复盘。
5. capability 未落地时会被明确挡在 `BLOCKED_NO_IMPL / NO_GO`，不会跳过灰度前置门禁。
