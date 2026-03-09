# MiniApp 功能优先级对齐决议 v1（2026-03-09）

## 1. 文档目标
- 目标：对 `gift-card / referral / technician-feed` 给出单一生效的 P 级与 RB 批次，消除历史漂移。
- 适用范围：`feat/ui-four-account-reconcile-ops` 分支的产品、契约、联调执行。

## 2. 对齐基线与优先级判定顺序
### 2.1 对齐文档
- `docs/products/miniapp/2026-03-09-miniapp-feature-inventory-and-release-matrix-v1.md`
- `docs/products/miniapp/2026-03-09-miniapp-gift-card-business-prd-v1.md`
- `docs/products/miniapp/2026-03-09-miniapp-referral-business-prd-v1.md`
- `docs/products/miniapp/2026-03-09-miniapp-technician-feed-product-policy-v1.md`
- `docs/contracts/2026-03-09-miniapp-gift-card-domain-contract-v1.md`
- `docs/contracts/2026-03-09-miniapp-referral-domain-contract-v1.md`
- `docs/contracts/2026-03-09-miniapp-technician-feed-contract-v1.md`
- `docs/contracts/2026-03-09-miniapp-errorcode-canonical-register-v1.md`

### 2.2 判定顺序（单一真值）
1. 发布批次与优先级：以 `feature-inventory-and-release-matrix` 为准。
2. 错误码与降级语义：以 `contract + canonical register` 为准。
3. 业务流程与页面承接：以对应 PRD 为准。

## 3. 漂移问题与收口结论
- 历史漂移：一致性审计记录了 gift/referral/feed 在不同文档中的 P 级差异。
- 本文结论：以“发布可用性 + 合约生效条件”收口，形成最终执行口径。

## 4. 最终 P 级与 RB 批次（冻结口径）

| 功能域 | 最终 P 级 | 最终 RB 批次 | 发布状态 | 生效条件 | 说明 |
|---|---|---|---|---|---|
| Gift Card（礼品卡） | P2 | RB3-P2 | 规划中（契约已冻结） | `miniapp.gift-card=on` 且相关 RESERVED_DISABLED 错误码启用条件达成 | 当前不进入 RB1/RB2 交付闸门 |
| Referral（邀请有礼） | P2 | RB3-P2 | 规划中（契约已冻结） | `miniapp.referral=on` 且奖励账本链路上线 | 当前以业务规则与客服口径预埋，不做发布阻断 |
| Technician Feed（技师动态） | P2 | RB3-P2 | 规划中（契约已冻结） | `feed` 审核链路启用且 `TECHNICIAN_FEED_AUDIT_BLOCKED(1030009901)` 生效条件满足 | 当前仅保留治理策略与审核口径 |

## 5. 对齐核对表（feature-matrix / PRD / contract）

| 功能域 | Feature Matrix | PRD | Contract | 对齐结果 |
|---|---|---|---|---|
| Gift Card | P2 / RB3-P2 | 业务流程完整，未强制声明上线批次 | API 与错误码为规划态，含 `RESERVED_DISABLED` 约束 | 一致 |
| Referral | P2 / RB3-P2 | 业务流程完整，未强制声明上线批次 | API 与错误码为规划态，含 `RESERVED_DISABLED` 约束 | 一致 |
| Technician Feed | P2 / RB3-P2 | 治理策略完整，未强制声明上线批次 | feed 契约规划态，审核阻断码为 `RESERVED_DISABLED` | 一致 |

## 6. 执行约束
- 不允许再出现“同一功能多套 P 级/RB”描述。
- 所有联调、验收、客服升级策略均按本决议执行。
- 若需调整 P 级或 RB 批次，按 `ready-to-frozen-review` 规则先回退 `Ready` 再重新冻结。

## 7. 对 A/C/D 的同步要求
- A：在集成与发布看板中将三项统一标记为 `P2/RB3-P2`。
- C：契约继续保持 `RESERVED_DISABLED` 治理，不提前放开生效态。
- D：验收与监控不将三项纳入 RB1/RB2 阻断指标，仅跟踪前置准备度。
