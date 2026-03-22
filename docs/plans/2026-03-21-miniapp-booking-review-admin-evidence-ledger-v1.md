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
4. 店长路由核查页已具备：
   - `managerAdminUserId`
   - `managerWecomUserId`
   - `bindingStatus / effectiveTime / expireTime`
   - `appRoutingLabel / wecomRoutingLabel / repairHint`
   - `覆盖率概览`
   - `只看缺任一绑定 / 缺 App / 缺企微 / 双缺失 / 双通道就绪` 快捷筛选

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

## 4. 自动化证据

| 命令 | 结果 | 说明 |
|---|---|---|
| `mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewControllerTest,BookingReviewNotifyOutboxControllerTest,BookingReviewManagerAccountRoutingControllerTest,BookingReviewServiceImplTest,BookingReviewNotifyOutboxServiceTest,BookingReviewNotifyDispatchJobTest,BookingReviewManagerAccountRoutingQueryServiceImplTest,BookingReviewManagerTodoSlaReminderJobTest test` | PASS，`50` tests | 覆盖 controller / service / notify dispatch / manager routing / SLA reminder |
| `node --test tests/booking-review-admin-notify-outbox.test.mjs tests/booking-review-admin-detail-timeline.test.mjs tests/booking-review-admin-query-helpers.test.mjs tests/booking-review-admin-ledger-efficiency.test.mjs tests/booking-review-admin-history-scan.test.mjs tests/booking-review-admin-manager-routing.test.mjs tests/booking-review-admin-sla-reminder.test.mjs` | PASS，`30` tests | 覆盖 admin 台账、详情、notify outbox、manager routing、快捷筛选、双通道摘要与 SLA 提醒 |
| `mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewManagerAccountRoutingControllerTest,BookingReviewManagerAccountRoutingQueryServiceImplTest,BookingReviewNotifyOutboxControllerTest,BookingReviewNotifyOutboxServiceTest test` | PASS，`25` tests | 覆盖 manager routing summary、覆盖率统计、缺失绑定过滤与 notify outbox 回归 |
| `git diff --check` | PASS | 无 whitespace / patch error |
| `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh` | PASS，`checked_files=3` | naming guard 正常 |
| `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh` | PASS，`checked_files=16`、`core_domains=0` | memory guard 正常 |

## 5. 当前还不能当作发布证据的项
1. 真实 App / 企微双通道消息发送样本 `未核出`。
2. 1000 家门店全量店长账号绑定与校验样本 `未核出`。
3. booking review 专属 runtime gate 脚本 `未核出`。
4. feature flag / rollout / rollback 控制面 `未核出`。
5. 发布级成功 / 失败样本包 `未核出`。

## 6. 当前结论

| 维度 | 当前结论 |
|---|---|
| 文档状态 | `Doc Closed` |
| 工程状态 | `Admin Ops Strengthened, Release Evidence Pending` |
| Can Develop | `Yes` |
| Can Release | `No` |
| Release Decision | `No-Go` |

## 7. No-Go
1. 不得把双通道路由页写成“店长企微 / App 双通道已正式上线”。
2. 不得把企微 sender 工程接入写成“真实企微送达已闭环”。
3. 不得把 node / maven 测试 PASS 写成发布级样本已齐。
4. 不得把 notify outbox 的审计增强写成“系统已稳定送达店长”。
5. 不得把 SLA reminder job 写成“超时升级链路已发布”。
