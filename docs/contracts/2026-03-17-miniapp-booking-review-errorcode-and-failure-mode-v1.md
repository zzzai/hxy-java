# MiniApp Booking Review ErrorCode And Failure Mode v1（2026-03-17）

## 1. 目标
- 固定 booking review 当前真实稳定错误码、失败模式、重试策略与合法空态。
- 不按 message 分支，不补造未落地的降级字段。

## 2. 当前错误码真值来源
- `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/enums/ErrorCodeConstants.java`
- `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/BookingReviewServiceImpl.java`
- `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/service/impl/BookingReviewServiceImplTest.java`

## 3. 当前稳定 errorCode

| code | 常量 | 当前触发范围 |
|---|---|---|
| `1030008000` | `BOOKING_REVIEW_NOT_EXISTS` | `GET /booking/review/get` 未命中；后台 `get / reply / follow-status` 未命中 |
| `1030008001` | `BOOKING_REVIEW_ALREADY_EXISTS` | 重复提交同一 booking 订单评价 |
| `1030008002` | `BOOKING_REVIEW_NOT_ELIGIBLE` | 订单未完成，不能创建评价 |
| `1030004000` | `BOOKING_ORDER_NOT_EXISTS` | 创建评价时 booking 订单不存在 |
| `1030004006` | `BOOKING_ORDER_NOT_OWNER` | 创建评价时当前用户不是订单 owner |

## 4. 不按 errorCode 返回的资格态

### `GET /booking/review/eligibility`
- 当前稳定口径是：`code=0` + 结构化 reason。
- 当前 reason 真值：
  - `ORDER_NOT_EXISTS`
  - `NOT_OWNER`
  - `ALREADY_REVIEWED`
  - `ORDER_NOT_COMPLETED`
- 这些是资格判断结果，不是对外稳定错误码。
- 当前页面做法：
  - 命中 reason 后显示固定文案
  - API 非 0 时统一降成 `暂不可评价`

## 5. Failure Mode Matrix

| 接口 | 当前失败模式 | 空态 / 特殊态 | 重试策略 | 当前前端处理 |
|---|---|---|---|---|
| `GET /booking/review/eligibility` | `FAIL_CLOSE` for transport / non-zero code；业务资格不走 errorCode，而走 `eligible=false` | `eligible=false` 是合法业务态，不是异常 | `NO_AUTO_RETRY` | 页面显示 `reasonText` 或统一 `暂不可评价` |
| `POST /booking/review/create` | `FAIL_CLOSE` | 无成功空态 | `MANUAL_RETRY` | 失败统一 toast `提交失败，请稍后重试`，表单保留 |
| `GET /booking/review/page` | `QUERY_ONLY` | `list=[]`、`total=0` 合法 | `MANUAL_RETRY` | 空态显示 `暂无评价` |
| `GET /booking/review/get` | `FAIL_CLOSE` | 无成功空态 | `MANUAL_RETRY` | 当前 miniapp `review-detail` 与后台详情页都失败关闭；用户侧显示显式空态/失败态 |
| `GET /booking/review/summary` | `QUERY_ONLY` | 各统计项 `0` 合法 | `MANUAL_RETRY` | 0 只算空态，不算成功运营样本 |
| `GET /booking/review/page`（admin） | `QUERY_ONLY` | `list=[]`、`total=0` 合法 | `MANUAL_RETRY` | 台账页保持空表 |
| `POST /booking/review/reply` | `FAIL_CLOSE` | 无成功空态 | `MANUAL_RETRY` | 后台详情页人工确认后重试 |
| `POST /booking/review/follow-status` | `FAIL_CLOSE` | 无成功空态 | `MANUAL_RETRY` | 后台详情页人工确认后重试 |
| `GET /booking/review/dashboard-summary` | `QUERY_ONLY` | 所有计数为 `0` 合法 | `MANUAL_RETRY` | 0 只算空态，不算恢复成功 |

## 6. 当前页面层恢复动作

### 6.1 用户侧
1. 资格校验失败：停留当前页，不自动跳转。
2. 提交失败：保持表单内容，不自动清空，不跳结果页。
3. 列表为空：展示 `暂无评价`。
4. 详情加载失败：显示显式空态/失败态，可返回我的评价。
5. 结果页未拿到 `reviewId`：显示 `提交未完成`。

### 6.2 后台
1. 台账空列表：只算合法空态。
2. 回复 / 跟进写失败：当前页面只有人工重试，没有自动重试。
3. 看板 0 值：只算当前无样本，不代表治理完成。

## 7. 当前没有证据的项
1. 没有已提交服务端 `degraded=true / degradeReason`。
2. 没有 booking review 专属自动告警 / 自动工单 / 自动通知错误码。
3. 没有“好评奖励成功 / 差评补偿成功”的稳定错误码，因为这些能力未实现。
4. `picUrls` 已接入提交页上传并随 `POST /booking/review/create` 发送；当前未核出 booking review 专属的上传失败错误码、独立恢复分支或发布样本。

## 8. ErrorCode 级 No-Go
1. 不得按 message 文本分支。
2. 不得把 `ORDER_NOT_EXISTS / NOT_OWNER / ALREADY_REVIEWED / ORDER_NOT_COMPLETED` 写成稳定 errorCode。
3. 不得把 `code=0` 但缺少 runtime 样本的写接口记为 release-ready。
4. 不得因为常量存在就补造自动奖励 / 自动补偿 / 自动通知的错误码分支。
