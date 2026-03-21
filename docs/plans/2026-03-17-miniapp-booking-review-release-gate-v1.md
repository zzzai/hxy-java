# MiniApp Booking Review Release Gate v1（2026-03-17）

## 1. 目标
- 对 booking review 当前工程能力给出可执行的 Go / No-Go 判断。
- 重点区分：
  - 页面 / API / controller / 双通道通知工程已落地
  - runtime 样本、灰度证据、发布门禁仍未闭环

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
- 2026-03-21 当前分支新增工程证据：
  - `storeId -> managerAdminUserId + managerWecomUserId` 双通道路由
  - `IN_APP / WECOM` 双通道 notify outbox
  - 企微机器人 sender 工程接入
  - `MANAGER_CLAIM_TIMEOUT / MANAGER_FIRST_ACTION_TIMEOUT / MANAGER_CLOSE_TIMEOUT` SLA reminder job
- evidence ledger：
  - `docs/plans/2026-03-21-miniapp-booking-review-admin-evidence-ledger-v1.md`
- 当前未核出：
  - 独立 booking review runtime gate 脚本
  - 发布级样本包
  - feature flag / switch
  - 企微真实送达与灰度回执归档
  - 稳定全量 `store -> manager account` 发布样本

## 3. Gate 判定表

| 判定项 | 当前状态 | 结论 |
|---|---|---|
| 用户侧真实 route | Yes | `/pages/booking/review-add`、`/pages/booking/review-result`、`/pages/booking/review-list` 已存在 |
| 用户侧真实 API | Yes | `/booking/review/*` 5 条 app API 已存在 |
| 后端 app controller | Yes | `AppBookingReviewController` 已存在 |
| 后台 admin controller | Yes | `BookingReviewController` 已存在 |
| 后台 overlay 页面 | Yes | 台账、详情、看板、notify outbox、manager routing 页面已存在 |
| 双通道路由模型 | Yes | `managerAdminUserId + managerWecomUserId` 已进入工程真值 |
| 双通道 outbox | Yes | 同一条差评会拆成 `IN_APP / WECOM` 两条记录 |
| 企微 sender 工程接入 | Yes | 已有 webhook sender 与通道开关配置读取 |
| SLA reminder job | Yes | 已按认领超时 / 首次处理超时 / 闭环超时生成双通道提醒 outbox |
| Doc set | Yes | PRD / routing truth / runbook / gate / final review / evidence 已齐 |
| 发布级 runtime 样本 | No | 未核到真实线上或准线上样本包 |
| feature flag / rollout control | No | 未核到独立开关 |
| 独立 release gate | No | 当前没有 booking review 专属 runtime gate |
| 企微真实送达回执 | No | 当前只有工程接入，没有发布级送达证据 |

## 4. 当前结论

| 维度 | 当前结论 |
|---|---|
| 文档状态 | `Doc Closed` |
| 工程状态 | `Feature Implemented, Release Evidence Pending` |
| Can Develop | `Yes` |
| Can Release | `No` |
| Release Decision | `No-Go` |

补充说明：
1. 双通道通知已进入工程真值，但这只说明“通道模型与异步派发能力存在”，不说明“每家门店店长都已收到真实消息”。
2. 03-21 的 manager routing、notify outbox、企微 sender、SLA reminder 仍属于 admin-only 治理与工程闭环增强，不构成 release-ready 样本。
3. 历史差评仍不会在 read-path 自动补齐店长待办字段，因此 dashboard / SLA 统计不覆盖所有历史数据。

## 5. blocker_pool
1. 未核到 booking review 发布级成功 / 失败样本包。
2. 未核到企微真实送达样本、回执归档和灰度范围证据。
3. 未核到 feature flag 或独立灰度控制面。
4. 未核到独立 booking review runtime gate。
5. `serviceOrderId` 当前仍是 best-effort 回填，trace 未命中或异常时允许为空。
6. `picUrls` 已完成前端提交链路，但历史 / 详情 / 运营回显证据仍未闭环。
7. 双通道路由虽已建模，但全量数据覆盖、修复流程、发布验收证据仍未闭环。
8. 历史差评待办字段当前只在写动作时 lazy-init，read-path 不自动回填。
9. 当前共享企微发送端只证明“工程可发送”，不证明“当前配置已可发布”。

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
- 由于未核到独立 feature flag，当前只能按代码回滚或配置关闭企微发送端理解。
- 不得把“人工通知补位”写成系统化回滚措施。

### 7.2 未来 release 重入条件
1. 具备真实 runtime 样本包。
2. 具备明确 rollout / rollback 控制面。
3. 双通道通知至少有真实送达样本、失败样本与审计归档。
4. `serviceOrderId` best-effort 回填与 `picUrls` 提交后回显等关键缺口达成明确 release 结论。
5. 新一轮 A 集成文档重新评估后，才能从 `No-Go` 升级。

## 8. Gate No-Go 条件
1. 把页面和 controller 已存在写成“可以放心放量”。
2. 把双通道 outbox、企微 sender 或 SLA reminder job 写成“店长通知已正式上线”。
3. 把 node / maven 测试 PASS 写成 release-ready。
4. 把后台台账、看板存在写成“恢复体系已正式上线”。
5. 把 `picUrls`、`serviceOrderId`、feature flag、runtime 样本未闭环的能力写成“已补齐”。
6. 把 03-21 双通道路由真值外推成“1000 家门店都已完成店长绑定”。
