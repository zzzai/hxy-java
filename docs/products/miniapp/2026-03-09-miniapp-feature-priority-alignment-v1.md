# MiniApp 功能优先级对齐决议 v1（2026-03-09）

## 1. 文档目标
- 目标：对 `gift-card / referral / technician-feed` 给出单一生效的 P 级、RB 批次与 runtime 边界，消除“规划冻结 = 运行上线”的误写。
- 适用范围：`feat/ui-four-account-reconcile-ops` 分支的产品、契约、联调执行、发布门禁。

## 2. 对齐基线与判定顺序

### 2.1 对齐文档
- `docs/products/miniapp/2026-03-09-miniapp-feature-inventory-and-release-matrix-v1.md`
- `docs/products/miniapp/2026-03-09-miniapp-gift-card-business-prd-v1.md`
- `docs/products/miniapp/2026-03-09-miniapp-referral-business-prd-v1.md`
- `docs/products/miniapp/2026-03-09-miniapp-technician-feed-product-policy-v1.md`
- `docs/contracts/2026-03-09-miniapp-gift-card-domain-contract-v1.md`
- `docs/contracts/2026-03-09-miniapp-referral-domain-contract-v1.md`
- `docs/contracts/2026-03-09-miniapp-technician-feed-contract-v1.md`
- `docs/plans/2026-03-11-miniapp-reserved-runtime-readiness-register-v1.md`
- `docs/products/miniapp/2026-03-14-miniapp-runtime-blocker-product-closure-v1.md`

### 2.2 判定顺序（单一真值）
1. P 级与 RB 批次：以 `feature-inventory-and-release-matrix` 为准。
2. runtime 是否存在：以真实 page、真实 app controller、真实样本为准。
3. 错误码与降级语义：以 contract 中已真实对外暴露的 code 为准；治理占位码不能反推成 runtime 已上线。
4. 业务流程与客服口径：以对应 PRD 为准。

## 3. 当前统一结论
- 三项 capability 的优先级结论不变：全部 `P2 / RB3-P2`。
- 三项 capability 的 runtime 结论不变：全部 `Doc Closed / Can Develop / Cannot Release`。
- 规划文档、治理文档、activation checklist、gray runbook 已齐，不等于 runtime 已上线。

## 4. 最终 P 级、RB 批次与 runtime 边界

| 功能域 | 最终 P 级 | 最终 RB 批次 | 当前页面真值 | 当前 app controller 真值 | 当前产品标签 | 当前结论 |
|---|---|---|---|---|---|---|
| Gift Card（礼品卡） | P2 | RB3-P2 | 无真实 `/pages/gift-card/*` | 无真实 `/promotion/gift-card/*` app controller | `Doc Closed / Can Develop / Cannot Release` | 规划冻结完成，但 runtime 未实现 |
| Referral（邀请有礼） | P2 | RB3-P2 | 无真实 `/pages/referral/*` | 无真实 `/promotion/referral/*` app controller | `Doc Closed / Can Develop / Cannot Release` | 规划冻结完成，但 runtime 未实现 |
| Technician Feed（技师动态） | P2 | RB3-P2 | 无真实 `/pages/technician/feed` | 无真实 `/booking/technician/feed/*` app controller | `Doc Closed / Can Develop / Cannot Release` | 规划冻结完成，但 runtime 未实现 |

## 5. 为什么“规划冻结”不能写成“运行上线”
1. 当前三项都没有真实用户页和真实 app controller，无法形成 runtime 能力。
2. `RESERVED_DISABLED`、activation checklist、gray runbook 只证明治理与灰度规则齐备，不证明能力已实现。
3. 没有真实抓包、真实样本、真实五键日志前，不得把任何一项写成“已进入灰度”或“可放量”。
4. 若无真实对外暴露证据，不得在产品 PRD 中承诺稳定错误码分支。

## 6. 开发进入条件
以下条件满足后，三项 capability 可以按 `Can Develop` 进入工程实现：

1. 真实页面文件存在并进入 `pages.json`。
2. 真实 app controller 存在，且 method + path 与 contract 对齐。
3. 开关默认关闭态、灰度态、开启态都写入发布口径。
4. capability ledger、release decision、PRD、contract 同步回填为真实 route 与真实 API。

## 7. 放量进入条件
以下条件任一缺失，三项 capability 均保持 `Cannot Release`：

1. 最小用户样本包可回放。
2. 五键日志可检索：`runId / orderId / payRefundId / sourceBizNo / errorCode`。
3. `RESERVED_DISABLED` 关闭态误返回计数为 `0`。
4. A 窗口重新签发 allowlist，D 窗口完成灰度观察与回滚演练。

## 8. 执行约束
- 不允许再出现“同一功能多套 P 级 / RB”描述。
- 不允许把 `P2 / RB3-P2` 写成“已上线但优先级较低”。
- 不允许把治理冻结、规划完备、文档齐全写成 runtime 已上线。
- 所有窗口后续只能使用 `Can Develop` 或 `Cannot Release`，不得使用“可放心放量”。

## 9. 对 A / C / D 的同步要求
- A：三项 capability 继续按 `P2 / RB3-P2 + Cannot Release` 管理，不能提前放入灰度包。
- C：只保留真实 method + path 与真实 outward code；没有 runtime 证据时不要承诺稳定错误码分支。
- D：继续把三项当作 reserved runtime 缺项，不进入本批放量门禁通过项。
