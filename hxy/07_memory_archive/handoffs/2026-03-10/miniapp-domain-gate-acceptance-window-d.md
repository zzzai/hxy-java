# MiniApp Domain Gate Acceptance - Window D Handoff (2026-03-10)

## 1. 变更摘要
- 新增统一验收矩阵：
  - `docs/plans/2026-03-10-miniapp-domain-release-acceptance-matrix-v1.md`
  - 把 `content / brokerage / catalog / marketing-expansion / reserved-expansion` 的用例、证据字段、Go/No-Go、回滚动作、负责角色收口到一张表。
- 新增统一告警 / 值班 / 人工接管文档：
  - `docs/plans/2026-03-10-miniapp-domain-alert-owner-routing-v1.md`
  - 统一五键、SLA、升级链、人工接管入口。
- 更新六份既有 D 窗口文档，统一：
  - `degraded=true` 不进主成功率 / 主 ROI
  - `RESERVED_DISABLED` 命中即误发布
  - `search-lite` 与 `search-canonical` 分池治理
  - content 话术不得套交易客服模板
  - brokerage 资金异常必须闭合“审核 -> 冲正 -> 申诉 -> 追溯”

## 2. 当前真值结论
- `content.kefu-article-faq`、`brokerage.center` 仍是 `PLANNED_RESERVED / BACKLOG-DOC-GAP`。
  - 本次补齐的是 D 窗口门禁、值班、回滚和验收真值，不等于 A 窗口可以直接改成 `ACTIVE`。
- `catalog-browse`、`search-lite` 仍按 `ACTIVE` 参与发布门禁。
  - `detail-comment-collect-history`、`search-canonical` 继续留在保留池，不得混算。
- `promotion.activity-growth` 仍是 `PLANNED_RESERVED / BACKLOG-DOC-GAP`。
  - 当前只具备联调 / 预发布 / 回滚 playbook，不得混入 `promotion.coupon/point-mall` 主发布分母。
- `gift-card / referral / technician-feed` 当前仍默认 `NO_GO`。
  - capability ledger 已明确页面与 controller 未实现、开关默认 `off`；灰度 runbook 现在是“可执行模板”，不是“当前可放量事实”。

## 3. 对窗口 A / B / C 的联调提醒
- A（集成 / 发布）
  - 只能依据 capability ledger + 本批 acceptance matrix 决定状态，不得因为 runbook 完整就把 `content / brokerage / marketing-expansion / reserved-expansion` 误改成 `ACTIVE`。
  - `search-lite` 与 `search-canonical` 需要两套分母、两套 TopN、两套门禁；A 侧发布看板不能再合并。
  - `promotion.activity-growth` 仍是 `CAP-PROMO-003 = PLANNED_RESERVED / BACKLOG-DOC-GAP`；除非 B/C 文档冻结，否则只能 `No-Go`。
- B（产品 / 运营 / 客服）
  - 内容域只允许用“帮助/咨询/内容同步中”口径，不允许套用交易域“退款中/到账中/售后处理中”话术。
  - 营销扩展活动一旦执行 `MKT-07`，客服公告和活动页说明要同步更新，避免用户继续尝试参与。
  - brokerage 资金异常不允许只回一层客服口径，必须能回到审核单、冲正单、申诉单和追溯工单。
- C（契约 / 后端）
  - 重点继续对齐错误码：`1008009902`、`1008009904`、`1030007011`、`1011009901`、`1011009902`、`1013009901`、`1013009902`、`1030009901`。
  - 保留能力的关闭态错误码返回仍一律视为误发布，不允许降级解释为“warning”。
  - 任何 fail-open / warning 样本如果输出 `degraded=true`，都必须继续走 `degraded_pool`，不得反算主成功率 / 主 ROI。

## 4. 固定验证命令
1. `git diff --check`
2. `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh`
3. `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`
