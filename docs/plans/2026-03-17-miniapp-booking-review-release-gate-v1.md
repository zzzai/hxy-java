# MiniApp Booking Review Release Gate v1（2026-03-17）

## 1. 目标
- 对 booking review 新域给出当前可执行的 Go / No-Go 判断。
- 重点区分：
  - 页面 / API / controller / 测试 已落地
  - runtime 发布证据 仍未闭环

## 2. 当前证据面
- 代码提交：
  - `38f1f939f9 feat(booking): add review persistence model`
  - `a04346b445 feat(booking): add app booking review APIs`
  - `b852b91081 feat(booking-admin): add review recovery controllers`
  - `390e74c246 feat(miniapp): add booking review pages and api client`
  - `dd2b70bd6c feat(miniapp): wire booking review entry points`
  - `b78c88edd3 feat(booking-admin-ui): add review recovery pages`
  - `200ee976ec feat(booking-review): add notify blocked diagnostics`
  - `7e14a98589 feat(booking-review): add manager routing readonly checks`
  - `9d5011feeb feat(booking-review): enhance notify outbox audit`
  - `d0674863c1 feat(booking-review): add ledger quick ops`
- 自动化：
  - `mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewControllerTest,BookingReviewNotifyOutboxControllerTest,BookingReviewManagerAccountRoutingControllerTest,BookingReviewServiceImplTest,BookingReviewNotifyOutboxServiceTest,BookingReviewNotifyDispatchJobTest,BookingReviewManagerAccountRoutingQueryServiceImplTest test`
  - `node --test tests/booking-review-admin-history-scan.test.mjs tests/booking-review-admin-query-helpers.test.mjs tests/booking-review-admin-detail-timeline.test.mjs tests/booking-review-admin-notify-outbox.test.mjs tests/booking-review-admin-manager-routing.test.mjs tests/booking-review-admin-ledger-efficiency.test.mjs`
  - 03-21 当前已补齐 `45` 个后端测试与 `20` 个 admin/node 测试样本
- evidence ledger：
  - `docs/plans/2026-03-21-miniapp-booking-review-admin-evidence-ledger-v1.md`
- 当前未核出：
  - 独立 booking review runtime gate 脚本
  - 发布级样本包
  - feature flag / switch
  - App / 企微双通道自动通知链路
  - 稳定双通道 `store -> manager account` 发布样本

## 3. Gate 判定表

| 判定项 | 当前状态 | 结论 |
|---|---|---|
| 用户侧真实 route | Yes | `/pages/booking/review-add`、`/pages/booking/review-result`、`/pages/booking/review-list` 已存在 |
| 用户侧真实 API | Yes | `/booking/review/*` 5 条 app API 已存在 |
| 后端 app controller | Yes | `AppBookingReviewController` 已存在 |
| 后台 admin controller | Yes | `BookingReviewController` 已存在 |
| 后台 overlay 页面 | Yes | 台账、详情、看板、notify outbox、manager routing 页面已存在 |
| 用户端 / admin node tests | Yes | 03-21 当前 admin/node 相关 `20` 项测试已通过 |
| 后端 controller / service tests | Yes | 03-21 当前 booking review 相关 `45` 项测试已通过 |
| admin-only 通知阻断诊断 | Yes | 已能区分发送失败、缺路由、缺账号、通道关闭 |
| admin-only 审计与快捷动作 | Yes | 已能查看最近动作/执行人/原因，并可在台账直接认领、记录首次处理、闭环 |
| Doc set | Yes | 本批 PRD / Contract / FailureMode / Runbook / Gate / Final Review 已齐 |
| 发布级 runtime 样本 | No | 未核到真实线上或准线上样本包 |
| 自动告警 / 店长通知 | No | 当前只有 admin-only 店长待办与 `IN_APP` 占位派发，没有真实 App / 企微外部通知链路 |
| feature flag / rollout control | No | 未核到独立开关 |
| 独立 release gate | No | 当前没有 booking review 专属 runtime gate |

## 4. 当前结论

| 维度 | 当前结论 |
|---|---|
| 文档状态 | `Doc Closed` |
| 工程状态 | `Feature Implemented, Release Evidence Pending` |
| Can Develop | `Yes` |
| Can Release | `No` |
| Release Decision | `No-Go` |

补充说明：
1. 03-19 的服务端状态机修复只说明写链更严谨，不代表 release 结论升级。
2. 03-19 复核确认：历史差评仍不会在 read-path 自动补齐店长待办字段，因此 dashboard / SLA 统计不覆盖所有历史数据。
3. 03-21 的 notify blocked diagnostics、manager routing、审计增强、台账快捷动作都只属于 admin-only 观察与操作面增强，不构成 release-ready 样本。

## 5. blocker_pool
1. 未核到 booking review 发布级成功 / 失败样本包。
2. 当前只有 admin-only 店长待办，未核到自动通知店长 / 技师负责人 / 客服负责人的链路。
3. 未核到 feature flag 或独立灰度范围控制。
4. 未核到独立 booking review runtime gate。
5. `serviceOrderId` 当前已改为后端按 `payOrderId -> TradeServiceOrderApi.listTraceByPayOrderId` best-effort 回填，但 trace 未命中或异常时仍允许为空。
6. `picUrls` 已完成前端提交链路，但历史 / 详情 / 运营回显证据仍未闭环。
7. 当前未核出稳定 `store -> managerUserId`，店长待办只停留在联系人快照治理，无法形成账号级通知或发布验收口径。
8. 历史差评待办字段当前只在写动作时 lazy-init，read-path 不自动回填。
9. 当前虽然已支持 `manager account routing + notify outbox + 审计增强 + 台账快捷动作`，但 App / 企微双通道发送、共享企微发送端、样本与门禁仍未闭环。

## 6. degraded_pool
1. 当前没有服务端 `degraded=true / degradeReason` 证据。
2. booking review 当前不应补造 `degraded_pool` 成功样本。
3. 合法空态只认：
   - 我的评价列表 `[]`
   - summary 计数 `0`
   - admin 台账 `[]`
   - dashboard 计数 `0`
4. 这些都只是空态，不是降级成功，也不是发布成功。

## 7. 回滚与重入条件

### 7.1 当前可执行回滚口径
- 由于未核到独立 feature flag，当前只能按代码回滚或后端资格控制收缩入口理解。
- 不得把“临时人工通知”写成系统化回滚措施。

### 7.2 未来 release 重入条件
1. 具备真实 runtime 样本包。
2. 具备明确 rollout / rollback 控制面。
3. 差评恢复链路有明确的通知或告警证据。
4. `serviceOrderId` best-effort 回填与 `picUrls` 提交后回显等关键缺口达成明确 release 结论。
5. 新一轮 A 集成文档重新评估后，才能从 `No-Go` 升级。
6. 若后续要做自动通知、自动派单或店长账号路由，必须先补 manager ownership 真值与运行样本。

## 8. Gate No-Go 条件
1. 把页面和 controller 已存在写成“可以放心放量”。
2. 把 node test PASS 写成 release-ready。
3. 把后台台账、看板存在写成“恢复体系已正式上线”。
4. 把人工通知店长写成“系统已支持即时通知店长”。
5. 把 `picUrls`、`serviceOrderId`、feature flag 未闭环的能力写成“已补齐”。
6. 把 03-19 状态机修复外推成“历史数据已自动修复”或“Can Release=Yes”。
