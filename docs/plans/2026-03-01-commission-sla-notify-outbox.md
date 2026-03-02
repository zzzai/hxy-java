# Commission SLA Notify Outbox Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 在技师佣金结算审批流中落地最小可用的 SLA 升级与通知闭环（P1 预警、P0 升级、通知出站与派发）。

**Architecture:** 在 `booking` 模块内新增 `notify_outbox` 表，按“状态机动作->写 outbox->异步派发”链路实现。P1 由临近超时触发，P0 由超时后延迟窗口触发。派发器先实现 `IN_APP` 占位通道，确保可观测、可重试、可幂等。

**Tech Stack:** Spring Boot, MyBatis-Plus, Quartz Job, JUnit5 + Mockito, MySQL migration SQL.

---

### Task 1: 数据模型与 SQL（outbox + 升级字段）

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/dal/dataobject/TechnicianCommissionSettlementDO.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/dal/dataobject/TechnicianCommissionSettlementNotifyOutboxDO.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/dal/mysql/TechnicianCommissionSettlementNotifyOutboxMapper.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/resources/sql/create_tables.sql`
- Modify: `ruoyi-vue-pro-master/sql/mysql/hxy/2026-03-01-hxy-technician-commission-settlement-workflow.sql`

**Step 1: 写失败测试**
- 在服务测试中新增“P0 升级只触发一次”的用例，先调用不存在的方法。

**Step 2: 运行测试确认失败**
- `mvn -pl yudao-module-mall/yudao-module-booking -Dtest=TechnicianCommissionSettlementServiceImplTest test`

**Step 3: 实现最小模型**
- 给结算单增加 `reviewEscalated/reviewEscalateTime` 字段。
- 新增 outbox DO 与 Mapper 基础查询/更新方法。
- SQL 与测试建表同步。

**Step 4: 运行测试确认编译通过**
- 同上命令。

### Task 2: SLA 触发逻辑（P1 预警 + P0 升级）写入 outbox

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/TechnicianCommissionSettlementService.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/TechnicianCommissionSettlementServiceImpl.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/dal/mysql/TechnicianCommissionSettlementMapper.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/service/impl/TechnicianCommissionSettlementServiceImplTest.java`

**Step 1: 写失败测试**
- 用例1：`warnNearDeadlinePending` 触发后写 `P1_WARN` outbox。
- 用例2：`escalateOverduePendingToP0` 触发后写 `P0_ESCALATE` outbox。
- 用例3：重复执行不重复写（受 `reviewWarned/reviewEscalated` 保护）。

**Step 2: 运行测试确认失败**
- `mvn -pl yudao-module-mall/yudao-module-booking -Dtest=TechnicianCommissionSettlementServiceImplTest test`

**Step 3: 最小实现**
- `warnNearDeadlinePending(lead,limit)` 继续保留，但增加写 outbox。
- 新增 `escalateOverduePendingToP0(delayMinutes, limit)`。
- 写操作日志动作：`SLA_WARN_P1`、`SLA_ESCALATE_P0`。

**Step 4: 运行测试确认通过**
- 同上命令。

### Task 3: 通知派发 Job（最小版）

**Files:**
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/job/TechnicianCommissionSettlementNotifyDispatchJob.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/TechnicianCommissionSettlementService.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/TechnicianCommissionSettlementServiceImpl.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/job/TechnicianCommissionSettlementNotifyDispatchJobTest.java`

**Step 1: 写失败测试**
- Job 默认参数执行时调用服务派发方法。
- 服务派发后 outbox 状态从 pending 变为 sent。

**Step 2: 运行测试确认失败**
- `mvn -pl yudao-module-mall/yudao-module-booking -Dtest=TechnicianCommissionSettlementNotifyDispatchJobTest,TechnicianCommissionSettlementServiceImplTest test`

**Step 3: 最小实现**
- 派发逻辑先做 `IN_APP` 占位发送：更新为 sent + 写 `NOTIFY_SENT` 日志。
- 支持简单重试字段更新（失败时 retry+1、next_retry_time）。

**Step 4: 运行测试确认通过**
- 同上命令。

### Task 4: 入口整合与回归

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/job/TechnicianCommissionSettlementSlaWarnJob.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/job/TechnicianCommissionSettlementSlaWarnJobTest.java`

**Step 1: 写失败测试**
- `SlaWarnJob` 同时触发 P1 预警和 P0 升级（参数含三元配置：`lead,delay,limit`）。

**Step 2: 运行测试确认失败**
- `mvn -pl yudao-module-mall/yudao-module-booking -Dtest=TechnicianCommissionSettlementSlaWarnJobTest test`

**Step 3: 最小实现**
- job 参数格式升级为 `leadMinutes,escalateDelayMinutes,limit`。
- 返回结果包含 warn/escalate 数量。

**Step 4: 全量定向验证**
- `mvn -pl yudao-module-mall/yudao-module-booking -Dtest=TechnicianCommissionSettlementServiceImplTest,TechnicianCommissionSettlementControllerTest,TechnicianCommissionSettlementSlaWarnJobTest,TechnicianCommissionSettlementSlaWarnJobTest,TechnicianCommissionSettlementNotifyDispatchJobTest test`
- `bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`

