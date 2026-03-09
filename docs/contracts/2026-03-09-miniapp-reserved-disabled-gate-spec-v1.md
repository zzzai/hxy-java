# MiniApp RESERVED_DISABLED Gate Spec v1 (2026-03-09)

## 1. 目标
- 将 `RESERVED_DISABLED` 错误码从“文档约定”升级为“可执行门禁”。
- 保证预留能力默认禁用，灰度可控，回滚可执行。
- 明确：`RESERVED_DISABLED` 误返回即 P1 事故。

## 2. 适用错误码集合
- `1008009901` `MINIAPP_HOME_ACTIVITY_CONTEXT_MISMATCH`
- `1008009902` `MINIAPP_CATALOG_VERSION_MISMATCH`
- `1008009903` `MINIAPP_ADDON_INTENT_CONFLICT`
- `1004009901` `MINIAPP_ASSET_LEDGER_MISMATCH`
- `1008009904` `MINIAPP_SEARCH_QUERY_INVALID`
- `1011009901` `GIFT_CARD_ORDER_NOT_FOUND`
- `1011009902` `GIFT_CARD_REDEEM_CONFLICT`
- `1013009901` `REFERRAL_BIND_CONFLICT`
- `1013009902` `REFERRAL_REWARD_LEDGER_MISMATCH`
- `1030009901` `TECHNICIAN_FEED_AUDIT_BLOCKED`

## 3. 开关规范

| 开关键 | 默认值 | 作用域 | 生效对象 | 负责人 |
|---|---|---|---|---|
| `miniapp.home.context-check` | `off` | 全局 + 门店白名单 | `1008009901` | Product on-call |
| `miniapp.catalog.version-guard` | `off` | 全局 + 门店白名单 | `1008009902` | Product on-call |
| `miniapp.addon.intent-idempotency` | `off` | 全局 + 请求头白名单 | `1008009903` | Booking on-call |
| `miniapp.asset.ledger` | `off` | 全局 + 用户分组白名单 | `1004009901` | Member on-call |
| `miniapp.search.validation` | `off` | 全局 + 用户分组白名单 | `1008009904` | Search owner |
| `miniapp.gift-card` | `off` | 全局 + 门店白名单 | `1011009901/1011009902` | Trade on-call |
| `miniapp.referral` | `off` | 全局 + 用户分组白名单 | `1013009901/1013009902` | Promotion on-call |
| `miniapp.technician-feed.audit` | `off` | 全局 + 门店白名单 | `1030009901` | Booking + Content on-call |

## 4. 灰度策略
1. 准入条件
- 契约冻结完成（含 API 字段、错误码、降级语义）。
- 监控项就绪：错误码计数、成功率、降级率、P0/P1 告警。
- 客服/运营处置 SOP 已演练。

2. 灰度节奏
- `5% -> 20% -> 50% -> 100%`，每阶段观测至少 30 分钟。
- 阶段放量门槛：
  - 不得出现新增 P0。
  - RESERVED_DISABLED 误返回计数 = 0。
  - 主链路成功率不低于发布前基线。

3. 阶段阻断条件
- 任一 RESERVED_DISABLED 码在禁用态返回。
- 冲突码或资金链路错误码趋势异常上升。
- 客服工单在 15 分钟内出现集中爆发。

## 5. 回滚规范
- 回滚触发：满足任一阶段阻断条件。
- 回滚步骤：
  1. 将对应开关立即置 `off`。
  2. 恢复上一稳定配置快照。
  3. 触发补偿任务（如 ticket sync、资产对账、缓存刷新）。
  4. 发布客服与运营口径更新。
  5. 复核指标并确认恢复。
- 回滚时限：15 分钟内完成开关回退与公告同步。

## 6. 误返回即 P1 处置流程

### 6.1 定义
- 误返回：任一 `RESERVED_DISABLED` 错误码在“开关为 off 或未命中灰度范围”条件下返回给用户端。

### 6.2 处置等级
- 严重级别：P1（固定）。

### 6.3 处置时序（SLA）
- 5 分钟内：值班 SRE 确认并拉起 P1 频道。
- 10 分钟内：对应域 on-call 完成开关回滚。
- 30 分钟内：完成影响范围确认与用户侧止血口径。
- 24 小时内：完成复盘与防再发措施。

### 6.4 责任人（RACI）
- Incident Commander：值班 SRE
- Technical Owner：对应域 on-call（Product/Booking/Member/Trade/Promotion/Content）
- Business Owner：运营值班负责人
- User Communication Owner：客服组长

### 6.5 必填审计字段
- `runId/orderId/payRefundId/sourceBizNo/errorCode`
- 额外建议：`switchKey/switchValue/grayScope/matchedRule`

## 7. 门禁执行建议
- 发布前：扫描近 24 小时日志，确保 RESERVED_DISABLED 误返回计数为 0。
- 发布中：每 5 分钟自动巡检一次 reserved-disabled 错误码。
- 发布后：24 小时内保留高敏告警阈值（1 次即告警）。
