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
