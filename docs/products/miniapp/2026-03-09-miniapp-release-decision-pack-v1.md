# MiniApp Release Decision Pack v1 (2026-03-09)

## 1. 目标与决策边界
- 目标：形成发布决策单一真值包，统一 P 级、RB 批次、错误码策略、门禁流程。
- 边界：仅覆盖 miniapp 发布相关产品/契约/运营/数据文档，不涉及 overlay 页面与业务代码变更。
- 依赖基线：
  - `docs/products/miniapp/2026-03-09-miniapp-feature-inventory-and-release-matrix-v1.md`
  - `docs/contracts/2026-03-09-miniapp-errorcode-canonical-register-v1.md`
  - `docs/products/miniapp/2026-03-09-miniapp-ready-to-frozen-review-v1.md`

## 2. 最终发布范围（P0/P1/P2）与 RB 批次

### 2.1 RB1-P0（上线必备）
- 支付结果、订单列表/详情、售后申请/列表/详情、退款进度、预约列表、地址管理、领券中心、积分商城。

### 2.2 RB2-P1（效率与增长收口）
- 首页运营位真值绑定、项目页同源刷新、加购冲突处理、预约排期、会员资产账本、搜索发现。

### 2.3 RB3-P2（规模化增强）
- 礼品卡域、邀请有礼、技师动态广场（均受 `RESERVED_DISABLED` 开关与灰度门禁控制）。

## 3. Go/No-Go 决策表

| 决策项 | 触发条件 | 决策结果 | 负责人 | 处置时限 |
|---|---|---|---|---|
| 错误码稳定性 | 发布矩阵仍存在 `TBD_*` 或同码多义 | No-Go | A + C | 4小时内修订并复审 |
| API 确定性 | 仍存在通配 API（如 `/*`、泛化命名） | No-Go | A + B + C | 4小时内回填具体接口 |
| 优先级一致性 | 同一能力在矩阵/PRD/contract 出现 P级冲突 | No-Go | A + B | 当日修订 |
| RESERVED_DISABLED 门禁 | 禁用态错误码在生产误返回 | No-Go（灰度回滚） | C + D | 15分钟回滚开关 |
| P2 前置越级上线 | RB3-P2 能力未经开关审批直接进入 RB1/RB2 发布范围 | No-Go | A + C | 发布前阻断并回退配置 |
| 降级池隔离 | `degraded=true` 流量进入主成功率/主ROI | No-Go | D | 30分钟修复口径 |
| 可追踪性 | 关键检索键缺失（runId/orderId/payRefundId/sourceBizNo/errorCode） | No-Go | A + D | 1小时补齐并复检 |
| 联调基线 | 文档状态未 Frozen 或未更新索引 | No-Go | A | 当日收口 |
| 全量门禁通过 | 上述条件全满足 | Go | A（最终签发） | 发布窗口内 |

## 4. 关键风险台账

| 风险ID | 风险描述 | 风险等级 | 监控指标 | 回滚策略 |
|---|---|---|---|---|
| R-01 | 预留错误码误返回导致端侧误判 | High | `reserved_disabled_hit_count` | 立即关闭对应开关，回退到稳定码路径 |
| R-02 | API 名称不一致导致联调脚本误路由 | High | `api_contract_mismatch_count` | 以 matrix 为准回填，重跑契约校验 |
| R-03 | 优先级漂移造成排期与资源错配 | Medium | `priority_conflict_count` | 以 release-decision-pack 为唯一优先级来源 |
| R-03A | P2 规划能力被误当作已发布能力 | High | `rb_scope_violation_count` | 立即回滚对应开关并下线入口 |
| R-04 | 降级流量污染主经营口径 | High | `degraded_pool_leak_rate` | 立即修复分池规则并重算当日指标 |
| R-05 | 冲突码被自动重试导致重复副作用 | High | `conflict_code_retry_count` | 禁用自动重试，改人工接管流程 |
| R-06 | 关键日志键缺失导致不可追踪 | High | `trace_key_missing_rate` | 阻断发布并补齐日志契约后重放验证 |

## 5. 跨窗口责任矩阵（A/B/C/D）

| 事项 | A（集成） | B（产品） | C（契约） | D（数据/运营） |
|---|---|---|---|---|
| 发布范围与优先级单一真值 | A/R | C | C | I |
| 页面-接口-字段映射准确性 | A | R | C | I |
| 错误码注册与语义稳定 | C | I | A/R | C |
| fail-open/fail-close 边界 | A | I | R | C |
| RESERVED_DISABLED 开关与回滚 | A | I | R | R |
| degraded_pool 口径与看板隔离 | C | I | C | A/R |
| 发布门禁执行与签发 | A/R | C | C | C |

> 说明：R=Responsible，A=Accountable，C=Consulted，I=Informed

## 6. 决策结论
- 当前结论：**Go with Gate**。
- 放行前置：
  1. 所有发布相关文档状态保持 Frozen。
  2. 错误码、API、优先级三项冲突计数均为 0。
  3. RB3-P2 能力不得进入 RB1/RB2 发布范围。
  4. `RESERVED_DISABLED` 与 `degraded_pool` 监控项在灰度窗口内无异常。
- 任一前置不满足则自动降为 No-Go，并执行相应回滚策略。

## 7. 03-10 终审集成增量

### 7.1 本批集成原则
1. 只吸收当前分支真实存在且已正式提交的 03-10 文档。
2. 03-09 `Frozen` 基线绝不回退。
3. 03-10 只有在“真实 route/API/contract/runbook 全闭环”时，才允许进入 `Frozen Candidate`。
4. 03-11 新增 blocker checklist 只作为 gate evidence，不改变当前域状态结论。

### 7.2 03-10 目标域决策状态

| 域 | 当前状态 | 发布决策含义 |
|---|---|---|
| Member | Ready | 文档输入可作为后续冻结评审依据，但缺页能力与 `PLANNED_RESERVED` API 仍阻断 Frozen Candidate |
| Booking | `Doc Closed / Can Develop / Cannot Release` | query-only 范围可继续维护，但 create / cancel / addon 仍不得进入任何发布放量或冻结候选 |
| Content / Customer Service | Ready | 文档包已齐，但能力边界仍受 `PLANNED_RESERVED / ACTIVE_BE_ONLY` 约束，不能把聊天/文章/FAQ 壳页整体并入已发布范围 |
| Brokerage | Ready | 文档包已齐，但“提现申请成功 != 到账成功”，申诉/撤回/取消提现仍是缺页能力，不进入放量范围 |
| Product / Search / Catalog | Ready | 文档包已齐，但必须继续分离 `search-lite` 与 `search-canonical`，并把评论/收藏/历史维持在 `PLANNED_RESERVED` |
| Marketing Expansion | Ready | 文档包已齐，但秒杀/拼团/满减送整域仍按 `PLANNED_RESERVED` 管理，砍价只属后端存在 |
| Reserved Activation | Ready | 激活治理文档可作为执行输入，但 gift/referral/feed runtime 仍为 `PLANNED_RESERVED`，关闭态命中一律 No-Go |

### 7.3 当前门禁结论
1. 03-10 本批新增 `Frozen Candidate = 0`。
2. 截至 2026-03-11，03-10/03-11 `Ready` 文档共 `36` 份，可作为下一轮冻结评审输入，但不能被当作放量或签发依据。
3. 若任何窗口把以下对象回退为旧真值，或误标为 `ACTIVE`、`Frozen Candidate`、准发布范围，直接触发 No-Go：
   - `/pages/user/level`
   - `/pages/profile/assets`
   - `/pages/user/tag`
   - `GET /booking/technician/list-by-store`
   - `GET /booking/time-slot/list`
   - `PUT /booking/order/cancel`
   - `POST /booking/addon/create`
4. 以下 runtime 边界继续作为 03-10 No-Go 触发器：
   - 把 `/pages/public/faq` 当成独立 FAQ 数据页，而不是 FAQ 壳跳转到 `/pages/public/richtext?title=常见问题`
   - 把 `/product/favorite/exits` 改写成 `/exists`
   - 把 `search-lite` 与 `search-canonical` 混算，或把 `1008009904` 用到 lite 路径
   - 把 `type=2 bargain` 营销聚合当成真实前端可跳转能力
   - 在 `miniapp.gift-card / miniapp.referral / miniapp.technician-feed.audit = off` 时仍命中 `RESERVED_DISABLED`
5. 当前总体决策保持：
   - 03-09 Frozen 基线仍可按既有 `Go with Gate` 规则继续执行；
   - 03-10 新增域全部停留在文档 `Ready` 层，不构成新增签发范围；
   - 03-11 booking/member/reserved blocker checklist 只定义退出条件，不构成新增签发范围。

## 8. 03-14 Runtime Blocker Final Integration

### 8.1 当前项目级决策
- 对 03-09 Frozen 基线：继续保持 `Go with Gate`。
- 对剩余 blocker scope：统一按 `No-Go for Release` 管理。
- 对真值修复开发：统一按 `Go for Engineering Closure` 管理。

### 8.2 剩余 blocker scope 决策表

| scope | 文档状态 | 工程状态 | 开发决策 | 放量决策 | No-Go 条件 |
|---|---|---|---|---|---|
| Booking | 文档已闭环 | canonical 代码已收口，shared chain 已接入 booking runtime gate，但 gate summary 仍固定 `can_release=NO`；create / cancel / addon 仍缺发布级运行证据 | Go for Engineering Closure | No-Go | 把 smoke/runtime gate `PASS` 误写成 release-ready；混淆 query-only 与 write-chain；回退到旧 path/method；或移除 shared chain 的 booking runtime gate |
| Member 缺页能力 | 文档已闭环 | `/pages/user/level`、`/pages/profile/assets`、`/pages/user/tag` 未实现 | Go for Engineering Closure | No-Go | 把上述缺页能力写成 `ACTIVE`、可发布页面或新增签发范围 |
| Reserved runtime | 文档已闭环 | gift / referral / technician-feed 仍无 runtime 落地 | Go for Engineering Closure | No-Go | 因治理文档完整就把 gift/referral/feed 当成已上线能力，或 `RESERVED_DISABLED` 关闭态仍命中 |
| `BO-004` Finance Ops Admin | 文档已闭环 | 仅接口闭环 + 页面真值待核 | Go for Engineering Closure | No-Go | 未核到独立后台页面文件 / 独立前端 API 文件；写接口只验 `true` 不验写后回读；把 `commission-settlement/*.vue` 反推成 BO-004 页面 |

### 8.3 当前最终门禁结论
1. 当前项目不再存在“缺文档导致的 No-Go”。
2. 当前项目仍存在“工程真值阻断导致的 No-Go”。
3. 若进入下一阶段开发，只允许围绕 blocker scope 做真值修复与实现闭环，不得把 blocker scope 当成现成放量能力。

## 9. 03-15 Booking Runtime Final Integration Review

### 9.1 吸收边界
1. 只吸收当前分支真实存在、已正式提交的 booking runtime 代码、测试、gate、shared-chain 接入证据。
2. 2026-03-15 当前分支已正式具备 B/C/D booking runtime 窗口产出：
   - B：`docs/products/miniapp/2026-03-15-miniapp-booking-runtime-acceptance-and-recovery-prd-v1.md`
   - C：`docs/contracts/2026-03-15-miniapp-booking-runtime-release-evidence-contract-v1.md`
   - D：`docs/plans/2026-03-15-miniapp-booking-runtime-release-runbook-v1.md`
3. 当前 booking release 状态的单一真值从本节开始，只认：
   - `docs/products/miniapp/2026-03-15-miniapp-booking-runtime-release-evidence-review-v1.md`

### 9.2 当前 booking 结论
1. booking 当前最终状态固定为：`Doc Closed / Can Develop / Cannot Release`。
2. query-only `ACTIVE` 范围只认：
   - `/pages/booking/technician-list`
   - `/pages/booking/technician-detail`
   - `/pages/booking/order-list`
   - `/pages/booking/order-detail`
3. write-chain blocker 只认：
   - `POST /booking/order/create`
   - `POST /booking/order/cancel`
   - `POST /app-api/booking/addon/create`
4. 这三条写链路当前可以继续开发，但不得写成 `Ready`、`Frozen Candidate`、`Go` 或可放量能力。

### 9.3 Shared gate 证据
1. `check_booking_miniapp_runtime_gate.sh` 当前成功输出仍固定：
   - `doc_closed=YES`
   - `can_develop=YES`
   - `can_release=NO`
   - `result=PASS`
2. `run_ops_stageb_p1_local_ci.sh` 已把 booking runtime gate 接进 shared chain，且轻量运行时 `booking_miniapp_runtime_gate_rc=0`。
3. 但 shared chain booking gate 的成功只代表“边界被守住”，不代表“写链路已 release-ready”。

### 9.4 当前 Booking No-Go 条件
1. 用 smoke test 或 runtime gate `PASS` 去冲抵 create / cancel / addon 的真实发布证据缺口。
2. 把 query-only `ACTIVE` 范围外推成 booking 整域可放量。
3. 在 capability ledger、business ledger、release pack、联调口径或巡检口径中重新引入旧 path/method。
4. 吸收未正式提交的后续窗口增量作为 booking 发布依据，而不是继续只认当前分支已正式提交产出。
