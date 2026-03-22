# MiniApp Booking Review Final Integration Review v1（2026-03-17）

## 1. 目标与吸收边界
- 目标：把 booking review 当前代码、测试与文档统一成单一真值，并明确哪些只是工程能力，哪些仍未达到发布证据。
- 本文吸收的正式提交包括：
  - `38f1f939f9 feat(booking): add review persistence model`
  - `a04346b445 feat(booking): add app booking review APIs`
  - `b852b91081 feat(booking-admin): add review recovery controllers`
  - `390e74c246 feat(miniapp): add booking review pages and api client`
  - `dd2b70bd6c feat(miniapp): wire booking review entry points`
  - `b78c88edd3 feat(booking-admin-ui): add review recovery pages`
  - `2ea755b034 feat(booking-review): add member review detail page`
  - `3c34cdc52f feat(booking-review): add negative manager todo workflow`
  - `f1569f30b4 feat(admin): add booking review manager todo ops`
  - `b7a51b9a0f docs(booking-review): freeze manager todo truth`
  - `200ee976ec feat(booking-review): add notify blocked diagnostics`
  - `7e14a98589 feat(booking-review): add manager routing readonly checks`
  - `9d5011feeb feat(booking-review): enhance notify outbox audit`
  - `d0674863c1 feat(booking-review): add ledger quick ops`
- 本文同时吸收 2026-03-21 当前分支的新增工程真值：
  - 差评通知按 `IN_APP / WECOM` 生成两条独立 outbox
  - `storeId -> managerAdminUserId + managerWecomUserId` 双通道路由真值
  - 企微机器人发送器接入工程实现
  - 店长待办 SLA 提醒任务接入同一套 outbox
- 本文同时吸收 2026-03-22 booking review P2 的正式窗口输出：
  - `acd28f8dbf feat(booking-review): improve ops efficiency drilldown`
  - `ccfdcddd8f982a1ed3c3620ac479736eafeedbb1 feat(booking-review): close ops alert contract truth`
  - `5060844bac docs(booking-review): close ops alert runbook gate`
- 03-22 P2 吸收范围固定为：
  - 看板/台账观察入口与治理入口分离
  - admin query / detail / dashboard-summary contract 收口
  - ops alert runbook / acceptance SOP / threshold / blocker 口径收口
- 本文不吸收：
  - 未落盘的运行样本
  - 未验证的真实企微送达回执
  - 奖励、补偿、区域负责人升级等当前未实现能力

## 2. 最终结论

| 判断项 | 当前结论 | 说明 |
|---|---|---|
| 文档状态 | `Doc Closed` | PRD、字段、routing truth、runbook、gate、final review、evidence 已落盘 |
| 工程状态 | `Admin Ops Strengthened, Release Evidence Pending` | route、API、controller、页面、双通道 outbox、P2 query/alert 收口与测试都已存在 |
| 已完成联调 | `Yes` | B/C/D 的 query 字段、status/code、runbook/gate 口径已对齐 |
| admin-only 已可用 | `Yes` | 仅后台值班/治理/观测面可用，不能外推成用户侧放量能力 |
| 当前是否可开发 | `Yes` | 可以继续补样本、补配置、补发布证据 |
| 当前是否可放量 | `No` | 仍不能写成 release-ready |
| Release Decision | `No-Go` | 只允许作为工程闭环输入 |
| 单一真值引用 | `docs/products/miniapp/2026-03-22-miniapp-booking-review-p2-final-integration-v1.md` | 03-22 P2 后最终判断优先只认 A 窗口集成文档 |

## 3. 已闭环的真值项
1. booking review 已形成 booking 子域内独立评价能力，不再依赖商品评论真值。
2. 用户侧真实 route 已存在：
   - `/pages/booking/review-add`
   - `/pages/booking/review-detail`
   - `/pages/booking/review-result`
   - `/pages/booking/review-list`
3. 用户侧真实 API 已存在：
   - `GET /booking/review/eligibility`
   - `POST /booking/review/create`
   - `GET /booking/review/page`
   - `GET /booking/review/get`
   - `GET /booking/review/summary`
4. 后台真实 controller 与 overlay 页面已存在：
   - 评价台账
   - 详情
   - 回复
   - 跟进状态更新
   - 店长待办认领 / 首次处理 / 闭环
   - 看板
   - 历史治理扫描页（scan-only）
   - notify outbox 台账
   - manager routing 只读核查页
5. 差评当前已具备 admin-only 店长待办层，联系人真值仍固定为门店 `contactName/contactMobile` 快照。
6. notify outbox 当前已补齐：
   - `NEGATIVE_REVIEW_CREATED` 的双通道出站：`IN_APP`、`WECOM`
   - 通道级 `receiverAccount / receiverUserId`
   - 标准化阻断诊断、最近动作说明 / 最近动作人 / 动作原因
   - `FAILED` 人工重试
   - `BLOCKED_NO_OWNER` 跳转 manager routing 只读核查
7. manager routing 当前已补齐：
   - `storeId -> managerAdminUserId`
   - `storeId -> managerWecomUserId`
   - `bindingStatus / effectiveTime / expireTime / source / lastVerifiedTime`
   - App / 企微各自的 routingLabel 与 repairHint
8. 店长待办 SLA 当前已补齐自动提醒出站：
   - `MANAGER_CLAIM_TIMEOUT`
   - `MANAGER_FIRST_ACTION_TIMEOUT`
   - `MANAGER_CLOSE_TIMEOUT`
   - 三类提醒继续走 App / 企微双通道 outbox，而不是旁路线
9. 相关测试已落地：
   - booking review controller / service tests
   - notify outbox / manager routing / dispatch / SLA reminder tests
   - notify outbox / manager routing / detail / ledger admin node tests
10. 03-22 P2 已固定后台页面入口真值：
   - `/mall/booking/review/dashboard` 是观察入口 + 治理跳转入口
   - `/mall/booking/review` 是唯一运营治理台账入口
11. 03-22 P2 已固定 admin query / detail / dashboard-summary 的稳定字段：
   - `managerSlaStage`
   - `managerTimeoutCategory`
   - `priorityLevel`
   - `priorityReasonCode`
   - `notifyAuditStage`
   - `priorityP0Count~P3Count`
   - `managerTimeoutDueSoonCount / managerTimeoutCount`
   - `notifyAuditBlockedCount / notifyAuditFailedCount / notifyAuditManualRetryPendingCount / notifyAuditDivergedCount`
12. 03-22 P2 已固定告警联动边界：
   - `CLAIM_DUE_SOON / FIRST_ACTION_DUE_SOON / CLOSE_DUE_SOON` 只是观察态
   - `CLAIM_TIMEOUT / FIRST_ACTION_TIMEOUT / CLOSE_TIMEOUT / ANY_BLOCKED` 才是 blocker 语义
   - PASS 只表示边界守住，不表示 release-ready

## 4. 当前仍未闭环的工程项
1. 发布级 runtime 样本包 `未核出`。
2. booking review 专属 release gate 脚本 `未核出`。
3. feature flag / rollout 控制面 `未核出`。
4. 企微真实送达样本、回执归档、灰度范围 `未核出`。
5. `serviceOrderId` 当前仍是后端按 `payOrderId -> TradeServiceOrderApi.listTraceByPayOrderId` best-effort 回填；trace 未命中或异常时允许 `null`。
6. `picUrls` 已完成提交链路，但历史 / 详情 / 运营回显证据仍未闭环。
7. 03-18 的 detail acceptance checklist 只补 query-side 页面验收，不构成独立 release evidence。
8. 历史差评若 `managerTodoStatus` 仍为空，当前不会在 list / dashboard / read-path 自动回填；只会在首次执行店长待办写动作时 lazy-init。
9. 双通道路由已具备工程模型，但数据覆盖、全量核验、运行样本与发布证据未闭环，不能外推成“1000 家门店店长通知已正式上线”。
10. 当前共享企微发送端只是工程接入，不代表所有门店和店长企微账号都已完成真实联调。
11. `priorityLevel / priorityReason / notifyRiskSummary` 当前只是返回字段或展示文案；并未形成独立 query capability。
12. `notifyAuditStage=PENDING_DISPATCH`、缺 notify 记录、全 0 dashboard、`ANY_FAILED / MANUAL_RETRY_PENDING / DIVERGED` 当前都还是观察态，不是发布级成功/失败样本。
13. `SENT / DUAL_SENT` 当前只能解释为出站记录存在，不能解释为店长已读、门店已处理或问题已闭环。
14. 03-22 runbook / acceptance SOP / reminder 测试 PASS 只证明 admin-only 值班边界守住，不构成 release evidence。
15. 当前仍没有服务端 `degraded=true / degradeReason` 真实证据，不能补写假降级样本。

## 5. 评价域能力拆分

### 5.1 Query-side
- `/pages/booking/review-list`
- `/pages/booking/review-detail`
- `GET /booking/review/page`
- `GET /booking/review/get`
- `GET /booking/review/summary`

当前判断：
- 页面与 API 已落地
- 合法空态已明确
- 可继续维护

### 5.2 Write-side
- 订单列表 / 订单详情中的 `去评价` 入口
- `/pages/booking/review-add`
- `GET /booking/review/eligibility`
- `POST /booking/review/create`
- 后台 `reply / follow-status`
- 差评 notify outbox 创建 / dispatch / retry / SLA reminder

当前判断：
- 功能已实现
- 但仍缺 runtime 样本与发布证据
- 只能写成 `Can Develop / Cannot Release`

## 6. 与产品设计的偏差表

| 设计期待 | 当前代码真值 | 当前口径 |
|---|---|---|
| 独立 review route 目录 | 实际使用扁平 route：`review-add / review-detail / review-result / review-list` | 只认当前代码 route |
| 支持图片评价 | 提交页已支持上传并提交 `picUrls`；历史 / 详情回显未单独闭环 | 不能写成整链路已 release-ready |
| 店长即时通知 | 已落地后台店长待办 + 双通道路由模型 + 双通道 notify outbox + admin 观测 + SLA reminder + 企微 sender 工程接入 | 当前仍只能写成 `Can Develop / Cannot Release` |
| 店长账号归属 | 当前已新增 `storeId -> managerAdminUserId + managerWecomUserId` | 不能写成全量账号路由与真实送达已正式上线 |
| 自动好评奖励 | 设计明确不做 | 当前仍不做 |
| 自动差评补偿 | 设计明确不做 | 当前仍不做 |
| 履约单绑定 | 设计建议保留 `serviceOrderId` | 当前已改成 best-effort 回填，但仍允许为空 |
| 区域负责人升级 | 当前不做 | 不得补写成已存在链路 |

## 7. 当前 No-Go 条件
1. 把 booking review 写成已放量新能力。
2. 把后台治理能力写成“店长已真实收到 App / 企微通知”。
3. 把双通道 outbox 或企微 sender 工程接入写成“发送样本已闭环”。
4. 把 node / maven 测试 PASS 写成真实线上可放量证据。
5. 把 `serviceOrderId`、`picUrls`、feature flag、runtime sample 缺口忽略掉。
6. 把 `managerClaimedByUserId` 或后台登录人误写成门店店长账号真值。
7. 把历史差评 read-path 未初始化的现状忽略掉，并直接把 dashboard / SLA 统计写成覆盖全量历史记录。
8. 把 scan-only 历史治理扫描页或 admin 快捷动作写成修复工具、批量治理工具或发布级证据。
9. 把 `priorityLevel`、`priorityReason`、`notifyRiskSummary` 写成独立筛选、稳定 code 或 release capability。
10. 把 `SENT / DUAL_SENT`、dashboard 聚合计数、job / outbox / routing 的存在写成真实外部通知闭环。
11. 把 due soon 观察态、`ANY_FAILED`、`MANUAL_RETRY_PENDING`、`DIVERGED` 写成自动告警联动已上线。
12. 补造 `degraded=true / degradeReason` 或 admin 专属稳定错误码样本。

## 8. 单一真值引用
- `docs/products/miniapp/2026-03-17-miniapp-booking-review-service-quality-prd-v1.md`
- `docs/products/miniapp/2026-03-17-miniapp-booking-review-page-field-dictionary-v1.md`
- `docs/contracts/2026-03-17-miniapp-booking-review-contract-v1.md`
- `docs/contracts/2026-03-17-miniapp-booking-review-errorcode-and-failure-mode-v1.md`
- `docs/plans/2026-03-17-miniapp-booking-review-service-recovery-runbook-v1.md`
- `docs/plans/2026-03-17-miniapp-booking-review-release-gate-v1.md`
- `docs/products/miniapp/2026-03-18-miniapp-booking-review-detail-acceptance-checklist-v1.md`
- `docs/products/miniapp/2026-03-19-miniapp-booking-review-history-and-boundary-audit-v1.md`
- `docs/products/miniapp/2026-03-19-miniapp-booking-review-manager-ownership-truth-review-v1.md`
- `docs/products/miniapp/2026-03-21-miniapp-booking-review-manager-account-routing-truth-v1.md`
- `docs/plans/2026-03-21-miniapp-booking-review-admin-evidence-ledger-v1.md`
- `docs/products/miniapp/2026-03-22-miniapp-booking-review-p2-final-integration-v1.md`
