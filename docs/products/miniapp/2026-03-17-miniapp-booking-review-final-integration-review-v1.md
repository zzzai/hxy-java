# MiniApp Booking Review Final Integration Review v1（2026-03-17）

## 1. 目标与吸收边界
- 目标：把 booking review 当前代码、测试与新文档集成成单一真值。
- 本文只吸收当前分支已正式提交代码证据：
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
- 本文不吸收：
  - 未提交的人工口头结论
  - 未落盘的运行样本
  - 设计上想要但当前代码未实现的奖励、补偿、自动通知能力

## 2. 最终结论

| 判断项 | 当前结论 | 说明 |
|---|---|---|
| 文档状态 | `Doc Closed` | PRD、字段、contract、failureMode、runbook、gate、final review、detail acceptance 已落盘 |
| 工程状态 | `Feature Implemented, Release Evidence Pending` | route、API、controller、页面、测试都已存在，但发布证据仍缺 |
| 当前是否可开发 | `Yes` | 可以继续完善细节、补齐样本、补齐通知与灰度控制 |
| 当前是否可放量 | `No` | 仍不能写成 release-ready |
| Release Decision | `No-Go` | 当前只允许作为工程闭环输入 |
| 单一真值引用 | `docs/products/miniapp/2026-03-17-miniapp-booking-review-final-integration-review-v1.md` | 03-17 起 booking review 最终判断统一只认本文 |

## 3. 已闭环的真值项
1. booking review 已经形成独立 booking 子域，不再依赖商品评论真值。
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
5. 差评当前已具备 admin-only 店长待办层，联系人真值固定为门店 `contactName/contactMobile` 快照。
6. 相关测试已落地：
   - booking review API alignment
   - booking review page smoke
   - booking review detail page smoke
   - 订单页评价入口 smoke
   - booking review controller / service tests
   - 03-19 post-launch residual tests（店长待办状态机、历史差评 lazy-init 边界）

## 4. 当前仍未闭环的工程项
1. 发布级 runtime 样本包 `未核出`。
2. booking review 专属 release gate `未核出`。
3. feature flag / rollout 控制面 `未核出`。
4. 当前分支已落地 booking review 专属 `manager account routing + notify outbox + admin 观测 + IN_APP` 占位派发 job；但仍缺真实模板配置、运行样本与发布门禁，只能写成 `Can Develop / Cannot Release`。
5. `serviceOrderId` 当前改为后端按 `payOrderId -> TradeServiceOrderApi.listTraceByPayOrderId` best-effort 回填；trace 未命中或异常时仍允许写 `null`。
6. `picUrls` 已在用户端提交页接入上传并随创建请求发送，但历史 / 详情 / 运营回显证据仍未闭环。
7. 03-18 的 detail acceptance checklist 只补齐 query-side 页面验收，不构成独立 release evidence。
8. 历史差评若 `managerTodoStatus` 仍为空，当前不会在 list / dashboard / page read-path 自动回填；只会在首次执行店长待办写动作时 lazy-init。
9. 当前分支已新增 `booking_review_manager_account_routing` 作为工程 owner 映射模型；但联系人快照仍独立存在，且 routing 数据覆盖、样本与发布证据未闭环，不能外推成“门店店长账号通知已上线”。
10. 03-21 新增的历史治理扫描页只做人工触发识别，不做 repair write；不能把“能扫出候选”写成“历史数据已修复”。

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

当前判断：
- 功能已实现
- 但仍缺 release proof
- 只能写成 `Can Develop / Cannot Release`

## 6. 与产品设计的偏差表

| 设计期待 | 当前代码真值 | 当前口径 |
|---|---|---|
| 独立 review route 目录 | 实际使用扁平 route：`review-add / review-detail / review-result / review-list` | 只认当前代码 route |
| 支持图片评价 | 提交页已支持上传并提交 `picUrls`；历史 / 详情回显未单独闭环 | 不能写成整链路已 release-ready |
| 店长即时通知 | 已落地后台店长待办 + routing model + notify outbox + admin 观测 + `IN_APP` 占位派发 | 当前仍只能写成 `Can Develop / Cannot Release`，不能写成正式上线 |
| 店长账号归属 | 当前只核到 `contactName/contactMobile` 快照 | 不能写成 `managerUserId` 已闭环 |
| 自动好评奖励 | 设计明确不做 | 当前仍不做 |
| 自动差评补偿 | 设计明确不做 | 当前仍不做 |
| 履约单绑定 | 设计建议保留 `serviceOrderId` | 当前已改成 best-effort 回填，但仍允许为空 |
| 店长待办状态流转 | 当前后端已守住 `待认领 -> 已认领/处理中 -> 已闭环` | 不能再把前端按钮禁用当成唯一约束 |

## 7. 当前 No-Go 条件
1. 把 booking review 写成已放量新能力。
2. 把 booking review 写成商品评论能力的 UI 换皮。
3. 把后台恢复台账写成“已自动通知店长并自动升级”。
4. 把 node test PASS 写成真实线上可放量证据。
5. 把 `serviceOrderId` 的 best-effort 语义、`picUrls` 仅完成提交链路、feature flag、runtime sample 缺口忽略掉。
6. 把 `managerClaimedByUserId` 或后台登录人误写成门店店长账号真值。
7. 把历史差评在 read-path 未初始化的现状忽略掉，并直接把 dashboard / SLA 统计写成覆盖全量历史记录。
8. 把 scan-only 的历史治理扫描页写成修复工具、批量治理工具或发布级证据。

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
