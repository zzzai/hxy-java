# API接口文档

## 基础信息

- **Base URL**: `http://localhost:8080`
- **Content-Type**: `application/json`
- **认证方式**: Token (Header: `Authorization: Bearer {token}`)

---

## 1. 技师管理

### 1.1 查询技师列表

**接口**: `GET /api/admin/technician/list`

**参数**:
```json
{
  "storeId": 1
}
```

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": [{
    "id": 1,
    "storeId": 1,
    "name": "张技师",
    "avatar": "https://...",
    "level": 3,
    "serviceYears": 5.5,
    "skillTags": "推拿,按摩,足疗",
    "intro": "从业5年，擅长...",
    "rating": 4.8,
    "orderCount": 1250,
    "status": 1
  }]
}
```

### 1.2 查询技师详情

**接口**: `GET /api/admin/technician/detail/{id}`

**响应**: 同上单个对象

### 1.3 新增技师

**接口**: `POST /api/admin/technician/add`

**请求体**:
```json
{
  "storeId": 1,
  "name": "李技师",
  "avatar": "https://...",
  "level": 2,
  "serviceYears": 3.0,
  "skillTags": "推拿,按摩",
  "intro": "从业3年...",
  "status": 1
}
```

### 1.4 更新技师

**接口**: `POST /api/admin/technician/update`

**请求体**: 同新增，需包含id

---

## 2. 排班管理

### 2.1 批量生成排班

**接口**: `POST /api/admin/schedule/batch-generate`

**参数**:
```json
{
  "storeId": 1,
  "technicianIds": [1, 2, 3],
  "startDate": "2026-02-14",
  "endDate": "2026-02-20"
}
```

**响应**:
```json
{
  "code": 200,
  "message": "成功生成21条排班记录",
  "data": 21
}
```

### 2.2 生成单个排班

**接口**: `POST /api/admin/schedule/generate`

**参数**:
```json
{
  "storeId": 1,
  "technicianId": 1,
  "serviceSkuId": 1,
  "workDate": "2026-02-14",
  "startTime": "09:00",
  "endTime": "21:00",
  "slotDuration": 60,
  "basePrice": 128.0
}
```

### 2.3 应用闲时优惠

**接口**: `POST /api/admin/schedule/apply-offpeak`

**参数**:
```json
{
  "scheduleId": 1,
  "offpeakRules": {
    "discount_rate": 0.8,
    "periods": [
      {"start": "09:00", "end": "12:00"},
      {"start": "14:00", "end": "17:00"}
    ]
  }
}
```

### 2.4 查询技师排班

**接口**: `GET /api/admin/schedule/technician`

**参数**:
```json
{
  "technicianId": 1,
  "workDate": "2026-02-14"
}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "technicianId": 1,
    "workDate": "2026-02-14",
    "timeSlots": "{\"slots\":[...]}",
    "totalSlots": 12,
    "availableSlots": 10,
    "status": 1
  }
}
```

---

## 3. 预约管理

### 3.1 预约时间槽

**接口**: `POST /api/admin/booking/book`

**参数**:
```json
{
  "userId": 1,
  "scheduleId": 1,
  "slotId": "20260214_0900"
}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "schedule_id": 1,
    "slot_id": "20260214_0900",
    "locked_expire": 1739520000,
    "price": 128.00,
    "offpeak_price": 102.40
  }
}
```

### 3.2 确认预约

**接口**: `POST /api/admin/booking/confirm`

**参数**:
```json
{
  "scheduleId": 1,
  "slotId": "20260214_0900",
  "orderId": 1001
}
```

---

## 4. 库存管理

### 4.1 锁定库存

**接口**: `POST /api/admin/stock/lock`

**参数**:
```json
{
  "storeId": 1,
  "skuId": 1,
  "quantity": 2,
  "orderId": 1001,
  "operatorId": 1
}
```

### 4.2 释放库存

**接口**: `POST /api/admin/stock/release`

**参数**: 同锁定库存

### 4.3 扣减库存

**接口**: `POST /api/admin/stock/deduct`

**参数**: 同锁定库存

### 4.4 退款回库存

**接口**: `POST /api/admin/stock/refund`

**参数**: 同锁定库存

---

## 5. 会员卡管理

### 5.1 使用次卡

**接口**: `POST /api/admin/member-card/use-times`

**参数**:
```json
{
  "userCardId": 1,
  "times": 2,
  "orderId": 1001,
  "storeId": 1,
  "technicianId": 1
}
```

### 5.2 使用储值卡

**接口**: `POST /api/admin/member-card/use-stored`

**参数**:
```json
{
  "userCardId": 2,
  "amount": 128.00,
  "orderId": 1002,
  "storeId": 1,
  "technicianId": 1
}
```

### 5.3 查询会员卡

**接口**: `GET /api/admin/member-card/detail/{id}`

**响应**:
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "userId": 1,
    "cardId": 1,
    "cardNo": "MC202602130001",
    "remainingTimes": 8,
    "remainingAmount": 0,
    "status": 1,
    "activeAt": 1739404800,
    "expireAt": 1770940800
  }
}
```

### 5.4 检查会员卡可用性

**接口**: `GET /api/admin/member-card/check/{id}`

**响应**:
```json
{
  "code": 200,
  "data": true
}
```

---

## 6. 预约订单

### 6.1 创建订单

**接口**: `POST /api/admin/booking-order/create`

**参数**:
```json
{
  "userId": 1,
  "storeId": 1,
  "technicianId": 1,
  "scheduleId": 1,
  "slotId": "20260214_0900",
  "serviceSkuId": 1
}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "order_no": "BO17394048001234",
    "order_id": 1001,
    "original_price": 128.00,
    "paid_amount": 102.40,
    "is_offpeak": true,
    "booking_date": "2026-02-14",
    "booking_time": "09:00-10:00",
    "locked_expire": 1739520000
  }
}
```

### 6.2 支付订单

**接口**: `POST /api/admin/booking-order/pay`

**参数**:
```json
{
  "orderNo": "BO17394048001234",
  "payType": 1,
  "memberCardId": null
}
```

**支付方式**:
- 1: 微信支付
- 2: 支付宝
- 3: 会员卡

### 6.3 取消订单

**接口**: `POST /api/admin/booking-order/cancel`

**参数**:
```json
{
  "orderNo": "BO17394048001234"
}
```

### 6.4 核销订单

**接口**: `POST /api/admin/booking-order/verify`

**参数**:
```json
{
  "verifyCode": "123456",
  "verifiedBy": 1
}
```

### 6.5 完成订单

**接口**: `POST /api/admin/booking-order/complete`

**参数**:
```json
{
  "orderNo": "BO17394048001234"
}
```

### 6.6 查询订单详情

**接口**: `GET /api/admin/booking-order/detail`

**参数**:
```json
{
  "orderNo": "BO17394048001234"
}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "id": 1001,
    "orderNo": "BO17394048001234",
    "userId": 1,
    "storeId": 1,
    "technicianId": 1,
    "bookingDate": "2026-02-14",
    "bookingTime": "09:00-10:00",
    "originalPrice": 128.00,
    "paidAmount": 102.40,
    "isOffpeak": 1,
    "payType": 1,
    "status": 2,
    "verifyCode": "123456",
    "createdAt": 1739404800,
    "paidAt": 1739404900
  }
}
```

---

## 7. 闲时优惠

### 7.1 创建规则

**接口**: `POST /api/admin/offpeak/create`

**请求体**:
```json
{
  "ruleName": "工作日上午优惠",
  "storeId": 1,
  "discountRate": 0.8,
  "timePeriods": "{\"periods\":[{\"start\":\"09:00\",\"end\":\"12:00\"}]}",
  "weekdays": "1,2,3,4,5",
  "isEnabled": 1,
  "priority": 10
}
```

### 7.2 更新规则

**接口**: `POST /api/admin/offpeak/update`

**请求体**: 同创建，需包含id

### 7.3 删除规则

**接口**: `POST /api/admin/offpeak/delete/{id}`

### 7.4 查询门店规则

**接口**: `GET /api/admin/offpeak/store/{storeId}`

### 7.5 查询全局规则

**接口**: `GET /api/admin/offpeak/global`

### 7.6 启用/禁用规则

**接口**: `POST /api/admin/offpeak/toggle`

**参数**:
```json
{
  "ruleId": 1,
  "enabled": true
}
```

---

## 8. 用户数据合规治理（新增）

### 8.1 前台隐私中心（front）

基础路径：`/api/front/privacy`

1. 授权：`POST /api/front/privacy/consent/grant`
请求体：
```json
{
  "scenarioCode": "HEALTH_BASIC",
  "policyVersion": "v1.0.0",
  "consentTextHash": "a94a8fe5ccb19ba61c4c0873d391e987982fbbd3f1d5d36f0a8cd824f8f2f21d",
  "dataScopeJson": "{\"fields\":[\"complaint_type\",\"discomfort_score_before\"]}",
  "purposeCodesJson": "[\"SERVICE_PLAN\",\"EFFECT_EVAL\"]",
  "sourceChannel": "miniapp",
  "storeId": 1
}
```

2. 撤回授权：`POST /api/front/privacy/consent/withdraw`
请求体：
```json
{
  "consentId": 1
}
```

3. 授权记录：`GET /api/front/privacy/consent/list?page=1&limit=10`

4. 提交删除工单：`POST /api/front/privacy/deletion/request`
请求体：
```json
{
  "scopeCode": "HEALTH_DATA",
  "scopeJson": "{\"deleteFields\":[\"tongue_photo_url\"]}"
}
```

5. 撤销删除工单：`POST /api/front/privacy/deletion/cancel`
请求体：
```json
{
  "ticketId": 1
}
```

6. 我的删除工单：`GET /api/front/privacy/deletion/list?page=1&limit=10`

### 8.2 管理端治理中心（admin）

基础路径：`/api/admin/data/governance`

1. 字段治理目录：`GET /api/admin/data/governance/field/list?necessityLevel=1&page=1&limit=10`
2. 授权记录：`GET /api/admin/data/governance/consent/list?scenarioCode=HEALTH_BASIC&status=1&page=1&limit=10`
3. 创建访问工单：`POST /api/admin/data/governance/access-ticket/create`
4. 工单审批通过：`POST /api/admin/data/governance/access-ticket/approve`
5. 工单驳回：`POST /api/admin/data/governance/access-ticket/reject`
6. 工单关闭：`POST /api/admin/data/governance/access-ticket/close`
7. 访问工单列表：`GET /api/admin/data/governance/access-ticket/list?status=0&page=1&limit=10`
8. 删除工单列表：`GET /api/admin/data/governance/deletion/list?status=1&page=1&limit=10`
9. 标签策略列表：`GET /api/admin/data/governance/label-policy/list?riskLevel=2&enabled=1&page=1&limit=10`
10. 标签策略开关：`POST /api/admin/data/governance/label-policy/update-status`

补充：
1. `access-ticket/create` 的 `dataLevel>=3` 会进入审批态（`status=0`）。
2. 高争议标签默认关闭，启用前需要合规评估与人工复核规则。

---

## 状态码说明

### 订单状态
- 1: 待支付
- 2: 已支付
- 3: 已完成
- 4: 已取消
- 5: 已退款

### 时间槽状态
- 1: 可预约
- 2: 已锁定
- 3: 已预约
- 4: 已完成

### 会员卡状态
- 1: 正常
- 2: 已用完
- 3: 已过期
- 4: 已冻结

---

## 错误码

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未授权 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器错误 |

