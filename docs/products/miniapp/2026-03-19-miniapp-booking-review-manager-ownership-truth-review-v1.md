# MiniApp Booking Review Manager Ownership Truth Review v1（2026-03-19）

## 1. 目标
- 审计 booking review 当前“店长待办”到底绑定到什么对象。
- 冻结“门店联系人快照”和“后台操作人”之间的边界，避免误写成 `managerUserId` 已闭环。

## 2. 审计证据
- 门店主数据：
  - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/dal/dataobject/store/ProductStoreDO.java`
- 评价数据对象：
  - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/dal/dataobject/BookingReviewDO.java`
- 评价服务：
  - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/BookingReviewServiceImpl.java`
- 仓内检索：
  - `store -> managerUserId`
  - `store_user / storeManager / ownerUserId`
  - `manager_id / store_employee`

## 3. 当前冻结结论

| 审计项 | 当前真值 | 结论 |
|---|---|---|
| 门店 owner 真值 | 当前只核到 `contactName / contactMobile` | `已核实` |
| 账号级店长映射 | 当前未核出稳定 `store -> managerUserId` / `ownerUserId` / 关系表 | `未核出` |
| 后台认领人字段 | `managerClaimedByUserId / managerLatestActionByUserId` 仅是执行动作的后台操作人 | `已核实` |
| 自动通知目标 | 当前没有账号级通知目标真值 | `未核出` |

## 4. 代码级真值说明
1. `ProductStoreDO` 当前稳定字段只提供：
   - `contactName`
   - `contactMobile`
2. `BookingReviewDO` 当前与店长待办相关的稳定字段只提供：
   - `managerContactName`
   - `managerContactMobile`
   - `managerTodoStatus`
   - `managerClaimedByUserId`
   - `managerLatestActionByUserId`
3. `BookingReviewServiceImpl.populateManagerTodoFields(...)` 当前只会从门店主数据回填联系人快照，不会回填后台账号 ID。

## 5. 当前不能成立的说法
1. “系统已经找到门店店长账号并自动分派待办。”
2. “店长待办已绑定后台店长账号权限体系。”
3. “差评会自动通知对应店长账号、客服账号或区域负责人账号。”
4. “`managerClaimedByUserId` 就是门店店长账号。”

## 6. 当前可成立的说法
1. 差评当前已具备 admin-only 店长待办治理层。
2. 待办目标对象当前只冻结到门店联系人快照。
3. 后台登录人只表示谁执行了认领 / 首次处理 / 闭环动作，不表示门店 owner 真值。
4. 后续若要做消息通知、自动派单或绩效路由，必须先补稳定账号归属模型。

## 7. 对后续开发的直接约束

### 7.1 可进入开发的项
1. 后台列表 / 详情上补更多“联系人快照真值”提示。
2. admin-only 的手工治理增强，例如 drill-down、批注、历史修复工具。

### 7.2 当前禁止误升的项
1. 自动通知店长。
2. 店长账号级待办分派。
3. 账号级 SLA 考核口径。
4. 基于店长账号的自动补偿或审批链路。

## 8. 单一真值引用
- `docs/products/miniapp/2026-03-17-miniapp-booking-review-service-quality-prd-v1.md`
- `docs/contracts/2026-03-17-miniapp-booking-review-contract-v1.md`
- `docs/products/miniapp/2026-03-17-miniapp-booking-review-final-integration-review-v1.md`
