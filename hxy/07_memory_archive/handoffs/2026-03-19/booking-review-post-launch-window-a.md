# Booking Review Post-Launch Window A Handoff（2026-03-19）

## 1. 本批目标
- 完成 booking review 03-19 上线后复核的历史边界、店长归属真值、admin-only 增强 backlog 和 release 语义加固。

## 2. 本批吸收的既有提交
- `3c34cdc52f feat(booking-review): add negative manager todo workflow`
- `f1569f30b4 feat(admin): add booking review manager todo ops`
- `b7a51b9a0f docs(booking-review): freeze manager todo truth`

## 3. 本批新增真值
1. 店长待办状态机当前以服务端校验为准，非法流转 fail-close。
2. 历史差评若 `managerTodoStatus=null`，只会在店长待办写动作时 lazy-init，不会在 list / dashboard read-path 自动修复。
3. 缺失 booking order 行的历史差评仍可在写路径补齐 `negativeTriggerType` 与 SLA 截止时间，但联系人快照可能为空。
4. 当前只核到门店 `contactName / contactMobile`，没有稳定 `store -> managerUserId`。

## 4. 本批新增文档
- `docs/products/miniapp/2026-03-19-miniapp-booking-review-history-and-boundary-audit-v1.md`
- `docs/products/miniapp/2026-03-19-miniapp-booking-review-manager-ownership-truth-review-v1.md`
- `docs/plans/2026-03-19-booking-review-admin-ops-enhancement-backlog-v1.md`

## 5. 主索引已同步更新
- `docs/contracts/2026-03-17-miniapp-booking-review-contract-v1.md`
- `docs/contracts/2026-03-17-miniapp-booking-review-errorcode-and-failure-mode-v1.md`
- `docs/plans/2026-03-17-miniapp-booking-review-release-gate-v1.md`
- `docs/products/miniapp/2026-03-10-miniapp-capability-status-ledger-v1.md`
- `docs/products/miniapp/2026-03-12-miniapp-business-function-truth-ledger-v1.md`
- `docs/products/miniapp/2026-03-17-miniapp-booking-review-page-field-dictionary-v1.md`

## 6. 当前结论

| 维度 | 当前结论 |
|---|---|
| 文档状态 | `Doc Closed` |
| 当前是否可开发 | `Yes` |
| 当前是否可放量 | `No` |
| Release Decision | `No-Go` |

## 7. 对后续窗口的联调注意点

### 7.1 产品 / PRD
1. 不得把后台店长待办写成自动店长通知。
2. 不得把历史差评可写入修复写成“系统已自动补齐历史数据”。

### 7.2 Contract / ErrorCode
1. `BOOKING_REVIEW_NOT_ELIGIBLE(1030008002)` 当前覆盖非法 manager todo 状态流转。
2. 不得补造 `managerUserId`、`ownerUserId` 或消息通道错误码。

### 7.3 Runbook / Gate
1. 03-19 修复只代表 fail-close 更稳，不代表 release-ready。
2. dashboard / SLA 统计仍不覆盖所有历史未初始化差评。
