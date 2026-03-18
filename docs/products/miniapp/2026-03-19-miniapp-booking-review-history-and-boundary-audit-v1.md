# MiniApp Booking Review History And Boundary Audit v1（2026-03-19）

## 1. 目标
- 固定 booking review 03-19 上线后复核到的历史数据边界、读写路径差异和店长待办状态机真值。
- 防止后续文档把“历史差评可人工修复”误写成“系统已自动补齐历史数据”。

## 2. 审计范围与证据
- 服务实现：
  - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/BookingReviewServiceImpl.java`
- 单元测试：
  - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/service/impl/BookingReviewServiceImplTest.java`
- 后台页面真值：
  - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/index.vue`
  - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/detail/index.vue`
  - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/dashboard/index.vue`

## 3. 当前审计结论

| 审计项 | 当前真值 | 当前结论 |
|---|---|---|
| 历史差评待办初始化 | 历史差评若 `managerTodoStatus=null`，当前只会在首次执行 `claim / first-action / close` 写动作时 lazy-init | `Doc Closed / Can Develop / Cannot Release` |
| 历史差评读路径 | admin list / detail / dashboard 当前不会在 read-path 自动补齐 `managerTodo*` 字段 | `工程未闭环` |
| 缺失 booking order 行的历史差评 | 仍可在写路径补齐 `negativeTriggerType` 和 SLA 截止时间；联系人快照可能继续为空 | `可开发但不可放量` |
| 店长待办状态机 | 03-19 已确认服务端守住状态流转，不再只靠前端按钮禁用 | `已修正` |

## 4. 服务端边界真值

### 4.1 状态机真值
1. `POST /booking/review/manager-todo/claim` 只允许从 `待认领` 进入。
2. `POST /booking/review/manager-todo/first-action` 只允许从 `已认领 / 处理中` 进入。
3. `POST /booking/review/manager-todo/close` 只允许从 `已认领 / 处理中` 进入。
4. `已闭环` 视为终态，不能再次认领或重复闭环。
5. 非法流转当前服务端统一按 `BOOKING_REVIEW_NOT_ELIGIBLE(1030008002)` fail-close。

### 4.2 lazy-init 真值
1. `ensureManagerTodoReady(...)` 当前只在店长待办写接口里触发。
2. 触发 lazy-init 时，系统会尝试补齐：
   - `negativeTriggerType=REVIEW_LEVEL_NEGATIVE`
   - `managerTodoStatus=待认领`
   - `managerClaimDeadlineAt`
   - `managerFirstActionDeadlineAt`
   - `managerCloseDeadlineAt`
3. 若 `bookingOrderId` 已失效或查不到 booking order，系统仍会继续按 `review.storeId` 尝试回填联系人快照。
4. 若 `storeId` 也为空，或门店主数据未命中，`managerContactName / managerContactMobile` 可以继续为 `null`。

## 5. 读写路径差异

### 5.1 写路径当前成立
1. 历史差评首次认领时可以补齐待办字段。
2. 缺失 booking order 行的历史差评仍能进入店长待办状态机。
3. 当前后台详情页已经明确给出“首次点击认领时后端补齐待办字段”的提示文案。

### 5.2 读路径当前不成立
1. admin 列表页当前不会自动把 `managerTodoStatus=null` 的历史差评补成待办。
2. dashboard 统计也不会把这类尚未初始化的历史差评自动算入店长待办池。
3. 因此“后台看板已覆盖全量历史差评”当前不成立。

## 6. 历史数据 No-Go
1. 不得把“首次写入时 lazy-init”写成“系统已自动修复历史数据”。
2. 不得把 read-path 未补齐的历史差评写成“已纳入全量 SLA 统计”。
3. 不得把 `managerContactName / managerContactMobile` 为空的历史差评写成“账号路由异常”；当前真实原因可能只是门店主数据未命中。
4. 不得把服务端状态机修复写成“booking review 已可放量”。

## 7. 当前开发进入条件与放量结论

| 维度 | 当前结论 |
|---|---|
| 文档状态 | `Doc Closed` |
| 当前是否可开发 | `Yes` |
| 当前是否可放量 | `No` |
| Release 结论 | `No-Go` |

## 8. 单一真值引用
- `docs/products/miniapp/2026-03-17-miniapp-booking-review-final-integration-review-v1.md`
- `docs/plans/2026-03-17-miniapp-booking-review-service-recovery-runbook-v1.md`
- `docs/contracts/2026-03-17-miniapp-booking-review-errorcode-and-failure-mode-v1.md`
