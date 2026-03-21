# MiniApp Booking Review Admin Evidence Ledger v1（2026-03-21）

## 1. 目标
- 归档 2026-03-21 booking review admin-only 增量能力的真实提交、验证命令与当前发布结论。
- 明确这些证据只证明“治理与可观测面增强”，不证明“已可放量”。

## 2. 本批纳入的正式提交

| Commit | 类型 | 结论 |
|---|---|---|
| `200ee976ec` | 通知阻断诊断 | 已把 notify outbox 区分为发送失败、缺路由、缺账号、通道关闭等可读阻断态 |
| `7e14a98589` | 店长路由只读核查 | 已提供 `storeId -> managerAdminUserId` 只读核查入口，但仍未形成发布真值闭环 |
| `9d5011feeb` | 通知审计增强 | 已可查看最近动作、执行人和原因，并按 `lastActionCode` 过滤 |
| `d0674863c1` | 台账效率增强 | 已支持 SLA 快捷筛选与店长待办快捷动作，降低值班点击成本 |

## 3. 当前已核实证据

### 3.1 后台页面 / 能力证据
1. 评价台账页已具备：
   - 差评 / 风险 / 跟进 / SLA 基础筛选
   - `待认领优先 / 认领超时 / 首次处理超时 / 闭环超时 / 历史待初始化` 快捷筛选
   - `快速认领 / 记录首次处理 / 标记闭环` 快捷动作
2. 评价详情页已具备：
   - 最近动作时间线
   - notify outbox 观测块
   - 店长待办认领 / 首次处理 / 闭环
3. notify outbox 台账已具备：
   - `BLOCKED_NO_OWNER / FAILED / PENDING` 区分
   - 诊断结论、修复建议、是否允许人工重试
   - 最近动作说明、最近动作人、动作原因
4. 店长路由核查页已具备：
   - `storeId`
   - `managerAdminUserId`
   - `bindingStatus`
   - `effectiveTime / expireTime`
   - `source / lastVerifiedTime`

### 3.2 自动化证据

| 命令 | 结果 | 说明 |
|---|---|---|
| `mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewControllerTest,BookingReviewNotifyOutboxControllerTest,BookingReviewManagerAccountRoutingControllerTest,BookingReviewServiceImplTest,BookingReviewNotifyOutboxServiceTest,BookingReviewNotifyDispatchJobTest,BookingReviewManagerAccountRoutingQueryServiceImplTest test` | PASS，`45` tests | 覆盖 controller / service / notify dispatch / manager routing |
| `node --test tests/booking-review-admin-history-scan.test.mjs tests/booking-review-admin-query-helpers.test.mjs tests/booking-review-admin-detail-timeline.test.mjs tests/booking-review-admin-notify-outbox.test.mjs tests/booking-review-admin-manager-routing.test.mjs tests/booking-review-admin-ledger-efficiency.test.mjs` | PASS，`20` tests | 覆盖 admin 台账、详情、notify outbox、manager routing、快捷筛选与快捷动作 |
| `git diff --check` | PASS | 无 whitespace / patch error |
| `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh` | PASS | naming guard 正常 |
| `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh` | PASS | memory guard 正常 |

## 4. 当前还不能当作发布证据的项
1. 真实 App / 企微双通道消息发送样本 `未核出`。
2. 店长企微账号映射与共享发送端配置 `未核出`。
3. booking review 专属 runtime gate 脚本 `未核出`。
4. feature flag / rollout / rollback 控制面 `未核出`。
5. 发布级成功 / 失败样本包 `未核出`。

## 5. 当前结论

| 维度 | 当前结论 |
|---|---|
| 文档状态 | `Doc Closed` |
| 工程状态 | `Admin Ops Strengthened, Release Evidence Pending` |
| Can Develop | `Yes` |
| Can Release | `No` |
| Release Decision | `No-Go` |

## 6. No-Go
1. 不得把 admin-only 快捷动作写成自动通知链路闭环。
2. 不得把 `manager-routing` 只读核查页写成“店长企微 / App 双通道已打通”。
3. 不得把 node / maven 测试 PASS 写成发布级样本已齐。
4. 不得把 notify outbox 的审计增强写成“系统已稳定送达店长”。
