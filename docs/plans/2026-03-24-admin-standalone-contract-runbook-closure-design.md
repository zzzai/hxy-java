# Admin Standalone Contract/Runbook Closure Design

## 1. Goal
- 把当前后台能力从“PRD 已完整，但独立 contract/runbook 多数未核出”推进到“按业务域具备独立 contract + 独立 runbook + 总台账已回灌”的状态。
- 本轮不改业务代码、不改前端页面、不补 release-ready 结论；只做后台文档真值体系收口。

## 2. Current Truth
- 已完成 PRD 收口的后台能力包括：
  - Finance Ops：`BO-001`、`BO-002`、`BO-003`、`BO-004`
  - Product：`ADM-001`、`ADM-002`
  - Store：`ADM-003` ~ `ADM-006`
  - Store Product：`ADM-007`、`ADM-008`
  - Supply Chain：`ADM-009`、`ADM-010`
  - Store Governance：`ADM-011` ~ `ADM-013`
  - Trade Ops：`ADM-014` ~ `ADM-016`
- 当前明确短板：除 `BO-004` 外，多数后台能力仍缺独立 contract/runbook，因此在总台账里只能写“未核出独立 contract/runbook”。
- 当前风险不是“没有 PRD”，而是“文档体系还不能支持更严肃的跨团队联调、冻结归档、灰度/回滚审计和 handoff”。

## 3. Approaches

### 方案 A：按业务域成组补齐独立 contract/runbook
- 做法：以现有正式 PRD 分组为边界，为每个后台业务域建立一对独立文档。
- 预计分组：
  - Finance Ops Core：`BO-001` ~ `BO-003`
  - Product SPU/Template：`ADM-001` ~ `ADM-002`
  - Store Master：`ADM-003` ~ `ADM-006`
  - Store Product / SKU Ops：`ADM-007` ~ `ADM-008`
  - Supply Chain Stock Approval：`ADM-009` ~ `ADM-010`
  - Store Lifecycle Governance：`ADM-011` ~ `ADM-013`
  - Trade Ops After-sale / Review Ticket：`ADM-014` ~ `ADM-016`
- 优点：边界与现有 PRD 一致，文档数量可控，后续维护成本低。
- 缺点：单份文档内会包含 2~4 个功能，需要更严格写清页面/API 边界。

### 方案 B：只补一份总后台 contract 和一份总后台 runbook
- 优点：最快。
- 缺点：体积过大，边界不清，后续几乎必然再次拆分。

### 方案 C：每个功能单独补一对 contract/runbook
- 优点：边界最细。
- 缺点：文档数量爆炸，维护成本最高，且当前后台很多能力天然按域共用一套运行规则。

## 4. Recommendation
- 采用方案 A。
- 理由：它和现有 PRD 分组、后台真实页面/API 组织方式、当前总台账的能力拆分最一致，是“补齐独立文档”与“控制维护成本”之间的最优平衡。

## 5. Scope

### 5.1 新增文档
- Contract：7 份
- Runbook：7 份

### 5.2 更新文档
- `docs/products/2026-03-15-hxy-full-project-business-function-ledger-v1.md`
- `docs/products/2026-03-16-hxy-full-project-function-prd-completion-review-v1.md`
- `docs/products/2026-03-16-hxy-full-project-function-doc-completion-publishable-list-v1.md`
- `hxy/07_memory_archive/handoffs/2026-03-24/admin-standalone-contract-runbook-closure-window-a.md`

## 6. Contract Writing Rules
- 只认真实后台页面文件、真实 API 文件、真实 controller 路径。
- 不拿 design、implementation、historical checklist、handoff 充当 contract。
- 每份 contract 必须明确：页面入口、真实请求路径、关键 query/body/resp 字段、空态语义、fail-close / fail-open 边界、禁止误写内容。
- `BO-003` 与 `BO-004` 必须继续分离；不得相互借文档。

## 7. Runbook Writing Rules
- 每份 runbook 必须覆盖：操作入口、关键操作顺序、审计键、成功/失败判断、人工接管、回滚边界、升级路径。
- 不能把域级发布门禁矩阵直接顶替功能 runbook。
- 不能把“页面存在”写成“release-ready”；后台大多数能力在本轮只升级为“文档体系完整”，不改变发布结论。

## 8. Verification Strategy
- 文档层验证：
  - 总台账里原“未核出独立 contract/runbook”条目被对应新文档替换。
  - 每个分组 PRD 都能映射到唯一独立 contract + runbook。
- 仓库门禁：
  - `git diff --check`
  - `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh`
  - `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`

## 9. Final Outcome
- 本轮完成后，后台将从“PRD 完整，但 contract/runbook 多数未核出”升级为“后台主能力 PRD + contract + runbook 成体系”。
- 这不自动改变任何后台能力的 `Can Release` 结论，只解决文档体系完整度问题。
