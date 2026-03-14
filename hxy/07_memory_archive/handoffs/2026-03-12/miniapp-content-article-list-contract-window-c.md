# Window C Handoff - MiniApp Content Article List / Category / Writeback Contract（2026-03-12）

## 1. 本批交付
- 分支：`feat/ui-four-account-reconcile-ops`
- 交付类型：仅补 contract / errorCode register / handoff；未改业务代码、未改 overlay 页面、未动 `.codex`、未改历史 handoff、未处理无关 untracked。
- 新增文件：
  1. `docs/contracts/2026-03-12-miniapp-content-article-list-category-writeback-contract-v1.md`
  2. `hxy/07_memory_archive/handoffs/2026-03-12/miniapp-content-article-list-contract-window-c.md`
- 更新文件：
  1. `docs/contracts/2026-03-10-miniapp-content-customer-service-contract-v1.md`
  2. `docs/contracts/2026-03-09-miniapp-errorcode-canonical-register-v1.md`

## 2. 核心收口结论

### 2.1 BF-027 已拆成独立 contract 真值
- 新 contract 只覆盖以下 5 个真实接口：
  - `GET /promotion/article/list`
  - `GET /promotion/article/page`
  - `GET /promotion/article-category/list`
  - `PUT /promotion/article/add-browse-count`
  - `PUT /promotion/kefu-message/update-read-status`
- `GET /promotion/article/get` 明确保留在 BF-026：
  - `docs/contracts/2026-03-10-miniapp-content-customer-service-contract-v1.md`
- 旧 mixed contract 已改成拆分衔接口径，不再把 BF-027 五个接口和 BF-026 混写。

### 2.2 当前状态固定
- BF-027 能力包整体：`PLANNED_RESERVED`
- 5 个接口逐条状态：`ACTIVE_BE_ONLY`
- 依据：
  - `yudao-mall-uniapp/sheep/api/promotion/article.js` 当前只有 `GET /promotion/article/get`
  - `yudao-mall-uniapp/sheep/api/promotion/kefu.js` 当前只有 `send/list`
  - `pages.json` 当前没有文章列表 / 分类 / 文章中心真实用户页
- 结论：
  - 后端真值存在
  - 当前没有真实 FE API / 页面绑定
  - 不得提前写成 `ACTIVE`

### 2.3 空态 / 错误码 / 降级行为固定
- `article/list`
  - 合法空态：`[]`
  - `null` 不是合法成功态
  - `FAIL_OPEN`
  - `REFRESH_ONCE`
  - 无 `degraded` 字段
- `article/page`
  - 合法空态：`total=0,list=[]`
  - `null` 不是合法成功态
  - `FAIL_OPEN`
  - `REFRESH_ONCE`
  - 无 `degraded` 字段
- `article-category/list`
  - 合法空态：`[]`
  - `null` 不是合法成功态
  - `FAIL_OPEN`
  - `REFRESH_ONCE`
  - 无 `degraded` 字段
- `article/add-browse-count`
  - 成功体只允许 `true`
  - 允许 fail-open，因为统计写回不阻断正文/列表主流程
  - 不允许自动重试，`retryClass=NO_AUTO_RETRY`
  - 错误码锚点：`ARTICLE_NOT_EXISTS(1013016000)`
  - 无 `degraded` 字段
- `kefu-message/update-read-status`
  - 成功体只允许 `true`
  - 不允许 fail-open，会话不存在/归属不符必须阻断
  - 不允许自动重试，`retryClass=REFRESH_ONCE` 仅代表用户刷新后重进
  - 错误码锚点：`KEFU_CONVERSATION_NOT_EXISTS(1013019000)`
  - 无 `degraded` 字段

## 3. 给窗口 A / B / D 的联调注意点

### 3.1 给窗口 A
- 不要把 BF-027 的 5 个接口纳入 `ACTIVE` allowlist。
- 如果后续补文章中心或客服已读同步，只能按以下真实字段接入：
  - `GET /promotion/article/list`：`recommendHot?`、`recommendBanner?`
  - `GET /promotion/article/page`：`categoryId?`、`pageNo`、`pageSize`
  - `GET /promotion/article-category/list`：无 query
  - `PUT /promotion/article/add-browse-count`：`id`
  - `PUT /promotion/kefu-message/update-read-status`：`conversationId`
- 空态处理必须按结构化返回：
  - `[]`
  - `total=0,list=[]`
  - 不能把 `null` 当成功空态

### 3.2 给窗口 B
- BF-026 / BF-027 边界已经固定：
  - BF-026 = 聊天 / 文章详情 / FAQ 壳页 / WebView
  - BF-027 = 列表 / 分类 / 浏览回写 / 已读回写
- 文档口径里不得把“后端已存在”直接改写成“用户能力已上线”。
- `ARTICLE_NOT_EXISTS(1013016000)` 当前只锚定 browse-count 回写，不要扩写成列表/分页接口的通用错误码。

### 3.3 给窗口 D
- 当前这 5 个接口都没有服务端 `degraded` 字段，runbook / 验收不要自造 `degraded_pool`。
- 降级与阻断要分开验：
  - browse-count：`FAIL_OPEN`
  - read-status：`FAIL_CLOSE`
- 错误码判断只按 code：
  - `1013016000`
  - `1013019000`
  - 不能按 message 文案分支

## 4. 风险与建议
- 风险 1：现有产品 PRD 仍是 content/customer-service 合包描述，A/B 若忽略本次 contract 拆分，容易再次把 BF-027 接口混回 BF-026。
- 风险 2：richtext 当前没有 browse-count 回写，后续若接入时把失败当主链失败，会错误放大统计接口的重要性。
- 风险 3：已读回写若被误当成 fail-open，会把“越权/会话不存在”误吞成静默成功。
- 建议：
  1. A 侧后续接入这 5 个接口前，直接引用本 handoff 和新 contract，不再抄旧 mixed contract。
  2. B 侧若继续拆 PRD，可在下一批把 BF-027 产品边界独立化，但本批不改产品文档。
  3. D 侧把“无 degraded 字段、browse-count 不自动重试、read-status 不允许 fail-open”列为验收必查项。
