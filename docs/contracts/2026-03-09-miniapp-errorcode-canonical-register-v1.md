# MiniApp ErrorCode Canonical Register v1 (2026-03-09)

## 1. 目标与范围
- 目标：完成 miniapp 错误码收口，形成唯一可执行注册表。
- 适用范围：trade/booking/product/member/promotion + reserved-disabled 预留能力。
- 强约束：
  - 未注册错误码禁止对外返回。
  - `RESERVED_DISABLED` 错误码必须满足“开关 on + 命中灰度范围”才允许返回。
  - 所有错误码必须显式声明 `failureMode`（`FAIL_OPEN` 或 `FAIL_CLOSE`）。
  - 模块常量、治理文档或 switch 存在，不等于 canonical register 自动生效，也不等于对应 runtime capability 已上线。

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
| 1008005003 | SPU_NOT_ENABLE | product | ACTIVE | FAIL_CLOSE | P2 | NO_AUTO_RETRY | 商品未上架或已下架 | 返回商品列表并改选可售商品 | 商品运营核验上下架状态与发布时间 | 已生效 | - |
| 1008006000 | SKU_NOT_EXISTS | product | ACTIVE | FAIL_CLOSE | P2 | REFRESH_ONCE | SKU 不存在 | 刷新规格并重选 | 商品 on-call 校验 SKU 映射 | 已生效 | - |
| 1008006004 | SKU_STOCK_NOT_ENOUGH | product | ACTIVE | FAIL_CLOSE | P2 | NO_AUTO_RETRY | SKU 库存不足 | 减少数量后重试 | 运营关注库存阈值 | 已生效 | - |
| 1008008000 | FAVORITE_EXISTS | product | ACTIVE | FAIL_CLOSE | P2 | NO_AUTO_RETRY | 重复收藏同一商品 | 保持当前收藏态，不重复提交 | 商品前台负责人核验幂等与收藏态同步 | 已生效 | - |
| 1008008001 | FAVORITE_NOT_EXISTS | product | ACTIVE | FAIL_CLOSE | P2 | NO_AUTO_RETRY | 取消收藏时收藏记录不存在 | 刷新收藏列表后再操作 | 商品前台负责人排查收藏态脏读与并发删除 | 已生效 | - |
| 1008009006 | STORE_SKU_STOCK_BIZ_KEY_CONFLICT | product | ACTIVE | FAIL_CLOSE | P1 | NO_AUTO_RETRY | 门店库存幂等冲突 | 刷新库存并重提 | 商品 on-call 排查并发/幂等 | 已生效 | - |
| 1030002001 | SCHEDULE_CONFLICT | booking | ACTIVE | FAIL_CLOSE | P1 | NO_AUTO_RETRY | 排班冲突 | 重选时段 | 预约排期负责人复核排班 | 已生效 | - |
| 1030003001 | TIME_SLOT_NOT_AVAILABLE | booking | ACTIVE | FAIL_CLOSE | P2 | REFRESH_ONCE | 时段不可预约 | 选择其他时段 | 检查时段实时性 | 已生效 | - |
| 1030003002 | TIME_SLOT_ALREADY_BOOKED | booking | ACTIVE | FAIL_CLOSE | P2 | NO_AUTO_RETRY | 时段被占用 | 重新选时段 | 监控时段抢占冲突率 | 已生效 | - |
| 1030004000 | BOOKING_ORDER_NOT_EXISTS | booking | ACTIVE | FAIL_CLOSE | P2 | REFRESH_ONCE | 预约详情/取消/加项时订单不存在 | 刷新预约列表后重试 | 预约域核验订单归属与清理异常引用 | 已生效 | - |
| 1030004001 | BOOKING_ORDER_STATUS_ERROR | booking | ACTIVE | FAIL_CLOSE | P1 | NO_AUTO_RETRY | 预约订单状态不允许当前操作 | 刷新订单状态后按当前可用动作继续 | 预约域排查状态流转与重复操作冲突 | 已生效 | - |
| 1004001000 | USER_NOT_EXISTS | member | ACTIVE | FAIL_CLOSE | P1 | NO_AUTO_RETRY | 用户身份无效 | 重新登录 | 账号体系值班排查 | 已生效 | - |
| 1004001001 | USER_MOBILE_NOT_EXISTS | member/auth | ACTIVE | FAIL_CLOSE | P2 | NO_AUTO_RETRY | 重置密码或短信场景手机号未注册 | 核对手机号或去注册 | 会员认证负责人核验注册漏数与短信场景配置 | 已生效 | - |
| 1004001002 | USER_MOBILE_USED | member/profile | ACTIVE | FAIL_CLOSE | P2 | NO_AUTO_RETRY | 修改手机号时目标手机号已被占用 | 更换手机号后重试 | 会员资料负责人排查重复绑定与脏数据 | 已生效 | - |
| 1004001003 | USER_POINT_NOT_ENOUGH | member/asset | ACTIVE | FAIL_CLOSE | P2 | NO_AUTO_RETRY | 积分扣减时余额不足 | 调整兑换/支付方案 | 会员资产负责人核验余额流水与扣减规则 | 已生效 | - |
| 1004003000 | AUTH_LOGIN_BAD_CREDENTIALS | member/auth | ACTIVE | FAIL_CLOSE | P2 | NO_AUTO_RETRY | 手机号或密码错误 | 核对账号密码或改用短信登录 | 15 分钟内异常峰值上升则认证负责人排查攻击与配置 | 已生效 | - |
| 1004003001 | AUTH_LOGIN_USER_DISABLED | member/auth | ACTIVE | FAIL_CLOSE | P1 | NO_AUTO_RETRY | 账号被禁用 | 停止重试并联系客服 | 会员认证负责人核验封禁策略与用户状态 | 已生效 | - |
| 1004003005 | AUTH_SOCIAL_USER_NOT_FOUND | member/auth | ACTIVE | FAIL_CLOSE | P2 | NO_AUTO_RETRY | 社交授权成功但未解析到会员绑定信息 | 重新授权 1 次；仍失败再联系客服 | 会员认证负责人排查 social user 映射与 state 校验 | 已生效 | - |
| 1004003007 | AUTH_MOBILE_USED | member/auth | ACTIVE | FAIL_CLOSE | P2 | NO_AUTO_RETRY | 发送修改手机号验证码时目标手机号已被占用 | 更换手机号，不自动重发 | 会员认证负责人排查手机号冲突数据 | 已生效 | - |
| 1004004000 | ADDRESS_NOT_EXISTS | member/profile | ACTIVE | FAIL_CLOSE | P2 | REFRESH_ONCE | 地址编号不存在或不属于当前用户 | 刷新地址列表后重试 | 会员资料负责人排查并发删除/脏读 | 已生效 | - |
| 1004008000 | POINT_RECORD_BIZ_NOT_SUPPORT | member | ACTIVE | FAIL_CLOSE | P1 | NO_AUTO_RETRY | 资产账本业务类型不支持 | 稍后再查账本 | 会员资产负责人排查业务映射 | 已生效 | - |
| 1004009000 | SIGN_IN_CONFIG_NOT_EXISTS | member/growth | ACTIVE | FAIL_CLOSE | P1 | NO_AUTO_RETRY | 签到规则缺失 | 暂停签到并稍后再试 | 会员增长负责人恢复签到规则配置 | 已生效 | - |
| 1004010000 | SIGN_IN_RECORD_TODAY_EXISTS | member/growth | ACTIVE | FAIL_CLOSE | P2 | NO_AUTO_RETRY | 当日重复签到 | 停止重试，次日再签 | 无需人工补单；若集中出现排查时区与幂等 | 已生效 | - |
| 1004011000 | LEVEL_NOT_EXISTS | member/growth | ACTIVE | FAIL_CLOSE | P1 | NO_AUTO_RETRY | 用户等级配置缺失 | 刷新个人页；持续失败联系客服 | 会员增长负责人修复等级配置与用户等级引用 | 已生效 | - |
| 1004011201 | EXPERIENCE_BIZ_NOT_SUPPORT | member/growth | ACTIVE | FAIL_CLOSE | P1 | NO_AUTO_RETRY | 经验业务类型未注册 | 稍后再查成长记录 | 会员增长负责人补齐经验业务映射 | 已生效 | - |
| 1002014000 | SMS_CODE_NOT_FOUND | system/sms(member-auth) | ACTIVE | FAIL_CLOSE | P2 | NO_AUTO_RETRY | 验证码不存在 | 重新获取验证码 | 会员认证负责人核验短信场景配置与投递延迟 | 已生效 | - |
| 1002014001 | SMS_CODE_EXPIRED | system/sms(member-auth) | ACTIVE | FAIL_CLOSE | P2 | NO_AUTO_RETRY | 验证码已过期 | 重新获取验证码 | 会员认证负责人关注验证码过期率 | 已生效 | - |
| 1002014002 | SMS_CODE_USED | system/sms(member-auth) | ACTIVE | FAIL_CLOSE | P2 | NO_AUTO_RETRY | 验证码已被使用 | 重新获取验证码 | 会员认证负责人排查重复提交峰值 | 已生效 | - |
| 1002014004 | SMS_CODE_EXCEED_SEND_MAXIMUM_QUANTITY_PER_DAY | system/sms(member-auth) | ACTIVE | FAIL_CLOSE | P2 | NO_AUTO_RETRY | 单手机号当日短信发送超限 | 次日再试或联系客服 | 会员认证负责人核验防刷阈值与短信成本 | 已生效 | - |
| 1002014005 | SMS_CODE_SEND_TOO_FAST | system/sms(member-auth) | ACTIVE | FAIL_CLOSE | P2 | NO_AUTO_RETRY | 短时间内重复发送验证码 | 等待冷却后再试 | 会员认证负责人核验发送频控配置 | 已生效 | - |
| 1002018000 | SOCIAL_USER_AUTH_FAILURE | system/social(member-auth) | ACTIVE | FAIL_CLOSE | P1 | NO_AUTO_RETRY | 社交授权失败 | 重新拉起授权 1 次；仍失败联系客服 | 会员认证负责人排查微信/Social 网关与 state 校验 | 已生效 | - |
| 1002018001 | SOCIAL_USER_NOT_FOUND | system/social(member-auth) | ACTIVE | FAIL_CLOSE | P2 | NO_AUTO_RETRY | 社交账号未找到 | 重新授权或改用短信登录 | 会员认证负责人排查绑定丢失与账号映射 | 已生效 | - |
| 1002018200 | SOCIAL_CLIENT_WEIXIN_MINI_APP_PHONE_CODE_ERROR | system/social(member-auth) | ACTIVE | FAIL_CLOSE | P2 | NO_AUTO_RETRY | 小程序 `phoneCode` 无效或过期 | 重新执行 `wx.getPhoneNumber` 获取新 code | 会员认证负责人排查微信凭证链路与客户端配置 | 已生效 | - |
| 1013005000 | COUPON_NOT_EXISTS | promotion | ACTIVE | FAIL_CLOSE | P2 | REFRESH_ONCE | 优惠券记录不存在 | 刷新券列表 | 运营核验发券流水 | 已生效 | - |
| 1013004000 | COUPON_TEMPLATE_NOT_EXISTS | promotion | ACTIVE | FAIL_CLOSE | P2 | NO_AUTO_RETRY | 券模板不存在 | 选择其他活动券 | 运营核验模板配置 | 已生效 | - |
| 1013012000 | BARGAIN_ACTIVITY_NOT_EXISTS | promotion | ACTIVE | FAIL_CLOSE | P2 | NO_AUTO_RETRY | 发起砍价时活动不存在或已失效 | 返回活动列表并改选其他活动 | 营销负责人核验砍价活动上下线与缓存刷新 | 已生效 | - |
| 1013013001 | BARGAIN_RECORD_CREATE_FAIL_EXISTS | promotion | ACTIVE | FAIL_CLOSE | P2 | NO_AUTO_RETRY | 用户重复参与同一砍价活动 | 进入当前砍价记录，不重复发起 | 营销负责人核验参与幂等与记录状态 | 已生效 | - |
| 1013013002 | BARGAIN_RECORD_CREATE_FAIL_LIMIT | promotion | ACTIVE | FAIL_CLOSE | P2 | NO_AUTO_RETRY | 用户已达当前砍价活动参与上限 | 停止重试并改选其他活动 | 营销负责人核验活动限次配置 | 已生效 | - |
| 1013014000 | BARGAIN_HELP_CREATE_FAIL_RECORD_NOT_IN_PROCESS | promotion | ACTIVE | FAIL_CLOSE | P2 | NO_AUTO_RETRY | 助力目标砍价记录不在进行中 | 返回记录页并刷新状态 | 营销负责人排查活动状态与助力窗口关闭时机 | 已生效 | - |
| 1013014001 | BARGAIN_HELP_CREATE_FAIL_RECORD_SELF | promotion | ACTIVE | FAIL_CLOSE | P2 | NO_AUTO_RETRY | 用户尝试给自己的砍价记录助力 | 停止当前操作并提示分享给好友 | 营销负责人确认自助力守卫持续生效 | 已生效 | - |
| 1013014002 | BARGAIN_HELP_CREATE_FAIL_LIMIT | promotion | ACTIVE | FAIL_CLOSE | P2 | NO_AUTO_RETRY | 用户已达到当前活动助力上限 | 停止重试，改为等待他人助力 | 营销负责人核验助力限次配置 | 已生效 | - |
| 1013014004 | BARGAIN_HELP_CREATE_FAIL_HELP_EXISTS | promotion | ACTIVE | FAIL_CLOSE | P2 | NO_AUTO_RETRY | 用户已为当前记录助力过 | 刷新助力列表，不重复提交 | 营销负责人排查助力幂等与分享链路重复点击 | 已生效 | - |
| 1013016000 | ARTICLE_NOT_EXISTS | promotion/content | ACTIVE | FAIL_OPEN | P2 | NO_AUTO_RETRY | 文章浏览量回写时目标文章不存在 | 忽略统计回写失败，继续阅读正文；不自动重试 | 内容域核验文章上下架状态与 browse-count 入参来源 | 已生效 | - |
| 1013019000 | KEFU_CONVERSATION_NOT_EXISTS | promotion | ACTIVE | FAIL_CLOSE | P2 | REFRESH_ONCE | 标记已读或拉取会话时会话不存在 | 刷新会话列表并重新进入会话 | 客服域核验会话建链与清理策略 | 已生效 | - |
| 1030001000 | TECHNICIAN_NOT_EXISTS | booking | ACTIVE | FAIL_CLOSE | P2 | NO_AUTO_RETRY | 技师不存在 | 更换技师 | 预约域核验技师数据 | 已生效 | - |
| 1030001001 | TECHNICIAN_DISABLED | booking | ACTIVE | FAIL_CLOSE | P2 | NO_AUTO_RETRY | 技师已禁用 | 更换技师 | 预约域核验启停状态 | 已生效 | - |
| 1011007002 | BROKERAGE_BIND_SELF | trade/brokerage | ACTIVE | FAIL_CLOSE | P2 | NO_AUTO_RETRY | 用户尝试绑定自己为推广人 | 停止绑定并更换推广人 | 分销负责人核验分享链路与自绑定拦截 | 已生效 | - |
| 1011007003 | BROKERAGE_BIND_USER_NOT_ENABLED | trade/brokerage | ACTIVE | FAIL_CLOSE | P2 | NO_AUTO_RETRY | 绑定的推广人没有推广资格 | 更换有资格的推广人后重试 | 分销负责人核验推广资格与开通状态 | 已生效 | - |
| 1011007005 | BROKERAGE_BIND_MODE_REGISTER | trade/brokerage | ACTIVE | FAIL_CLOSE | P1 | NO_AUTO_RETRY | 非注册阶段尝试绑定推广人 | 停止当前绑定，回退正常注册/登录流程 | 分销负责人核验绑定时机与渠道埋点 | 已生效 | - |
| 1011007006 | BROKERAGE_BIND_OVERRIDE | trade/brokerage | ACTIVE | FAIL_CLOSE | P1 | NO_AUTO_RETRY | 用户已绑定推广人仍再次发起绑定 | 停止当前操作并展示既有绑定关系 | 分销负责人核验覆盖保护与异常重放 | 已生效 | - |
| 1011007007 | BROKERAGE_BIND_LOOP | trade/brokerage | ACTIVE | FAIL_CLOSE | P1 | NO_AUTO_RETRY | 下级尝试绑定自己的上级形成闭环 | 停止绑定并更换推广关系 | 分销负责人核验上下级关系图与导入数据 | 已生效 | - |
| 1011008002 | BROKERAGE_WITHDRAW_MIN_PRICE | trade/brokerage | ACTIVE | FAIL_CLOSE | P2 | NO_AUTO_RETRY | 提现金额低于系统最小门槛 | 提高提现金额后重试 | 分销负责人核验最小提现门槛配置 | 已生效 | - |
| 1011008003 | BROKERAGE_WITHDRAW_USER_BALANCE_NOT_ENOUGH | trade/brokerage | ACTIVE | FAIL_CLOSE | P2 | REFRESH_ONCE | 用户可提现余额不足 | 刷新余额后调整金额重试 | 分销负责人核验余额汇总、冻结佣金与提现申请并发 | 已生效 | - |
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

## 3.1 2026-03-14 BO-004 No-Add 说明
- 本批未为 `BO-004 /booking/commission/*` 新增 canonical 错误码。
- 原因固定为：
  - 当前只核到 `TechnicianCommissionController` 的 controller-only contract，未核到独立 admin page/API binding，runtime capability 仍未证实。
  - `COMMISSION_NOT_EXISTS(1030007000)`、`COMMISSION_ALREADY_SETTLED(1030007001)` 当前只存在于模块常量；在 `TechnicianCommissionController -> TechnicianCommissionServiceImpl` 的真实对外路径里，未核到稳定抛出与稳定暴露证据。
  - `POST /booking/commission/settle`、`POST /booking/commission/batch-settle` 当前可能 `true` + no-op；这不足以形成稳定错误码锚点。
  - 当前也没有该路径的服务端 `degraded=true / degradeReason` 证据。
- 因此 canonical register 本批保持“不新增、不影射、不提前登记”的口径；若后续出现真实对外暴露证据，再另行评审是否登记。

## 4. 预留码段
- Product：`1_008_099_001` ~ `1_008_099_099`
- Member：`1_004_099_001` ~ `1_004_099_099`
- Trade：`1_011_099_001` ~ `1_011_099_099`
- Promotion：`1_013_099_001` ~ `1_013_099_099`
- Booking：`1_030_099_001` ~ `1_030_099_099`

## 4.1 自动重试与人工接管规则
- 只有 `retryClass=BG_RETRY_JOB` 或 `retryClass=POLL_5S_X3` 允许自动重试；其余 `NO_AUTO_RETRY`、`REFRESH_ONCE`、`MANUAL_RETRY_3` 都不属于自动重试。
- 会员域当前允许后台自动补偿的仅有：
  - `MINIAPP_ASSET_LEDGER_MISMATCH(1004009901)`：后台补偿/对账任务可自动执行，前台只展示 `degraded`。
- 会员域必须人工接管的错误码：
  - `AUTH_LOGIN_USER_DISABLED(1004003001)`
  - `POINT_RECORD_BIZ_NOT_SUPPORT(1004008000)`
  - `SIGN_IN_CONFIG_NOT_EXISTS(1004009000)`
  - `LEVEL_NOT_EXISTS(1004011000)`
  - `EXPERIENCE_BIZ_NOT_SUPPORT(1004011201)`
  - 任一 `RESERVED_DISABLED` 错误码在禁用态返回
- 会员域禁止自动重试、仅允许用户纠正后重提的错误码：
  - `AUTH_LOGIN_BAD_CREDENTIALS(1004003000)`
  - `USER_MOBILE_NOT_EXISTS(1004001001)`
  - `USER_MOBILE_USED(1004001002)`
  - `USER_POINT_NOT_ENOUGH(1004001003)`
  - `AUTH_SOCIAL_USER_NOT_FOUND(1004003005)`
  - `AUTH_MOBILE_USED(1004003007)`
  - `ADDRESS_NOT_EXISTS(1004004000)`
  - `SIGN_IN_RECORD_TODAY_EXISTS(1004010000)`
  - `SMS_CODE_NOT_FOUND(1002014000)`
  - `SMS_CODE_EXPIRED(1002014001)`
  - `SMS_CODE_USED(1002014002)`
  - `SMS_CODE_EXCEED_SEND_MAXIMUM_QUANTITY_PER_DAY(1002014004)`
  - `SMS_CODE_SEND_TOO_FAST(1002014005)`
  - `SOCIAL_USER_AUTH_FAILURE(1002018000)`
  - `SOCIAL_USER_NOT_FOUND(1002018001)`
  - `SOCIAL_CLIENT_WEIXIN_MINI_APP_PHONE_CODE_ERROR(1002018200)`

## 5. 生效门禁
- 进入 ACTIVE 前必须满足：
  1. 契约冻结 + 回归用例覆盖（happy/error/degrade）
  2. 客服 SOP 与运营处置演练通过
  3. 灰度 `5% -> 20% -> 50% -> 100%` 无新增 P0
- `RESERVED_DISABLED` 错误码被登记，只表示治理锚点已固定；若真实页面、真实 controller、真实前端绑定或运行样本缺失，对应 capability 仍继续 `PLANNED_RESERVED / Release Blocked`。
- 任一 `RESERVED_DISABLED` 在禁用态返回：固定按 P1 处理。
