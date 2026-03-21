# MiniApp Booking Review Manager Account Routing Truth v1（2026-03-21）

## 1. 目标
- 审计 booking review 差评通知升级为“账号级通知”时，仓库里到底有哪些稳定真值可用。
- 冻结“门店联系人快照”和“门店店长账号路由”之间的边界，防止把未核实字段误写成已闭环能力。

## 2. 审计范围与证据
- 评价服务：
  - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/BookingReviewServiceImpl.java`
  - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/BookingReviewNotifyOutboxServiceImpl.java`
- 路由与发送器：
  - `BookingReviewManagerAccountRoutingDO / Mapper / QueryService`
  - `BookingReviewWecomRobotSender`
  - `BookingReviewNotifyDispatchJob`
  - `BookingReviewManagerTodoSlaReminderJob`
- 门店主数据：
  - `ProductStoreDO`
- 既有 owner 真值文档：
  - `docs/products/miniapp/2026-03-19-miniapp-booking-review-manager-ownership-truth-review-v1.md`

## 3. 当前冻结结论

| 审计项 | 当前真值 | 结论 |
|---|---|---|
| 差评提交流程触发点 | `BookingReviewServiceImpl.createReview(...)` 成功写入评价记录后即可判定是否差评 | `已核实` |
| 店长联系人快照 | 当前只核到 `ProductStoreDO.contactName / contactMobile` | `已核实` |
| 门店 -> 店长账号映射 | 当前分支已新增 `booking_review_manager_account_routing`，字段包含 `storeId / managerAdminUserId / managerWecomUserId / bindingStatus / effectiveTime / expireTime / source / lastVerifiedTime` | `已新增工程真值，发布未闭环` |
| 后台只读核查入口 | 当前分支已新增 `/booking/review/manager-routing/get`、`/booking/review/manager-routing/page` 与 `/mall/booking/review/manager-routing` | `已落地（admin-only）` |
| 后台执行动作账号 | `managerClaimedByUserId / managerLatestActionByUserId` 只表示执行认领/处理/闭环的后台操作人 | `已核实` |
| 自动通知目标账号 | 第一版只认 `managerAdminUserId` 和 `managerWecomUserId`；任一通道缺账号时只阻断对应通道 | `已新增工程口径，发布未闭环` |
| booking review 独立 outbox | 当前分支已新增双通道 `booking_review_notify_outbox`、admin 观测面与异步派发 job | `已核实` |

## 4. 当前代码级真值说明
1. `ProductStoreDO` 当前稳定可用字段仍只提供：
   - `id`
   - `name`
   - `contactName`
   - `contactMobile`
2. `BookingReviewDO` 当前与店长待办相关的稳定字段仍只提供：
   - `managerContactName`
   - `managerContactMobile`
   - `managerTodoStatus`
   - `managerClaimedByUserId`
   - `managerLatestActionByUserId`
3. `BookingReviewServiceImpl.populateManagerTodoFields(...)` 当前只会从门店主数据回填联系人快照，不会回填任何后台账号或企微账号。
4. `BookingReviewServiceImpl.createReview(...)` 当前仍是差评创建后的唯一稳定同步触发点；若做“差评提交成功后立即触发通知”，只能基于这里落通知意图。
5. 当前分支已新增双通道路由真值：
   - `managerAdminUserId` 供 `IN_APP` 通道使用
   - `managerWecomUserId` 供 `WECOM` 通道使用
   - 两个通道都受同一条 `bindingStatus / effectiveTime / expireTime` 约束
6. 当前 notify outbox 的工程语义固定为：
   - 有有效 App / 企微接收账号时，写 `PENDING`
   - 缺任一通道账号时，仅该通道写 `BLOCKED_NO_OWNER`
   - 幂等键固定带 `notifyType + reviewId + channel + receiver`
   - 同一条差评至少可能产生两条记录：`IN_APP`、`WECOM`
7. 当前运营核查路径已固定为：
   - 详情页/通知台账里的 `查看店长路由`
   - 只读核查页按 `storeId / storeName / contactMobile` 查询
   - 页面展示 App / 企微各自的 routingLabel、repairHint、账号字段，不提供在线改绑

## 5. 当前不能成立的说法
1. “系统已经找到全量门店店长账号并自动发送差评通知。”
2. “`managerClaimedByUserId` 就是门店店长账号。”
3. “门店联系人手机号可以直接当自动通知目标。”
4. “差评通知已经具备发布级账号归属、全量数据绑定和运行样本证据。”
5. “booking review 的 notify outbox / dispatch job / wecom sender 已经等于自动通知正式上线。”

## 6. 当前可成立的说法
1. 差评当前已具备 admin-only 店长待办治理层。
2. 差评提交成功后的同步触发点已经存在，可以诚实写成“立即生成通知意图”。
3. 当前分支已新增 `storeId -> managerAdminUserId + managerWecomUserId` 的双通道路由工程模型，但其数据覆盖、样本和发布证据仍未闭环。
4. 当前分支已新增 booking review 专属双通道 notify outbox、admin-only 观测页、人工重试与 SLA reminder job。
5. 当前运营已能通过只读核查页判断某个阻断到底是无路由、缺 App 账号、缺企微账号、路由未启用、未生效、已过期还是通道关闭。
6. 即使已经有 routing、outbox、sender 和 job，也只能写成 `Can Develop / Cannot Release`。

## 7. 对后续设计与开发的直接约束

### 7.1 当前允许进入设计的项
1. `booking review manager account routing truth` 真值审计。
2. `booking review dual-channel notify outbox` 方案设计。
3. admin-only 的通知可观测台账 / 明细设计。
4. `BLOCKED_NO_OWNER` 阻断池与运营治理视图设计。

### 7.2 当前禁止误升的项
1. 自动通知店长账号已放量、已完成真实模板配置或已具备发布证据。
2. 直接以 `contactMobile` 兜底短信作为自动通知主方案。
3. 店长账号级待办分派、客服升级、区域负责人升级。
4. 基于账号归属的 SLA 考核、补偿或审批链路。

## 8. 推荐的单一真值口径
- 第一版账号级通知只允许写成：
  - `差评提交成功后立即生成双通道通知意图`
  - `若存在稳定 managerAdminUserId / managerWecomUserId，则进入对应通道的可派发状态`
  - `若不存在稳定账号，则对应通道进入 BLOCKED_NO_OWNER`
- 当前分支虽已实现 `routing + notify outbox + wecom sender + admin 观测 + SLA reminder`，但在双通道路由数据闭环、运行样本与发布证据补齐之前，不得把本专题写成“自动通知已上线”。

## 9. 单一真值引用
- `docs/products/miniapp/2026-03-19-miniapp-booking-review-manager-ownership-truth-review-v1.md`
- `docs/products/miniapp/2026-03-17-miniapp-booking-review-service-quality-prd-v1.md`
- `docs/products/miniapp/2026-03-17-miniapp-booking-review-final-integration-review-v1.md`
- `docs/plans/2026-03-21-booking-review-dual-channel-manager-notify-design.md`
