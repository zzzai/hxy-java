# MiniApp ErrorCode Canonical Register v1 (2026-03-09)

## 1. 目标与范围
- 目标：完成 miniapp 错误码收口，形成唯一可执行注册表。
- 适用范围：trade/booking/product/member/promotion + reserved-disabled 预留能力。
- 强约束：
  - 未注册错误码禁止对外返回。
  - `RESERVED_DISABLED` 错误码必须满足“开关 on + 命中灰度范围”才允许返回。
  - 所有错误码必须显式声明 `failureMode`（`FAIL_OPEN` 或 `FAIL_CLOSE`）。

## 2. 分类定义
- `FAIL_CLOSE`：业务冲突/资金一致性/库存冲突/状态非法，必须阻断。
- `FAIL_OPEN`：下游依赖异常但主链路可继续，返回 warning/degraded 并补偿。
- `severity`：`P0/P1/P2` 为处置等级。
- `retryClass`：统一可执行策略，禁止自由解释。

## 3. Canonical Register

| code | name | domain | status | failureMode | severity | retryClass | trigger | userAction | opsAction | 生效条件 | 禁用态 |
|---:|---|---|---|---|---|---|---|---|---|---|---|
| 1011000011 | ORDER_NOT_FOUND | trade | ACTIVE | FAIL_CLOSE | P2 | MANUAL_RETRY_3 | 订单查询不存在 | 返回列表后重试 | 连续 3 次失败转 P1 工单 | 已生效 | - |
| 1011000100 | AFTER_SALE_NOT_FOUND | trade | ACTIVE | FAIL_CLOSE | P2 | POLL_5S_X3 | 售后单不存在 | 回退售后列表 | 记录 warning 并观察趋势 | 已生效 | - |
| 1011000125 | AFTER_SALE_REFUND_FAIL_BUNDLE_CHILD_FULFILLED | trade | ACTIVE | FAIL_CLOSE | P1 | NO_AUTO_RETRY | 已履约子项阻断退款 | 转人工审核 | 30 分钟内转交易 on-call | 已生效 | - |
| 1030004012 | BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT | booking | ACTIVE | FAIL_CLOSE | P0 | NO_AUTO_RETRY | 同单不同退款单号/同键异参 | 停止重试并联系客服 | 立即拉起 P0，冻结重复请求 | 已生效 | - |
| 1030004016 | BOOKING_ORDER_REFUND_REPLAY_RUN_ID_NOT_EXISTS | booking | ACTIVE | FAIL_CLOSE | P2 | REFRESH_ONCE | runId 不存在 | 刷新 run 列表 | 审计告警，必要时运维排查 | 已生效 | - |
| 1011003004 | PRICE_CALCULATE_POINT_TOTAL_LIMIT_COUNT | trade | ACTIVE | FAIL_CLOSE | P2 | NO_AUTO_RETRY | 积分规则限购命中 | 调整购买数量/组合 | 客服解释规则，无技术升级 | 已生效 | - |
| TAG | TICKET_SYNC_DEGRADED | booking/trade | ACTIVE_TAG | FAIL_OPEN | P2 | BG_RETRY_JOB | ticket 同步下游失败 | 主链路继续，稍后查看结果 | 触发后台重试并监控恢复 | 已生效 | - |
| REASON | PAY_ORDER_NOT_FOUND | trade/pay | ACTIVE_REASON | FAIL_OPEN | P2 | MANUAL_RETRY_3 | pay 聚合缺失 pay 单 | 下拉刷新（最多 3 次） | 监控支付依赖，超阈值转 P1 | 已生效 | - |
| 1008001000 | CATEGORY_NOT_EXISTS | product | ACTIVE | FAIL_CLOSE | P2 | REFRESH_ONCE | 类目不存在 | 切换默认类目并重试 | 核验类目配置 | 已生效 | - |
| 1008005000 | SPU_NOT_EXISTS | product | ACTIVE | FAIL_CLOSE | P2 | NO_AUTO_RETRY | SPU 不存在 | 返回上一页并重选商品 | 商品运营核验上下架 | 已生效 | - |
| 1008006000 | SKU_NOT_EXISTS | product | ACTIVE | FAIL_CLOSE | P2 | REFRESH_ONCE | SKU 不存在 | 刷新规格并重选 | 商品 on-call 校验 SKU 映射 | 已生效 | - |
| 1008006004 | SKU_STOCK_NOT_ENOUGH | product | ACTIVE | FAIL_CLOSE | P2 | NO_AUTO_RETRY | SKU 库存不足 | 减少数量后重试 | 运营关注库存阈值 | 已生效 | - |
| 1008009006 | STORE_SKU_STOCK_BIZ_KEY_CONFLICT | product | ACTIVE | FAIL_CLOSE | P1 | NO_AUTO_RETRY | 门店库存幂等冲突 | 刷新库存并重提 | 商品 on-call 排查并发/幂等 | 已生效 | - |
| 1030002001 | SCHEDULE_CONFLICT | booking | ACTIVE | FAIL_CLOSE | P1 | NO_AUTO_RETRY | 排班冲突 | 重选时段 | 预约排期负责人复核排班 | 已生效 | - |
| 1030003001 | TIME_SLOT_NOT_AVAILABLE | booking | ACTIVE | FAIL_CLOSE | P2 | REFRESH_ONCE | 时段不可预约 | 选择其他时段 | 检查时段实时性 | 已生效 | - |
| 1030003002 | TIME_SLOT_ALREADY_BOOKED | booking | ACTIVE | FAIL_CLOSE | P2 | NO_AUTO_RETRY | 时段被占用 | 重新选时段 | 监控时段抢占冲突率 | 已生效 | - |
| 1004001000 | USER_NOT_EXISTS | member | ACTIVE | FAIL_CLOSE | P1 | NO_AUTO_RETRY | 用户身份无效 | 重新登录 | 账号体系值班排查 | 已生效 | - |
| 1004008000 | POINT_RECORD_BIZ_NOT_SUPPORT | member | ACTIVE | FAIL_CLOSE | P1 | NO_AUTO_RETRY | 资产账本业务类型不支持 | 稍后再查账本 | 会员资产负责人排查业务映射 | 已生效 | - |
| 1013005000 | COUPON_NOT_EXISTS | promotion | ACTIVE | FAIL_CLOSE | P2 | REFRESH_ONCE | 优惠券记录不存在 | 刷新券列表 | 运营核验发券流水 | 已生效 | - |
| 1013004000 | COUPON_TEMPLATE_NOT_EXISTS | promotion | ACTIVE | FAIL_CLOSE | P2 | NO_AUTO_RETRY | 券模板不存在 | 选择其他活动券 | 运营核验模板配置 | 已生效 | - |
| 1030001000 | TECHNICIAN_NOT_EXISTS | booking | ACTIVE | FAIL_CLOSE | P2 | NO_AUTO_RETRY | 技师不存在 | 更换技师 | 预约域核验技师数据 | 已生效 | - |
| 1030001001 | TECHNICIAN_DISABLED | booking | ACTIVE | FAIL_CLOSE | P2 | NO_AUTO_RETRY | 技师已禁用 | 更换技师 | 预约域核验启停状态 | 已生效 | - |
| 1008009901 | MINIAPP_HOME_ACTIVITY_CONTEXT_MISMATCH | product/promotion | RESERVED_DISABLED | FAIL_OPEN | P1 | REFRESH_ONCE | 首页活动与上下文不一致 | 强制刷新后重试 | 若禁用态返回，立即回滚开关并开 P1 | `miniapp.home.context-check=on` 且命中灰度 | 默认禁用 |
| 1008009902 | MINIAPP_CATALOG_VERSION_MISMATCH | product | RESERVED_DISABLED | FAIL_OPEN | P1 | REFRESH_ONCE | 目录版本快照不一致 | 强制全量刷新 | 若禁用态返回，立即回滚并开 P1 | `miniapp.catalog.version-guard=on` 且命中灰度 | 默认禁用 |
| 1008009903 | MINIAPP_ADDON_INTENT_CONFLICT | product/booking | RESERVED_DISABLED | FAIL_CLOSE | P1 | NO_AUTO_RETRY | 加购意图同键异参冲突 | 按服务端可购量重提 | 若禁用态返回，立即回滚并开 P1 | `miniapp.addon.intent-idempotency=on` 且命中灰度 | 默认禁用 |
| 1004009901 | MINIAPP_ASSET_LEDGER_MISMATCH | member/promotion | RESERVED_DISABLED | FAIL_OPEN | P1 | BG_RETRY_JOB | 资产账本口径不一致 | 展示处理中，稍后刷新 | 若禁用态返回，立即回滚并开 P1 | `miniapp.asset.ledger=on` 且命中灰度 | 默认禁用 |
| 1008009904 | MINIAPP_SEARCH_QUERY_INVALID | product/search | RESERVED_DISABLED | FAIL_CLOSE | P2 | MANUAL_RETRY_3 | 搜索 query 非法 | 修正关键词后重试 | 若禁用态返回，校验开关与参数校验链路 | `miniapp.search.validation=on` 且命中灰度 | 默认禁用 |
| 1011009901 | GIFT_CARD_ORDER_NOT_FOUND | trade | RESERVED_DISABLED | FAIL_CLOSE | P1 | NO_AUTO_RETRY | 礼品卡订单不存在 | 返回订单列表 | 若禁用态返回，立即回滚并开 P1 | `miniapp.gift-card=on` 且命中灰度 | 默认禁用 |
| 1011009902 | GIFT_CARD_REDEEM_CONFLICT | trade | RESERVED_DISABLED | FAIL_CLOSE | P1 | NO_AUTO_RETRY | 礼品卡核销同键异参 | 停止重试并联系客服 | 若禁用态返回，立即回滚并开 P1 | `miniapp.gift-card=on` 且核销能力灰度开启 | 默认禁用 |
| 1013009901 | REFERRAL_BIND_CONFLICT | promotion | RESERVED_DISABLED | FAIL_CLOSE | P1 | NO_AUTO_RETRY | 邀请绑定冲突 | 停止绑定并联系客服 | 若禁用态返回，立即回滚并开 P1 | `miniapp.referral=on` 且命中灰度 | 默认禁用 |
| 1013009902 | REFERRAL_REWARD_LEDGER_MISMATCH | promotion/member | RESERVED_DISABLED | FAIL_OPEN | P1 | BG_RETRY_JOB | 邀请奖励账本不一致 | 展示到账处理中 | 若禁用态返回，立即回滚并开 P1 | `miniapp.referral=on` 且对账灰度开启 | 默认禁用 |
| 1030009901 | TECHNICIAN_FEED_AUDIT_BLOCKED | booking/content | RESERVED_DISABLED | FAIL_CLOSE | P1 | NO_AUTO_RETRY | 技师动态审核阻断 | 内容不可见并提示审核中 | 若禁用态返回，立即回滚并开 P1 | `miniapp.technician-feed.audit=on` 且命中灰度 | 默认禁用 |

## 4. 预留码段
- Product：`1_008_099_001` ~ `1_008_099_099`
- Member：`1_004_099_001` ~ `1_004_099_099`
- Trade：`1_011_099_001` ~ `1_011_099_099`
- Promotion：`1_013_099_001` ~ `1_013_099_099`
- Booking：`1_030_099_001` ~ `1_030_099_099`

## 5. 生效门禁
- 进入 ACTIVE 前必须满足：
  1. 契约冻结 + 回归用例覆盖（happy/error/degrade）
  2. 客服 SOP 与运营处置演练通过
  3. 灰度 `5% -> 20% -> 50% -> 100%` 无新增 P0
- 任一 `RESERVED_DISABLED` 在禁用态返回：固定按 P1 处理。
