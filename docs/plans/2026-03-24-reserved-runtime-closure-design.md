# Reserved Runtime Closure Design

## 1. Goal
- 把 `Reserved` 三项能力从“只有 PRD / contract / gate 文档”推进到“真实页面 + 真实 app controller + 真实前后端绑定 + 自动化测试”的工程闭环。
- 本轮不把任何能力写成 `release-ready`；默认仍保持关闭态，发布结论继续是 `Can Develop / Cannot Release`，直到真实样本、灰度、回滚和 sign-off 证据补齐。

## 2. Current Truth
- 小程序前端真实目录在 `yudao-mall-uniapp`，目前已经存在 `booking`、`commission`、`member` 等页面，但不存在以下 reserved 页面：
  - `pages/gift-card/*`
  - `pages/referral/*`
  - `pages/technician/feed`
- 后端真实 app controller 已存在 `booking/order`、`booking/review`、`trade/brokerage-*`、`promotion/point-activity` 等模式，但不存在：
  - `/promotion/gift-card/*`
  - `/promotion/referral/*`
  - `/booking/technician/feed/*`
- 因此当前 blocker 不是文档缺失，而是 runtime 三件套缺失：页面、controller、样本/测试。

## 3. Approaches

### 方案 A：三域全部从零独立建模
- 做法：gift-card、referral、technician-feed 都新建独立 DO / mapper / service / page。
- 优点：边界清晰，后续可独立扩展。
- 缺点：重复造轮子最多，工期最长；`Referral` 明显会和现有 `Brokerage` 重叠，造成双真值风险。

### 方案 B：按域选择“复用现有真值 + 补新域对象”
- 做法：
  - `Referral`：复用 `trade/brokerage-user` 与 `trade/brokerage-record` 真值，在 promotion 模块新增 referral façade controller 与小程序独立页面。
  - `Technician Feed`：在 booking 模块新增轻量 feed 真域对象、controller 与页面，复用既有技师详情链路。
  - `Gift Card`：在 promotion 模块新增最小 gift-card 域对象、controller、页面与 SQL。
- 优点：整体实现量最稳，避免 `Referral` 双账本；三域都能形成长期可维护真值。
- 缺点：`Referral` 对 brokerage 语义有适配层，需要清楚写明“referral 页面 != 直接复用 commission 页面”。

### 方案 C：只补页面和 controller 壳，业务先返回静态样本
- 优点：最快能把页面路径和 controller 路径补出来。
- 缺点：这是假闭环，无法支撑长期开发，也会制造新的伪真值；不符合当前项目的真值治理原则。

## 4. Recommended Design
- 采用方案 B。
- 关键裁决：
  - `Referral` 不新造奖励账本，直接用已存在的分销绑定/分销记录作为真实奖励来源；通过新 controller 把对外 contract 固化为 `/promotion/referral/*`。
  - `Technician Feed` 独立建模，最小范围只覆盖 `page / like / comment/create`，并保留 `REVIEWING`、`degraded`、幂等键和审计字段。
  - `Gift Card` 独立建模，先做模板分页、订单创建、订单详情、核销、退款申请五个 contract 真值接口，并补对应页面。
- 所有域统一加 runtime gate：
  - `miniapp.gift-card`
  - `miniapp.referral`
  - `miniapp.technician-feed.audit`
- gate 默认关闭；关闭态命中不伪成功，不写成上线能力。

## 5. Scope

### 5.1 Referral
- 前端：
  - `pages/referral/index.vue`
  - 真实入口接到个人中心或分销相关真实入口，不依赖临时深链。
  - API 模块：`sheep/api/promotion/referral.js`
- 后端：
  - `AppReferralController`
  - `bind-inviter`
  - `overview`
  - `reward-ledger/page`
- 真实数据来源：
  - 绑定关系 -> `BrokerageUserService`
  - 奖励台账 -> `BrokerageRecordService`

### 5.2 Technician Feed
- 前端：
  - `pages/technician/feed.vue`
  - 从技师详情页提供真实入口。
  - API 模块：`sheep/api/trade/technicianFeed.js`
- 后端：
  - `AppTechnicianFeedController`
  - `GET /booking/technician/feed/page`
  - `POST /booking/technician/feed/like`
  - `POST /booking/technician/feed/comment/create`
- 数据模型：
  - `technician_feed_post`
  - `technician_feed_like`
  - `technician_feed_comment`

### 5.3 Gift Card
- 前端：
  - `pages/gift-card/list.vue`
  - `pages/gift-card/order-detail.vue`
  - `pages/gift-card/redeem.vue`
  - `pages/gift-card/refund.vue`
  - API 模块：`sheep/api/promotion/giftCard.js`
- 后端：
  - `AppGiftCardController`
  - `GET /promotion/gift-card/template/page`
  - `POST /promotion/gift-card/order/create`
  - `GET /promotion/gift-card/order/get`
  - `POST /promotion/gift-card/redeem`
  - `POST /promotion/gift-card/refund/apply`
- 数据模型：
  - `gift_card_template`
  - `gift_card_order`
  - `gift_card`
  - `gift_card_redeem_record`

## 6. Runtime / Release Boundary
- 本轮完成后可以更新为：
  - `runtime implemented`
  - `controller/page binding closed`
  - `Can Develop`
- 本轮完成后仍不得更新为：
  - `release-ready`
  - `可放量`
  - `灰度已通过`
- 继续保留的发布 blocker：
  - 真实线上开关审批样本未核出
  - 真实运行样本包未核出
  - 回滚/误发布 evidence 未核出
  - sign-off 未核出

## 7. Testing Strategy
- 小程序：
  - `pages.json` 路由注册 smoke test
  - API alignment test
  - 页面入口与关键文案 smoke test
- Java：
  - app controller unit test
  - service 级核心规则测试（幂等、状态流转、关闭态）
- 文档：
  - 回写 readiness register / blocker final integration / truth ledger / release pack

## 8. Implementation Order
1. `Referral`：最快打通一条完整 reserved runtime，建立 promotion façade + uniapp page 模式。
2. `Technician Feed`：在 booking 域落轻量内容流，建立 feed 数据模型与互动测试模式。
3. `Gift Card`：最后补最重的交易型 reserved 域。
4. 回写总账、readiness register、handoff 和记忆文档。

## 9. Non-Goals
- 不在本轮实现真实灰度、真实生产导出、真实 sign-off。
- 不把关闭态 gate 写成“系统故障”；它是刻意保留的发布门禁。
- 不为了追求快而做 controller/page 占位空壳。
