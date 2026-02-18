# Sprint1 后端任务拆解（代码级）

> 范围：2026-02-17 ~ 2026-03-01  
> 目标：预约与排班主链路可用（可查槽、可下单、可查单）

---

## 1. 已完成（本轮落地）

### 1.1 新增 API（前台）

1. `GET /api/front/booking/slots/{scheduleId}`
2. `POST /api/front/booking/create`
3. `GET /api/front/booking/detail/{orderNo}`
4. `POST /api/front/booking/pay`

控制器文件：
- `/root/crmeb-java/crmeb_java/crmeb/crmeb-front/src/main/java/com/zbkj/front/controller/ServiceBookingController.java`

### 1.2 新增请求/响应对象

1. `ServiceBookingCreateRequest`
2. `ServiceBookingOrderResponse`
3. `ServiceBookingSlotResponse`
4. `ServiceBookingPayRequest`

文件：
- `/root/crmeb-java/crmeb_java/crmeb/crmeb-common/src/main/java/com/zbkj/common/request/ServiceBookingCreateRequest.java`
- `/root/crmeb-java/crmeb_java/crmeb/crmeb-common/src/main/java/com/zbkj/common/request/ServiceBookingPayRequest.java`
- `/root/crmeb-java/crmeb_java/crmeb/crmeb-common/src/main/java/com/zbkj/common/response/ServiceBookingOrderResponse.java`
- `/root/crmeb-java/crmeb_java/crmeb/crmeb-common/src/main/java/com/zbkj/common/response/ServiceBookingSlotResponse.java`

### 1.3 新增服务与 DAO

1. `ServiceBookingService`
2. `ServiceBookingServiceImpl`
3. `BookingOrderDao`
4. `TechnicianScheduleDao`

文件：
- `/root/crmeb-java/crmeb_java/crmeb/crmeb-service/src/main/java/com/zbkj/service/service/ServiceBookingService.java`
- `/root/crmeb-java/crmeb_java/crmeb/crmeb-service/src/main/java/com/zbkj/service/service/impl/ServiceBookingServiceImpl.java`
- `/root/crmeb-java/crmeb_java/crmeb/crmeb-service/src/main/java/com/zbkj/service/dao/BookingOrderDao.java`
- `/root/crmeb-java/crmeb_java/crmeb/crmeb-service/src/main/java/com/zbkj/service/dao/TechnicianScheduleDao.java`

### 1.4 数据模型修正

`BookingOrder` 已按 `eb_booking_order` 字段口径对齐。

文件：
- `/root/crmeb-java/crmeb_java/crmeb/crmeb-service/src/main/java/com/zbkj/service/model/BookingOrder.java`

---

## 2. 当前关键能力说明

1. 下单幂等：基于 `idempotentToken` + Redis 键实现。
2. 时间槽并发保护：基于 `DistributedLockUtil` 锁 `scheduleId+slotId`。
3. 槽位锁定：下单后将槽状态写为 `locked`，并写 `lockedExpire`。
4. 订单落库：写入 `eb_booking_order`，状态为 `1=待支付`。
5. 已支持预约支付发起：微信预下单使用 `outTradeNo=booking.orderNo` 与 `attach.type=booking`。
6. 已支持预约支付结果查询：`/api/front/pay/queryPayResult` 可直接查询预约订单号。

---

## 3. 下一批必须完成（Sprint1 剩余）

## 3.1 支付回调对齐（P0）

1. 已完成预约订单支付成功处理：
   - `status: 1 -> 2`
   - 时间槽 `locked -> booked`
2. 已接入微信回调扩展类型：
   - `attach.type=booking` 时进入预约支付成功处理。
3. 回调接入约束：
   - 预约支付下单时需确保 `outTradeNo = booking.orderNo`。

落地文件：
- `ServiceBookingServiceImpl`（新增 `paySuccess`）
- `CallbackServiceImpl`（新增 `booking` 类型分支）
- `Constants`（新增 `SERVICE_PAY_TYPE_BOOKING`）

## 3.2 取消/超时释放（P0）

1. 已完成主动取消预约：释放时间槽并回补 `availableSlots`。
2. 已完成超时未支付（`lockedExpire`）释放：
   - 服务方法 `releaseExpiredLocks`
   - 管理端手动触发接口
   - 定时任务类（每分钟执行）

落地文件：
- `ServiceBookingServiceImpl`（新增 `cancel` / `releaseExpiredLocks`）
- `ServiceBookingController`（新增用户取消接口）
- `BookingOrderOpsController`（新增管理端运维接口）
- 定时任务：`BookingOrderTimeoutReleaseTask`

## 3.3 可观测性（P1）

1. 预约下单日志（orderNo、scheduleId、slotId、uid）。
2. 冲突与失败原因分类统计。

## 3.4 下一步（P1）

1. 补预约支付发起/查询的集成测试（小程序、公众号、H5 三种渠道）。
2. 增加预约支付失败的业务错误码映射（前端可直连提示）。
3. 增加定时任务启停配置开关（避免测试环境误触发）。

---

## 4. 联调用例（最小集）

1. 同一槽位并发 2 次下单，只有 1 次成功。
2. 同一 `idempotentToken` 重试返回同一 `orderNo`。
3. 非订单归属用户查询 `detail` 返回权限错误。
4. 过期锁位可被重新预约（待实现自动释放后验证）。

---

## 5. 编译验证

已执行：

```bash
cd /root/crmeb-java/crmeb_java/crmeb
mvn -pl crmeb-front -am -DskipTests compile
mvn -pl crmeb-admin -am -DskipTests compile
```

结果：编译通过。
