# Booking Review Negative Manager Todo Design

**Date:** 2026-03-18
**Status:** Approved design, not implemented
**Scope:** 在现有 `BO-005` 预约评价恢复台账上补“差评店长待办”能力，仅限后台待办/台账提醒

---

## 1. 背景与当前真值

当前仓库里 booking review 已经具备这些真实能力：
- 小程序评价提交、结果页、历史列表、详情回看已落地
- 后台已有评价台账、详情、回复、跟进状态更新、看板
- 差评会在数据层自动派生：
  - `reviewLevel=3`
  - `riskLevel=2`
  - `displayStatus=review_pending`
  - `followStatus=待跟进`

当前仓库里也已经明确这些“不存在”的真值：
- 没有自动通知店长
- 没有自动通知技师负责人 / 客服 / 区域负责人
- 没有自动补偿
- 没有自动奖励
- 没有 booking review 专属 release gate、rollout control、runtime sample pack

因此这次设计不是“补消息系统”，也不是“补自动恢复引擎”，而是在已有后台恢复链路上，新增一层面向店长的差评待办治理能力。

---

## 2. 已冻结的前提

本轮已确认的产品前提如下：

1. 只通知差评
   触发条件固定为：
   - `reviewLevel = 3`
   - 或 `overallScore <= 2`

2. 只做后台待办 / 台账提醒
   第一版不做：
   - 站内消息
   - 微信消息
   - 短信

3. 只通知门店店长
   第一版不扩到：
   - 技师负责人
   - 客服恢复 owner
   - 区域负责人

4. SLA 固定为：
   - 10 分钟认领
   - 30 分钟首次处理
   - 24 小时闭环

---

## 3. 方案对比

### 方案 A：复用现有 `BO-005` 评价恢复台账，补“店长待办”层

做法：
- 不新建独立系统
- 在现有后台评价台账、详情、看板上增加差评待办字段、状态和筛选
- 差评记录自动进入“店长待办池”

优点：
- 最贴合当前代码真值
- 与现有人工恢复链路连续
- 成本最低，落地最快

缺点：
- 第一版仍是后台治理能力，不是强触达通知能力

结论：采用。

### 方案 B：独立新建“差评待办台账”

做法：
- 从 booking review 中拆出一张独立待办台账和后台页面

优点：
- 职责更纯粹
- 二期扩展工单或消息更方便

缺点：
- 第一版过重
- 容易把当前恢复链路割裂成两套系统

结论：否决。

### 方案 C：直接接统一工单中心

做法：
- 差评自动建工单并指派店长

优点：
- 长期治理最标准

缺点：
- 当前 booking review 没有工单联动真值
- 需要额外补路由、SLA、升级、审计、回滚

结论：否决。

---

## 4. 核心设计决策

### 4.1 能力形态

第一版能力形态固定为：
- 负向评价自动进入后台“店长待办池”
- 店长待办只是后台运营能力，不是外部消息能力
- 页面基座复用 `BO-005`：
  - `/mall/booking/review`
  - `/mall/booking/review/detail`
  - `/mall/booking/review/dashboard`

### 4.2 店长对象真值

当前仓库里稳定可核的门店主数据来源为：
- `ProductStoreDO.contactName`
- `ProductStoreDO.contactMobile`

当前没有稳定的 `managerUserId` / “店长后台账号映射”真值。

所以第一版设计采用：
- `门店联系人(contactName/contactMobile)` 作为“店长联系人真值”
- 后台页面显示“待同步店长联系人”
- 不假设系统已经具备账号级消息路由

这意味着：
- 第一版能做“店长待办归属展示”
- 但还不能诚实地写成“系统已把消息推给某个后台店长账号”

如果后续要升级到账号级通知，再单独冻结二期能力，不在本次设计内。

### 4.3 不复用现有 `followStatus`

现有 `followStatus` / `followOwnerId` 已经用于“服务恢复跟进”。

第一版不建议把店长待办状态直接塞进 `followStatus`，否则会把两件事混成一件事：
- 服务恢复是否跟进
- 店长待办是否认领、首次处理、闭环

因此本次设计建议新增独立的“店长待办状态层”，而不是复用已有跟进状态。

---

## 5. 状态机设计

### 5.1 店长待办状态

第一版只保留 4 个状态：

- `PENDING_CLAIM`
  - 差评命中后进入待办池
  - 尚未认领

- `CLAIMED`
  - 已认领
  - 尚未记录首次处理动作

- `PROCESSING`
  - 已有首次处理动作
  - 仍在处理中

- `CLOSED`
  - 已确认闭环

### 5.2 状态迁移

- 新建差评待办 -> `PENDING_CLAIM`
- 认领 -> `CLAIMED`
- 记录首次处理 -> `PROCESSING`
- 标记闭环 -> `CLOSED`

### 5.3 与现有恢复状态的关系

- `managerTodoStatus` 是店长待办状态
- `followStatus` 仍是服务恢复状态

第一版不自动双向同步。

原因：
- 避免把“店长认领了”误写成“服务问题已经解决”
- 避免把“客服更新跟进状态”误写成“店长已认领”

---

## 6. SLA 设计

### 6.1 截止时间

针对差评待办，系统在创建待办时写入 3 个截止时间：
- `claimDeadlineAt = submitTime + 10 分钟`
- `firstActionDeadlineAt = submitTime + 30 分钟`
- `closeDeadlineAt = submitTime + 24 小时`

### 6.2 SLA 状态

第一版不建议持久化 `slaStatus`，而是查询时按当前时间动态计算：
- `NORMAL`
- `CLAIM_TIMEOUT`
- `FIRST_ACTION_TIMEOUT`
- `CLOSE_TIMEOUT`

这样好处是：
- 不需要额外调度任务去反复刷新状态
- 不会引入“状态已过时”的双写风险

### 6.3 不做的事情

第一版不做：
- 超时自动提醒
- 超时自动升级
- 超时自动建工单

超时仅表现为：
- 列表筛选
- 看板统计
- 详情页告警标记

---

## 7. 字段模型

### 7.1 建议新增字段

建议在 `booking_review` 记录上增加以下字段：

- `negativeTriggerType`
  - `REVIEW_LEVEL_NEGATIVE`
  - `LOW_SCORE_NEGATIVE`
- `managerContactName`
- `managerContactMobile`
- `managerTodoStatus`
- `managerClaimDeadlineAt`
- `managerFirstActionDeadlineAt`
- `managerCloseDeadlineAt`
- `managerClaimedByUserId`
- `managerClaimedAt`
- `managerFirstActionAt`
- `managerClosedAt`
- `managerLatestActionRemark`
- `managerLatestActionByUserId`

### 7.2 字段语义

- `managerContactName/contactMobile`
  - 来自门店主数据 `contactName/contactMobile`
  - 作为店长联系人快照，不代表系统已具备账号级消息路由

- `managerClaimedByUserId`
  - 记录是谁在后台执行了认领动作
  - 不等于“系统自动找到店长账号”

- `managerLatestActionRemark`
  - 记录最近一次处理动作说明
  - 用于台账审计和交接

### 7.3 不新增的字段

第一版不新增：
- `managerMessageId`
- `notifyChannel`
- `pushStatus`
- `compensationStatus`
- `rewardStatus`

---

## 8. 页面设计

### 8.1 台账页 `/mall/booking/review`

新增筛选：
- 仅看店长待办
- 店长待办状态
- SLA 状态

新增展示列：
- 店长联系人
- 店长待办状态
- 认领截止时间
- 首次处理截止时间
- 闭环截止时间
- SLA 状态

### 8.2 详情页 `/mall/booking/review/detail`

新增一个“店长待办”卡片，展示：
- 店长联系人
- 当前待办状态
- 3 条 SLA 截止时间
- 最近处理备注

新增动作：
- 认领
- 记录首次处理
- 标记闭环

保留原有动作：
- 回复评价
- 更新跟进状态

第一版不做：
- 自动同步店长外部通知结果
- 自动补偿动作

### 8.3 看板页 `/mall/booking/review/dashboard`

新增统计卡片：
- 今日新增差评待办
- 待认领数
- 首次处理超时数
- 闭环超时数
- 已闭环数

这些卡片只表示后台治理统计，不表示外部通知已触达。

---

## 9. 计划中的后台 API

以下仅为设计态 API，不是当前仓库既有真值：

### 9.1 复用并扩展

- `GET /booking/review/page`
  - 扩展返回店长待办字段
  - 支持新增筛选项

- `GET /booking/review/get`
  - 扩展返回单条店长待办字段

- `GET /booking/review/dashboard-summary`
  - 扩展返回待办与 SLA 统计

### 9.2 新增动作接口

- `POST /booking/review/manager-todo/claim`
- `POST /booking/review/manager-todo/first-action`
- `POST /booking/review/manager-todo/close`

设计原则：
- 不复用 `follow-status` 去承接店长待办状态
- 不把“回复评价”当成“认领待办”

---

## 10. MiniApp 边界

本设计不改小程序页面：
- 不新增“联系店长”
- 不新增“投诉升级”
- 不新增“自动补偿进度”
- 不新增“店长已受理”状态回显

用户侧只继续保持：
- 可提交评价
- 可回看评价
- 可查看商家回复

---

## 11. Non-Goals

第一版明确不做：
- 微信 / 短信 / 站内消息
- 自动补偿
- 自动好评奖励
- 自动升级区域负责人
- 工单系统联动
- 小程序端受理进度回显

---

## 12. 风险与依赖

### 12.1 当前最大风险

当前“店长”只存在为门店联系人真值：
- `contactName`
- `contactMobile`

如果实际业务上门店联系人并不总等于店长，待办归属会有偏差。

### 12.2 第一版接受的约束

第一版接受这个约束，前提是：
- PRD 明确写为“店长联系人待办”
- 不把它写成“账号级店长通知”

### 12.3 二期升级条件

如果未来要升级成真正的“系统通知店长账号”，必须补齐：
- `store -> managerUserId` 真值
- 消息通道真值
- 已读 / 重试 / 失败审计

这些都不属于本次设计。

---

## 13. Release Truth

这份文档只是设计冻结，不改变当前 release 判断。

当前 booking review 仍然只能写成：
- `Doc Closed`
- `Can Develop`
- `Cannot Release`

不得因为这份设计文档存在，就把以下能力误写成已上线：
- 差评自动通知店长
- 差评自动升级
- 差评自动补偿
- booking review 已 release-ready

