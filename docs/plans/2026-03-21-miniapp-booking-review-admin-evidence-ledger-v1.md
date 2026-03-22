# MiniApp Booking Review Admin Evidence Ledger v1（2026-03-21）

## 1. 目标
- 归档 2026-03-21 booking review admin-only 增量能力的真实实现、验证命令与当前发布结论。
- 明确这些证据只证明“治理、可观测与通知工程增强”，不证明“已可放量”。

## 2. 本批纳入的工程主题
1. 通知阻断诊断闭环。
2. 门店店长双通道路由真值闭环。
3. 通知链路审计增强。
4. App / 企微双通道出站与共享企微 sender 工程接入。
5. 店长待办 SLA 自动提醒出站。
6. 详情页双通道通知审计摘要。
7. 店长路由覆盖率与缺失绑定运营视图。
8. 门店绑定治理工作台。
9. notify outbox 跨通道派发审计二期。
10. 门店绑定来源真值闭环。
11. P2 运营效率 + 告警联动最终集成。

## 3. 当前已核实证据

### 3.1 后台页面 / 能力证据
1. 评价台账页已具备：
   - 差评 / 风险 / 跟进 / SLA 基础筛选
   - `待认领优先 / 认领超时提醒 / 首次处理超时提醒 / 闭环超时提醒 / 历史待初始化` 快捷筛选
   - `快速认领 / 记录首次处理 / 标记闭环` 快捷动作
2. 评价详情页已具备：
   - 最近动作时间线
   - notify outbox 观测块
   - `双通道摘要 / App 通道 / 企微通道 / 最近一条通知真值` 四层观测
   - 同一条评价下的 `IN_APP / WECOM` 双通道记录
   - 店长待办认领 / 首次处理 / 闭环
3. notify outbox 台账已具备：
   - `PENDING / SENT / FAILED / BLOCKED_NO_OWNER` 区分
   - `receiverAccount / receiverUserId / channel / notifyType`
   - 诊断结论、修复建议、是否允许人工重试
   - 最近动作说明、最近动作人、动作原因
   - `跨通道审计概览`
   - `跨通道结论 / 跨通道说明`
   - review 级 `双通道已发送 / 存在阻断 / 存在失败 / 人工重试待复核 / 跨通道分裂` 聚合统计
4. 店长路由核查页已具备：
   - `managerAdminUserId`
   - `managerWecomUserId`
   - `bindingStatus / effectiveTime / expireTime`
   - `appRoutingLabel / wecomRoutingLabel / repairHint`
   - `覆盖率概览`
   - `只看缺任一绑定 / 缺 App / 缺企微 / 双缺失 / 双通道就绪` 快捷筛选
   - `治理优先级 / 治理分组 / 核验状态 / 来源闭环 / 治理归口 / 交接摘要`
   - `只看立即治理 / 来源待闭环 / 长期未核验 / 可观察就绪` 快捷筛选
   - `来源闭环概览`
   - `来源结论 / 来源说明 / 下一步动作`
   - `只看来源已确认 / 来源缺失 / 联系人待转绑定 / 联系人缺失 / 来源待复核` 快捷筛选

### 3.2 工程能力证据
1. 差评创建后会按门店路由生成两条独立 outbox：
   - `IN_APP`
   - `WECOM`
2. 企微发送端当前已接入共享机器人 sender，并读取：
   - `hxy.booking.review.notify.wecom.enabled`
   - `hxy.booking.review.notify.wecom.webhook-url`
   - `hxy.booking.review.notify.wecom.app-name`
3. SLA 提醒已接入同一套 outbox：
   - `MANAGER_CLAIM_TIMEOUT`
   - `MANAGER_FIRST_ACTION_TIMEOUT`
   - `MANAGER_CLOSE_TIMEOUT`
4. 通道缺账号、无路由、企微关闭时统一落 `BLOCKED_NO_OWNER`，并通过 `lastErrorMsg` 区分 `NO_OWNER / NO_APP_ACCOUNT / NO_WECOM_ACCOUNT / CHANNEL_DISABLED`。

### 3.3 03-22 P2 正式窗口证据
1. 已核实并吸收的正式提交：
   - B：`acd28f8dbf9d3b41211a779dd2f9c07d39b94d45`
   - C：`ccfdcddd8f982a1ed3c3620ac479736eafeedbb1`
   - D：`5060844bacec84f25b99a8af6ea2c5d040a4d4e4`
2. P2 后台运营效率真值已固定：
   - `/mall/booking/review/dashboard` 只认观察入口 + 治理跳转入口
   - `/mall/booking/review` 只认唯一运营治理台账
   - 看板卡片与台账快捷筛选统一收口到同一套 query helper
3. P2 contract 真值已固定：
   - query 字段只认 `id / bookingOrderId / storeId / technicianId / memberId / reviewLevel / riskLevel / followStatus / onlyManagerTodo / onlyPendingInit / managerTodoStatus / managerSlaStatus / replyStatus / submitTime[]`
   - `priorityLevel / priorityReason / notifyRiskSummary` 当前只是返回字段或展示文案，不是 query 字段
   - `page/get/dashboard-summary` 已固定 `managerTimeoutCategory / priorityReasonCode / notifyAuditStage / priorityP0Count~P3Count / managerTimeout* / notifyAudit*`
4. P2 alert/runbook 真值已固定：
   - due soon 只认观察态：`CLAIM_DUE_SOON / FIRST_ACTION_DUE_SOON / CLOSE_DUE_SOON`
   - blocker 只认：`CLAIM_TIMEOUT / FIRST_ACTION_TIMEOUT / CLOSE_TIMEOUT / ANY_BLOCKED / BLOCKED_NO_OWNER:* / FAILED`
   - 当前没有服务端 `degraded=true / degradeReason` 真实证据
5. 03-22 P2 最终结论：
   - `已完成开发`
   - `已完成联调`
   - `admin-only 已可用`
   - `Can Release=No`
   - `Release Decision=No-Go`

## 4. 自动化证据

| 命令 | 结果 | 说明 |
|---|---|---|
| `mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewControllerTest,BookingReviewNotifyOutboxControllerTest,BookingReviewManagerAccountRoutingControllerTest,BookingReviewServiceImplTest,BookingReviewNotifyOutboxServiceTest,BookingReviewNotifyDispatchJobTest,BookingReviewManagerAccountRoutingQueryServiceImplTest,BookingReviewManagerTodoSlaReminderJobTest test` | PASS，`50` tests | 覆盖 controller / service / notify dispatch / manager routing / SLA reminder |
| `node --test tests/booking-review-admin-notify-outbox.test.mjs tests/booking-review-admin-detail-timeline.test.mjs tests/booking-review-admin-query-helpers.test.mjs tests/booking-review-admin-ledger-efficiency.test.mjs tests/booking-review-admin-history-scan.test.mjs tests/booking-review-admin-manager-routing.test.mjs tests/booking-review-admin-sla-reminder.test.mjs` | PASS，`30` tests | 覆盖 admin 台账、详情、notify outbox、manager routing、快捷筛选、双通道摘要与 SLA 提醒 |
| `mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewManagerAccountRoutingControllerTest,BookingReviewManagerAccountRoutingQueryServiceImplTest,BookingReviewNotifyOutboxControllerTest,BookingReviewNotifyOutboxServiceTest test` | PASS，`25` tests | 覆盖 manager routing summary、覆盖率统计、缺失绑定过滤与 notify outbox 回归 |
| `mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewManagerAccountRoutingControllerTest,BookingReviewManagerAccountRoutingQueryServiceImplTest,BookingReviewNotifyOutboxControllerTest,BookingReviewNotifyOutboxServiceTest test` | PASS，`26` tests | 覆盖治理字段派生、治理分组过滤、manager routing summary 与 notify outbox 回归 |
| `node --test tests/booking-review-admin-manager-routing.test.mjs tests/booking-review-admin-notify-outbox.test.mjs` | PASS，`9` tests | 覆盖门店绑定治理工作台、来源闭环概览、来源结论/说明/下一步动作，以及 notify outbox 跳转回归 |
| `node --test tests/booking-review-admin-notify-outbox.test.mjs tests/booking-review-admin-detail-timeline.test.mjs tests/booking-review-admin-manager-routing.test.mjs` | PASS，`14` tests | 覆盖 notify outbox 跨通道审计概览、详情双通道观测、manager routing 联动回归 |
| `mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewNotifyOutboxControllerTest,BookingReviewNotifyOutboxServiceTest,BookingReviewManagerAccountRoutingControllerTest,BookingReviewManagerAccountRoutingQueryServiceImplTest test` | PASS，`28` tests | 覆盖 notify outbox summary、review 级跨通道审计派生、manager routing 既有回归 |
| `mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewManagerAccountRoutingControllerTest,BookingReviewManagerAccountRoutingQueryServiceImplTest,BookingReviewNotifyOutboxControllerTest,BookingReviewNotifyOutboxServiceTest test` | PASS，`31` tests | 覆盖来源真值分层、来源概览统计、来源真值筛选，以及非 ACTIVE 路由记录的来源真值回归 |
| `node --test tests/booking-review-admin-ledger-efficiency.test.mjs tests/booking-review-admin-query-helpers.test.mjs` | PASS | 覆盖 P2 看板/台账 drill-down 与 query helper 回归 |
| `mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewControllerTest,BookingReviewServiceImplTest test` | PASS，`27` tests | 覆盖 P2 admin query / detail / dashboard-summary 合同与派生字段回归 |
| `node --test tests/booking-review-admin-sla-reminder.test.mjs` | PASS，`5` tests | 覆盖 P2 SLA reminder 阈值与观察/阻断边界 |
| `mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewManagerTodoSlaReminderJobTest test` | PASS，`2` tests | 覆盖 P2 reminder job 边界，不把 due soon 外推成 timeout reminder |
| `git diff --check` | PASS | 无 whitespace / patch error |
| `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh` | PASS，`checked_files=2` | naming guard 正常 |
| `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh` | PASS，`checked_files=13`、`core_domains=0` | memory guard 正常 |

## 5. 当前还不能当作发布证据的项
1. 真实 App / 企微双通道消息发送样本 `未核出`。
2. 1000 家门店全量店长账号绑定与校验样本 `未核出`。
3. booking review 专属 runtime gate 脚本 `未核出`。
4. feature flag / rollout / rollback 控制面 `未核出`。
5. 发布级成功 / 失败样本包 `未核出`。
6. `priorityLevel / priorityReason / notifyRiskSummary` 独立 query / release capability `未核出`。
7. 稳定 admin 专属错误码 `未核出`。
8. 服务端 `degraded=true / degradeReason` 真实证据 `未核出`。

## 6. 当前结论

| 维度 | 当前结论 |
|---|---|
| 文档状态 | `Doc Closed` |
| 工程状态 | `Admin Ops Strengthened, Release Evidence Pending` |
| 已完成联调 | `Yes` |
| admin-only 已可用 | `Yes` |
| Can Develop | `Yes` |
| Can Release | `No` |
| Release Decision | `No-Go` |

## 7. No-Go
1. 不得把双通道路由页写成“店长企微 / App 双通道已正式上线”。
2. 不得把企微 sender 工程接入写成“真实企微送达已闭环”。
3. 不得把 node / maven 测试 PASS 写成发布级样本已齐。
4. 不得把 notify outbox 的审计增强写成“系统已稳定送达店长”。
5. 不得把 SLA reminder job 写成“超时升级链路已发布”。
6. 不得把 `priorityLevel`、`priorityReason`、`notifyRiskSummary` 写成独立 query 或稳定 release capability。
7. 不得把 `SENT / DUAL_SENT`、dashboard 聚合计数或 notify audit 阶段写成真实外部送达率、已读率、闭环率。
8. 不得把 due soon 观察态、`ANY_FAILED`、`MANUAL_RETRY_PENDING`、`DIVERGED` 写成自动告警联动已上线。
