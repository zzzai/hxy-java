# MiniApp Booking Review Manager Account Routing Truth v1（2026-03-21）

## 1. 目标
- 审计 booking review 差评通知如果要升级为“账号级通知”，当前仓库里到底有哪些稳定真值可用。
- 冻结“门店联系人快照”和“门店店长后台账号”之间的边界，防止后续把未核实字段误写成已闭环能力。

## 2. 审计范围与证据
- 评价服务：
  - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/BookingReviewServiceImpl.java`
- 评价数据对象：
  - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/dal/dataobject/BookingReviewDO.java`
- 门店主数据：
  - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/dal/dataobject/store/ProductStoreDO.java`
- 既有 owner 真值文档：
  - `docs/products/miniapp/2026-03-19-miniapp-booking-review-manager-ownership-truth-review-v1.md`
- 仓内 notify / outbox 参考：
  - `docs/plans/2026-03-01-commission-sla-notify-outbox.md`
  - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/dal/dataobject/TechnicianCommissionSettlementNotifyOutboxDO.java`
- 仓内检索关键词：
  - `managerUserId`
  - `ownerUserId`
  - `storeManager`
  - `store -> managerUserId`
  - `contactName / contactMobile`
  - `notify / notification / sendSms / sendMessage`

## 3. 当前冻结结论

| 审计项 | 当前真值 | 结论 |
|---|---|---|
| 差评提交流程触发点 | `BookingReviewServiceImpl.createReview(...)` 成功写入评价记录后即可判定是否差评 | `已核实` |
| 店长联系人快照 | 当前只核到 `ProductStoreDO.contactName / contactMobile` | `已核实` |
| 门店 -> 店长后台账号映射 | 当前分支已新增 `booking_review_manager_account_routing`，字段包含 `storeId / managerAdminUserId / bindingStatus / effectiveTime / expireTime / source / lastVerifiedTime` | `已新增工程真值，发布未闭环` |
| 后台执行动作账号 | `managerClaimedByUserId / managerLatestActionByUserId` 只表示执行认领/处理/闭环的后台操作人 | `已核实` |
| 自动通知目标账号 | 第一版只认 `booking_review_manager_account_routing.managerAdminUserId`，无 owner 时进入 `BLOCKED_NO_OWNER` | `已新增工程口径，发布未闭环` |
| booking review 独立 outbox | 当前分支已新增 `booking_review_notify_outbox`、admin 观测面与异步派发 job | `已核实` |

## 4. 当前代码级真值说明
1. `ProductStoreDO` 当前稳定可用字段只提供：
   - `id`
   - `name`
   - `contactName`
   - `contactMobile`
2. `BookingReviewDO` 当前与店长待办相关的稳定字段只提供：
   - `managerContactName`
   - `managerContactMobile`
   - `managerTodoStatus`
   - `managerClaimedByUserId`
   - `managerLatestActionByUserId`
3. `BookingReviewServiceImpl.populateManagerTodoFields(...)` 当前只会从门店主数据回填联系人快照，不会回填任何后台账号 ID。
4. `BookingReviewServiceImpl.createReview(...)` 当前是差评创建后的唯一稳定同步触发点；若以后要做“差评提交成功后立即触发通知”，只能基于这里落通知意图，不能依赖 read-path 或人工二次刷新。
5. 当前分支已新增：
   - `BookingReviewManagerAccountRoutingDO / Mapper`
   - `BookingReviewNotifyOutboxDO / Mapper / Service`
   - `BookingReviewNotifyOutboxController`
   - `BookingReviewNotifyDispatchJob`
6. 当前通知链路的工程真值是：
   - 有有效 `managerAdminUserId` 时，差评提交后写 `PENDING` outbox
   - 无有效 owner 时，写 `BLOCKED_NO_OWNER`
   - dispatch job 只做 `IN_APP` 占位派发，不改变 `Can Develop / Cannot Release`

## 5. 当前不能成立的说法
1. “系统已经找到门店店长后台账号并自动发送差评通知。”
2. “`managerClaimedByUserId` 就是门店店长账号。”
3. “门店联系人手机号可以直接当自动通知目标。”
4. “差评通知已经具备发布级账号归属、全量数据绑定和运行样本证据。”
5. “booking review 的 notify outbox / dispatch job 已经等于自动通知正式上线。”

## 6. 当前可成立的说法
1. 差评当前已具备 admin-only 店长待办治理层。
2. 差评提交成功后的同步触发点已经存在，可以写“通知意图”，但不能直接诚实写成“账号已收到通知”。
3. 当前分支已新增 `storeId -> managerAdminUserId` 的工程真值模型，但其数据覆盖、样本和发布证据仍未闭环。
4. 当前分支已新增 booking review 专属 notify outbox、admin-only 观测页与 `IN_APP` 占位派发 job。
5. 即使已经有 outbox 和 dispatch job，也只能写成 `Can Develop / Cannot Release`。

## 7. 对后续设计与开发的直接约束

### 7.1 当前允许进入设计的项
1. `booking review manager account routing truth` 真值审计。
2. `booking review notify outbox` 方案设计。
3. admin-only 的通知可观测台账 / 明细设计。
4. `BLOCKED_NO_OWNER` 阻断池与运营治理视图设计。

### 7.2 当前禁止误升的项
1. 自动通知店长账号已放量、已完成真实消息模板配置或已具备发布证据。
2. 直接以 `contactMobile` 兜底短信作为自动通知主方案。
3. 店长账号级待办分派、客服升级、区域负责人升级。
4. 基于账号归属的 SLA 考核、补偿或审批链路。

## 8. 推荐的单一真值口径
- 第一版账号级通知设计只允许写成：
  - `差评提交成功后立即生成通知意图`
  - `若存在稳定 managerAdminUserId，则进入可派发状态`
  - `若不存在稳定 managerAdminUserId，则进入 BLOCKED_NO_OWNER`
- 当前分支虽已实现 `notify outbox + admin 观测 + dispatch job`，但在 `managerAdminUserId` 数据闭环、运行样本与发布证据补齐之前，不得把本专题写成“自动通知已上线”。

## 9. 单一真值引用
- `docs/products/miniapp/2026-03-19-miniapp-booking-review-manager-ownership-truth-review-v1.md`
- `docs/products/miniapp/2026-03-17-miniapp-booking-review-service-quality-prd-v1.md`
- `docs/products/miniapp/2026-03-17-miniapp-booking-review-final-integration-review-v1.md`
