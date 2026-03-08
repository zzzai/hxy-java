# MiniApp ErrorCode Canonical Register v1 (2026-03-09)

## 1. 目标与范围
- 目标：完成 miniapp 错误码“去占位符”收口，形成唯一注册口径。
- 范围：支付/交易/预约/商品/会员/促销 + P1/P2 预留能力（首页、项目页、加购冲突、预约排期、资产账本、搜索、礼品卡、邀请、技师 feed）。
- 强约束：
  - 不允许返回未注册错误码。
  - 预留错误码必须声明“生效条件 + 禁用态”。
  - 前端、客服、运营必须按错误码处理，不按 message 处理。

## 2. 判定规则：fail-open / fail-close

### 2.1 fail-close（直接阻断）
- 资金一致性、幂等冲突、库存冲突、状态机非法流转。
- 典型错误码：`1030004012`、`1030002001`、`1030003002`、`1008006004`。
- 行为：返回业务错误，禁止自动重试，进入人工/运营流程。

### 2.2 fail-open（主链路继续）
- 下游依赖异常但不影响主交易事实写入（工单、聚合、异步统计）。
- 典型锚点：`TICKET_SYNC_DEGRADED`、`PAY_ORDER_NOT_FOUND`（degradeReason）。
- 行为：主链路成功 + `degraded/warning` 标记 + 可追踪补偿任务。

## 3. 错误码全量注册表

| code | name | domain | 状态 | 触发条件 | 前端动作 | 客服动作 | 重试策略 | 生效条件 | 禁用态 |
|---:|---|---|---|---|---|---|---|---|---|
| 1011000011 | ORDER_NOT_FOUND | trade | ACTIVE | 订单查询不存在 | 错误态保活，允许返回列表 | 连续 3 次失败升级 P1 | 手动重试最多 3 次 | 已生效 | - |
| 1011000100 | AFTER_SALE_NOT_FOUND | trade | ACTIVE | 售后单不存在 | 回退售后列表并保活页面 | 记录 warning，观察趋势 | 5s*3 轮询后停止 | 已生效 | - |
| 1011000125 | AFTER_SALE_REFUND_FAIL_BUNDLE_CHILD_FULFILLED | trade | ACTIVE | 履约子项阻断退款 | 阻断提交，提示人工审核 | 30 分钟内转人工复核 | 不自动重试 | 已生效 | - |
| 1030004012 | BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT | booking | ACTIVE | 同单不同退款单号/同键异参冲突 | 阻断并展示冲突码 | 立即升级 P0 | 禁止自动重试 | 已生效 | - |
| 1030004016 | BOOKING_ORDER_REFUND_REPLAY_RUN_ID_NOT_EXISTS | booking | ACTIVE | runId 不存在 | 页面保活，提示刷新 run 列表 | 审计告警，必要时转运维 | 刷新 1 次，失败人工排查 | 已生效 | - |
| 1011003004 | PRICE_CALCULATE_POINT_TOTAL_LIMIT_COUNT | trade | ACTIVE | 积分规则限购命中 | 提示调整数量/组合 | 解释规则，无需技术升级 | 不自动重试 | 已生效 | - |
| TAG | TICKET_SYNC_DEGRADED | booking/trade | ACTIVE_TAG | 工单同步下游失败 | 主链路成功 + warning | 监控并跟进补偿 | 后台任务重试 | 已生效 | - |
| REASON | PAY_ORDER_NOT_FOUND | trade/pay | ACTIVE_REASON | 支付聚合缺失 pay 单 | 展示 WAITING + warning | 指导刷新，超阈值升级 | 手动重试最多 3 次 | 已生效 | - |
| 1008001000 | CATEGORY_NOT_EXISTS | product | ACTIVE | 类目不存在 | 回退类目默认页 | 核验活动配置 | 不自动重试 | 已生效 | - |
| 1008005000 | SPU_NOT_EXISTS | product | ACTIVE | SPU 不存在 | 显示下架/不可售 | 提供替代商品 | 不自动重试 | 已生效 | - |
| 1008006000 | SKU_NOT_EXISTS | product | ACTIVE | SKU 不存在 | 提示刷新规格 | 核验商品配置 | 不自动重试 | 已生效 | - |
| 1008006004 | SKU_STOCK_NOT_ENOUGH | product | ACTIVE | 库存不足 | 回显可购数量 | 引导补货通知 | 可手动重试 1 次 | 已生效 | - |
| 1008009006 | STORE_SKU_STOCK_BIZ_KEY_CONFLICT | product | ACTIVE | 门店库存幂等冲突 | 阻断并提示刷新库存 | 升级商品 on-call | 不自动重试 | 已生效 | - |
| 1030002001 | SCHEDULE_CONFLICT | booking | ACTIVE | 排班冲突 | 展示冲突并给可选时段 | 转预约排期人工辅助 | 不自动重试 | 已生效 | - |
| 1030003001 | TIME_SLOT_NOT_AVAILABLE | booking | ACTIVE | 时段不可预约 | 提示换时段 | 协助改约 | 可手动重选后重试 | 已生效 | - |
| 1030003002 | TIME_SLOT_ALREADY_BOOKED | booking | ACTIVE | 时段已被占用 | 刷新时段并重新选择 | 协助用户改期 | 不自动重试 | 已生效 | - |
| 1004001000 | USER_NOT_EXISTS | member | ACTIVE | 用户不存在 | 引导重新登录/注册 | 核验账号状态 | 不自动重试 | 已生效 | - |
| 1004008000 | POINT_RECORD_BIZ_NOT_SUPPORT | member | ACTIVE | 资产账本业务类型不支持 | 提示稍后查看账本 | 升级会员资产负责人 | 不自动重试 | 已生效 | - |
| 1013005000 | COUPON_NOT_EXISTS | promotion | ACTIVE | 优惠券记录不存在 | 局部刷新券列表 | 协助核验发券记录 | 手动刷新 1 次 | 已生效 | - |
| 1013004000 | COUPON_TEMPLATE_NOT_EXISTS | promotion | ACTIVE | 券模板不存在 | 隐藏不可用模板 | 核验活动配置 | 不自动重试 | 已生效 | - |
| 1030001000 | TECHNICIAN_NOT_EXISTS | booking | ACTIVE | 技师不存在 | 列表回退并提示重选 | 核验技师上下架 | 不自动重试 | 已生效 | - |
| 1030001001 | TECHNICIAN_DISABLED | booking | ACTIVE | 技师被禁用 | 提示不可预约并推荐其他技师 | 协助改约 | 不自动重试 | 已生效 | - |
| 1008009901 | MINIAPP_HOME_ACTIVITY_CONTEXT_MISMATCH | product/promotion | RESERVED_DISABLED | 首页活动与门店上下文不一致 | 触发强制刷新并降级展示 | 记录配置冲突工单 | 自动重试关闭 | 功能开关 `miniapp.home.context-check=on` 且 A/B 验证通过 | 默认禁用；线上不应返回 |
| 1008009902 | MINIAPP_CATALOG_VERSION_MISMATCH | product | RESERVED_DISABLED | 项目页版本快照不一致 | 强制全量刷新类目与库存 | 通知商品运营核对版本 | 立即刷新 1 次 | 功能开关 `miniapp.catalog.version-guard=on` | 默认禁用；线上不应返回 |
| 1008009903 | MINIAPP_ADDON_INTENT_CONFLICT | product/booking | RESERVED_DISABLED | 加购意图同键异参冲突 | 阻断并回显服务端数量 | 转运营核验库存与并发策略 | 不自动重试 | 功能开关 `miniapp.addon.intent-idempotency=on` | 默认禁用；线上不应返回 |
| 1004009901 | MINIAPP_ASSET_LEDGER_MISMATCH | member/promotion | RESERVED_DISABLED | 券/积分/权益口径不一致 | 展示局部降级标签 | 升级资产账本负责人 | 后台补偿后重查 | 开启账本统一聚合 `miniapp.asset.ledger=on` | 默认禁用；线上不应返回 |
| 1008009904 | MINIAPP_SEARCH_QUERY_INVALID | product/search | RESERVED_DISABLED | 搜索 query 不合法 | 保留 query 并提示修正 | 指导用户简化关键词 | 可手动重试 | 功能开关 `miniapp.search.validation=on` | 默认禁用；线上不应返回 |
| 1011009901 | GIFT_CARD_ORDER_NOT_FOUND | trade | RESERVED_DISABLED | 礼品卡订单查询不存在 | 提示订单不存在并回到列表 | 转礼品卡人工核验 | 不自动重试 | gift-card 域上线且 `miniapp.gift-card=on` | 默认禁用；线上不应返回 |
| 1011009902 | GIFT_CARD_REDEEM_CONFLICT | trade | RESERVED_DISABLED | 礼品卡核销同键异参 | 阻断并提示联系客服 | 升级礼品卡 on-call | 不自动重试 | gift-card 域上线且核销接口启用 | 默认禁用；线上不应返回 |
| 1013009901 | REFERRAL_BIND_CONFLICT | promotion | RESERVED_DISABLED | 被邀请人重复绑定不同邀请人 | 阻断并提示规则 | 转邀请活动运营复核 | 不自动重试 | referral 域上线且 `miniapp.referral=on` | 默认禁用；线上不应返回 |
| 1013009902 | REFERRAL_REWARD_LEDGER_MISMATCH | promotion/member | RESERVED_DISABLED | 邀请奖励账本与订单事实不一致 | 展示到账处理中 | 转运营+技术对账 | 后台对账任务重试 | referral 域上线且对账链路启用 | 默认禁用；线上不应返回 |
| 1030009901 | TECHNICIAN_FEED_AUDIT_BLOCKED | booking/content | RESERVED_DISABLED | 技师动态审核阻断 | 展示审核中/不可见 | 转内容审核组处理 | 不自动重试 | feed 域上线且审核开关启用 | 默认禁用；线上不应返回 |

## 4. 预留码段说明
- Product 预留：`1_008_099_001` ~ `1_008_099_099`
- Member 预留：`1_004_099_001` ~ `1_004_099_099`
- Trade 预留：`1_011_099_001` ~ `1_011_099_099`
- Promotion 预留：`1_013_099_001` ~ `1_013_099_099`
- Booking 预留：`1_030_099_001` ~ `1_030_099_099`

## 5. 生效治理
- 新增错误码进入 ACTIVE 前必须满足：
  1. 契约文档冻结
  2. 回归用例覆盖（happy/error/degrade）
  3. 客服与运营动作完成演练
  4. 灰度 5% -> 20% -> 50% -> 100% 无新增 P0
- RESERVED_DISABLED 错误码在禁用态下若被返回，按 P1 配置异常处理。
